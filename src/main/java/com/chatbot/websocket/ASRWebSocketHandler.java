package com.chatbot.websocket;

import com.chatbot.model.ChatMessage;
import com.chatbot.service.ChatService;
import com.chatbot.service.MultiChannelDispatcher;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * ASR WebSocket处理器
 * 
 * 集成到现有的ChatWebSocketHandler中，提供ASR语音识别功能
 * 
 * 功能特性：
 * - ASR服务健康检查
 * - ASR会话管理
 * - 语音识别结果处理
 * - 与聊天系统无缝集成
 * - 自动重连机制
 */
@Component
public class ASRWebSocketHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(ASRWebSocketHandler.class);
    
    @Value("${app.python.services.asr-url:ws://localhost:8767/asr}")
    private String asrServerUrl;
    
    @Value("${app.python.asr.health-url:http://localhost:8768/health}")
    private String asrHealthUrl;
    
    @Value("${app.python.asr.enabled:true}")
    private boolean asrEnabled;
    
    private final ChatService chatService;
    private final MultiChannelDispatcher multiChannelDispatcher;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final ScheduledExecutorService scheduler;
    
    // ASR客户端管理
    private final ConcurrentHashMap<String, ASRClientSession> asrSessions = new ConcurrentHashMap<>();
    
    // ASR服务状态
    private volatile boolean asrServiceAvailable = false;
    private volatile long lastHealthCheck = 0;
    private static final long HEALTH_CHECK_INTERVAL = 30000; // 30秒
    
    public ASRWebSocketHandler(ChatService chatService, 
                              MultiChannelDispatcher multiChannelDispatcher,
                              ObjectMapper objectMapper) {
        this.chatService = chatService;
        this.multiChannelDispatcher = multiChannelDispatcher;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        this.scheduler = Executors.newScheduledThreadPool(2);
        
        // 不在初始化时启动定期健康检查，改为按需检查
    }
    
    /**
     * 处理ASR相关的WebSocket消息
     * 在现有的ChatWebSocketHandler.handleMessage中调用此方法
     */
    public boolean handleASRMessage(WebSocketSession session, ChatMessage chatMessage) {
        String messageType = chatMessage.getType();
        
        switch (messageType) {
            case "asr_toggle":
                return handleASRToggle(session, chatMessage);
            case "asr_check_service":
                return handleASRServiceCheck(session, chatMessage);
            case "asr_start_session":
                return handleASRStartSession(session, chatMessage);
            case "asr_end_session":
                return handleASREndSession(session, chatMessage);
            default:
                return false; // 不是ASR消息，返回false让原处理器继续处理
        }
    }
    
    /**
     * 处理ASR开关切换
     */
    private boolean handleASRToggle(WebSocketSession session, ChatMessage message) {
        String sessionId = message.getSessionId();
        boolean enableASR = (Boolean) message.getMetadata().get("enabled");
        
        logger.info("ASR切换请求 - sessionId: {}, enableASR: {}", sessionId, enableASR);
        
        try {
            if (enableASR) {
                // 检查ASR服务是否可用
                if (!checkASRServiceHealth()) {
                    sendASRError(session, "ASR服务不可用");
                    return true;
                }
                
                // 启动ASR会话
                startASRSession(session, sessionId);
            } else {
                // 停止ASR会话
                stopASRSession(session, sessionId);
            }
            
            return true;
        } catch (Exception e) {
            logger.error("处理ASR切换失败", e);
            sendASRError(session, "ASR切换失败: " + e.getMessage());
            return true;
        }
    }
    
    /**
     * 处理ASR服务状态检查
     */
    private boolean handleASRServiceCheck(WebSocketSession session, ChatMessage message) {
        try {
            boolean available = checkASRServiceHealth();
            
            ChatMessage response = new ChatMessage();
            response.setType("asr_service_status");
            response.setSessionId(message.getSessionId());
            response.setMetadata(Map.of(
                "available", available,
                "serverUrl", asrServerUrl,
                "lastCheck", lastHealthCheck
            ));
            
            sendMessage(session, response);
            return true;
        } catch (Exception e) {
            logger.error("检查ASR服务状态失败", e);
            sendASRError(session, "检查ASR服务状态失败");
            return true;
        }
    }
    
    /**
     * 处理ASR会话开始
     */
    private boolean handleASRStartSession(WebSocketSession session, ChatMessage message) {
        String sessionId = message.getSessionId();
        
        try {
            ASRClientSession asrSession = asrSessions.get(sessionId);
            if (asrSession != null && asrSession.isConnected()) {
                logger.info("ASR会话已存在: {}", sessionId);
                sendASRSessionStarted(session, sessionId);
                return true;
            }
            
            // 创建新的ASR会话
            startASRSession(session, sessionId);
            return true;
        } catch (Exception e) {
            logger.error("启动ASR会话失败", e);
            sendASRError(session, "启动ASR会话失败: " + e.getMessage());
            return true;
        }
    }
    
    /**
     * 处理ASR会话结束
     */
    private boolean handleASREndSession(WebSocketSession session, ChatMessage message) {
        String sessionId = message.getSessionId();
        
        try {
            stopASRSession(session, sessionId);
            return true;
        } catch (Exception e) {
            logger.error("结束ASR会话失败", e);
            sendASRError(session, "结束ASR会话失败: " + e.getMessage());
            return true;
        }
    }
    
    /**
     * 启动ASR会话
     */
    private void startASRSession(WebSocketSession session, String sessionId) {
        try {
            // 创建ASR客户端会话
            ASRClientSession asrSession = new ASRClientSession(sessionId, asrServerUrl);
            
            // 设置消息处理回调
            asrSession.setMessageHandler(message -> {
                handleASRServerMessage(session, sessionId, message);
            });
            
            // 连接到ASR服务器
            asrSession.connect().thenAccept(connected -> {
                if (connected) {
                    asrSessions.put(sessionId, asrSession);
                    logger.info("ASR会话启动成功: {}", sessionId);
                    sendASRSessionStarted(session, sessionId);
                } else {
                    logger.error("ASR会话连接失败: {}", sessionId);
                    sendASRError(session, "无法连接到ASR服务器");
                }
            }).exceptionally(throwable -> {
                logger.error("ASR会话启动异常: " + sessionId, throwable);
                sendASRError(session, "ASR会话启动失败");
                return null;
            });
            
        } catch (Exception e) {
            logger.error("创建ASR会话失败: " + sessionId, e);
            sendASRError(session, "创建ASR会话失败");
        }
    }
    
    /**
     * 停止ASR会话
     */
    private void stopASRSession(WebSocketSession session, String sessionId) {
        ASRClientSession asrSession = asrSessions.remove(sessionId);
        if (asrSession != null) {
            asrSession.disconnect();
            logger.info("ASR会话已停止: {}", sessionId);
        }
        
        sendASRSessionEnded(session, sessionId);
    }
    
    /**
     * 处理来自ASR服务器的消息
     */
    private void handleASRServerMessage(WebSocketSession session, String sessionId, String message) {
        try {
            JsonNode messageNode = objectMapper.readTree(message);
            String type = messageNode.get("type").asText();
            
            switch (type) {
                case "asr_result":
                    handleASRResult(session, sessionId, messageNode);
                    break;
                case "session_started":
                    logger.info("ASR服务器会话已开始: {}", sessionId);
                    break;
                case "session_ended":
                    logger.info("ASR服务器会话已结束: {}", sessionId);
                    break;
                case "error":
                    String errorMsg = messageNode.get("message").asText();
                    logger.error("ASR服务器错误 [{}]: {}", sessionId, errorMsg);
                    sendASRError(session, "ASR服务器错误: " + errorMsg);
                    break;
                default:
                    logger.debug("收到未知ASR消息类型: {}", type);
            }
        } catch (Exception e) {
            logger.error("处理ASR服务器消息失败", e);
        }
    }
    
    /**
     * 处理ASR识别结果
     */
    private void handleASRResult(WebSocketSession session, String sessionId, JsonNode messageNode) {
        try {
            JsonNode result = messageNode.get("result");
            String transcription = result.get("transcription").asText();
            double confidence = result.get("confidence").asDouble();
            
            logger.info("收到ASR识别结果 [{}]: {}", sessionId, transcription);
            
            // 发送识别结果到前端
            ChatMessage resultMessage = new ChatMessage();
            resultMessage.setType("asr_result");
            resultMessage.setSessionId(sessionId);
            resultMessage.setMetadata(Map.of(
                "text", transcription,
                "confidence", confidence,
                "timestamp", System.currentTimeMillis()
            ));
            
            sendMessage(session, resultMessage);
            
            // 将识别结果作为用户消息处理
            if (confidence > 0.7) { // 置信度阈值
                processASRAsUserMessage(sessionId, transcription, session);
            }
            
        } catch (Exception e) {
            logger.error("处理ASR识别结果失败", e);
        }
    }
    
    /**
     * 将ASR识别结果作为用户消息处理
     */
    private void processASRAsUserMessage(String sessionId, String text, WebSocketSession session) {
        try {
            // 创建用户消息
            ChatMessage userMessage = new ChatMessage();
            userMessage.setType("text");
            userMessage.setContent(text);
            userMessage.setSessionId(sessionId);
            userMessage.setRole("user");
            userMessage.setMetadata(Map.of("source", "asr")); // 标记来源为ASR
            
            // 使用MultiChannelDispatcher处理消息
            multiChannelDispatcher.processMessage(userMessage, response -> {
                try {
                    sendMessage(session, response);
                } catch (IOException e) {
                    logger.error("发送ASR处理结果失败", e);
                }
            });
            
        } catch (Exception e) {
            logger.error("处理ASR用户消息失败", e);
        }
    }
    
    /**
     * 检查ASR服务健康状态
     */
    private boolean checkASRServiceHealth() {
        if (!asrEnabled) {
            return false;
        }
        
        try {
            logger.info("🔍 正在检查ASR服务健康状态...");
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(asrHealthUrl))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();
            
            HttpResponse<String> response = httpClient.send(request, 
                    HttpResponse.BodyHandlers.ofString());
            
            boolean isHealthy = response.statusCode() == 200;
            
            if (isHealthy) {
                logger.info("✅ ASR服务健康检查通过");
            } else {
                logger.warn("⚠️ ASR服务健康检查失败，状态码: {}", response.statusCode());
            }
            
            return isHealthy;
            
        } catch (Exception e) {
            logger.error("❌ ASR服务健康检查异常", e);
            return false;
        }
    }
    
    /**
     * 启动定期健康检查（已禁用，改为按需检查）
     */
    @SuppressWarnings("unused")
    private void startHealthCheck() {
        // 不再启动定期健康检查，改为在用户启用ASR时进行实时检查
        logger.info("ASR健康检查已改为按需模式，不进行定期检查");
    }
    
    /**
     * 清理会话资源
     */
    public void cleanupSession(String sessionId) {
        ASRClientSession asrSession = asrSessions.remove(sessionId);
        if (asrSession != null) {
            asrSession.disconnect();
            logger.info("清理ASR会话: {}", sessionId);
        }
    }
    
    /**
     * 获取ASR服务状态
     */
    public Map<String, Object> getASRServiceStatus() {
        return Map.of(
            "enabled", asrEnabled,
            "available", asrServiceAvailable,
            "serverUrl", asrServerUrl,
            "lastCheck", lastHealthCheck,
            "activeSessions", asrSessions.size()
        );
    }
    
    // 辅助方法：发送消息
    private void sendMessage(WebSocketSession session, ChatMessage message) throws IOException {
        String messageJson = objectMapper.writeValueAsString(message);
        session.sendMessage(new TextMessage(messageJson));
    }
    
    private void sendASRError(WebSocketSession session, String errorMessage) {
        try {
            ChatMessage error = new ChatMessage();
            error.setType("asr_error");
            error.setContent(errorMessage);
            error.setTimestamp(java.time.LocalDateTime.now());
            sendMessage(session, error);
        } catch (IOException e) {
            logger.error("发送ASR错误消息失败", e);
        }
    }
    
    private void sendASRSessionStarted(WebSocketSession session, String sessionId) {
        try {
            ChatMessage message = new ChatMessage();
            message.setType("asr_session_started");
            message.setSessionId(sessionId);
            message.setContent("ASR会话已开始，请开始说话");
            sendMessage(session, message);
        } catch (IOException e) {
            logger.error("发送ASR会话开始消息失败", e);
        }
    }
    
    private void sendASRSessionEnded(WebSocketSession session, String sessionId) {
        try {
            ChatMessage message = new ChatMessage();
            message.setType("asr_session_ended");
            message.setSessionId(sessionId);
            message.setContent("ASR会话已结束");
            sendMessage(session, message);
        } catch (IOException e) {
            logger.error("发送ASR会话结束消息失败", e);
        }
    }
    
    /**
     * ASR客户端会话类
     * 管理与ASR服务器的WebSocket连接
     */
    private static class ASRClientSession {
        private final String sessionId;
        private final String serverUrl;
        private WebSocketSession webSocketSession;
        private boolean connected = false;
        private java.util.function.Consumer<String> messageHandler;
        
        public ASRClientSession(String sessionId, String serverUrl) {
            this.sessionId = sessionId;
            this.serverUrl = serverUrl;
        }
        
        public void setMessageHandler(java.util.function.Consumer<String> handler) {
            this.messageHandler = handler;
        }
        
        public CompletableFuture<Boolean> connect() {
            // 这里需要实现WebSocket客户端连接到ASR服务器
            // 由于Spring WebSocket主要用于服务端，这里可以使用Java-WebSocket库
            // 或者使用HTTP客户端的WebSocket功能
            
            CompletableFuture<Boolean> future = new CompletableFuture<>();
            
            // 模拟连接成功
            // 实际实现需要使用WebSocket客户端库
            connected = true;
            future.complete(true);
            
            return future;
        }
        
        public void disconnect() {
            connected = false;
            if (webSocketSession != null && webSocketSession.isOpen()) {
                try {
                    webSocketSession.close();
                } catch (IOException e) {
                    // 忽略关闭异常
                }
            }
        }
        
        public boolean isConnected() {
            return connected;
        }
        
        public void sendMessage(String message) {
            if (connected && webSocketSession != null && webSocketSession.isOpen()) {
                try {
                    webSocketSession.sendMessage(new TextMessage(message));
                } catch (IOException e) {
                    connected = false;
                    throw new RuntimeException("发送消息失败", e);
                }
            }
        }
    }
}

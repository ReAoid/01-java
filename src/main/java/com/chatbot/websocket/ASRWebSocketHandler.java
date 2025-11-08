package com.chatbot.websocket;

import com.chatbot.model.domain.ChatMessage;
import com.chatbot.service.MultiChannelDispatcher;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.TextMessage;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

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
    
    private final MultiChannelDispatcher multiChannelDispatcher;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    
    // 全局ASR连接管理 - 应用级单例连接
    private static volatile ASRGlobalConnection globalASRConnection = null;
    private static final Object connectionLock = new Object();
    private static volatile boolean connectionInitialized = false;
    
    // ASR服务状态
    private volatile boolean asrServiceAvailable = false;
    private volatile long lastHealthCheck = 0;
    
    // 当前活跃的ASR会话 - 只有一个，最新启用的会话
    private volatile WebSocketSession currentASRSession = null;
    private volatile String currentASRSessionId = null;
    private volatile long lastASRActivationTime = 0;
    
    public ASRWebSocketHandler(MultiChannelDispatcher multiChannelDispatcher,
                              ObjectMapper objectMapper) {
        this.multiChannelDispatcher = multiChannelDispatcher;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        
        // 应用启动时初始化ASR连接（可选，按需连接）
        logger.info("ASR WebSocket处理器已初始化，等待首次ASR启用时建立连接");
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
                
                // 启用ASR会话
                return handleASRStartSession(session, message);
            } else {
                // 禁用ASR会话
                return handleASREndSession(session, message);
            }
            
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
            
            sendMessageToSession(session, response);
            return true;
        } catch (Exception e) {
            logger.error("检查ASR服务状态失败", e);
            sendASRError(session, "检查ASR服务状态失败");
            return true;
        }
    }
    
    /**
     * 处理ASR会话开始 - 启用ASR功能（最新会话优先）
     */
    private boolean handleASRStartSession(WebSocketSession session, ChatMessage message) {
        String sessionId = message.getSessionId();
        long currentTime = System.currentTimeMillis();
        
        try {
            // 检查全局ASR连接
            ensureGlobalASRConnection();
            
            // 如果有其他会话正在使用ASR，先通知其停止
            if (currentASRSession != null && !sessionId.equals(currentASRSessionId)) {
                notifyASRSessionTakenOver(currentASRSession, currentASRSessionId, sessionId);
            }
            
            // 设置当前ASR会话为最新的会话
            currentASRSession = session;
            currentASRSessionId = sessionId;
            lastASRActivationTime = currentTime;
            
            logger.info("ASR会话已启用: {} (最新会话，时间: {})", sessionId, currentTime);
            sendASRSessionStarted(session, sessionId);
            
            return true;
        } catch (Exception e) {
            logger.error("启用ASR会话失败", e);
            sendASRError(session, "启用ASR会话失败: " + e.getMessage());
            return true;
        }
    }
    
    /**
     * 处理ASR会话结束 - 禁用ASR功能
     */
    private boolean handleASREndSession(WebSocketSession session, ChatMessage message) {
        String sessionId = message.getSessionId();
        
        try {
            // 只有当前活跃会话才能禁用ASR
            if (sessionId.equals(currentASRSessionId)) {
                currentASRSession = null;
                currentASRSessionId = null;
                lastASRActivationTime = 0;
                
                logger.info("ASR会话已禁用: {} (当前活跃会话)", sessionId);
                sendASRSessionEnded(session, sessionId);
            } else {
                logger.info("忽略ASR禁用请求: {} (非当前活跃会话: {})", sessionId, currentASRSessionId);
            }
            
            // 注意：不关闭全局ASR连接，保持持久连接
            
            return true;
        } catch (Exception e) {
            logger.error("禁用ASR会话失败", e);
            sendASRError(session, "禁用ASR会话失败: " + e.getMessage());
            return true;
        }
    }
    
    /**
     * 确保全局ASR连接存在 - 应用级单例模式
     */
    private void ensureGlobalASRConnection() throws Exception {
        // 如果连接已存在且正常，直接返回
        if (globalASRConnection != null && globalASRConnection.isConnected()) {
            return;
        }
        
        synchronized (connectionLock) {
            // 双重检查锁定模式
            if (globalASRConnection != null && globalASRConnection.isConnected()) {
                return;
            }
            
            // 如果连接已初始化但断开，不要重新创建，让重连机制处理
            if (connectionInitialized && globalASRConnection != null) {
                logger.info("ASR连接已存在但断开，等待自动重连...");
                throw new Exception("ASR连接暂时不可用，请稍后重试");
            }
            
            logger.info("🚀 初始化全局ASR连接到: {}", asrServerUrl);
            
            // 创建唯一的全局ASR连接
            globalASRConnection = new ASRGlobalConnection(asrServerUrl, this);
            connectionInitialized = true;
            
            // 设置消息处理回调
            globalASRConnection.setMessageHandler(this::handleGlobalASRMessage);
            
            // 连接到ASR服务器
            boolean connected = globalASRConnection.connectToServer();
            if (!connected) {
                connectionInitialized = false; // 连接失败，允许重新初始化
                globalASRConnection = null;
                throw new Exception("无法连接到ASR服务器");
            }
            
            logger.info("✅ 全局ASR连接建立成功，应用级单例已激活");
        }
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
            resultMessage.setType("system");
            resultMessage.setSessionId(sessionId);
            resultMessage.setContent("ASR识别结果");
            resultMessage.setMetadata(Map.of(
                "asr_result", true,
                "transcription", transcription,
                "confidence", confidence,
                "is_final", true,
                "timestamp", System.currentTimeMillis()
            ));
            
            sendMessageToSession(session, resultMessage);
            
            // 将识别结果作为用户消息处理
            if (confidence > 0.7) { // 置信度阈值
                processASRAsUserMessage(sessionId, transcription, session);
            }
            
        } catch (Exception e) {
            logger.error("处理ASR识别结果失败", e);
        }
    }
    
    /**
     * 处理来自前端的音频数据块 - 只处理当前活跃会话的音频
     */
    public void handleAudioChunk(WebSocketSession session, ChatMessage message) {
        String sessionId = message.getSessionId();
        
        try {
            // 检查是否为当前活跃的ASR会话
            if (!sessionId.equals(currentASRSessionId)) {
                logger.warn("忽略非活跃会话的音频数据: sessionId={}, 当前活跃会话: {}", sessionId, currentASRSessionId);
                return;
            }
            
            // 获取音频数据
            String audioData = (String) message.getMetadata().get("audio_data");
            if (audioData == null || audioData.isEmpty()) {
                logger.warn("收到空的音频数据: sessionId={}", sessionId);
                return;
            }
            
            logger.debug("处理音频数据块: sessionId={}, dataLength={}", sessionId, audioData.length());
            
            // 确保全局ASR连接存在
            ensureGlobalASRConnection();
            
            // 通过全局连接发送音频数据
            globalASRConnection.sendAudioChunk(sessionId, audioData);
            
        } catch (Exception e) {
            logger.error("处理音频数据块失败: sessionId={}", sessionId, e);
        }
    }
    
    
    /**
     * 处理全局ASR消息
     */
    private void handleGlobalASRMessage(String message) {
        try {
            JsonNode messageNode = objectMapper.readTree(message);
            String type = messageNode.get("type").asText();
            String sessionId = messageNode.has("session_id") ? messageNode.get("session_id").asText() : null;
            
            logger.debug("收到全局ASR消息: type={}, sessionId={}", type, sessionId);
            
            switch (type) {
                case "final_result":
                    handleASRFinalResult(messageNode, sessionId);
                    break;
                case "partial_result":
                    handleASRPartialResult(messageNode, sessionId);
                    break;
                case "speech_status":
                    handleASRSpeechStatus(messageNode, sessionId);
                    break;
                case "error":
                    handleASRErrorMessage(messageNode, sessionId);
                    break;
                default:
                    logger.debug("未处理的ASR消息类型: {}", type);
            }
            
        } catch (Exception e) {
            logger.error("处理全局ASR消息失败", e);
        }
    }
    
    /**
     * 处理ASR最终识别结果 - 只处理当前活跃会话的结果
     */
    private void handleASRFinalResult(JsonNode messageNode, String sessionId) {
        try {
            // 只处理当前活跃会话的ASR结果
            if (!sessionId.equals(currentASRSessionId)) {
                logger.debug("忽略非活跃会话的ASR结果: sessionId={}, 当前活跃会话: {}", sessionId, currentASRSessionId);
                return;
            }
            
            if (currentASRSession == null || !currentASRSession.isOpen()) {
                logger.warn("当前ASR会话不存在或已关闭: sessionId={}", sessionId);
                return;
            }
            
            JsonNode result = messageNode.get("result");
            String transcription = result.get("transcription").asText();
            double confidence = result.get("confidence").asDouble();
            
            logger.info("收到ASR最终识别结果: sessionId={}, text={}, confidence={}", 
                sessionId, transcription, confidence);
            
            // 发送识别结果到前端
            ChatMessage resultMessage = new ChatMessage();
            resultMessage.setType("system");
            resultMessage.setSessionId(sessionId);
            resultMessage.setContent("ASR识别结果");
            resultMessage.setMetadata(Map.of(
                "asr_result", true,
                "transcription", transcription,
                "confidence", confidence,
                "is_final", true,
                "timestamp", System.currentTimeMillis()
            ));
            
            sendMessageToSession(currentASRSession, resultMessage);
            
            // 如果置信度足够高，作为用户消息处理（实现打断机制）
            if (confidence > 0.7 && !transcription.trim().isEmpty()) {
                processASRAsUserMessage(sessionId, transcription, currentASRSession);
            }
            
        } catch (Exception e) {
            logger.error("处理ASR最终结果失败: sessionId={}", sessionId, e);
        }
    }
    
    /**
     * 将ASR识别结果作为用户消息处理 - 实现打断机制
     */
    private void processASRAsUserMessage(String sessionId, String text, WebSocketSession session) {
        try {
            // 检查会话状态
            if (session == null || !session.isOpen()) {
                logger.warn("WebSocket会话已关闭，跳过ASR消息处理: sessionId={}", sessionId);
                return;
            }
            
            // 创建用户消息
            ChatMessage userMessage = new ChatMessage();
            userMessage.setType("text");
            userMessage.setContent(text);
            userMessage.setSessionId(sessionId);
            userMessage.setRole("user");
            userMessage.setMetadata(Map.of("source", "asr")); // 标记来源为ASR
            
            logger.info("处理ASR用户消息（将打断之前的请求）: sessionId={}, text={}", sessionId, text);
            
            // 关键：使用MultiChannelDispatcher处理消息，这会自动打断之前的请求
            // 就像普通文字消息一样
            multiChannelDispatcher.processMessage(userMessage, response -> {
                try {
                    // 在回调中再次检查会话状态
                    if (session.isOpen()) {
                        sendMessageToSession(session, response);
                    } else {
                        logger.warn("WebSocket会话在处理过程中关闭，跳过响应发送: sessionId={}", sessionId);
                    }
                } catch (Exception e) {
                    logger.error("发送ASR处理结果失败: sessionId={}", sessionId, e);
                }
            });
            
        } catch (Exception e) {
            logger.error("处理ASR用户消息失败: sessionId={}", sessionId, e);
        }
    }
    
    /**
     * 处理ASR部分识别结果
     */
    private void handleASRPartialResult(JsonNode messageNode, String sessionId) {
        try {
            // 只处理当前活跃会话的部分结果
            if (!sessionId.equals(currentASRSessionId)) return;
            if (currentASRSession == null || !currentASRSession.isOpen()) return;
            
            JsonNode result = messageNode.get("result");
            String transcription = result.get("transcription").asText();
            double confidence = result.get("confidence").asDouble();
            
            // 发送部分结果到前端
            ChatMessage resultMessage = new ChatMessage();
            resultMessage.setType("system");
            resultMessage.setSessionId(sessionId);
            resultMessage.setContent("ASR部分结果");
            resultMessage.setMetadata(Map.of(
                "asr_result", true,
                "transcription", transcription,
                "confidence", confidence,
                "is_final", false,
                "timestamp", System.currentTimeMillis()
            ));
            
            sendMessageToSession(currentASRSession, resultMessage);
            
        } catch (Exception e) {
            logger.error("处理ASR部分结果失败: sessionId={}", sessionId, e);
        }
    }
    
    /**
     * 处理ASR语音状态
     */
    private void handleASRSpeechStatus(JsonNode messageNode, String sessionId) {
        try {
            // 只处理当前活跃会话的语音状态
            if (!sessionId.equals(currentASRSessionId)) return;
            if (currentASRSession == null || !currentASRSession.isOpen()) return;
            
            String status = messageNode.get("status").asText();
            logger.debug("ASR语音状态: sessionId={}, status={}", sessionId, status);
            
            // 可以根据需要发送状态到前端
            
        } catch (Exception e) {
            logger.error("处理ASR语音状态失败: sessionId={}", sessionId, e);
        }
    }
    
    /**
     * 处理ASR错误消息
     */
    private void handleASRErrorMessage(JsonNode messageNode, String sessionId) {
        try {
            String errorMessage = messageNode.get("message").asText();
            logger.error("ASR错误: sessionId={}, error={}", sessionId, errorMessage);
            
            // 只向当前活跃会话发送错误消息
            if (sessionId != null && sessionId.equals(currentASRSessionId) && 
                currentASRSession != null && currentASRSession.isOpen()) {
                sendASRError(currentASRSession, errorMessage);
            }
            
        } catch (Exception e) {
            logger.error("处理ASR错误消息失败: sessionId={}", sessionId, e);
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
    
    // 健康检查已改为按需模式（在用户启用ASR时进行实时检查）
    
    /**
     * 清理会话资源 - 只清理当前会话，保持全局ASR连接
     */
    public void cleanupSession(String sessionId) {
        // 如果是当前活跃的ASR会话，清理它
        if (sessionId.equals(currentASRSessionId)) {
            currentASRSession = null;
            currentASRSessionId = null;
            lastASRActivationTime = 0;
            logger.info("清理当前ASR会话: sessionId={}", sessionId);
        }
        
        // 注意：不关闭全局ASR连接，保持持久连接
        // 全局连接会在应用关闭时或连接异常时才关闭
    }
    
    /**
     * 通知ASR会话被接管
     */
    private void notifyASRSessionTakenOver(WebSocketSession oldSession, String oldSessionId, String newSessionId) {
        if (oldSession != null && oldSession.isOpen()) {
            try {
                ChatMessage takeoverMessage = new ChatMessage();
                takeoverMessage.setType("system");
                takeoverMessage.setSessionId(oldSessionId);
                takeoverMessage.setContent("ASR功能已被新窗口接管，当前窗口ASR已自动关闭");
                takeoverMessage.setMetadata(Map.of(
                    "asr_session_taken_over", true,
                    "old_session_id", oldSessionId,
                    "new_session_id", newSessionId,
                    "asr_auto_disabled", true,
                    "timestamp", System.currentTimeMillis()
                ));
                
                sendMessageToSession(oldSession, takeoverMessage);
                logger.info("已通知旧会话ASR被接管: {} → {}", oldSessionId, newSessionId);
                
            } catch (Exception e) {
                logger.error("通知ASR会话接管时出错: oldSessionId={}", oldSessionId, e);
            }
        }
    }
    
    /**
     * 通知前端ASR连接失败
     */
    private void notifyASRConnectionFailed() {
        // 只通知当前活跃的ASR会话
        if (currentASRSession != null && currentASRSession.isOpen()) {
            try {
                ChatMessage failureMessage = new ChatMessage();
                failureMessage.setType("system");
                failureMessage.setSessionId(currentASRSessionId);
                failureMessage.setContent("ASR服务连接失败，已自动关闭");
                failureMessage.setMetadata(Map.of(
                    "asr_connection_failed", true,
                    "asr_auto_disabled", true,
                    "max_retries_reached", true,
                    "timestamp", System.currentTimeMillis()
                ));
                
                sendMessageToSession(currentASRSession, failureMessage);
                logger.info("已通知前端ASR连接失败: sessionId={}", currentASRSessionId);
                
            } catch (Exception e) {
                logger.error("通知前端ASR连接失败时出错: sessionId={}", currentASRSessionId, e);
            }
        }
        
        // 清空当前会话
        currentASRSession = null;
        currentASRSessionId = null;
        lastASRActivationTime = 0;
        logger.info("已清空当前ASR活跃会话");
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
            "currentSessionId", currentASRSessionId != null ? currentASRSessionId : "none",
            "lastActivationTime", lastASRActivationTime,
            "connectionInitialized", connectionInitialized,
            "globalConnectionActive", globalASRConnection != null && globalASRConnection.isConnected()
        );
    }
    
    /**
     * 获取全局连接信息（用于调试）
     */
    public String getConnectionInfo() {
        if (globalASRConnection == null) {
            return "ASR连接未初始化";
        }
        
        return String.format("ASR连接状态: %s, 重连次数: %d, 连接URI: %s", 
            globalASRConnection.isConnected() ? "已连接" : "已断开",
            globalASRConnection.reconnectAttempts,
            globalASRConnection.getURI());
    }
    
    // 辅助方法：发送消息
    private void sendMessageToSession(WebSocketSession session, ChatMessage message) throws IOException {
        if (session == null) {
            logger.warn("WebSocket会话为null，无法发送消息");
            return;
        }
        
        if (!session.isOpen()) {
            logger.warn("WebSocket会话已关闭，跳过消息发送: sessionId={}", 
                message.getSessionId());
            return;
        }
        
        try {
        String messageJson = objectMapper.writeValueAsString(message);
        session.sendMessage(new TextMessage(messageJson));
            logger.debug("消息发送成功: sessionId={}, type={}", 
                message.getSessionId(), message.getType());
        } catch (IllegalStateException e) {
            logger.warn("WebSocket会话状态异常，无法发送消息: sessionId={}, error={}", 
                message.getSessionId(), e.getMessage());
        } catch (IOException e) {
            logger.error("发送WebSocket消息失败: sessionId={}", message.getSessionId(), e);
            throw e;
        }
    }
    
    private void sendASRError(WebSocketSession session, String errorMessage) {
        try {
            ChatMessage error = new ChatMessage();
            error.setType("asr_error");
            error.setContent(errorMessage);
            error.setTimestamp(java.time.LocalDateTime.now());
            sendMessageToSession(session, error);
        } catch (IOException e) {
            logger.error("发送ASR错误消息失败", e);
        }
    }
    
    private void sendASRSessionStarted(WebSocketSession session, String sessionId) {
        try {
            ChatMessage message = new ChatMessage();
            message.setType("system");
            message.setSessionId(sessionId);
            message.setContent("ASR会话已开始，请开始说话");
            message.setMetadata(Map.of("asr_session_started", true, "timestamp", System.currentTimeMillis()));
            sendMessageToSession(session, message);
        } catch (Exception e) {
            logger.error("发送ASR会话开始消息失败", e);
        }
    }
    
    private void sendASRSessionEnded(WebSocketSession session, String sessionId) {
        try {
            ChatMessage message = new ChatMessage();
            message.setType("asr_session_ended");
            message.setSessionId(sessionId);
            message.setContent("ASR会话已结束");
            sendMessageToSession(session, message);
        } catch (IOException e) {
            logger.error("发送ASR会话结束消息失败", e);
        }
    }
    
    
    
    /**
     * 真实的ASR全局连接类
     * 使用Java-WebSocket库连接到SenseVoice ASR服务器
     */
    private static class ASRGlobalConnection extends org.java_websocket.client.WebSocketClient {
        private volatile boolean connected = false;
        private java.util.function.Consumer<String> messageHandler;
        private final ObjectMapper objectMapper = new ObjectMapper();
        private final String sessionId = "global_asr_session";
        
        // 重连控制
        private volatile int reconnectAttempts = 0;
        private static final int MAX_RECONNECT_ATTEMPTS = 3;
        
        // 外部处理器引用
        private final ASRWebSocketHandler parentHandler;
        
        public ASRGlobalConnection(String serverUrl, ASRWebSocketHandler parentHandler) throws Exception {
            super(new java.net.URI(serverUrl));
            this.parentHandler = parentHandler;
            
            // 设置连接超时
            setConnectionLostTimeout(30);
        }
        
        public void setMessageHandler(java.util.function.Consumer<String> handler) {
            this.messageHandler = handler;
        }
        
        @Override
        public void onOpen(org.java_websocket.handshake.ServerHandshake handshake) {
            connected = true;
            reconnectAttempts = 0; // 连接成功，重置重连次数
            logger.info("✅ ASR WebSocket连接已建立: {}", getURI());
            
            // 发送初始化消息到ASR服务器
            sendInitMessage();
        }
        
        @Override
        public void onMessage(String message) {
            logger.debug("收到ASR服务器消息: {}", message);
            
            // 通过消息处理器处理结果
            if (messageHandler != null) {
                messageHandler.accept(message);
            }
        }
        
        @Override
        public void onClose(int code, String reason, boolean remote) {
            connected = false;
            logger.warn("ASR WebSocket连接已关闭: code={}, reason={}, remote={}", code, reason, remote);
            
            // 如果是异常关闭且未超过重连次数限制，尝试重连
            if (code != 1000 && reconnectAttempts < MAX_RECONNECT_ATTEMPTS) { // 1000 = 正常关闭
                reconnectAttempts++;
                logger.info("准备第 {} 次重连ASR服务器（最大{}次）", reconnectAttempts, MAX_RECONNECT_ATTEMPTS);
                scheduleReconnect();
            } else if (reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) {
                logger.error("❌ ASR服务器重连失败，已达到最大重连次数 {}，停止重连", MAX_RECONNECT_ATTEMPTS);
                // 通知前端ASR连接失败，需要关闭ASR按钮
                parentHandler.notifyASRConnectionFailed();
            }
        }
        
        @Override
        public void onError(Exception ex) {
            connected = false;
            logger.error("ASR WebSocket连接错误", ex);
        }
        
        private void sendInitMessage() {
            try {
                // 发送会话开始消息到ASR服务器
                Map<String, Object> startMessage = Map.of(
                    "type", "start_session",
                    "language", "auto",
                    "session_id", sessionId
                );
                
                String json = objectMapper.writeValueAsString(startMessage);
                send(json);
                
                logger.info("已发送ASR初始化消息");
                
            } catch (Exception e) {
                logger.error("发送ASR初始化消息失败", e);
            }
        }
        
        public void sendAudioChunk(String userSessionId, String audioData) {
            if (!connected || !isOpen()) {
                logger.warn("ASR连接未建立，无法发送音频数据");
                return;
            }
            
            try {
                // 构造发送给ASR服务器的消息
                Map<String, Object> message = Map.of(
                    "type", "audio_chunk",
                    "session_id", userSessionId, // 使用用户会话ID
                    "audio_data", audioData
                );
                
                String json = objectMapper.writeValueAsString(message);
                send(json);
                
                logger.debug("已发送音频数据到ASR服务器: sessionId={}, dataLength={}", 
                    userSessionId, audioData.length());
                
            } catch (Exception e) {
                logger.error("发送音频数据到ASR服务器失败", e);
            }
        }
        
        public boolean connectToServer() {
            try {
                logger.info("正在连接到ASR服务器: {}", getURI());
                
                // 阻塞连接，等待连接建立
                boolean success = connectBlocking(10, java.util.concurrent.TimeUnit.SECONDS);
                
                if (success) {
                    logger.info("ASR服务器连接成功");
                } else {
                    logger.error("ASR服务器连接超时");
                }
                
                return success;
                
            } catch (Exception e) {
                logger.error("连接ASR服务器失败", e);
                connected = false;
                return false;
            }
        }
        
        public boolean isConnected() {
            return connected && isOpen();
        }
        
        public void resetReconnectAttempts() {
            reconnectAttempts = 0;
            logger.info("🔄 ASR重连计数器已重置");
        }
        
        public void disconnect() {
            try {
                if (isOpen()) {
                    // 发送会话结束消息
                    Map<String, Object> endMessage = Map.of(
                        "type", "end_session"
                    );
                    String json = objectMapper.writeValueAsString(endMessage);
                    send(json);
                    
                    // 关闭连接
                    close(1000, "正常关闭");
                }
            } catch (Exception e) {
                logger.error("关闭ASR连接时出错", e);
            } finally {
                connected = false;
                logger.info("ASR全局连接已断开");
            }
        }
        
        private void scheduleReconnect() {
            // 使用单一线程池避免多线程重连
            java.util.concurrent.Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "ASR-Reconnect-" + reconnectAttempts);
                t.setDaemon(true);
                return t;
            }).schedule(() -> {
                logger.info("🔄 开始第 {} 次重连ASR服务器... (单例连接)", reconnectAttempts);
                try {
                    // 使用现有连接的重连方法，不创建新实例
                    reconnect();
                } catch (Exception e) {
                    logger.error("❌ 第 {} 次ASR重连失败: {}", reconnectAttempts, e.getMessage());
                }
            }, 5, java.util.concurrent.TimeUnit.SECONDS);
        }
    }
}


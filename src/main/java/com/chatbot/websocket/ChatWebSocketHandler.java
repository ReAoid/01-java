package com.chatbot.websocket;

import com.chatbot.model.ChatMessage;
import com.chatbot.service.ChatService;
import com.chatbot.service.OllamaService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 聊天WebSocket处理器
 * 处理WebSocket连接、消息收发和会话管理
 */
@Component
public class ChatWebSocketHandler implements WebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(ChatWebSocketHandler.class);

    private final ChatService chatService;
    private final OllamaService ollamaService;
    private final ObjectMapper objectMapper;

    // 存储活跃的WebSocket会话
    private final ConcurrentHashMap<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    // 生成唯一会话ID (备用，当前使用IdUtil工具类)
    @SuppressWarnings("unused")
    private final AtomicLong sessionIdGenerator = new AtomicLong(0);

    public ChatWebSocketHandler(ChatService chatService, OllamaService ollamaService, ObjectMapper objectMapper) {
        logger.info("初始化ChatWebSocketHandler");
        this.chatService = chatService;
        this.ollamaService = ollamaService;
        this.objectMapper = objectMapper;
        logger.debug("ChatWebSocketHandler初始化完成");
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        long startTime = System.currentTimeMillis();
        String sessionId = generateSessionId();
        session.getAttributes().put("sessionId", sessionId);
        sessions.put(sessionId, session);

        logger.info("WebSocket连接建立，会话ID: {}, 当前活跃连接数: {}", sessionId, sessions.size());
        logger.debug("WebSocket连接详情 - RemoteAddress: {}, Uri: {}",
                session.getRemoteAddress(), session.getUri());

        // 发送连接成功消息
        ChatMessage welcomeMessage = new ChatMessage();
        welcomeMessage.setType("system");
        welcomeMessage.setContent("连接成功，欢迎使用AI聊天机器人！");
        welcomeMessage.setSessionId(sessionId);

        sendMessage(session, welcomeMessage);

        long connectionTime = System.currentTimeMillis() - startTime;
        logger.debug("WebSocket连接建立完成，sessionId: {}, 耗时: {}ms", sessionId, connectionTime);
    }

    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
        String sessionId = (String) session.getAttributes().get("sessionId");
        long startTime = System.currentTimeMillis();

        logger.debug("接收到WebSocket消息，sessionId: {}, messageType: {}",
                sessionId, message.getClass().getSimpleName());

        if (message instanceof TextMessage textMessage) {
            try {
                // 解析收到的消息
                String payload = textMessage.getPayload();

                ChatMessage chatMessage = objectMapper.readValue(payload, ChatMessage.class);
                chatMessage.setSessionId(sessionId);

                // 记录用户消息接收时间戳
                long userMessageTimestamp = System.currentTimeMillis();

                // 检查是否是系统命令
                if ("system".equals(chatMessage.getType()) && chatMessage.getMetadata() != null) {
                    String action = (String) chatMessage.getMetadata().get("action");
                    
                    if ("check_service".equals(action)) {
                        // 处理Ollama服务状态检查
                        handleOllamaStatusCheck(session, sessionId);
                        return;
                    } else if ("toggle_thinking".equals(action)) {
                        // 处理思考显示切换
                        handleThinkingToggle(session, sessionId, chatMessage);
                        return;
                    } else if ("toggle_thinking_save".equals(action)) {
                        // 处理思考过程保存切换
                        handleThinkingSaveToggle(session, sessionId, chatMessage);
                        return;
                    }
                }
                
                // 用于跟踪是否是第一次响应
                final boolean[] isFirstResponse = {true};
                
                chatService.processMessage(chatMessage, response -> {
                    try {
                        // 记录第一次响应时间
                        if (isFirstResponse[0] && response.getContent() != null && !response.getContent().isEmpty()) {
                            long firstResponseTime = System.currentTimeMillis();
                            long timeToFirstResponse = firstResponseTime - userMessageTimestamp;
                            
                            logger.info("🚀 首次响应时间统计 - sessionId: {}, 从用户消息到首次响应: {}ms",
                                       sessionId, timeToFirstResponse);
                            
                            isFirstResponse[0] = false;
                        }
                        
                        // 优化流式消息发送
                        sendStreamingMessage(session, response, sessionId);
                        
                    } catch (IOException e) {
                        logger.error("发送消息失败，sessionId: {}", sessionId, e);
                    }
                });

            } catch (Exception e) {
                long processingTime = System.currentTimeMillis() - startTime;
                logger.error("处理WebSocket消息时发生错误，sessionId: {}, 处理时间: {}ms",
                        sessionId, processingTime, e);

                // 发送错误消息
                ChatMessage errorMessage = new ChatMessage();
                errorMessage.setType("error");
                errorMessage.setContent("处理消息时发生错误，请稍后重试");
                errorMessage.setSessionId(sessionId);

                sendMessage(session, errorMessage);
            }
        } else {
            logger.warn("收到不支持的消息类型，sessionId: {}, messageType: {}",
                    sessionId, message.getClass().getSimpleName());
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        String sessionId = (String) session.getAttributes().get("sessionId");
        logger.error("WebSocket传输错误，会话ID: {}", sessionId, exception);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
        String sessionId = (String) session.getAttributes().get("sessionId");
        sessions.remove(sessionId);

        logger.info("WebSocket连接关闭，会话ID: {}, 关闭状态: {}", sessionId, closeStatus);

        // 清理会话相关资源
        chatService.cleanupSession(sessionId);
    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }

    /**
     * 发送消息到客户端
     */
    private void sendMessage(WebSocketSession session, ChatMessage message) throws IOException {
        if (session.isOpen()) {
            String messageJson = objectMapper.writeValueAsString(message);
            session.sendMessage(new TextMessage(messageJson));
        } else {
            logger.warn("WebSocket会话已关闭，无法发送消息，sessionId: {}", message.getSessionId());
        }
    }
    
    /**
     * 优化的流式消息发送
     */
    private void sendStreamingMessage(WebSocketSession session, ChatMessage message, String sessionId) throws IOException {
        if (!session.isOpen()) {
            logger.warn("WebSocket会话已关闭，无法发送消息，sessionId: {}", sessionId);
            return;
        }
        
        try {
            // 对于流式消息，优化JSON序列化
            String messageJson;
            if (message.isStreaming() && message.getContent() != null) {
                // 使用简化的JSON结构减少序列化开销
                messageJson = String.format(
                    "{\"type\":\"%s\",\"content\":\"%s\",\"role\":\"%s\",\"sessionId\":\"%s\",\"streaming\":%s,\"streamComplete\":%s}",
                    message.getType(),
                    escapeJson(message.getContent()),
                    message.getRole(),
                    message.getSessionId(),
                    message.isStreaming(),
                    message.isStreamComplete()
                );
            } else {
                // 非流式消息使用正常序列化
                messageJson = objectMapper.writeValueAsString(message);
            }
            
            session.sendMessage(new TextMessage(messageJson));
            
        } catch (Exception e) {
            logger.error("发送流式消息失败，sessionId: {}", sessionId, e);
            throw new IOException("发送流式消息失败", e);
        }
    }
    
    /**
     * 转义JSON字符串
     */
    private String escapeJson(String input) {
        if (input == null) return "";
        return input.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }

    /**
     * 生成基于日期的会话ID (格式: YYYYMMDD)
     */
    private String generateSessionId() {
        return java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
    }

    /**
     * 处理Ollama服务状态检查
     */
    private void handleOllamaStatusCheck(WebSocketSession session, String sessionId) {
        try {
            boolean isAvailable = ollamaService.isServiceAvailable();

            ChatMessage statusMessage = new ChatMessage();
            statusMessage.setType("system");
            statusMessage.setContent("Ollama服务状态已更新");
            statusMessage.setSessionId(sessionId);
            statusMessage.setMetadata(Map.of("ollama_status", isAvailable ? "available" : "unavailable"));

            sendMessage(session, statusMessage);

        } catch (Exception e) {
            logger.error("检查Ollama服务状态时发生错误，会话ID: {}", sessionId, e);

            try {
                ChatMessage errorMessage = new ChatMessage();
                errorMessage.setType("system");
                errorMessage.setContent("检查服务状态时发生错误");
                errorMessage.setSessionId(sessionId);
                errorMessage.setMetadata(Map.of("ollama_status", "unavailable"));

                sendMessage(session, errorMessage);
            } catch (IOException ex) {
                logger.error("发送错误消息失败", ex);
            }
        }
    }

    /**
     * 处理思考显示切换
     */
    private void handleThinkingToggle(WebSocketSession session, String sessionId, ChatMessage message) {
        try {
            Boolean showThinking = (Boolean) message.getMetadata().get("showThinking");
            if (showThinking == null) {
                showThinking = false;
            }
            
            // 设置用户偏好
            chatService.setUserThinkingPreference(sessionId, showThinking);
            
            // 发送确认消息
            ChatMessage confirmMessage = new ChatMessage();
            confirmMessage.setType("system");
            confirmMessage.setContent(showThinking ? "已开启思考过程显示" : "已关闭思考过程显示");
            confirmMessage.setSessionId(sessionId);
            confirmMessage.setMetadata(Map.of(
                "thinking_toggle", "confirmed",
                "showThinking", showThinking
            ));
            
            sendMessage(session, confirmMessage);
            
            logger.info("用户切换思考显示状态 - sessionId: {}, showThinking: {}", sessionId, showThinking);
            
        } catch (Exception e) {
            logger.error("处理思考显示切换时发生错误，会话 ID: {}", sessionId, e);
            
            try {
                ChatMessage errorMessage = new ChatMessage();
                errorMessage.setType("system");
                errorMessage.setContent("切换思考显示状态失败");
                errorMessage.setSessionId(sessionId);
                
                sendMessage(session, errorMessage);
            } catch (IOException ex) {
                logger.error("发送错误消息失败", ex);
            }
        }
    }
    
    /**
     * 处理思考过程保存切换
     */
    private void handleThinkingSaveToggle(WebSocketSession session, String sessionId, ChatMessage message) {
        try {
            Boolean saveThinking = (Boolean) message.getMetadata().get("saveThinking");
            if (saveThinking == null) {
                saveThinking = false;
            }
            
            // 设置用户偏好
            chatService.setUserThinkingSavePreference(sessionId, saveThinking);
            
            // 发送确认消息
            ChatMessage confirmMessage = new ChatMessage();
            confirmMessage.setType("system");
            confirmMessage.setContent(saveThinking ? "已开启思考过程保存到历史记录" : "已关闭思考过程保存到历史记录");
            confirmMessage.setSessionId(sessionId);
            confirmMessage.setMetadata(Map.of(
                "thinking_save_toggle", "confirmed",
                "saveThinking", saveThinking
            ));
            
            sendMessage(session, confirmMessage);
            
            logger.info("用户切换思考过程保存状态 - sessionId: {}, saveThinking: {}", sessionId, saveThinking);
            
        } catch (Exception e) {
            logger.error("处理思考过程保存切换时发生错误，会话 ID: {}", sessionId, e);
            
            try {
                ChatMessage errorMessage = new ChatMessage();
                errorMessage.setType("system");
                errorMessage.setContent("切换思考过程保存状态失败");
                errorMessage.setSessionId(sessionId);
                
                sendMessage(session, errorMessage);
            } catch (IOException ex) {
                logger.error("发送错误消息失败", ex);
            }
        }
    }
    
    /**
     * 获取活跃会话数量
     */
    public int getActiveSessionCount() {
        return sessions.size();
    }
}

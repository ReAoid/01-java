package com.chatbot.websocket;

import com.chatbot.model.ChatMessage;
import com.chatbot.service.ChatService;
import com.chatbot.service.OllamaService;
import com.chatbot.util.IdUtil;
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

    // 生成唯一会话ID
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
                logger.debug("消息载荷长度: {}, sessionId: {}", payload.length(), sessionId);

                ChatMessage chatMessage = objectMapper.readValue(payload, ChatMessage.class);
                chatMessage.setSessionId(sessionId);

                logger.info("收到用户消息，sessionId: {}, messageType: {}, contentLength: {}，content：{}",
                        sessionId, chatMessage.getType(),
                        chatMessage.getContent() != null ? chatMessage.getContent().length() : 0,
                        chatMessage.getContent() != null ? chatMessage.getContent() : "");

                // 检查是否是系统命令
                if ("system".equals(chatMessage.getType()) &&
                        chatMessage.getMetadata() != null &&
                        "check_service".equals(chatMessage.getMetadata().get("action"))) {

                    logger.debug("处理Ollama服务状态检查命令，sessionId: {}", sessionId);
                    // 处理Ollama服务状态检查
                    handleOllamaStatusCheck(session, sessionId);
                    return;
                }

                // 处理普通消息并获取回复（流式处理）
                logger.debug("开始处理普通消息，sessionId: {}", sessionId);
                chatService.processMessage(chatMessage, response -> {
                    try {
                        sendMessage(session, response);
                        logger.debug("向客户端发送响应消息，sessionId: {}, responseType: {}",
                                sessionId, response.getType());
                    } catch (IOException e) {
                        logger.error("发送消息失败，sessionId: {}", sessionId, e);
                    }
                });

                long processingTime = System.currentTimeMillis() - startTime;
                logger.debug("WebSocket消息处理完成，sessionId: {}, 处理时间: {}ms", sessionId, processingTime);

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
            logger.debug("消息发送成功，sessionId: {}, messageType: {}, messageLength: {}",
                    message.getSessionId(), message.getType(), messageJson.length());
        } else {
            logger.warn("WebSocket会话已关闭，无法发送消息，sessionId: {}", message.getSessionId());
        }
    }

    /**
     * 生成唯一会话ID (使用IdUtil工具类)
     */
    private String generateSessionId() {
        return IdUtil.sessionId();
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

            logger.debug("Ollama服务状态检查完成，会话ID: {}, 状态: {}", sessionId, isAvailable ? "可用" : "不可用");

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
     * 获取活跃会话数量
     */
    public int getActiveSessionCount() {
        return sessions.size();
    }
}

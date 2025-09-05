package com.chatbot.websocket;

import com.chatbot.model.ChatMessage;
import com.chatbot.service.ChatService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;

import java.io.IOException;
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
    private final ObjectMapper objectMapper;
    
    // 存储活跃的WebSocket会话
    private final ConcurrentHashMap<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    
    // 生成唯一会话ID
    private final AtomicLong sessionIdGenerator = new AtomicLong(0);
    
    public ChatWebSocketHandler(ChatService chatService) {
        this.chatService = chatService;
        this.objectMapper = new ObjectMapper();
    }
    
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String sessionId = generateSessionId();
        session.getAttributes().put("sessionId", sessionId);
        sessions.put(sessionId, session);
        
        logger.info("WebSocket连接建立，会话ID: {}", sessionId);
        
        // 发送连接成功消息
        ChatMessage welcomeMessage = new ChatMessage();
        welcomeMessage.setType("system");
        welcomeMessage.setContent("连接成功，欢迎使用AI聊天机器人！");
        welcomeMessage.setSessionId(sessionId);
        
        sendMessage(session, welcomeMessage);
    }
    
    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
        String sessionId = (String) session.getAttributes().get("sessionId");
        
        if (message instanceof TextMessage textMessage) {
            try {
                // 解析收到的消息
                ChatMessage chatMessage = objectMapper.readValue(textMessage.getPayload(), ChatMessage.class);
                chatMessage.setSessionId(sessionId);
                
                logger.debug("收到消息，会话ID: {}, 内容: {}", sessionId, chatMessage.getContent());
                
                // 处理消息并获取回复（流式处理）
                chatService.processMessage(chatMessage, response -> {
                    try {
                        sendMessage(session, response);
                    } catch (IOException e) {
                        logger.error("发送消息失败，会话ID: {}", sessionId, e);
                    }
                });
                
            } catch (Exception e) {
                logger.error("处理消息时发生错误，会话ID: {}", sessionId, e);
                
                // 发送错误消息
                ChatMessage errorMessage = new ChatMessage();
                errorMessage.setType("error");
                errorMessage.setContent("处理消息时发生错误，请稍后重试");
                errorMessage.setSessionId(sessionId);
                
                sendMessage(session, errorMessage);
            }
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
        }
    }
    
    /**
     * 生成唯一会话ID
     */
    private String generateSessionId() {
        return "session_" + sessionIdGenerator.incrementAndGet() + "_" + System.currentTimeMillis();
    }
    
    /**
     * 获取活跃会话数量
     */
    public int getActiveSessionCount() {
        return sessions.size();
    }
}

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
        this.chatService = chatService;
        this.ollamaService = ollamaService;
        this.objectMapper = objectMapper;
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
                
                // 检查是否是系统命令
                if ("system".equals(chatMessage.getType()) && 
                    chatMessage.getMetadata() != null && 
                    "check_service".equals(chatMessage.getMetadata().get("action"))) {
                    
                    // 处理Ollama服务状态检查
                    handleOllamaStatusCheck(session, sessionId);
                    return;
                }
                
                // 处理普通消息并获取回复（流式处理）
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

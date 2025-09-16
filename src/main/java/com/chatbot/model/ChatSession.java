package com.chatbot.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * 聊天会话模型
 * 管理会话状态和对话历史
 */
public class ChatSession {
    
    /**
     * 会话ID
     */
    private String sessionId;
    
    /**
     * 会话创建时间
     */
    private LocalDateTime createdAt;
    
    /**
     * 最后活动时间
     */
    private LocalDateTime lastActiveAt;
    
    /**
     * 对话历史（短期记忆）
     */
    private ConcurrentLinkedQueue<ChatMessage> messageHistory;
    
    /**
     * 当前上下文token数量
     */
    private int currentTokenCount;
    
    /**
     * 会话状态：active, idle, expired
     */
    private String status;
    
    /**
     * 用户偏好设置
     */
    private UserPreferences userPreferences;
    
    /**
     * 当前使用的人设ID
     */
    private String currentPersonaId;
    
    /**
     * 会话元数据（包括用户偏好设置等）
     */
    private Map<String, Object> metadata;
    
    public ChatSession(String sessionId) {
        this.sessionId = sessionId;
        this.createdAt = LocalDateTime.now();
        this.lastActiveAt = LocalDateTime.now();
        this.messageHistory = new ConcurrentLinkedQueue<>();
        this.currentTokenCount = 0;
        this.status = "active";
        this.userPreferences = new UserPreferences();
        this.metadata = new ConcurrentHashMap<>();
    }
    
    /**
     * 添加消息到历史记录
     */
    public void addMessage(ChatMessage message) {
        messageHistory.offer(message);
        updateLastActiveTime();
        
        // 简单的token计数估算（实际应该使用tokenizer）
        if (message.getContent() != null) {
            currentTokenCount += message.getContent().length() / 4; // 粗略估算
        }
    }
    
    /**
     * 获取最近N条消息
     */
    public List<ChatMessage> getRecentMessages(int count) {
        List<ChatMessage> messages = new ArrayList<>(messageHistory);
        int start = Math.max(0, messages.size() - count);
        return messages.subList(start, messages.size());
    }
    
    /**
     * 更新最后活动时间
     */
    public void updateLastActiveTime() {
        this.lastActiveAt = LocalDateTime.now();
    }
    
    /**
     * 检查会话是否过期
     */
    public boolean isExpired(int timeoutSeconds) {
        return lastActiveAt.isBefore(LocalDateTime.now().minusSeconds(timeoutSeconds));
    }
    
    // Getters and Setters
    public String getSessionId() {
        return sessionId;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public LocalDateTime getLastActiveAt() {
        return lastActiveAt;
    }
    
    public ConcurrentLinkedQueue<ChatMessage> getMessageHistory() {
        return messageHistory;
    }
    
    public int getCurrentTokenCount() {
        return currentTokenCount;
    }
    
    public void setCurrentTokenCount(int currentTokenCount) {
        this.currentTokenCount = currentTokenCount;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public UserPreferences getUserPreferences() {
        return userPreferences;
    }
    
    public void setUserPreferences(UserPreferences userPreferences) {
        this.userPreferences = userPreferences;
    }
    
    public String getCurrentPersonaId() {
        return currentPersonaId;
    }
    
    public void setCurrentPersonaId(String currentPersonaId) {
        this.currentPersonaId = currentPersonaId;
    }
    
    public Map<String, Object> getMetadata() {
        return metadata;
    }
    
    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
    
}

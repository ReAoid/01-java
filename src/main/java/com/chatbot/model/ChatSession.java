package com.chatbot.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
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
    
    public ChatSession(String sessionId) {
        this.sessionId = sessionId;
        this.createdAt = LocalDateTime.now();
        this.lastActiveAt = LocalDateTime.now();
        this.messageHistory = new ConcurrentLinkedQueue<>();
        this.currentTokenCount = 0;
        this.status = "active";
        this.userPreferences = new UserPreferences();
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
    
    /**
     * 用户偏好设置内部类
     */
    public static class UserPreferences {
        private String language = "zh-CN";
        private boolean enableVoice = false;
        private String preferredTtsVoice;
        private double responseSpeed = 1.0;
        
        // Getters and Setters
        public String getLanguage() {
            return language;
        }
        
        public void setLanguage(String language) {
            this.language = language;
        }
        
        public boolean isEnableVoice() {
            return enableVoice;
        }
        
        public void setEnableVoice(boolean enableVoice) {
            this.enableVoice = enableVoice;
        }
        
        public String getPreferredTtsVoice() {
            return preferredTtsVoice;
        }
        
        public void setPreferredTtsVoice(String preferredTtsVoice) {
            this.preferredTtsVoice = preferredTtsVoice;
        }
        
        public double getResponseSpeed() {
            return responseSpeed;
        }
        
        public void setResponseSpeed(double responseSpeed) {
            this.responseSpeed = responseSpeed;
        }
    }
}

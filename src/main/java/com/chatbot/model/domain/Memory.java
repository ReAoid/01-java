package com.chatbot.model.domain;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 记忆模型
 * 存储用户的长期记忆信息
 */
public class Memory {
    
    /**
     * 记忆ID
     */
    private String memoryId;
    
    /**
     * 会话ID
     */
    private String sessionId;
    
    /**
     * 记忆内容
     */
    private String content;
    
    /**
     * 记忆类型：fact(事实), preference(偏好), relationship(关系), event(事件)
     */
    private String type;
    
    /**
     * 重要性评分 (1-10)
     */
    private int importance;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    
    /**
     * 最后访问时间
     */
    private LocalDateTime lastAccessedAt;
    
    /**
     * 访问次数
     */
    private int accessCount;
    
    /**
     * 是否激活
     */
    private boolean active;
    
    /**
     * 关联的关键词
     */
    private String[] keywords;
    
    /**
     * 额外元数据
     */
    private Map<String, Object> metadata;
    
    public Memory() {
        this.createdAt = LocalDateTime.now();
        this.lastAccessedAt = LocalDateTime.now();
        this.accessCount = 0;
        this.active = true;
        this.importance = 5; // 默认中等重要性
    }
    
    public Memory(String sessionId, String content, String type) {
        this();
        this.sessionId = sessionId;
        this.content = content;
        this.type = type;
        this.memoryId = generateMemoryId();
    }
    
    /**
     * 更新访问信息
     */
    public void updateAccess() {
        this.lastAccessedAt = LocalDateTime.now();
        this.accessCount++;
    }
    
    /**
     * 生成记忆ID
     */
    private String generateMemoryId() {
        return "mem_" + System.currentTimeMillis() + "_" + System.nanoTime();
    }
    
    // Getters and Setters
    public String getMemoryId() {
        return memoryId;
    }
    
    public void setMemoryId(String memoryId) {
        this.memoryId = memoryId;
    }
    
    public String getSessionId() {
        return sessionId;
    }
    
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public int getImportance() {
        return importance;
    }
    
    public void setImportance(int importance) {
        this.importance = Math.max(1, Math.min(10, importance)); // 限制在1-10范围内
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getLastAccessedAt() {
        return lastAccessedAt;
    }
    
    public void setLastAccessedAt(LocalDateTime lastAccessedAt) {
        this.lastAccessedAt = lastAccessedAt;
    }
    
    public int getAccessCount() {
        return accessCount;
    }
    
    public void setAccessCount(int accessCount) {
        this.accessCount = accessCount;
    }
    
    public boolean isActive() {
        return active;
    }
    
    public void setActive(boolean active) {
        this.active = active;
    }
    
    public String[] getKeywords() {
        return keywords;
    }
    
    public void setKeywords(String[] keywords) {
        this.keywords = keywords;
    }
    
    public Map<String, Object> getMetadata() {
        return metadata;
    }
    
    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
}

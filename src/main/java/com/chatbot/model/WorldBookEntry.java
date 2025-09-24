package com.chatbot.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 世界书条目模型
 * 表示世界书中的一个知识条目
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WorldBookEntry {
    
    /**
     * 条目ID
     */
    private String entryId;
    
    /**
     * 条目标题/关键词
     */
    private String title;
    
    /**
     * 条目内容
     */
    private String content;
    
    /**
     * 条目类型：manual(手动配置), extracted(自动提取)
     */
    private String type;
    
    /**
     * 触发关键词列表
     */
    private List<String> keywords;
    
    /**
     * 重要性评分 (1-10)
     */
    private int importance;
    
    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
    
    /**
     * 最后更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
    
    /**
     * 最后使用时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastUsedAt;
    
    /**
     * 使用次数
     */
    private int usageCount;
    
    /**
     * 是否激活
     */
    private boolean active;
    
    /**
     * 相关性阈值（当用户输入的相关性超过此值时才会被激活）
     */
    private double relevanceThreshold;
    
    /**
     * 所属会话ID（对于extracted类型）
     */
    private String sessionId;
    
    public WorldBookEntry() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.usageCount = 0;
        this.active = true;
        this.importance = 5;
        this.relevanceThreshold = 0.3;
    }
    
    public WorldBookEntry(String title, String content, String type) {
        this();
        this.title = title;
        this.content = content;
        this.type = type;
        this.entryId = generateEntryId();
    }
    
    /**
     * 更新使用信息
     */
    public void updateUsage() {
        this.lastUsedAt = LocalDateTime.now();
        this.usageCount++;
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * 生成条目ID
     */
    private String generateEntryId() {
        return type + "_" + System.currentTimeMillis() + "_" + System.nanoTime();
    }
    
    // Getters and Setters
    public String getEntryId() {
        return entryId;
    }
    
    public void setEntryId(String entryId) {
        this.entryId = entryId;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
        this.updatedAt = LocalDateTime.now();
    }
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public List<String> getKeywords() {
        return keywords;
    }
    
    public void setKeywords(List<String> keywords) {
        this.keywords = keywords;
    }
    
    public int getImportance() {
        return importance;
    }
    
    public void setImportance(int importance) {
        this.importance = Math.max(1, Math.min(10, importance));
        this.updatedAt = LocalDateTime.now();
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public LocalDateTime getLastUsedAt() {
        return lastUsedAt;
    }
    
    public void setLastUsedAt(LocalDateTime lastUsedAt) {
        this.lastUsedAt = lastUsedAt;
    }
    
    public int getUsageCount() {
        return usageCount;
    }
    
    public void setUsageCount(int usageCount) {
        this.usageCount = usageCount;
    }
    
    public boolean isActive() {
        return active;
    }
    
    public void setActive(boolean active) {
        this.active = active;
        this.updatedAt = LocalDateTime.now();
    }
    
    public double getRelevanceThreshold() {
        return relevanceThreshold;
    }
    
    public void setRelevanceThreshold(double relevanceThreshold) {
        this.relevanceThreshold = relevanceThreshold;
        this.updatedAt = LocalDateTime.now();
    }
    
    public String getSessionId() {
        return sessionId;
    }
    
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
    
    @Override
    public String toString() {
        return "WorldBookEntry{" +
                "entryId='" + entryId + '\'' +
                ", title='" + title + '\'' +
                ", type='" + type + '\'' +
                ", importance=" + importance +
                ", active=" + active +
                '}';
    }
}

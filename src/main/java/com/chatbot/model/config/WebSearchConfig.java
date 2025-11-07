package com.chatbot.model.config;

/**
 * 联网搜索配置
 */
public class WebSearchConfig {
    
    private boolean enabled = false;
    private int maxResults = 5;
    private int timeout = 10;
    private String engine = "duckduckgo";
    // private String region = "zh-CN";  // 新增字段，暂时注释
    
    /**
     * 验证配置有效性
     */
    public boolean validate() {
        if (maxResults <= 0 || maxResults > 20) return false;
        if (timeout <= 0) return false;
        return true;
    }
    
    // Getters & Setters
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    public int getMaxResults() {
        return maxResults;
    }
    
    public void setMaxResults(int maxResults) {
        this.maxResults = maxResults;
    }
    
    public int getTimeout() {
        return timeout;
    }
    
    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }
    
    public String getEngine() {
        return engine;
    }
    
    public void setEngine(String engine) {
        this.engine = engine;
    }
}


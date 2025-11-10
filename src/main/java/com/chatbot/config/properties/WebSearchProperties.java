package com.chatbot.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 联网搜索配置
 */
@Component
@ConfigurationProperties(prefix = "app.web-search")
public class WebSearchProperties {
    private boolean enabled;
    private int maxResults;
    private int timeoutSeconds;
    private String defaultEngine;
    private boolean enableFallback;
    
    // API Keys (如果需要)
    private String serpApiKey;
    private String bingApiKey;
    
    // Getters and Setters
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    
    public int getMaxResults() { return maxResults; }
    public void setMaxResults(int maxResults) { this.maxResults = maxResults; }
    
    public int getTimeoutSeconds() { return timeoutSeconds; }
    public void setTimeoutSeconds(int timeoutSeconds) { this.timeoutSeconds = timeoutSeconds; }
    
    public String getDefaultEngine() { return defaultEngine; }
    public void setDefaultEngine(String defaultEngine) { this.defaultEngine = defaultEngine; }
    
    public boolean isEnableFallback() { return enableFallback; }
    public void setEnableFallback(boolean enableFallback) { this.enableFallback = enableFallback; }
    
    public String getSerpApiKey() { return serpApiKey; }
    public void setSerpApiKey(String serpApiKey) { this.serpApiKey = serpApiKey; }
    
    public String getBingApiKey() { return bingApiKey; }
    public void setBingApiKey(String bingApiKey) { this.bingApiKey = bingApiKey; }
}


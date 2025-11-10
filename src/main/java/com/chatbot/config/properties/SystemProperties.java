package com.chatbot.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 系统核心配置
 */
@Component
@ConfigurationProperties(prefix = "app.system")
public class SystemProperties {
    private int maxContextTokens;
    private int sessionTimeout;
    private WebSocketConfig websocket = new WebSocketConfig();
    
    // Getters and Setters
    public int getMaxContextTokens() { return maxContextTokens; }
    public void setMaxContextTokens(int maxContextTokens) { this.maxContextTokens = maxContextTokens; }
    
    public int getSessionTimeout() { return sessionTimeout; }
    public void setSessionTimeout(int sessionTimeout) { this.sessionTimeout = sessionTimeout; }
    
    public WebSocketConfig getWebsocket() { return websocket; }
    public void setWebsocket(WebSocketConfig websocket) { this.websocket = websocket; }
    
    public static class WebSocketConfig {
        private int pingInterval;
        private int maxReconnectAttempts;
        
        public int getPingInterval() { return pingInterval; }
        public void setPingInterval(int pingInterval) { this.pingInterval = pingInterval; }
        
        public int getMaxReconnectAttempts() { return maxReconnectAttempts; }
        public void setMaxReconnectAttempts(int maxReconnectAttempts) { this.maxReconnectAttempts = maxReconnectAttempts; }
    }
}


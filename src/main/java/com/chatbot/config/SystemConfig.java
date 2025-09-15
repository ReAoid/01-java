package com.chatbot.config;

import com.chatbot.model.WebSocketConfig;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 系统核心配置类
 * 支持热重载和分层配置管理
 */
@Component
@ConfigurationProperties(prefix = "system")
public class SystemConfig {
    
    /**
     * 最大上下文token数量
     */
    private int maxContextTokens = 8192;
    
    /**
     * 会话超时时间（秒）
     */
    private int sessionTimeout = 3600;
    
    /**
     * WebSocket配置
     */
    private WebSocketConfig websocket = new WebSocketConfig();
    
    // Getters and Setters
    public int getMaxContextTokens() {
        return maxContextTokens;
    }
    
    public void setMaxContextTokens(int maxContextTokens) {
        this.maxContextTokens = maxContextTokens;
    }
    
    public int getSessionTimeout() {
        return sessionTimeout;
    }
    
    public void setSessionTimeout(int sessionTimeout) {
        this.sessionTimeout = sessionTimeout;
    }
    
    public WebSocketConfig getWebsocket() {
        return websocket;
    }
    
    public void setWebsocket(WebSocketConfig websocket) {
        this.websocket = websocket;
    }
    
}

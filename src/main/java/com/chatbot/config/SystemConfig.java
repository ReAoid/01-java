package com.chatbot.config;

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
    
    /**
     * WebSocket配置子类
     */
    public static class WebSocketConfig {
        /**
         * 心跳间隔（秒）
         */
        private int pingInterval = 30;
        
        /**
         * 最大重连尝试次数
         */
        private int maxReconnectAttempts = 5;
        
        public int getPingInterval() {
            return pingInterval;
        }
        
        public void setPingInterval(int pingInterval) {
            this.pingInterval = pingInterval;
        }
        
        public int getMaxReconnectAttempts() {
            return maxReconnectAttempts;
        }
        
        public void setMaxReconnectAttempts(int maxReconnectAttempts) {
            this.maxReconnectAttempts = maxReconnectAttempts;
        }
    }
}

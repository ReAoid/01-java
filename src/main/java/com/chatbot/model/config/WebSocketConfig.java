package com.chatbot.model.config;

/**
 * WebSocket配置
 */
public class WebSocketConfig {
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

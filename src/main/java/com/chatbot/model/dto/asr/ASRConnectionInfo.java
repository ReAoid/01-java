package com.chatbot.model.dto.asr;

import java.io.Serializable;

/**
 * ASR连接信息DTO
 * 用于ASR WebSocket连接的配置和状态
 */
public class ASRConnectionInfo implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * ASR服务URL
     */
    private final String serviceUrl;
    
    /**
     * 连接状态：connecting, connected, disconnected, error
     */
    private final String status;
    
    /**
     * 会话ID
     */
    private final String sessionId;
    
    /**
     * 重连次数
     */
    private final int reconnectCount;
    
    /**
     * 最后一次错误信息
     */
    private final String lastError;
    
    public ASRConnectionInfo(String serviceUrl, String status, String sessionId, 
                             int reconnectCount, String lastError) {
        this.serviceUrl = serviceUrl;
        this.status = status;
        this.sessionId = sessionId;
        this.reconnectCount = reconnectCount;
        this.lastError = lastError;
    }
    
    // ========== Getters ==========
    
    public String getServiceUrl() {
        return serviceUrl;
    }
    
    public String getStatus() {
        return status;
    }
    
    public String getSessionId() {
        return sessionId;
    }
    
    public int getReconnectCount() {
        return reconnectCount;
    }
    
    public String getLastError() {
        return lastError;
    }
    
    public boolean isConnected() {
        return "connected".equals(status);
    }
    
    @Override
    public String toString() {
        return "ASRConnectionInfo{" +
                "serviceUrl='" + serviceUrl + '\'' +
                ", status='" + status + '\'' +
                ", sessionId='" + sessionId + '\'' +
                ", reconnectCount=" + reconnectCount +
                ", lastError='" + lastError + '\'' +
                '}';
    }
}


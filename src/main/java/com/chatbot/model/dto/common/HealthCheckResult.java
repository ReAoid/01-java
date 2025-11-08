package com.chatbot.model.dto.common;

import java.util.HashMap;
import java.util.Map;

/**
 * 统一的健康检查结果
 * 适用于所有服务的健康检查（TTS、ASR、OCR、WebSearch等）
 */
public class HealthCheckResult {
    private final String serviceName;      // 服务名称
    private final boolean healthy;         // 是否健康
    private final String status;           // 状态：UP、DOWN、DEGRADED
    private final long responseTime;       // 响应时间（毫秒）
    private final String version;          // 服务版本（可选）
    private final Map<String, Object> details; // 详细信息
    private final long timestamp;
    
    private HealthCheckResult(Builder builder) {
        this.serviceName = builder.serviceName;
        this.healthy = builder.healthy;
        this.status = builder.status;
        this.responseTime = builder.responseTime;
        this.version = builder.version;
        this.details = builder.details != null ? builder.details : new HashMap<>();
        this.timestamp = System.currentTimeMillis();
    }
    
    /**
     * Builder模式构建器
     */
    public static class Builder {
        private String serviceName;
        private boolean healthy;
        private String status;
        private long responseTime;
        private String version;
        private Map<String, Object> details = new HashMap<>();
        
        public Builder serviceName(String serviceName) {
            this.serviceName = serviceName;
            return this;
        }
        
        public Builder healthy(boolean healthy) {
            this.healthy = healthy;
            // 自动设置status
            if (this.status == null) {
                this.status = healthy ? "UP" : "DOWN";
            }
            return this;
        }
        
        public Builder status(String status) {
            this.status = status;
            return this;
        }
        
        public Builder responseTime(long responseTime) {
            this.responseTime = responseTime;
            return this;
        }
        
        public Builder version(String version) {
            this.version = version;
            return this;
        }
        
        public Builder detail(String key, Object value) {
            this.details.put(key, value);
            return this;
        }
        
        public Builder details(Map<String, Object> details) {
            if (details != null) {
                this.details.putAll(details);
            }
            return this;
        }
        
        public HealthCheckResult build() {
            if (serviceName == null) {
                throw new IllegalStateException("serviceName is required");
            }
            return new HealthCheckResult(this);
        }
    }
    
    // Getters
    public String getServiceName() { 
        return serviceName; 
    }
    
    public boolean isHealthy() { 
        return healthy; 
    }
    
    public String getStatus() { 
        return status; 
    }
    
    public long getResponseTime() { 
        return responseTime; 
    }
    
    public String getVersion() { 
        return version; 
    }
    
    public Map<String, Object> getDetails() { 
        return details; 
    }
    
    public long getTimestamp() { 
        return timestamp; 
    }
    
    /**
     * 获取详细信息中的某个值
     */
    public Object getDetail(String key) {
        return details.get(key);
    }
    
    @Override
    public String toString() {
        return String.format("HealthCheck{service='%s', status=%s, healthy=%s, responseTime=%dms, version='%s'}",
                           serviceName, status, healthy, responseTime, version);
    }
}


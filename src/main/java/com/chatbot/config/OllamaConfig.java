package com.chatbot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Ollama配置类
 * 管理Ollama服务的连接参数和模型配置
 */
@Component
@ConfigurationProperties(prefix = "ollama")
public class OllamaConfig {
    
    /**
     * Ollama服务基础URL
     */
    private String baseUrl = "http://localhost:11434";
    
    /**
     * 使用的模型名称
     */
    private String model = "qwen3:4b";
    
    /**
     * 请求超时时间（毫秒）
     */
    private int timeout = 30000;
    
    /**
     * 最大生成的token数量
     */
    private int maxTokens = 4096;
    
    /**
     * 温度参数（控制随机性）
     */
    private double temperature = 0.7;
    
    /**
     * 是否启用流式响应
     */
    private boolean stream = true;
    
    // Getters and Setters
    public String getBaseUrl() {
        return baseUrl;
    }
    
    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }
    
    public String getModel() {
        return model;
    }
    
    public void setModel(String model) {
        this.model = model;
    }
    
    public int getTimeout() {
        return timeout;
    }
    
    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }
    
    public int getMaxTokens() {
        return maxTokens;
    }
    
    public void setMaxTokens(int maxTokens) {
        this.maxTokens = maxTokens;
    }
    
    public double getTemperature() {
        return temperature;
    }
    
    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }
    
    public boolean isStream() {
        return stream;
    }
    
    public void setStream(boolean stream) {
        this.stream = stream;
    }
    
    /**
     * 获取生成API的完整URL
     */
    public String getGenerateUrl() {
        return baseUrl + "/api/generate";
    }
    
    /**
     * 获取聊天API的完整URL
     */
    public String getChatUrl() {
        return baseUrl + "/api/chat";
    }
    
    /**
     * 获取模型列表API的完整URL
     */
    public String getModelsUrl() {
        return baseUrl + "/api/tags";
    }
}

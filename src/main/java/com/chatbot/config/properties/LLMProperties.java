package com.chatbot.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * LLM (大语言模型) 配置
 * 支持 Ollama 及其他 LLM 服务
 */
@Component
@ConfigurationProperties(prefix = "app.ollama")
public class LLMProperties {
    private String baseUrl;
    private String model;
    private int timeout;
    private int maxTokens;
    private double temperature;
    private boolean stream;
    
    // Getters and Setters
    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    
    public int getTimeout() { return timeout; }
    public void setTimeout(int timeout) { this.timeout = timeout; }
    
    public int getMaxTokens() { return maxTokens; }
    public void setMaxTokens(int maxTokens) { this.maxTokens = maxTokens; }
    
    public double getTemperature() { return temperature; }
    public void setTemperature(double temperature) { this.temperature = temperature; }
    
    public boolean isStream() { return stream; }
    public void setStream(boolean stream) { this.stream = stream; }
    
    // 辅助方法
    public String getGenerateUrl() { return baseUrl + "/api/generate"; }
    public String getChatUrl() { return baseUrl + "/api/chat"; }
    public String getModelsUrl() { return baseUrl + "/api/tags"; }
}


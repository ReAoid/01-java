package com.chatbot.config;

import com.chatbot.model.EndpointsConfig;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Python API配置类
 * 用于配置各种Python服务的API端点
 */
@Component
@ConfigurationProperties(prefix = "python.api")
public class PythonApiConfig {
    
    /**
     * Python API基础URL
     */
    private String baseUrl = "http://localhost:5000";
    
    /**
     * API端点配置
     */
    private EndpointsConfig endpoints = new EndpointsConfig();
    
    public String getBaseUrl() {
        return baseUrl;
    }
    
    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }
    
    public EndpointsConfig getEndpoints() {
        return endpoints;
    }
    
    public void setEndpoints(EndpointsConfig endpoints) {
        this.endpoints = endpoints;
    }
    
}

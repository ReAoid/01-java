package com.chatbot.model;

/**
 * Ollama消息类
 * 用于Ollama API的消息格式
 */
public class OllamaMessage {
    public final String role;
    public final String content;
    
    public OllamaMessage(String role, String content) {
        this.role = role;
        this.content = content;
    }
    
    public String getRole() { 
        return role; 
    }
    
    public String getContent() { 
        return content; 
    }
}

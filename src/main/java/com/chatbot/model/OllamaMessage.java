package com.chatbot.model;

/**
 * Ollama消息类
 */
public class OllamaMessage {
    private final String role;
    private final String content;
    
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

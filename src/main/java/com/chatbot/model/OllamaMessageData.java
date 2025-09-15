package com.chatbot.model;

/**
 * Ollama消息数据类
 */
public class OllamaMessageData {
    public final String role;
    public final String content;
    
    public OllamaMessageData(String role, String content) {
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

package com.chatbot.model.record;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

/**
 * 简单消息类
 */
public class SimpleMessage {
    @JsonProperty("timestamp")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private LocalDateTime timestamp;
    
    @JsonProperty("role")
    private String role;
    
    @JsonProperty("content")
    private String content;
    
    // 构造函数
    public SimpleMessage() {}
    
    public SimpleMessage(LocalDateTime timestamp, String role, String content) {
        this.timestamp = timestamp;
        this.role = role;
        this.content = content;
    }
    
    // Getters and Setters
    public LocalDateTime getTimestamp() { 
        return timestamp; 
    }
    
    public void setTimestamp(LocalDateTime timestamp) { 
        this.timestamp = timestamp; 
    }
    
    public String getRole() { 
        return role; 
    }
    
    public void setRole(String role) { 
        this.role = role; 
    }
    
    public String getContent() { 
        return content; 
    }
    
    public void setContent(String content) { 
        this.content = content; 
    }
}

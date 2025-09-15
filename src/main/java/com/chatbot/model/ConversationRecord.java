package com.chatbot.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 简化版对话记录数据模型
 * 只保存核心的对话消息内容
 */
public class ConversationRecord {
    
    @JsonProperty("session_id")
    private String sessionId;
    
    @JsonProperty("messages")
    private List<SimpleMessage> messages;
    
    // 构造函数
    public ConversationRecord() {
        this.messages = new ArrayList<>();
    }
    
    public ConversationRecord(String sessionId) {
        this.sessionId = sessionId;
        this.messages = new ArrayList<>();
    }
    
    // Getters and Setters
    public String getSessionId() { 
        return sessionId; 
    }
    
    public void setSessionId(String sessionId) { 
        this.sessionId = sessionId; 
    }
    
    public List<SimpleMessage> getMessages() { 
        return messages; 
    }
    
    public void setMessages(List<SimpleMessage> messages) { 
        this.messages = messages; 
    }
    
    /**
     * 添加消息到记录中
     */
    public void addMessage(String role, String content) {
        SimpleMessage message = new SimpleMessage();
        message.setTimestamp(LocalDateTime.now());
        message.setRole(role);
        message.setContent(content);
        this.messages.add(message);
    }
    
    /**
     * 添加消息到记录中（指定时间戳）
     */
    public void addMessage(LocalDateTime timestamp, String role, String content) {
        SimpleMessage message = new SimpleMessage();
        message.setTimestamp(timestamp);
        message.setRole(role);
        message.setContent(content);
        this.messages.add(message);
    }
    
}

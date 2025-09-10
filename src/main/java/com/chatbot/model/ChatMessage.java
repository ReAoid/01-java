package com.chatbot.model;

import com.chatbot.util.IdUtil;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 聊天消息模型
 * 支持多种消息类型和元数据
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatMessage {
    
    /**
     * 消息类型：text, voice, image, system, error
     */
    private String type;
    
    /**
     * 消息内容
     */
    private String content;
    
    /**
     * 会话ID
     */
    private String sessionId;
    
    /**
     * 消息ID
     */
    private String messageId;
    
    /**
     * 发送者：user, assistant, system
     */
    private String sender;
    
    /**
     * 时间戳
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;
    
    /**
     * 是否为流式消息的一部分
     */
    private boolean streaming;
    
    /**
     * 流式消息是否完成
     */
    private boolean streamComplete;
    
    /**
     * 额外元数据
     */
    private Map<String, Object> metadata;
    
    public ChatMessage() {
        this.timestamp = LocalDateTime.now();
        this.messageId = generateMessageId();
    }
    
    // Getters and Setters
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public String getSessionId() {
        return sessionId;
    }
    
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
    
    public String getMessageId() {
        return messageId;
    }
    
    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }
    
    public String getSender() {
        return sender;
    }
    
    public void setSender(String sender) {
        this.sender = sender;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
    
    public boolean isStreaming() {
        return streaming;
    }
    
    public void setStreaming(boolean streaming) {
        this.streaming = streaming;
    }
    
    public boolean isStreamComplete() {
        return streamComplete;
    }
    
    public void setStreamComplete(boolean streamComplete) {
        this.streamComplete = streamComplete;
    }
    
    public Map<String, Object> getMetadata() {
        return metadata;
    }
    
    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
    
    /**
     * 生成唯一消息ID (使用IdUtil工具类)
     */
    private String generateMessageId() {
        return IdUtil.messageId();
    }
    
    @Override
    public String toString() {
        return "ChatMessage{type='" + type + "', content='" + content + "', sessionId='" + sessionId + "', sender='" + sender + "', timestamp=" + timestamp + "}";
    }
}

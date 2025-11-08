package com.chatbot.model.dto.websocket;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * WebSocket消息基础DTO
 * 所有WebSocket消息的统一基类，定义前后端通信的数据契约
 * 
 * 设计原则：
 * - 扁平化结构，避免深层嵌套
 * - 只包含前端需要的字段
 * - 使用多态实现不同类型消息
 * - 保持向后兼容性
 * 
 * @version 1.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.EXISTING_PROPERTY,
    property = "type",
    visible = true
)
@JsonSubTypes({
    @JsonSubTypes.Type(value = TextMessageDTO.class, name = "text"),
    @JsonSubTypes.Type(value = AudioMessageDTO.class, name = "audio"),
    @JsonSubTypes.Type(value = SystemMessageDTO.class, name = "system"),
    @JsonSubTypes.Type(value = ErrorMessageDTO.class, name = "error"),
    @JsonSubTypes.Type(value = ThinkingMessageDTO.class, name = "thinking")
})
public abstract class ChatMessageDTO implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 消息类型：text, audio, system, error, thinking
     */
    private String type;
    
    /**
     * 会话ID
     */
    private String sessionId;
    
    /**
     * 消息ID（可选）
     */
    private String messageId;
    
    /**
     * 时间戳
     */
    private LocalDateTime timestamp;
    
    /**
     * 渠道类型：chat_window, live2d
     */
    private String channelType;
    
    // ========== 构造函数 ==========
    
    protected ChatMessageDTO() {
        this.timestamp = LocalDateTime.now();
    }
    
    protected ChatMessageDTO(String type) {
        this.type = type;
        this.timestamp = LocalDateTime.now();
    }
    
    // ========== Getters & Setters ==========
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
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
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
    
    public String getChannelType() {
        return channelType;
    }
    
    public void setChannelType(String channelType) {
        this.channelType = channelType;
    }
    
    @Override
    public String toString() {
        return "ChatMessageDTO{" +
                "type='" + type + '\'' +
                ", sessionId='" + sessionId + '\'' +
                ", messageId='" + messageId + '\'' +
                ", timestamp=" + timestamp +
                ", channelType='" + channelType + '\'' +
                '}';
    }
}


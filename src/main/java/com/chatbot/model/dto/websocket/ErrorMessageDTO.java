package com.chatbot.model.dto.websocket;

import com.fasterxml.jackson.annotation.JsonAlias;

/**
 * 错误消息DTO
 * 用于错误信息的传输
 */
public class ErrorMessageDTO extends ChatMessageDTO {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 错误信息
     * 支持 "message" 和 "content" 两种字段名（向后兼容）
     */
    @JsonAlias("content")  // 兼容使用 "content" 的情况
    private String message;
    
    /**
     * 错误代码（可选）
     */
    private String errorCode;
    
    /**
     * 错误详情（可选）
     */
    private String details;
    
    public ErrorMessageDTO() {
        super("error");
    }
    
    public ErrorMessageDTO(String message) {
        this();
        this.message = message;
    }
    
    public ErrorMessageDTO(String errorCode, String message) {
        this();
        this.errorCode = errorCode;
        this.message = message;
    }
    
    // ========== Getters & Setters ==========
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
    
    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }
    
    public String getDetails() {
        return details;
    }
    
    public void setDetails(String details) {
        this.details = details;
    }
    
    @Override
    public String toString() {
        return "ErrorMessageDTO{" +
                "message='" + message + '\'' +
                ", errorCode='" + errorCode + '\'' +
                ", details='" + details + '\'' +
                ", " + super.toString() +
                '}';
    }
}


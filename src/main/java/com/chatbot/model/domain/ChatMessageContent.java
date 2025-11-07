package com.chatbot.model.domain;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 消息内容组件
 * 封装消息的文本内容和内容类型
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatMessageContent {
    
    /**
     * 文本内容
     */
    private String text;
    
    /**
     * 内容类型：text, html, markdown
     */
    private String contentType;
    
    public ChatMessageContent() {
        this.contentType = "text";
    }
    
    public ChatMessageContent(String text) {
        this.text = text;
        this.contentType = "text";
    }
    
    public ChatMessageContent(String text, String contentType) {
        this.text = text;
        this.contentType = contentType;
    }
    
    // Getters & Setters
    
    public String getText() {
        return text;
    }
    
    public void setText(String text) {
        this.text = text;
    }
    
    public String getContentType() {
        return contentType;
    }
    
    public void setContentType(String contentType) {
        this.contentType = contentType;
    }
    
    @Override
    public String toString() {
        return "ChatMessageContent{text='" + text + "', contentType='" + contentType + "'}";
    }
}


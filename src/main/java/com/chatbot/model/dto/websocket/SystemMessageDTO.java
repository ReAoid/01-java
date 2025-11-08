package com.chatbot.model.dto.websocket;

import com.fasterxml.jackson.annotation.JsonAlias;

import java.util.Map;

/**
 * 系统消息DTO
 * 用于系统通知、状态更新等
 */
public class SystemMessageDTO extends ChatMessageDTO {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 系统消息内容
     */
    private String content;
    
    /**
     * 系统消息子类型：welcome, status, info, interrupt_confirm等
     */
    private String subType;
    
    /**
     * 附加数据（可选）
     * 支持 "data" 和 "metadata" 两种字段名（向后兼容）
     */
    @JsonAlias("metadata")  // 兼容前端使用的 "metadata" 字段
    private Map<String, Object> data;
    
    public SystemMessageDTO() {
        super("system");
    }
    
    public SystemMessageDTO(String content) {
        this();
        this.content = content;
    }
    
    // ========== Getters & Setters ==========
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public String getSubType() {
        return subType;
    }
    
    public void setSubType(String subType) {
        this.subType = subType;
    }
    
    public Map<String, Object> getData() {
        return data;
    }
    
    public void setData(Map<String, Object> data) {
        this.data = data;
    }
    
    @Override
    public String toString() {
        return "SystemMessageDTO{" +
                "content='" + content + '\'' +
                ", subType='" + subType + '\'' +
                ", data=" + data +
                ", " + super.toString() +
                '}';
    }
}


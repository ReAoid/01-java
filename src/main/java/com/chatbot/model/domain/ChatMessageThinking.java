package com.chatbot.model.domain;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 思考数据组件
 * 封装AI的思考过程信息
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatMessageThinking {
    
    /**
     * 思考内容
     */
    private String thinkingContent;
    
    /**
     * 是否显示思考过程
     */
    private Boolean showThinking;
    
    /**
     * 思考类型：reasoning, planning, searching
     */
    private String thinkingType;
    
    public ChatMessageThinking() {
        this.showThinking = true;
    }
    
    public ChatMessageThinking(String thinkingContent) {
        this.thinkingContent = thinkingContent;
        this.showThinking = true;
    }
    
    public ChatMessageThinking(String thinkingContent, Boolean showThinking) {
        this.thinkingContent = thinkingContent;
        this.showThinking = showThinking;
    }
    
    // Getters & Setters
    
    public String getThinkingContent() {
        return thinkingContent;
    }
    
    public void setThinkingContent(String thinkingContent) {
        this.thinkingContent = thinkingContent;
    }
    
    public Boolean getShowThinking() {
        return showThinking;
    }
    
    public void setShowThinking(Boolean showThinking) {
        this.showThinking = showThinking;
    }
    
    public String getThinkingType() {
        return thinkingType;
    }
    
    public void setThinkingType(String thinkingType) {
        this.thinkingType = thinkingType;
    }
    
    @Override
    public String toString() {
        return "ChatMessageThinking{thinkingContent='" + thinkingContent + 
               "', showThinking=" + showThinking + "}";
    }
}


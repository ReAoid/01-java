package com.chatbot.model.domain;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 同步状态组件
 * 管理文本和音频的准备状态
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatMessageSync {
    
    /**
     * 文本是否准备就绪
     */
    private Boolean textReady;
    
    /**
     * 音频是否准备就绪
     */
    private Boolean audioReady;
    
    public ChatMessageSync() {}
    
    /**
     * 检查文本和音频是否都准备就绪
     */
    public boolean isBothReady() {
        return Boolean.TRUE.equals(textReady) && Boolean.TRUE.equals(audioReady);
    }
    
    // Getters & Setters
    
    public Boolean getTextReady() {
        return textReady;
    }
    
    public void setTextReady(Boolean textReady) {
        this.textReady = textReady;
    }
    
    public Boolean getAudioReady() {
        return audioReady;
    }
    
    public void setAudioReady(Boolean audioReady) {
        this.audioReady = audioReady;
    }
    
    @Override
    public String toString() {
        return "ChatMessageSync{textReady=" + textReady + 
               ", audioReady=" + audioReady + 
               ", bothReady=" + isBothReady() + "}";
    }
}


package com.chatbot.model.domain;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 输出配置组件
 * 封装消息的输出通道和TTS配置
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatMessageOutput {
    
    /**
     * TTS模式：none, char_stream, sentence_sync
     */
    private String ttsMode;
    
    /**
     * 输出通道类型：chat_window, live2d
     */
    private String channelType;
    
    /**
     * 说话人ID
     */
    private String speakerId;
    
    public ChatMessageOutput() {
        this.ttsMode = "none";
    }
    
    public ChatMessageOutput(String ttsMode, String channelType) {
        this.ttsMode = ttsMode;
        this.channelType = channelType;
    }
    
    // Getters & Setters
    
    public String getTtsMode() {
        return ttsMode;
    }
    
    public void setTtsMode(String ttsMode) {
        this.ttsMode = ttsMode;
    }
    
    public String getChannelType() {
        return channelType;
    }
    
    public void setChannelType(String channelType) {
        this.channelType = channelType;
    }
    
    public String getSpeakerId() {
        return speakerId;
    }
    
    public void setSpeakerId(String speakerId) {
        this.speakerId = speakerId;
    }
    
    @Override
    public String toString() {
        return "ChatMessageOutput{ttsMode='" + ttsMode + 
               "', channelType='" + channelType + 
               "', speakerId='" + speakerId + "'}";
    }
}


package com.chatbot.model.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Base64;

/**
 * 音频数据组件
 * 封装音频的二进制数据、URL和元信息
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatMessageAudio {
    
    /**
     * 音频二进制数据（不直接序列化）
     */
    @JsonIgnore
    private byte[] audioBytes;
    
    /**
     * 音频URL（用于大文件）
     */
    private String audioUrl;
    
    /**
     * 音频格式：wav, mp3, ogg
     */
    private String audioFormat;
    
    /**
     * 音频时长（毫秒）
     */
    private Integer audioDuration;
    
    /**
     * 音频是否准备就绪
     */
    private Boolean audioReady;
    
    public ChatMessageAudio() {
        this.audioFormat = "wav";
    }
    
    public ChatMessageAudio(byte[] audioBytes) {
        this.audioBytes = audioBytes;
        this.audioFormat = "wav";
    }
    
    public ChatMessageAudio(byte[] audioBytes, String audioFormat) {
        this.audioBytes = audioBytes;
        this.audioFormat = audioFormat;
    }
    
    // Base64序列化支持
    
    /**
     * 获取Base64编码的音频数据（用于JSON序列化）
     */
    @JsonProperty("audioData")
    public String getAudioDataBase64() {
        if (audioBytes == null) {
            return null;
        }
        return Base64.getEncoder().encodeToString(audioBytes);
    }
    
    /**
     * 设置Base64编码的音频数据（用于JSON反序列化）
     */
    @JsonProperty("audioData")
    public void setAudioDataBase64(String base64AudioData) {
        if (base64AudioData == null) {
            this.audioBytes = null;
        } else {
            this.audioBytes = Base64.getDecoder().decode(base64AudioData);
        }
    }
    
    // Getters & Setters
    
    @JsonIgnore
    public byte[] getAudioBytes() {
        return audioBytes;
    }
    
    @JsonIgnore
    public void setAudioBytes(byte[] audioBytes) {
        this.audioBytes = audioBytes;
    }
    
    public String getAudioUrl() {
        return audioUrl;
    }
    
    public void setAudioUrl(String audioUrl) {
        this.audioUrl = audioUrl;
    }
    
    public String getAudioFormat() {
        return audioFormat;
    }
    
    public void setAudioFormat(String audioFormat) {
        this.audioFormat = audioFormat;
    }
    
    public Integer getAudioDuration() {
        return audioDuration;
    }
    
    public void setAudioDuration(Integer audioDuration) {
        this.audioDuration = audioDuration;
    }
    
    public Boolean getAudioReady() {
        return audioReady;
    }
    
    public void setAudioReady(Boolean audioReady) {
        this.audioReady = audioReady;
    }
    
    @Override
    public String toString() {
        return "ChatMessageAudio{audioUrl='" + audioUrl + 
               "', audioFormat='" + audioFormat + 
               "', audioDuration=" + audioDuration + "}";
    }
}


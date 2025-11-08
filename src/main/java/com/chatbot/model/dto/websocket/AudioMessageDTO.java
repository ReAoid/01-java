package com.chatbot.model.dto.websocket;

/**
 * 音频消息DTO
 * 用于TTS音频数据的传输
 * 
 * 字段说明：
 * - audioData: Base64编码的音频数据，直接在根级别，前端可直接访问
 * - 采用扁平化结构，避免嵌套
 */
public class AudioMessageDTO extends ChatMessageDTO {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 对应的文本内容
     */
    private String content;
    
    /**
     * Base64编码的音频数据
     */
    private String audioData;
    
    /**
     * 音频格式：wav, mp3
     */
    private String audioFormat;
    
    /**
     * TTS模式：char_stream, sentence
     */
    private String ttsMode;
    
    /**
     * 句子顺序
     */
    private Integer sentenceOrder;
    
    /**
     * 句子ID
     */
    private String sentenceId;
    
    public AudioMessageDTO() {
        super("audio");
        this.audioFormat = "wav";
    }
    
    // ========== Getters & Setters ==========
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public String getAudioData() {
        return audioData;
    }
    
    public void setAudioData(String audioData) {
        this.audioData = audioData;
    }
    
    public String getAudioFormat() {
        return audioFormat;
    }
    
    public void setAudioFormat(String audioFormat) {
        this.audioFormat = audioFormat;
    }
    
    public String getTtsMode() {
        return ttsMode;
    }
    
    public void setTtsMode(String ttsMode) {
        this.ttsMode = ttsMode;
    }
    
    public Integer getSentenceOrder() {
        return sentenceOrder;
    }
    
    public void setSentenceOrder(Integer sentenceOrder) {
        this.sentenceOrder = sentenceOrder;
    }
    
    public String getSentenceId() {
        return sentenceId;
    }
    
    public void setSentenceId(String sentenceId) {
        this.sentenceId = sentenceId;
    }
    
    @Override
    public String toString() {
        return "AudioMessageDTO{" +
                "content='" + (content != null ? content.substring(0, Math.min(content.length(), 30)) + "..." : "null") + '\'' +
                ", hasAudioData=" + (audioData != null) +
                ", audioDataLength=" + (audioData != null ? audioData.length() : 0) +
                ", audioFormat='" + audioFormat + '\'' +
                ", ttsMode='" + ttsMode + '\'' +
                ", sentenceOrder=" + sentenceOrder +
                ", sentenceId='" + sentenceId + '\'' +
                ", " + super.toString() +
                '}';
    }
}


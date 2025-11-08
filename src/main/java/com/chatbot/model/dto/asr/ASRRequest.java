package com.chatbot.model.dto.asr;

import java.io.Serializable;

/**
 * ASR识别请求DTO
 * 用于语音识别的请求参数
 */
public class ASRRequest implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * Base64编码的音频数据
     */
    private final String audioData;
    
    /**
     * 音频格式：wav, pcm, opus等
     */
    private final String format;
    
    /**
     * 采样率：16000, 8000等
     */
    private final Integer sampleRate;
    
    /**
     * 语言代码：zh-CN, en-US等
     */
    private final String language;
    
    /**
     * 是否为流式识别
     */
    private final Boolean streaming;
    
    /**
     * 是否为最终结果（流式识别时使用）
     */
    private final Boolean isFinal;
    
    private ASRRequest(Builder builder) {
        this.audioData = builder.audioData;
        this.format = builder.format;
        this.sampleRate = builder.sampleRate;
        this.language = builder.language;
        this.streaming = builder.streaming;
        this.isFinal = builder.isFinal;
    }
    
    // ========== Getters ==========
    
    public String getAudioData() {
        return audioData;
    }
    
    public String getFormat() {
        return format;
    }
    
    public Integer getSampleRate() {
        return sampleRate;
    }
    
    public String getLanguage() {
        return language;
    }
    
    public Boolean getStreaming() {
        return streaming;
    }
    
    public Boolean getIsFinal() {
        return isFinal;
    }
    
    @Override
    public String toString() {
        return "ASRRequest{" +
                "hasAudioData=" + (audioData != null) +
                ", audioDataLength=" + (audioData != null ? audioData.length() : 0) +
                ", format='" + format + '\'' +
                ", sampleRate=" + sampleRate +
                ", language='" + language + '\'' +
                ", streaming=" + streaming +
                ", isFinal=" + isFinal +
                '}';
    }
    
    // ========== Builder ==========
    
    public static class Builder {
        private String audioData;
        private String format = "wav";
        private Integer sampleRate = 16000;
        private String language = "zh-CN";
        private Boolean streaming = false;
        private Boolean isFinal = true;
        
        public Builder audioData(String audioData) {
            this.audioData = audioData;
            return this;
        }
        
        public Builder format(String format) {
            this.format = format;
            return this;
        }
        
        public Builder sampleRate(Integer sampleRate) {
            this.sampleRate = sampleRate;
            return this;
        }
        
        public Builder language(String language) {
            this.language = language;
            return this;
        }
        
        public Builder streaming(Boolean streaming) {
            this.streaming = streaming;
            return this;
        }
        
        public Builder isFinal(Boolean isFinal) {
            this.isFinal = isFinal;
            return this;
        }
        
        public ASRRequest build() {
            return new ASRRequest(this);
        }
    }
}


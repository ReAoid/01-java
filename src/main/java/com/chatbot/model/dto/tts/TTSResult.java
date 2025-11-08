package com.chatbot.model.dto.tts;

import java.util.HashMap;
import java.util.Map;

/**
 * TTS结果 - 内部统一模型
 * 独立于具体的TTS引擎实现
 */
public class TTSResult {
    private final byte[] audioData;
    private final String format;
    private final int sampleRate;
    private final int duration; // 毫秒
    private final Map<String, Object> metadata;
    
    private TTSResult(Builder builder) {
        this.audioData = builder.audioData;
        this.format = builder.format;
        this.sampleRate = builder.sampleRate;
        this.duration = builder.duration;
        this.metadata = builder.metadata;
    }
    
    /**
     * Builder模式构建器
     */
    public static class Builder {
        private byte[] audioData;
        private String format = "wav";
        private int sampleRate = 22050;
        private int duration = 0;
        private Map<String, Object> metadata = new HashMap<>();
        
        public Builder audioData(byte[] audioData) {
            this.audioData = audioData;
            return this;
        }
        
        public Builder format(String format) {
            this.format = format;
            return this;
        }
        
        public Builder sampleRate(int sampleRate) {
            this.sampleRate = sampleRate;
            return this;
        }
        
        public Builder duration(int duration) {
            this.duration = duration;
            return this;
        }
        
        public Builder metadata(String key, Object value) {
            this.metadata.put(key, value);
            return this;
        }
        
        public Builder metadata(Map<String, Object> metadata) {
            if (metadata != null) {
                this.metadata.putAll(metadata);
            }
            return this;
        }
        
        public TTSResult build() {
            if (audioData == null || audioData.length == 0) {
                throw new IllegalArgumentException("Audio data cannot be empty");
            }
            return new TTSResult(this);
        }
    }
    
    // Getters
    public byte[] getAudioData() { 
        return audioData; 
    }
    
    public String getFormat() { 
        return format; 
    }
    
    public int getSampleRate() { 
        return sampleRate; 
    }
    
    public int getDuration() { 
        return duration; 
    }
    
    public Map<String, Object> getMetadata() { 
        return metadata; 
    }
    
    public Object getMetadata(String key) {
        return metadata.get(key);
    }
    
    public int getAudioSize() {
        return audioData != null ? audioData.length : 0;
    }
    
    @Override
    public String toString() {
        return String.format("TTSResult{audioSize=%d bytes, format='%s', sampleRate=%d Hz, duration=%d ms}",
                           getAudioSize(), format, sampleRate, duration);
    }
}


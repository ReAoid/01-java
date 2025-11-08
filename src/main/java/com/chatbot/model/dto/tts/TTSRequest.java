package com.chatbot.model.dto.tts;

import java.util.HashMap;
import java.util.Map;

/**
 * TTS请求 - 内部统一模型
 * 独立于具体的TTS引擎实现
 */
public class TTSRequest {
    private final String text;
    private final String speakerId;
    private final double speed;
    private final String format;
    private final Map<String, Object> engineSpecificOptions; // 引擎特定选项
    
    private TTSRequest(Builder builder) {
        this.text = builder.text;
        this.speakerId = builder.speakerId;
        this.speed = builder.speed;
        this.format = builder.format;
        this.engineSpecificOptions = builder.engineSpecificOptions;
    }
    
    /**
     * Builder模式构建器
     */
    public static class Builder {
        private String text;
        private String speakerId = "default";
        private double speed = 1.0;
        private String format = "wav";
        private Map<String, Object> engineSpecificOptions = new HashMap<>();
        
        public Builder text(String text) {
            this.text = text;
            return this;
        }
        
        public Builder speakerId(String speakerId) {
            this.speakerId = speakerId;
            return this;
        }
        
        public Builder speed(double speed) {
            this.speed = speed;
            return this;
        }
        
        public Builder format(String format) {
            this.format = format;
            return this;
        }
        
        public Builder option(String key, Object value) {
            this.engineSpecificOptions.put(key, value);
            return this;
        }
        
        public Builder options(Map<String, Object> options) {
            if (options != null) {
                this.engineSpecificOptions.putAll(options);
            }
            return this;
        }
        
        public TTSRequest build() {
            if (text == null || text.trim().isEmpty()) {
                throw new IllegalArgumentException("Text cannot be empty");
            }
            return new TTSRequest(this);
        }
    }
    
    // Getters
    public String getText() { 
        return text; 
    }
    
    public String getSpeakerId() { 
        return speakerId; 
    }
    
    public double getSpeed() { 
        return speed; 
    }
    
    public String getFormat() { 
        return format; 
    }
    
    public Map<String, Object> getEngineSpecificOptions() { 
        return engineSpecificOptions; 
    }
    
    public Object getOption(String key) {
        return engineSpecificOptions.get(key);
    }
    
    @Override
    public String toString() {
        return String.format("TTSRequest{text='%s...', speakerId='%s', speed=%.1f, format='%s'}",
                           text.length() > 20 ? text.substring(0, 20) : text, 
                           speakerId, speed, format);
    }
}


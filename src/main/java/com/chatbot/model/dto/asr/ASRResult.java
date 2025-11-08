package com.chatbot.model.dto.asr;

import java.util.HashMap;
import java.util.Map;

/**
 * ASR识别结果 - 内部统一模型
 */
public class ASRResult {
    private final String transcription;
    private final double confidence;
    private final boolean isFinal;
    private final String language;
    private final int duration; // 音频时长（毫秒）
    private final Map<String, Object> metadata;
    
    private ASRResult(Builder builder) {
        this.transcription = builder.transcription;
        this.confidence = builder.confidence;
        this.isFinal = builder.isFinal;
        this.language = builder.language;
        this.duration = builder.duration;
        this.metadata = builder.metadata;
    }
    
    /**
     * Builder模式构建器
     */
    public static class Builder {
        private String transcription;
        private double confidence = 0.0;
        private boolean isFinal = true;
        private String language;
        private int duration = 0;
        private Map<String, Object> metadata = new HashMap<>();
        
        public Builder transcription(String transcription) {
            this.transcription = transcription;
            return this;
        }
        
        public Builder confidence(double confidence) {
            this.confidence = confidence;
            return this;
        }
        
        public Builder isFinal(boolean isFinal) {
            this.isFinal = isFinal;
            return this;
        }
        
        public Builder language(String language) {
            this.language = language;
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
        
        public ASRResult build() {
            if (transcription == null) {
                transcription = "";
            }
            return new ASRResult(this);
        }
    }
    
    // Getters
    public String getTranscription() { 
        return transcription; 
    }
    
    public double getConfidence() { 
        return confidence; 
    }
    
    public boolean isFinal() { 
        return isFinal; 
    }
    
    public String getLanguage() { 
        return language; 
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
    
    public boolean isEmpty() {
        return transcription == null || transcription.trim().isEmpty();
    }
    
    @Override
    public String toString() {
        return String.format("ASRResult{transcription='%s', confidence=%.2f, isFinal=%s, language='%s', duration=%dms}",
                           transcription, confidence, isFinal, language, duration);
    }
}


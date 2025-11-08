package com.chatbot.model.dto.tts;

import java.util.HashMap;
import java.util.Map;

/**
 * 说话人信息 - 内部统一模型
 */
public class SpeakerInfo {
    private final String id;
    private final String name;
    private final String type; // "builtin" 或 "custom"
    private final String language;
    private final String gender;
    private final Map<String, Object> metadata;
    
    private SpeakerInfo(Builder builder) {
        this.id = builder.id;
        this.name = builder.name;
        this.type = builder.type;
        this.language = builder.language;
        this.gender = builder.gender;
        this.metadata = builder.metadata;
    }
    
    /**
     * Builder模式构建器
     */
    public static class Builder {
        private String id;
        private String name;
        private String type = "builtin";
        private String language;
        private String gender;
        private Map<String, Object> metadata = new HashMap<>();
        
        public Builder id(String id) {
            this.id = id;
            return this;
        }
        
        public Builder name(String name) {
            this.name = name;
            return this;
        }
        
        public Builder type(String type) {
            this.type = type;
            return this;
        }
        
        public Builder language(String language) {
            this.language = language;
            return this;
        }
        
        public Builder gender(String gender) {
            this.gender = gender;
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
        
        public SpeakerInfo build() {
            if (id == null || id.trim().isEmpty()) {
                throw new IllegalArgumentException("Speaker ID cannot be empty");
            }
            // 如果没有设置name，使用id作为name
            if (name == null) {
                name = id;
            }
            return new SpeakerInfo(this);
        }
    }
    
    // Getters
    public String getId() { 
        return id; 
    }
    
    public String getName() { 
        return name; 
    }
    
    public String getType() { 
        return type; 
    }
    
    public String getLanguage() { 
        return language; 
    }
    
    public String getGender() { 
        return gender; 
    }
    
    public Map<String, Object> getMetadata() { 
        return metadata; 
    }
    
    public Object getMetadata(String key) {
        return metadata.get(key);
    }
    
    public boolean isBuiltin() {
        return "builtin".equals(type);
    }
    
    public boolean isCustom() {
        return "custom".equals(type);
    }
    
    @Override
    public String toString() {
        return String.format("SpeakerInfo{id='%s', name='%s', type='%s', language='%s', gender='%s'}",
                           id, name, type, language, gender);
    }
}


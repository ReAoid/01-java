package com.chatbot.model.config;

/**
 * TTS (文本转语音) 配置
 * 负责语音合成的设置
 */
public class TTSConfig {
    
    // ========== 基础设置 ==========
    private boolean enabled = false;
    // private String engine = "cosyvoice";  // 新增字段，暂时注释
    private String preferredSpeaker = "派蒙";
    
    // ========== 语音参数 ==========
    private double speed = 1.0;
    // private double pitch = 1.0;  // 新增字段，暂时注释
    // private double volume = 1.0;  // 新增字段，暂时注释
    
    // ========== 合成策略 ==========
    // private String synthesisMode = "sentence";  // 新增字段，暂时注释
    // private boolean enableEmotionTags = false;  // 新增字段，暂时注释
    // private boolean autoDetectEmotion = false;  // 新增字段，暂时注释
    
    // ========== 音频格式 ==========
    // private String outputFormat = "wav";  // 新增字段，暂时注释
    // private int outputSampleRate = 24000;  // 新增字段，暂时注释
    // private int outputBitrate = 128;  // 新增字段，暂时注释
    
    // ========== 缓存设置 ==========
    // private boolean enableCache = true;  // 新增字段，暂时注释
    // private int cacheMaxSize = 100;  // 新增字段，暂时注释
    // private int cacheTTL = 3600;  // 新增字段，暂时注释
    
    // ========== 高级设置 ==========
    // private int maxConcurrentSynthesis = 3;  // 新增字段，暂时注释
    // private int synthesisTimeout = 10000;  // 新增字段，暂时注释
    // private boolean enableSSML = false;  // 新增字段，暂时注释
    
    /**
     * 验证配置有效性
     */
    public boolean validate() {
        if (speed < 0.5 || speed > 2.0) return false;
        return true;
    }
    
    // ========== Getters & Setters ==========
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    public String getPreferredSpeaker() {
        return preferredSpeaker;
    }
    
    public void setPreferredSpeaker(String preferredSpeaker) {
        this.preferredSpeaker = preferredSpeaker;
    }
    
    public double getSpeed() {
        return speed;
    }
    
    public void setSpeed(double speed) {
        this.speed = speed;
    }
}


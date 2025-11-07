package com.chatbot.model.config;

/**
 * ASR (自动语音识别) 配置
 * 负责语音输入的识别设置
 */
public class ASRConfig {
    
    // ========== 基础设置 ==========
    private boolean enabled = false;
    private String model = "whisper-medium";
    private String preferredLanguage = "zh-CN";
    
    // ========== 录音设置 ==========
    private boolean autoStart = false;
    private int maxRecordingTime = 60000;
    private String audioFormat = "wav";
    private int sampleRate = 16000;
    // private int bitDepth = 16;  // 新增字段，暂时注释
    
    // ========== VAD设置 ==========
    private boolean enableVAD = true;
    private double silenceThreshold = 2.0;
    // private double voiceThreshold = 0.5;  // 新增字段，暂时注释
    
    // ========== 识别设置 ==========
    private double confidenceThreshold = 0.7;
    // private boolean enablePunctuation = true;  // 新增字段，暂时注释
    // private boolean enableNumberConversion = true;  // 新增字段，暂时注释
    
    // ========== UI设置 ==========
    private boolean showStatusIndicator = true;
    private boolean showRecordingIndicator = true;
    // private boolean showTranscriptRealtime = true;  // 新增字段，暂时注释
    
    /**
     * 验证配置有效性
     */
    public boolean validate() {
        if (sampleRate <= 0) return false;
        if (maxRecordingTime <= 0) return false;
        if (confidenceThreshold < 0 || confidenceThreshold > 1) return false;
        return true;
    }
    
    // ========== Getters & Setters ==========
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    public String getModel() {
        return model;
    }
    
    public void setModel(String model) {
        this.model = model;
    }
    
    public String getPreferredLanguage() {
        return preferredLanguage;
    }
    
    public void setPreferredLanguage(String preferredLanguage) {
        this.preferredLanguage = preferredLanguage;
    }
    
    public boolean isAutoStart() {
        return autoStart;
    }
    
    public void setAutoStart(boolean autoStart) {
        this.autoStart = autoStart;
    }
    
    public int getMaxRecordingTime() {
        return maxRecordingTime;
    }
    
    public void setMaxRecordingTime(int maxRecordingTime) {
        this.maxRecordingTime = maxRecordingTime;
    }
    
    public String getAudioFormat() {
        return audioFormat;
    }
    
    public void setAudioFormat(String audioFormat) {
        this.audioFormat = audioFormat;
    }
    
    public int getSampleRate() {
        return sampleRate;
    }
    
    public void setSampleRate(int sampleRate) {
        this.sampleRate = sampleRate;
    }
    
    public boolean isEnableVAD() {
        return enableVAD;
    }
    
    public void setEnableVAD(boolean enableVAD) {
        this.enableVAD = enableVAD;
    }
    
    public double getSilenceThreshold() {
        return silenceThreshold;
    }
    
    public void setSilenceThreshold(double silenceThreshold) {
        this.silenceThreshold = silenceThreshold;
    }
    
    public double getConfidenceThreshold() {
        return confidenceThreshold;
    }
    
    public void setConfidenceThreshold(double confidenceThreshold) {
        this.confidenceThreshold = confidenceThreshold;
    }
    
    public boolean isShowStatusIndicator() {
        return showStatusIndicator;
    }
    
    public void setShowStatusIndicator(boolean showStatusIndicator) {
        this.showStatusIndicator = showStatusIndicator;
    }
    
    public boolean isShowRecordingIndicator() {
        return showRecordingIndicator;
    }
    
    public void setShowRecordingIndicator(boolean showRecordingIndicator) {
        this.showRecordingIndicator = showRecordingIndicator;
    }
}


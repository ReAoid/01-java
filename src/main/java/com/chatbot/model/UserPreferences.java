package com.chatbot.model;

/**
 * 用户偏好设置
 */
public class UserPreferences {
    private String language = "zh-CN";
    private boolean enableVoice = false;
    private String preferredTtsVoice;
    private double responseSpeed = 1.0;
    
    // 构造函数
    public UserPreferences() {}
    
    public UserPreferences(String language, boolean enableVoice, String preferredTtsVoice, double responseSpeed) {
        this.language = language;
        this.enableVoice = enableVoice;
        this.preferredTtsVoice = preferredTtsVoice;
        this.responseSpeed = responseSpeed;
    }
    
    // Getters and Setters
    public String getLanguage() {
        return language;
    }
    
    public void setLanguage(String language) {
        this.language = language;
    }
    
    public boolean isEnableVoice() {
        return enableVoice;
    }
    
    public void setEnableVoice(boolean enableVoice) {
        this.enableVoice = enableVoice;
    }
    
    public String getPreferredTtsVoice() {
        return preferredTtsVoice;
    }
    
    public void setPreferredTtsVoice(String preferredTtsVoice) {
        this.preferredTtsVoice = preferredTtsVoice;
    }
    
    public double getResponseSpeed() {
        return responseSpeed;
    }
    
    public void setResponseSpeed(double responseSpeed) {
        this.responseSpeed = responseSpeed;
    }
}

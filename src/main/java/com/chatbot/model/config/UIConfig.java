package com.chatbot.model.config;

/**
 * 界面配置
 * 控制前端UI的显示和行为
 */
public class UIConfig {
    
    private boolean darkMode = false;
    private boolean enableAnimations = true;
    private boolean autoScroll = true;
    private boolean soundNotification = false;
    // private String theme = "default";  // 新增字段，暂时注释
    // private int fontSize = 14;  // 新增字段，暂时注释
    
    // Getters & Setters
    
    public boolean isDarkMode() {
        return darkMode;
    }
    
    public void setDarkMode(boolean darkMode) {
        this.darkMode = darkMode;
    }
    
    public boolean isEnableAnimations() {
        return enableAnimations;
    }
    
    public void setEnableAnimations(boolean enableAnimations) {
        this.enableAnimations = enableAnimations;
    }
    
    public boolean isAutoScroll() {
        return autoScroll;
    }
    
    public void setAutoScroll(boolean autoScroll) {
        this.autoScroll = autoScroll;
    }
    
    public boolean isSoundNotification() {
        return soundNotification;
    }
    
    public void setSoundNotification(boolean soundNotification) {
        this.soundNotification = soundNotification;
    }
    
    // public String getTheme() { return theme; }
    // public void setTheme(String theme) { this.theme = theme; }
    
    // public int getFontSize() { return fontSize; }
    // public void setFontSize(int fontSize) { this.fontSize = fontSize; }
}


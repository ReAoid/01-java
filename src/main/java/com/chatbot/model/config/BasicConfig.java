package com.chatbot.model.config;

/**
 * 基础配置
 * 包含语言、时区等通用设置
 */
public class BasicConfig {
    
    private String language = "zh-CN";
    // private String timezone = "Asia/Shanghai";  // 新增字段，暂时注释
    // private String locale = "zh_CN";  // 新增字段，暂时注释
    
    // Getters & Setters
    
    public String getLanguage() {
        return language;
    }
    
    public void setLanguage(String language) {
        this.language = language;
    }
    
    // public String getTimezone() { return timezone; }
    // public void setTimezone(String timezone) { this.timezone = timezone; }
    
    // public String getLocale() { return locale; }
    // public void setLocale(String locale) { this.locale = locale; }
}


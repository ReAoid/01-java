package com.chatbot.model;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 用户偏好设置
 * 包含所有用户可配置的选项
 */
public class UserPreferences {
    
    // ========== 基础设置 ==========
    private String userId;
    private String language = "zh-CN";
    private LocalDateTime lastUpdated = LocalDateTime.now();
    
    // ========== 语音设置 ==========
    private boolean enableVoice = false;
    private String preferredTtsVoice = "zh-CN-XiaoxiaoNeural";
    private double responseSpeed = 1.0;
    private String asrModel = "whisper-medium";
    
    // ========== 界面设置 ==========
    private boolean darkMode = false;
    private boolean enableAnimations = true;
    private boolean autoScroll = true;
    private boolean soundNotification = false;
    
    // ========== 聊天设置 ==========
    private boolean showThinking = false;
    private boolean useWebSearch = false;
    private String defaultPersona = "default";
    private int maxContextTokens = 8192;
    private double temperature = 0.7;
    
    // ========== Ollama设置 ==========
    private String ollamaBaseUrl = "http://localhost:11434";
    private String ollamaModel = "qwen3:4b";
    private int ollamaTimeout = 30000;
    private int ollamaMaxTokens = 4096;
    private boolean ollamaStream = true;
    
    // ========== 联网搜索设置 ==========
    private boolean webSearchEnabled = false;
    private int webSearchMaxResults = 5;
    private int webSearchTimeout = 10;
    private String webSearchEngine = "duckduckgo";
    
    // ========== 流式输出设置 ==========
    private int streamingChunkSize = 16;
    private int streamingDelayMs = 50;
    
    // ========== 扩展配置 ==========
    private Map<String, Object> customSettings = new HashMap<>();
    
    // 构造函数
    public UserPreferences() {}
    
    public UserPreferences(String userId) {
        this.userId = userId;
        this.lastUpdated = LocalDateTime.now();
    }
    
    /**
     * 更新最后修改时间
     */
    public void updateLastModified() {
        this.lastUpdated = LocalDateTime.now();
    }
    
    /**
     * 设置自定义配置
     */
    public void setCustomSetting(String key, Object value) {
        if (customSettings == null) {
            customSettings = new HashMap<>();
        }
        customSettings.put(key, value);
        updateLastModified();
    }
    
    /**
     * 获取自定义配置
     */
    public Object getCustomSetting(String key) {
        return customSettings != null ? customSettings.get(key) : null;
    }
    
    /**
     * 获取自定义配置，带默认值
     */
    @SuppressWarnings("unchecked")
    public <T> T getCustomSetting(String key, T defaultValue) {
        if (customSettings == null) {
            return defaultValue;
        }
        Object value = customSettings.get(key);
        if (value == null) {
            return defaultValue;
        }
        try {
            return (T) value;
        } catch (ClassCastException e) {
            return defaultValue;
        }
    }
    
    // ========== Getters and Setters ==========
    
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public String getLanguage() {
        return language;
    }
    
    public void setLanguage(String language) {
        this.language = language;
        updateLastModified();
    }
    
    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }
    
    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
    
    public boolean isEnableVoice() {
        return enableVoice;
    }
    
    public void setEnableVoice(boolean enableVoice) {
        this.enableVoice = enableVoice;
        updateLastModified();
    }
    
    public String getPreferredTtsVoice() {
        return preferredTtsVoice;
    }
    
    public void setPreferredTtsVoice(String preferredTtsVoice) {
        this.preferredTtsVoice = preferredTtsVoice;
        updateLastModified();
    }
    
    public double getResponseSpeed() {
        return responseSpeed;
    }
    
    public void setResponseSpeed(double responseSpeed) {
        this.responseSpeed = responseSpeed;
        updateLastModified();
    }
    
    public String getAsrModel() {
        return asrModel;
    }
    
    public void setAsrModel(String asrModel) {
        this.asrModel = asrModel;
        updateLastModified();
    }
    
    public boolean isDarkMode() {
        return darkMode;
    }
    
    public void setDarkMode(boolean darkMode) {
        this.darkMode = darkMode;
        updateLastModified();
    }
    
    public boolean isEnableAnimations() {
        return enableAnimations;
    }
    
    public void setEnableAnimations(boolean enableAnimations) {
        this.enableAnimations = enableAnimations;
        updateLastModified();
    }
    
    public boolean isAutoScroll() {
        return autoScroll;
    }
    
    public void setAutoScroll(boolean autoScroll) {
        this.autoScroll = autoScroll;
        updateLastModified();
    }
    
    public boolean isSoundNotification() {
        return soundNotification;
    }
    
    public void setSoundNotification(boolean soundNotification) {
        this.soundNotification = soundNotification;
        updateLastModified();
    }
    
    public boolean isShowThinking() {
        return showThinking;
    }
    
    public void setShowThinking(boolean showThinking) {
        this.showThinking = showThinking;
        updateLastModified();
    }
    
    public boolean isUseWebSearch() {
        return useWebSearch;
    }
    
    public void setUseWebSearch(boolean useWebSearch) {
        this.useWebSearch = useWebSearch;
        updateLastModified();
    }
    
    public String getDefaultPersona() {
        return defaultPersona;
    }
    
    public void setDefaultPersona(String defaultPersona) {
        this.defaultPersona = defaultPersona;
        updateLastModified();
    }
    
    public int getMaxContextTokens() {
        return maxContextTokens;
    }
    
    public void setMaxContextTokens(int maxContextTokens) {
        this.maxContextTokens = maxContextTokens;
        updateLastModified();
    }
    
    public double getTemperature() {
        return temperature;
    }
    
    public void setTemperature(double temperature) {
        this.temperature = temperature;
        updateLastModified();
    }
    
    public String getOllamaBaseUrl() {
        return ollamaBaseUrl;
    }
    
    public void setOllamaBaseUrl(String ollamaBaseUrl) {
        this.ollamaBaseUrl = ollamaBaseUrl;
        updateLastModified();
    }
    
    public String getOllamaModel() {
        return ollamaModel;
    }
    
    public void setOllamaModel(String ollamaModel) {
        this.ollamaModel = ollamaModel;
        updateLastModified();
    }
    
    public int getOllamaTimeout() {
        return ollamaTimeout;
    }
    
    public void setOllamaTimeout(int ollamaTimeout) {
        this.ollamaTimeout = ollamaTimeout;
        updateLastModified();
    }
    
    public int getOllamaMaxTokens() {
        return ollamaMaxTokens;
    }
    
    public void setOllamaMaxTokens(int ollamaMaxTokens) {
        this.ollamaMaxTokens = ollamaMaxTokens;
        updateLastModified();
    }
    
    public boolean isOllamaStream() {
        return ollamaStream;
    }
    
    public void setOllamaStream(boolean ollamaStream) {
        this.ollamaStream = ollamaStream;
        updateLastModified();
    }
    
    public boolean isWebSearchEnabled() {
        return webSearchEnabled;
    }
    
    public void setWebSearchEnabled(boolean webSearchEnabled) {
        this.webSearchEnabled = webSearchEnabled;
        updateLastModified();
    }
    
    public int getWebSearchMaxResults() {
        return webSearchMaxResults;
    }
    
    public void setWebSearchMaxResults(int webSearchMaxResults) {
        this.webSearchMaxResults = webSearchMaxResults;
        updateLastModified();
    }
    
    public int getWebSearchTimeout() {
        return webSearchTimeout;
    }
    
    public void setWebSearchTimeout(int webSearchTimeout) {
        this.webSearchTimeout = webSearchTimeout;
        updateLastModified();
    }
    
    public String getWebSearchEngine() {
        return webSearchEngine;
    }
    
    public void setWebSearchEngine(String webSearchEngine) {
        this.webSearchEngine = webSearchEngine;
        updateLastModified();
    }
    
    public int getStreamingChunkSize() {
        return streamingChunkSize;
    }
    
    public void setStreamingChunkSize(int streamingChunkSize) {
        this.streamingChunkSize = streamingChunkSize;
        updateLastModified();
    }
    
    public int getStreamingDelayMs() {
        return streamingDelayMs;
    }
    
    public void setStreamingDelayMs(int streamingDelayMs) {
        this.streamingDelayMs = streamingDelayMs;
        updateLastModified();
    }
    
    public Map<String, Object> getCustomSettings() {
        return customSettings;
    }
    
    public void setCustomSettings(Map<String, Object> customSettings) {
        this.customSettings = customSettings;
        updateLastModified();
    }
}

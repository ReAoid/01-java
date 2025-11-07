package com.chatbot.model.config;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 用户偏好设置聚合根
 * 整合所有配置模块，提供统一的访问入口
 * 
 * @version 2.0 - 模块化重构
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserPreferences {
    
    // ========== 元数据 ==========
    private String userId;
    private LocalDateTime lastUpdated = LocalDateTime.now();
    
    // ========== 配置模块 (8个独立模块) ==========
    private BasicConfig basic = new BasicConfig();
    private UIConfig ui = new UIConfig();
    private ASRConfig asr = new ASRConfig();
    private TTSConfig tts = new TTSConfig();
    private LLMConfig llm = new LLMConfig();
    private WebSearchConfig webSearch = new WebSearchConfig();
    private StreamingConfig streaming = new StreamingConfig();
    private OutputChannelConfig outputChannel = new OutputChannelConfig();
    
    // ========== 扩展配置 ==========
    private Map<String, Object> customSettings = new HashMap<>();
    
    // ========== 构造函数 ==========
    
    public UserPreferences() {}
    
    public UserPreferences(String userId) {
        this.userId = userId;
        this.lastUpdated = LocalDateTime.now();
    }
    
    // ========== 核心方法 ==========
    
    /**
     * 更新最后修改时间
     */
    public void updateLastModified() {
        this.lastUpdated = LocalDateTime.now();
    }
    
    /**
     * 批量更新配置（触发一次时间戳更新）
     */
    public void updateConfigs(java.util.function.Consumer<UserPreferences> updater) {
        updater.accept(this);
        updateLastModified();
    }
    
    /**
     * 验证所有配置的有效性
     */
    public boolean validateAll() {
        return llm.validate() 
            && asr.validate() 
            && tts.validate()
            && webSearch.validate();
    }
    
    // ========== 自定义配置 ==========
    
    public void setCustomSetting(String key, Object value) {
        if (customSettings == null) {
            customSettings = new HashMap<>();
        }
        customSettings.put(key, value);
        updateLastModified();
    }
    
    public Object getCustomSetting(String key) {
        return customSettings != null ? customSettings.get(key) : null;
    }
    
    @SuppressWarnings("unchecked")
    public <T> T getCustomSetting(String key, T defaultValue) {
        if (customSettings == null) return defaultValue;
        Object value = customSettings.get(key);
        if (value == null) return defaultValue;
        try {
            return (T) value;
        } catch (ClassCastException e) {
            return defaultValue;
        }
    }
    
    // ========== Getters & Setters ==========
    
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }
    
    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
    
    public BasicConfig getBasic() {
        return basic;
    }
    
    public void setBasic(BasicConfig basic) {
        this.basic = basic;
    }
    
    public UIConfig getUi() {
        return ui;
    }
    
    public void setUi(UIConfig ui) {
        this.ui = ui;
    }
    
    public ASRConfig getAsr() {
        return asr;
    }
    
    public void setAsr(ASRConfig asr) {
        this.asr = asr;
    }
    
    public TTSConfig getTts() {
        return tts;
    }
    
    public void setTts(TTSConfig tts) {
        this.tts = tts;
    }
    
    public LLMConfig getLlm() {
        return llm;
    }
    
    public void setLlm(LLMConfig llm) {
        this.llm = llm;
    }
    
    public WebSearchConfig getWebSearch() {
        return webSearch;
    }
    
    public void setWebSearch(WebSearchConfig webSearch) {
        this.webSearch = webSearch;
    }
    
    public StreamingConfig getStreaming() {
        return streaming;
    }
    
    public void setStreaming(StreamingConfig streaming) {
        this.streaming = streaming;
    }
    
    public OutputChannelConfig getOutputChannel() {
        return outputChannel;
    }
    
    public void setOutputChannel(OutputChannelConfig outputChannel) {
        this.outputChannel = outputChannel;
    }
    
    public Map<String, Object> getCustomSettings() {
        return customSettings;
    }
    
    public void setCustomSettings(Map<String, Object> customSettings) {
        this.customSettings = customSettings;
    }
}

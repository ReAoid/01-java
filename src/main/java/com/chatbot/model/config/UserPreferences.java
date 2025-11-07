package com.chatbot.model.config;

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
    private String preferredSpeakerId = "派蒙";
    private double responseSpeed = 1.0;
    private String asrModel = "whisper-medium";
    
    // ========== 界面设置 ==========
    private boolean darkMode = false;
    private boolean enableAnimations = true;
    private boolean autoScroll = true;
    private boolean soundNotification = false;
    
    // ========== 聊天设置 ==========
    // 注意：显示思考过程和联网搜索功能已移至聊天界面直接控制
    // 这里保留一些基础的聊天配置
    
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
    
    // ========== 多通道TTS设置 ==========
    private ChatOutputConfig chatOutput = new ChatOutputConfig();
    private Live2DOutputConfig live2dOutput = new Live2DOutputConfig();
    
    // ========== ASR配置 ==========
    private ASRConfig asrConfig = new ASRConfig();
    
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
    
    public String getPreferredSpeakerId() {
        return preferredSpeakerId;
    }
    
    public void setPreferredSpeakerId(String preferredSpeakerId) {
        this.preferredSpeakerId = preferredSpeakerId;
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
    
    public ChatOutputConfig getChatOutput() {
        return chatOutput;
    }
    
    public void setChatOutput(ChatOutputConfig chatOutput) {
        this.chatOutput = chatOutput;
        updateLastModified();
    }
    
    public Live2DOutputConfig getLive2dOutput() {
        return live2dOutput;
    }
    
    public void setLive2dOutput(Live2DOutputConfig live2dOutput) {
        this.live2dOutput = live2dOutput;
        updateLastModified();
    }
    
    public ASRConfig getAsrConfig() {
        return asrConfig;
    }
    
    public void setAsrConfig(ASRConfig asrConfig) {
        this.asrConfig = asrConfig;
        updateLastModified();
    }
    
    /**
     * 聊天窗口输出配置
     */
    public static class ChatOutputConfig {
        private boolean enabled = false;    // 默认禁用TTS
        private String mode = "text_only";  // "text_only", "char_stream_tts"
        private boolean autoTTS = false;    // 是否自动启用TTS
        private String speakerId = "派蒙";   // CosyVoice说话人ID
        
        public boolean isEnabled() {
            return enabled;
        }
        
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
        
        public String getMode() {
            return mode;
        }
        
        public void setMode(String mode) {
            this.mode = mode;
        }
        
        public boolean isAutoTTS() {
            return autoTTS;
        }
        
        public void setAutoTTS(boolean autoTTS) {
            this.autoTTS = autoTTS;
        }
        
        public String getSpeakerId() {
            return speakerId;
        }
        
        public void setSpeakerId(String speakerId) {
            this.speakerId = speakerId;
        }
    }
    
    /**
     * Live2D输出配置
     */
    public static class Live2DOutputConfig {
        private boolean enabled = false;
        private String mode = "sentence_sync";  // 固定为句级同步
        private String speakerId = "派蒙";
        private double speed = 1.0;
        private boolean showBubble = true;      // 是否显示气泡
        private int bubbleTimeout = 5000;       // 气泡显示超时(毫秒)
        
        public boolean isEnabled() {
            return enabled;
        }
        
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
        
        public String getMode() {
            return mode;
        }
        
        public void setMode(String mode) {
            this.mode = mode;
        }
        
        public String getSpeakerId() {
            return speakerId;
        }
        
        public void setSpeakerId(String speakerId) {
            this.speakerId = speakerId;
        }
        
        public double getSpeed() {
            return speed;
        }
        
        public void setSpeed(double speed) {
            this.speed = speed;
        }
        
        public boolean isShowBubble() {
            return showBubble;
        }
        
        public void setShowBubble(boolean showBubble) {
            this.showBubble = showBubble;
        }
        
        public int getBubbleTimeout() {
            return bubbleTimeout;
        }
        
        public void setBubbleTimeout(int bubbleTimeout) {
            this.bubbleTimeout = bubbleTimeout;
        }
    }
    
    /**
     * ASR (语音识别) 配置
     */
    public static class ASRConfig {
        private boolean enabled = false;                    // 是否启用ASR
        private String preferredLanguage = "zh-CN";         // 首选语言
        private boolean autoStart = false;                  // 是否自动开始录音
        private int maxRecordingTime = 60000;               // 最大录音时间(毫秒)
        private double silenceThreshold = 2.0;              // 静音阈值
        private boolean showStatusIndicator = true;         // 显示状态指示器
        private boolean showRecordingIndicator = true;      // 显示录音指示器
        private double confidenceThreshold = 0.7;           // 置信度阈值
        private boolean enableVAD = true;                   // 启用语音活动检测
        private String audioFormat = "wav";                 // 音频格式
        private int sampleRate = 16000;                     // 采样率
        
        // Getters and Setters
        public boolean isEnabled() {
            return enabled;
        }
        
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
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
        
        public double getSilenceThreshold() {
            return silenceThreshold;
        }
        
        public void setSilenceThreshold(double silenceThreshold) {
            this.silenceThreshold = silenceThreshold;
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
        
        public double getConfidenceThreshold() {
            return confidenceThreshold;
        }
        
        public void setConfidenceThreshold(double confidenceThreshold) {
            this.confidenceThreshold = confidenceThreshold;
        }
        
        public boolean isEnableVAD() {
            return enableVAD;
        }
        
        public void setEnableVAD(boolean enableVAD) {
            this.enableVAD = enableVAD;
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
    }
}

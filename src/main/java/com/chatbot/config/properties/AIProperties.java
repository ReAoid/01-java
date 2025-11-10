package com.chatbot.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * AI相关配置
 */
@Component
@ConfigurationProperties(prefix = "app.ai")
public class AIProperties {
    private SystemPromptConfig systemPrompt = new SystemPromptConfig();
    private WebSearchDecisionConfig webSearchDecision = new WebSearchDecisionConfig();
    
    // Streaming配置 - 直接使用基本类型
    private int streamingChunkSize = 16;
    private int streamingDelayMs = 50;
    
    // Voice配置 - 直接使用基本类型
    private String voiceAsrModel = "whisper-medium";
    private String voiceTtsVoice = "派蒙";
    
    public SystemPromptConfig getSystemPrompt() { return systemPrompt; }
    public void setSystemPrompt(SystemPromptConfig systemPrompt) { this.systemPrompt = systemPrompt; }
    
    public WebSearchDecisionConfig getWebSearchDecision() { return webSearchDecision; }
    public void setWebSearchDecision(WebSearchDecisionConfig webSearchDecision) { this.webSearchDecision = webSearchDecision; }
    
    // Streaming配置的getter/setter
    public int getStreamingChunkSize() { return streamingChunkSize; }
    public void setStreamingChunkSize(int streamingChunkSize) { this.streamingChunkSize = streamingChunkSize; }
    
    public int getStreamingDelayMs() { return streamingDelayMs; }
    public void setStreamingDelayMs(int streamingDelayMs) { this.streamingDelayMs = streamingDelayMs; }
    
    // Voice配置的getter/setter
    public String getVoiceAsrModel() { return voiceAsrModel; }
    public void setVoiceAsrModel(String voiceAsrModel) { this.voiceAsrModel = voiceAsrModel; }
    
    public String getVoiceTtsVoice() { return voiceTtsVoice; }
    public void setVoiceTtsVoice(String voiceTtsVoice) { this.voiceTtsVoice = voiceTtsVoice; }
    
    /**
     * 系统提示词配置
     */
    public static class SystemPromptConfig {
        private String base;
        private String fallback;
        private boolean enablePersona;
        
        public String getBase() { return base; }
        public void setBase(String base) { this.base = base; }
        
        public String getFallback() { return fallback; }
        public void setFallback(String fallback) { this.fallback = fallback; }
        
        public boolean isEnablePersona() { return enablePersona; }
        public void setEnablePersona(boolean enablePersona) { this.enablePersona = enablePersona; }
    }
    
    /**
     * 联网搜索判断配置
     */
    public static class WebSearchDecisionConfig {
        private int timeoutSeconds; // AI判断超时时间（秒）
        private boolean enableTimeoutFallback; // 超时时是否采用保守策略
        
        public int getTimeoutSeconds() { return timeoutSeconds; }
        public void setTimeoutSeconds(int timeoutSeconds) { this.timeoutSeconds = timeoutSeconds; }
        
        public boolean isEnableTimeoutFallback() { return enableTimeoutFallback; }
        public void setEnableTimeoutFallback(boolean enableTimeoutFallback) { this.enableTimeoutFallback = enableTimeoutFallback; }
        
        // 辅助方法：获取毫秒超时时间
        public long getTimeoutMillis() { return timeoutSeconds * 1000L; }
    }
}


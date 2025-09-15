package com.chatbot.config;

import com.chatbot.model.StreamingConfig;
import com.chatbot.model.VoiceConfig;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * AI相关配置类
 * 包括流式处理和语音处理配置
 */
@Component
@ConfigurationProperties(prefix = "ai")
public class AIConfig {
    
    /**
     * 流式处理配置
     */
    private StreamingConfig streaming = new StreamingConfig();
    
    /**
     * 语音处理配置
     */
    private VoiceConfig voice = new VoiceConfig();
    
    public StreamingConfig getStreaming() {
        return streaming;
    }
    
    public void setStreaming(StreamingConfig streaming) {
        this.streaming = streaming;
    }
    
    public VoiceConfig getVoice() {
        return voice;
    }
    
    public void setVoice(VoiceConfig voice) {
        this.voice = voice;
    }
    
}

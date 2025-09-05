package com.chatbot.config;

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
    
    /**
     * 流式处理配置子类
     */
    public static class StreamingConfig {
        /**
         * 分块大小
         */
        private int chunkSize = 16;
        
        /**
         * 延迟毫秒数
         */
        private int delayMs = 50;
        
        public int getChunkSize() {
            return chunkSize;
        }
        
        public void setChunkSize(int chunkSize) {
            this.chunkSize = chunkSize;
        }
        
        public int getDelayMs() {
            return delayMs;
        }
        
        public void setDelayMs(int delayMs) {
            this.delayMs = delayMs;
        }
    }
    
    /**
     * 语音配置子类
     */
    public static class VoiceConfig {
        /**
         * ASR模型名称
         */
        private String asrModel = "whisper-medium";
        
        /**
         * TTS语音类型
         */
        private String ttsVoice = "zh-CN-XiaoxiaoNeural";
        
        public String getAsrModel() {
            return asrModel;
        }
        
        public void setAsrModel(String asrModel) {
            this.asrModel = asrModel;
        }
        
        public String getTtsVoice() {
            return ttsVoice;
        }
        
        public void setTtsVoice(String ttsVoice) {
            this.ttsVoice = ttsVoice;
        }
    }
}

package com.chatbot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Python API配置类
 * 用于配置各种Python服务的API端点
 */
@Component
@ConfigurationProperties(prefix = "python.api")
public class PythonApiConfig {
    
    /**
     * Python API基础URL
     */
    private String baseUrl = "http://localhost:5000";
    
    /**
     * API端点配置
     */
    private EndpointsConfig endpoints = new EndpointsConfig();
    
    public String getBaseUrl() {
        return baseUrl;
    }
    
    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }
    
    public EndpointsConfig getEndpoints() {
        return endpoints;
    }
    
    public void setEndpoints(EndpointsConfig endpoints) {
        this.endpoints = endpoints;
    }
    
    /**
     * API端点配置子类
     */
    public static class EndpointsConfig {
        /**
         * ASR语音转文本端点
         */
        private String asr = "/api/asr";
        
        /**
         * TTS文本转语音端点
         */
        private String tts = "/api/tts";
        
        /**
         * VAD语音活动检测端点
         */
        private String vad = "/api/vad";
        
        /**
         * OCR图像识别端点
         */
        private String ocr = "/api/ocr";
        
        public String getAsr() {
            return asr;
        }
        
        public void setAsr(String asr) {
            this.asr = asr;
        }
        
        public String getTts() {
            return tts;
        }
        
        public void setTts(String tts) {
            this.tts = tts;
        }
        
        public String getVad() {
            return vad;
        }
        
        public void setVad(String vad) {
            this.vad = vad;
        }
        
        public String getOcr() {
            return ocr;
        }
        
        public void setOcr(String ocr) {
            this.ocr = ocr;
        }
    }
}

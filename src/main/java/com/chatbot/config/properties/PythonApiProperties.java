package com.chatbot.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Python API配置
 * 每个服务都配置独立的完整URL，支持不同的域名和端口
 */
@Component
@ConfigurationProperties(prefix = "app.python")
public class PythonApiProperties {
    private ServicesConfig services = new ServicesConfig();
    private TimeoutProperties timeout = new TimeoutProperties();
    
    public ServicesConfig getServices() { return services; }
    public void setServices(ServicesConfig services) { this.services = services; }
    
    public TimeoutProperties getTimeout() { return timeout; }
    public void setTimeout(TimeoutProperties timeout) { this.timeout = timeout; }
    
    /**
     * 各个服务的独立URL配置
     */
    public static class ServicesConfig {
        // ASR (语音识别) 服务
        private String asrUrl;
        
        // TTS (文本转语音) 服务 - CosyVoice默认端口
        private String ttsUrl;
        
        // VAD (语音活动检测) 服务
        private String vadUrl;
        
        // OCR (图像识别) 服务
        private String ocrUrl;
        
        // Getters and Setters
        public String getAsrUrl() { return asrUrl; }
        public void setAsrUrl(String asrUrl) { this.asrUrl = asrUrl; }
        
        public String getTtsUrl() { return ttsUrl; }
        public void setTtsUrl(String ttsUrl) { this.ttsUrl = ttsUrl; }
        
        public String getVadUrl() { return vadUrl; }
        public void setVadUrl(String vadUrl) { this.vadUrl = vadUrl; }
        
        public String getOcrUrl() { return ocrUrl; }
        public void setOcrUrl(String ocrUrl) { this.ocrUrl = ocrUrl; }
        
        // 便捷方法：获取TTS健康检查URL
        public String getTtsHealthUrl() {
            return ttsUrl + (ttsUrl.endsWith("/") ? "health" : "/health");
        }
        
        // 便捷方法：获取TTS注册说话人URL
        public String getTtsRegisterSpeakerUrl() {
            return ttsUrl + (ttsUrl.endsWith("/") ? "register_speaker" : "/register_speaker");
        }
        
        // 便捷方法：获取TTS自定义说话人合成URL
        public String getTtsCustomSpeakerUrl() {
            return ttsUrl + (ttsUrl.endsWith("/") ? "inference_custom_speaker" : "/inference_custom_speaker");
        }
        
        // 便捷方法：获取TTS删除说话人URL
        public String getTtsDeleteSpeakerUrl(String speakerName) {
            return ttsUrl + (ttsUrl.endsWith("/") ? "speaker/" : "/speaker/") + speakerName;
        }
    }
}


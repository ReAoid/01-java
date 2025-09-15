package com.chatbot.model;

/**
 * API端点配置
 */
public class EndpointsConfig {
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

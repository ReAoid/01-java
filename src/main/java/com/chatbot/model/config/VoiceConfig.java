package com.chatbot.model.config;

/**
 * 语音配置
 */
public class VoiceConfig {
    /**
     * ASR模型名称
     */
    private String asrModel = "sensevoice";
    
    /**
     * TTS语音类型
     */
    private String ttsVoice = "cosyvoice";
    
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

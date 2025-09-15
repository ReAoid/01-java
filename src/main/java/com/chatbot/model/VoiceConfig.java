package com.chatbot.model;

/**
 * 语音配置
 */
public class VoiceConfig {
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

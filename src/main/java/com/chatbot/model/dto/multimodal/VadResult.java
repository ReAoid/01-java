package com.chatbot.model.dto.multimodal;

/**
 * VAD语音活动检测结果
 */
public class VadResult {
    private final boolean hasVoice;
    private final double confidence;
    
    public VadResult(boolean hasVoice, double confidence) {
        this.hasVoice = hasVoice;
        this.confidence = confidence;
    }
    
    public boolean hasVoice() {
        return hasVoice;
    }
    
    public double getConfidence() {
        return confidence;
    }
}


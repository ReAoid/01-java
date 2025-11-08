package com.chatbot.model.dto.asr;

import java.io.Serializable;
import java.util.Map;

/**
 * ASR识别响应DTO
 * 用于返回语音识别结果
 */
public class ASRResponse implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 识别出的文本
     */
    private final String transcription;
    
    /**
     * 识别置信度（0.0-1.0）
     */
    private final double confidence;
    
    /**
     * 是否为最终结果
     */
    private final boolean isFinal;
    
    /**
     * 识别耗时（毫秒）
     */
    private final long processingTime;
    
    /**
     * 额外元数据
     */
    private final Map<String, Object> metadata;
    
    public ASRResponse(String transcription, double confidence, boolean isFinal, 
                       long processingTime, Map<String, Object> metadata) {
        this.transcription = transcription;
        this.confidence = confidence;
        this.isFinal = isFinal;
        this.processingTime = processingTime;
        this.metadata = metadata;
    }
    
    // ========== Getters ==========
    
    public String getTranscription() {
        return transcription;
    }
    
    public double getConfidence() {
        return confidence;
    }
    
    public boolean isFinal() {
        return isFinal;
    }
    
    public long getProcessingTime() {
        return processingTime;
    }
    
    public Map<String, Object> getMetadata() {
        return metadata;
    }
    
    @Override
    public String toString() {
        return "ASRResponse{" +
                "transcription='" + (transcription != null ? 
                    transcription.substring(0, Math.min(transcription.length(), 50)) + "..." : "null") + '\'' +
                ", confidence=" + confidence +
                ", isFinal=" + isFinal +
                ", processingTime=" + processingTime + "ms" +
                ", metadata=" + metadata +
                '}';
    }
}


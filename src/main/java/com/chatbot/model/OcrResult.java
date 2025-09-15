package com.chatbot.model;

/**
 * OCR光学字符识别结果
 */
public class OcrResult {
    private final String text;
    private final double confidence;
    
    public OcrResult(String text, double confidence) {
        this.text = text;
        this.confidence = confidence;
    }
    
    public String getText() {
        return text;
    }
    
    public double getConfidence() {
        return confidence;
    }
}

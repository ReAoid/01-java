package com.chatbot.model.config;

/**
 * 流式处理配置
 */
public class StreamingConfig {
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

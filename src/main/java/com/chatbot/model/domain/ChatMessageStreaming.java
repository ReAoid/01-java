package com.chatbot.model.domain;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 流式控制组件
 * 管理流式输出的状态和句子分段信息
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatMessageStreaming {
    
    /**
     * 是否为流式消息
     */
    private boolean streaming;
    
    /**
     * 流式消息是否完成
     */
    private boolean streamComplete;
    
    /**
     * 块索引（用于排序）
     */
    private Integer chunkIndex;
    
    /**
     * 句子唯一标识
     */
    private String sentenceId;
    
    /**
     * 句子在会话中的顺序
     */
    private Integer sentenceOrder;
    
    /**
     * 是否为句子结束
     */
    private Boolean sentenceComplete;
    
    public ChatMessageStreaming() {
        this.streaming = true;
        this.streamComplete = false;
    }
    
    // Getters & Setters
    
    public boolean isStreaming() {
        return streaming;
    }
    
    public void setStreaming(boolean streaming) {
        this.streaming = streaming;
    }
    
    public boolean isStreamComplete() {
        return streamComplete;
    }
    
    public void setStreamComplete(boolean streamComplete) {
        this.streamComplete = streamComplete;
    }
    
    public Integer getChunkIndex() {
        return chunkIndex;
    }
    
    public void setChunkIndex(Integer chunkIndex) {
        this.chunkIndex = chunkIndex;
    }
    
    public String getSentenceId() {
        return sentenceId;
    }
    
    public void setSentenceId(String sentenceId) {
        this.sentenceId = sentenceId;
    }
    
    public Integer getSentenceOrder() {
        return sentenceOrder;
    }
    
    public void setSentenceOrder(Integer sentenceOrder) {
        this.sentenceOrder = sentenceOrder;
    }
    
    public Boolean getSentenceComplete() {
        return sentenceComplete;
    }
    
    public void setSentenceComplete(Boolean sentenceComplete) {
        this.sentenceComplete = sentenceComplete;
    }
    
    @Override
    public String toString() {
        return "ChatMessageStreaming{streaming=" + streaming + 
               ", streamComplete=" + streamComplete + 
               ", sentenceId='" + sentenceId + "'}";
    }
}


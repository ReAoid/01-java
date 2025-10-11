package com.chatbot.model;

import com.chatbot.util.IdUtil;

/**
 * 句子项模型
 * 用于多通道TTS处理中的句子管理
 */
public class SentenceItem {
    
    /**
     * 句子文本内容
     */
    private final String text;
    
    /**
     * 句子在会话中的顺序
     */
    private final int order;
    
    /**
     * 句子唯一标识
     */
    private final String id;
    
    /**
     * 所属会话ID
     */
    private final String sessionId;
    
    public SentenceItem(String text, int order, String sessionId) {
        this.text = text;
        this.order = order;
        this.sessionId = sessionId;
        this.id = generateSentenceId(sessionId, order);
    }
    
    /**
     * 生成句子唯一标识
     */
    private String generateSentenceId(String sessionId, int order) {
        return IdUtil.prefixedId("sentence_" + sessionId + "_" + order);
    }
    
    public String getText() {
        return text;
    }
    
    public int getOrder() {
        return order;
    }
    
    public String getId() {
        return id;
    }
    
    public String getSessionId() {
        return sessionId;
    }
    
    @Override
    public String toString() {
        return "SentenceItem{" +
                "text='" + text + '\'' +
                ", order=" + order +
                ", id='" + id + '\'' +
                ", sessionId='" + sessionId + '\'' +
                '}';
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        SentenceItem that = (SentenceItem) o;
        return id.equals(that.id);
    }
    
    @Override
    public int hashCode() {
        return id.hashCode();
    }
}

package com.chatbot.service;

import com.chatbot.model.domain.SentenceItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 共享句子队列
 * 用于多通道TTS处理中的句子管理和同步
 * 
 * 注意：这不是Spring Bean，而是运行时动态创建的对象
 */
public class SharedSentenceQueue {
    
    private static final Logger logger = LoggerFactory.getLogger(SharedSentenceQueue.class);
    
    private final String sessionId;
    private final List<SentenceItem> sentences = new ArrayList<>();
    private final AtomicInteger orderCounter = new AtomicInteger(0);
    private final SentenceBuffer sentenceBuffer = new SentenceBuffer();
    
    public SharedSentenceQueue(String sessionId) {
        this.sessionId = sessionId;
    }
    
    /**
     * 添加新句子到队列
     * @param text 句子文本
     * @return 句子顺序号
     */
    public int addSentence(String text) {
        if (text == null || text.trim().isEmpty()) {
            logger.warn("尝试添加空句子，sessionId: {}", sessionId);
            return -1;
        }
        
        int order = orderCounter.getAndIncrement();
        SentenceItem sentence = new SentenceItem(text.trim(), order, sessionId);
        
        synchronized (sentences) {
            sentences.add(sentence);
        }
        
        logger.debug("添加句子到队列: order={}, text={}, sessionId={}", 
                    order, text.substring(0, Math.min(text.length(), 50)) + "...", sessionId);
        
        return order;
    }
    
    /**
     * 根据顺序获取句子
     * @param order 句子顺序号
     * @return 句子项，如果不存在返回null
     */
    public SentenceItem getSentence(int order) {
        synchronized (sentences) {
            return sentences.stream()
                    .filter(s -> s.getOrder() == order)
                    .findFirst()
                    .orElse(null);
        }
    }
    
    /**
     * 获取所有句子的副本
     * @return 句子列表副本
     */
    public List<SentenceItem> getAllSentences() {
        synchronized (sentences) {
            return new ArrayList<>(sentences);
        }
    }
    
    /**
     * 获取句子总数
     * @return 句子数量
     */
    public int size() {
        synchronized (sentences) {
            return sentences.size();
        }
    }
    
    /**
     * 清空队列
     */
    public void clear() {
        synchronized (sentences) {
            sentences.clear();
            orderCounter.set(0);
        }
        logger.debug("清空句子队列，sessionId: {}", sessionId);
    }
    
    /**
     * 获取会话ID
     * @return 会话ID
     */
    public String getSessionId() {
        return sessionId;
    }
    
    /**
     * 获取下一个句子的顺序号
     * @return 下一个顺序号
     */
    public int getNextOrder() {
        return orderCounter.get();
    }
    
    /**
     * 检查是否为空
     * @return 是否为空
     */
    public boolean isEmpty() {
        synchronized (sentences) {
            return sentences.isEmpty();
        }
    }
    
    /**
     * 添加文本块到句子缓冲区
     * @param chunk 文本块
     */
    public void addTextChunk(String chunk) {
        sentenceBuffer.addChunk(chunk);
    }
    
    /**
     * 检查是否有待处理的完整句子
     * @return 是否有完整句子
     */
    public boolean hasPendingSentence() {
        return sentenceBuffer.hasPendingSentence();
    }
    
    /**
     * 提取下一个完整句子
     * @return 完整句子，如果没有返回null
     */
    public String extractSentence() {
        return sentenceBuffer.extractSentence();
    }
    
    /**
     * 获取剩余文本内容
     * @return 剩余文本
     */
    public String getRemainingText() {
        return sentenceBuffer.getRemainingText();
    }
    
    @Override
    public String toString() {
        return "SharedSentenceQueue{" +
                "sessionId='" + sessionId + '\'' +
                ", sentenceCount=" + size() +
                ", nextOrder=" + getNextOrder() +
                '}';
    }
}

package com.chatbot.service;

import com.chatbot.model.domain.ChatMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * 多通道上下文
 * 用于在不同输出通道之间共享数据和发送消息
 */
public class MultiChannelContext {
    
    private static final Logger logger = LoggerFactory.getLogger(MultiChannelContext.class);
    
    private final String sessionId;
    private final Consumer<ChatMessage> responseCallback;
    private final Map<String, Object> sharedData = new ConcurrentHashMap<>();
    private final SharedSentenceQueue sharedQueue;
    
    public MultiChannelContext(String sessionId, 
                              Consumer<ChatMessage> responseCallback,
                              SharedSentenceQueue sharedQueue) {
        this.sessionId = sessionId;
        this.responseCallback = responseCallback;
        this.sharedQueue = sharedQueue;
    }
    
    /**
     * 发送消息到指定通道
     * @param message 消息对象
     * @param channelType 通道类型
     */
    public void sendMessage(ChatMessage message, String channelType) {
        if (message == null) {
            logger.warn("尝试发送空消息，channelType: {}, sessionId: {}", channelType, sessionId);
            return;
        }
        
        // 设置通道类型和会话ID
        message.setChannelType(channelType);
        message.setSessionId(sessionId);
        
        logger.info("发送消息到通道: type={}, channelType={}, sessionId={}, hasAudioData={}", 
                    message.getType(), channelType, sessionId, message.getAudioData() != null);
        
        try {
            responseCallback.accept(message);
            logger.info("消息发送成功: type={}, channelType={}, sessionId={}", 
                       message.getType(), channelType, sessionId);
        } catch (Exception e) {
            logger.error("发送消息到通道失败: channelType={}, sessionId={}", 
                        channelType, sessionId, e);
        }
    }
    
    /**
     * 设置共享数据
     * @param key 数据键
     * @param value 数据值
     */
    public void setSharedData(String key, Object value) {
        if (key != null) {
            sharedData.put(key, value);
            logger.trace("设置共享数据: key={}, sessionId={}", key, sessionId);
        }
    }
    
    /**
     * 获取共享数据
     * @param key 数据键
     * @param type 数据类型
     * @return 数据值，如果不存在或类型不匹配返回null
     */
    public <T> T getSharedData(String key, Class<T> type) {
        if (key == null || type == null) {
            return null;
        }
        
        Object value = sharedData.get(key);
        if (value == null) {
            return null;
        }
        
        try {
            return type.cast(value);
        } catch (ClassCastException e) {
            logger.warn("共享数据类型转换失败: key={}, expectedType={}, actualType={}, sessionId={}", 
                       key, type.getSimpleName(), value.getClass().getSimpleName(), sessionId);
            return null;
        }
    }
    
    /**
     * 获取共享数据，带默认值
     * @param key 数据键
     * @param defaultValue 默认值
     * @return 数据值或默认值
     */
    @SuppressWarnings("unchecked")
    public <T> T getSharedData(String key, T defaultValue) {
        if (key == null) {
            return defaultValue;
        }
        
        Object value = sharedData.get(key);
        if (value == null) {
            return defaultValue;
        }
        
        try {
            return (T) value;
        } catch (ClassCastException e) {
            logger.warn("共享数据类型转换失败，返回默认值: key={}, sessionId={}", key, sessionId);
            return defaultValue;
        }
    }
    
    /**
     * 移除共享数据
     * @param key 数据键
     * @return 被移除的值
     */
    public Object removeSharedData(String key) {
        if (key != null) {
            Object removed = sharedData.remove(key);
            logger.trace("移除共享数据: key={}, sessionId={}", key, sessionId);
            return removed;
        }
        return null;
    }
    
    /**
     * 清除所有共享数据
     */
    public void clearSharedData() {
        sharedData.clear();
        logger.debug("清除所有共享数据，sessionId: {}", sessionId);
    }
    
    /**
     * 获取会话ID
     * @return 会话ID
     */
    public String getSessionId() {
        return sessionId;
    }
    
    /**
     * 获取共享句子队列
     * @return 共享句子队列
     */
    public SharedSentenceQueue getSharedQueue() {
        return sharedQueue;
    }
    
    /**
     * 检查是否包含指定的共享数据
     * @param key 数据键
     * @return 是否包含
     */
    public boolean containsSharedData(String key) {
        return key != null && sharedData.containsKey(key);
    }
    
    /**
     * 获取共享数据的键集合
     * @return 键集合
     */
    public java.util.Set<String> getSharedDataKeys() {
        return sharedData.keySet();
    }
    
    @Override
    public String toString() {
        return "MultiChannelContext{" +
                "sessionId='" + sessionId + '\'' +
                ", sharedDataCount=" + sharedData.size() +
                ", queueSize=" + (sharedQueue != null ? sharedQueue.size() : 0) +
                '}';
    }
}

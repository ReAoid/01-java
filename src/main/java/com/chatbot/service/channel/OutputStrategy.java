package com.chatbot.service.channel;

import com.chatbot.model.domain.ChatMessage;

import java.util.function.Consumer;

/**
 * 输出策略接口
 * 定义不同输出模式的处理策略，简化多通道架构
 * 
 * 策略模式替代原有的 OutputChannel 接口，减少不必要的空方法实现
 */
public interface OutputStrategy {
    
    /**
     * 获取策略名称
     * @return 策略名称（如 "text_only", "char_stream_tts", "sentence_sync"）
     */
    String getStrategyName();
    
    /**
     * 处理流式文本块
     * 
     * @param chunk 文本块（可能是字符、词或句子）
     * @param callback 响应回调，用于发送消息到前端
     * @param sessionId 会话ID
     * @param isThinking 是否为思考内容
     */
    void processChunk(String chunk, Consumer<ChatMessage> callback, String sessionId, boolean isThinking);
    
    /**
     * 流式响应完成通知
     * 
     * @param callback 响应回调
     * @param sessionId 会话ID
     */
    void onStreamComplete(Consumer<ChatMessage> callback, String sessionId);
    
    /**
     * 清理会话资源
     * 
     * @param sessionId 会话ID
     */
    default void cleanup(String sessionId) {
        // 默认实现为空，子类可以重写
    }
    
    /**
     * 检查策略是否可用
     * 
     * @param sessionId 会话ID
     * @return 是否可用
     */
    default boolean isAvailable(String sessionId) {
        return true;
    }
}


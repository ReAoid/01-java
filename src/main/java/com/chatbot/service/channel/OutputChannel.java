package com.chatbot.service.channel;

import com.chatbot.service.MultiChannelContext;

/**
 * 输出通道接口
 * 定义不同输出通道的统一行为
 */
public interface OutputChannel {
    
    /**
     * 获取通道类型标识
     * @return 通道类型（如 "chat_window", "live2d"）
     */
    String getChannelType();
    
    /**
     * 获取通道模式
     * @return 通道模式（如 "text_only", "char_stream", "sentence_sync"）
     */
    String getMode();
    
    /**
     * 接收新句子通知（但不立即处理）
     * @param sentence 句子文本
     * @param order 句子顺序
     * @param context 多通道上下文
     */
    void onNewSentence(String sentence, int order, MultiChannelContext context);
    
    /**
     * 开始处理队列中的所有句子
     * @param context 多通道上下文
     */
    void startProcessing(MultiChannelContext context);
    
    /**
     * 处理完成通知
     * @param context 多通道上下文
     */
    void onProcessingComplete(MultiChannelContext context);
    
    /**
     * 检查通道是否启用
     * @param context 多通道上下文
     * @return 是否启用
     */
    boolean isEnabled(MultiChannelContext context);
    
    /**
     * 清理通道资源
     * @param context 多通道上下文
     */
    default void cleanup(MultiChannelContext context) {
        // 默认实现为空，子类可以重写
    }
    
    /**
     * 获取通道状态信息
     * @return 状态描述
     */
    default String getStatus() {
        return "active";
    }
}

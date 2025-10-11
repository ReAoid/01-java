package com.chatbot.service;

import com.chatbot.model.ChatMessage;
import com.chatbot.service.channel.OutputChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;
import java.util.function.Consumer;

/**
 * 同步处理器
 * 负责协调多个输出通道的句子处理和同步
 * 
 * 注意：这不是Spring Bean，而是运行时动态创建的对象
 */
public class SynchronizedProcessor {
    
    private static final Logger logger = LoggerFactory.getLogger(SynchronizedProcessor.class);
    
    private final SharedSentenceQueue sharedQueue;
    private final List<OutputChannel> channels;
    private final SentenceBuffer buffer;
    private final MultiChannelContext context;
    
    public SynchronizedProcessor(SharedSentenceQueue sharedQueue,
                               List<OutputChannel> channels,
                               Consumer<ChatMessage> responseCallback) {
        this.sharedQueue = sharedQueue;
        this.channels = channels;
        this.buffer = new SentenceBuffer();
        this.context = new MultiChannelContext(sharedQueue.getSessionId(), responseCallback, sharedQueue);
        
        logger.info("创建同步处理器: sessionId={}, channelCount={}", 
                   sharedQueue.getSessionId(), channels.size());
    }
    
    /**
     * 处理文本块
     * @param chunk 文本块
     */
    public void processChunk(String chunk) {
        if (chunk == null || chunk.isEmpty()) {
            return;
        }
        
        logger.trace("处理文本块: {}", chunk.substring(0, Math.min(chunk.length(), 50)) + "...");
        
        // 添加到句子缓冲区
        buffer.addChunk(chunk);
        
        // 检测并处理完整句子
        while (buffer.hasPendingSentence()) {
            String sentence = buffer.extractSentence();
            
            if (sentence != null && !sentence.trim().isEmpty()) {
                processSentence(sentence);
            }
        }
    }
    
    /**
     * 完成处理
     * 处理剩余文本并启动各通道的处理流程
     */
    public void finishProcessing() {
        logger.debug("完成文本流处理，sessionId: {}", sharedQueue.getSessionId());
        
        // 处理剩余文本
        String remaining = buffer.getRemainingText();
        if (!remaining.isEmpty()) {
            processSentence(remaining);
        }
        
        // 记录最终的句子统计
        logger.info("句子分割完成: sessionId={}, totalSentences={}", 
                   sharedQueue.getSessionId(), sharedQueue.size());
        
        // 通知所有通道开始处理
        for (OutputChannel channel : channels) {
            try {
                logger.debug("启动通道处理: channelType={}, sessionId={}", 
                           channel.getChannelType(), sharedQueue.getSessionId());
                channel.startProcessing(context);
            } catch (Exception e) {
                logger.error("启动通道处理失败: channelType={}, sessionId={}", 
                           channel.getChannelType(), sharedQueue.getSessionId(), e);
            }
        }
    }
    
    /**
     * 处理单个句子
     * @param sentence 句子文本
     */
    private void processSentence(String sentence) {
        // 添加到共享队列
        int order = sharedQueue.addSentence(sentence);
        
        if (order >= 0) {
            logger.debug("处理句子: order={}, text={}, sessionId={}", 
                        order, sentence.substring(0, Math.min(sentence.length(), 50)) + "...", 
                        sharedQueue.getSessionId());
            
            // 通知所有通道有新句子
            for (OutputChannel channel : channels) {
                try {
                    channel.onNewSentence(sentence, order, context);
                } catch (Exception e) {
                    logger.error("通知通道新句子失败: channelType={}, order={}, sessionId={}", 
                               channel.getChannelType(), order, sharedQueue.getSessionId(), e);
                }
            }
        }
    }
    
    /**
     * 获取处理统计信息
     * @return 统计信息字符串
     */
    public String getProcessingStats() {
        return String.format("SynchronizedProcessor{sessionId='%s', channelCount=%d, sentenceCount=%d, bufferSize=%d}",
                sharedQueue.getSessionId(), channels.size(), sharedQueue.size(), buffer.size());
    }
    
    /**
     * 清理资源
     */
    public void cleanup() {
        logger.debug("清理同步处理器资源: sessionId={}", sharedQueue.getSessionId());
        
        buffer.clear();
        context.clearSharedData();
        
        // 通知所有通道处理完成
        for (OutputChannel channel : channels) {
            try {
                channel.onProcessingComplete(context);
            } catch (Exception e) {
                logger.error("通知通道处理完成失败: channelType={}, sessionId={}", 
                           channel.getChannelType(), sharedQueue.getSessionId(), e);
            }
        }
    }
    
    /**
     * 获取共享队列
     * @return 共享句子队列
     */
    public SharedSentenceQueue getSharedQueue() {
        return sharedQueue;
    }
    
    /**
     * 获取活跃通道列表
     * @return 通道列表
     */
    public List<OutputChannel> getChannels() {
        return channels;
    }
    
    /**
     * 获取多通道上下文
     * @return 上下文对象
     */
    public MultiChannelContext getContext() {
        return context;
    }
}

package com.chatbot.service;

import com.chatbot.model.ChatMessage;
import com.chatbot.model.UserPreferences;
import com.chatbot.service.channel.OutputChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * 多通道分发器
 * 负责将AI响应分发到不同的输出通道
 */
@Service
public class MultiChannelDispatcher {
    
    private static final Logger logger = LoggerFactory.getLogger(MultiChannelDispatcher.class);
    
    private final List<OutputChannel> availableChannels;
    private final UserPreferencesService userPreferencesService;
    private final ChatService chatService;
    
    // 会话级别的队列管理
    private final Map<String, SharedSentenceQueue> sessionQueues = new ConcurrentHashMap<>();
    
    public MultiChannelDispatcher(List<OutputChannel> availableChannels,
                                 UserPreferencesService userPreferencesService,
                                 ChatService chatService) {
        this.availableChannels = availableChannels;
        this.userPreferencesService = userPreferencesService;
        this.chatService = chatService;
        
        logger.info("初始化多通道分发器，可用通道数: {}", availableChannels.size());
        availableChannels.forEach(channel -> 
            logger.info("注册输出通道: type={}, mode={}", channel.getChannelType(), channel.getMode()));
    }
    
    /**
     * 处理消息并分发到多个通道
     * @param message 输入消息
     * @param responseCallback 响应回调
     * @return 任务ID
     */
    public String processMessage(ChatMessage message, Consumer<ChatMessage> responseCallback) {
        String sessionId = message.getSessionId();
        
        logger.info("开始多通道消息处理: sessionId={}", sessionId);
        
        try {
            // 获取用户偏好
            UserPreferences prefs = userPreferencesService.getUserPreferences(sessionId);
            
            // 获取启用的通道
            List<OutputChannel> activeChannels = getActiveChannels(prefs);
            
            if (activeChannels.isEmpty()) {
                logger.warn("没有启用的输出通道，降级为基础处理: sessionId={}", sessionId);
                return chatService.processMessage(message, responseCallback);
            }
            
            logger.info("启用的输出通道: sessionId={}, channels={}", sessionId, 
                       activeChannels.stream().map(OutputChannel::getChannelType).toList());
            
            // 创建或获取共享句子队列
            SharedSentenceQueue sharedQueue = getOrCreateSharedQueue(sessionId);
            
            // 创建多通道回调包装器，实现流式响应的拦截和分发
            Consumer<ChatMessage> multiChannelCallback = createMultiChannelCallback(
                sharedQueue, activeChannels, responseCallback);
            
            // 使用ChatService处理消息，但拦截响应进行多通道分发
            return chatService.processMessage(message, multiChannelCallback);
            
        } catch (Exception e) {
            logger.error("多通道消息处理失败: sessionId={}", sessionId, e);
            // 降级处理
            return chatService.processMessage(message, responseCallback);
        }
    }
    
    /**
     * 创建多通道回调包装器
     * @param sharedQueue 共享句子队列
     * @param activeChannels 活跃通道列表
     * @param originalCallback 原始回调
     * @return 多通道回调包装器
     */
    private Consumer<ChatMessage> createMultiChannelCallback(
            SharedSentenceQueue sharedQueue,
            List<OutputChannel> activeChannels, 
            Consumer<ChatMessage> originalCallback) {
        
        // 创建同步处理器
        SynchronizedProcessor processor = new SynchronizedProcessor(
            sharedQueue, activeChannels, originalCallback);
        
        return chatMessage -> {
            try {
                // 如果是思考内容，直接转发不进行TTS处理
                if (chatMessage.isThinking()) {
                    logger.debug("跳过思考内容的TTS处理: sessionId={}", chatMessage.getSessionId());
                    originalCallback.accept(chatMessage);
                    return;
                }
                
                // 如果是流式文本消息，进行句子分割处理
                if ("text".equals(chatMessage.getType()) && chatMessage.isStreaming()) {
                    String content = chatMessage.getContent();
                    if (content != null && !content.isEmpty()) {
                        processor.processChunk(content);
                    }
                    
                    // 如果流式完成，结束处理
                    if (chatMessage.isStreamComplete()) {
                        processor.finishProcessing();
                    }
                } else {
                    // 非流式消息或其他类型消息直接转发
                    originalCallback.accept(chatMessage);
                }
            } catch (Exception e) {
                logger.error("多通道回调处理失败: sessionId={}", 
                           chatMessage.getSessionId(), e);
                // 错误时直接转发原始消息
                originalCallback.accept(chatMessage);
            }
        };
    }
    
    /**
     * 获取启用的输出通道
     * @param prefs 用户偏好
     * @return 启用的通道列表
     */
    private List<OutputChannel> getActiveChannels(UserPreferences prefs) {
        List<OutputChannel> active = new ArrayList<>();
        
        for (OutputChannel channel : availableChannels) {
            try {
                // 创建临时上下文用于检查是否启用
                MultiChannelContext tempContext = new MultiChannelContext(
                    "temp", null, null);
                tempContext.setSharedData("userPreferences", prefs);
                
                if (channel.isEnabled(tempContext)) {
                    active.add(channel);
                    logger.debug("启用输出通道: type={}, mode={}", 
                               channel.getChannelType(), channel.getMode());
                }
            } catch (Exception e) {
                logger.error("检查通道启用状态失败: channelType={}", 
                           channel.getChannelType(), e);
            }
        }
        
        return active;
    }
    
    /**
     * 获取或创建共享句子队列
     * @param sessionId 会话ID
     * @return 共享句子队列
     */
    private SharedSentenceQueue getOrCreateSharedQueue(String sessionId) {
        return sessionQueues.computeIfAbsent(sessionId, SharedSentenceQueue::new);
    }
    
    /**
     * 清理会话资源
     * @param sessionId 会话ID
     */
    public void cleanupSession(String sessionId) {
        logger.info("清理多通道会话资源: sessionId={}", sessionId);
        
        SharedSentenceQueue queue = sessionQueues.remove(sessionId);
        if (queue != null) {
            queue.clear();
        }
        
        // 通知所有通道清理会话资源
        for (OutputChannel channel : availableChannels) {
            try {
                MultiChannelContext context = new MultiChannelContext(sessionId, null, queue);
                channel.cleanup(context);
            } catch (Exception e) {
                logger.error("通道清理失败: channelType={}, sessionId={}", 
                           channel.getChannelType(), sessionId, e);
            }
        }
    }
    
    /**
     * 根据类型获取通道
     * @param channelType 通道类型
     * @return 通道实例，如果不存在返回null
     */
    public OutputChannel getChannelByType(String channelType) {
        return availableChannels.stream()
                .filter(channel -> channel.getChannelType().equals(channelType))
                .findFirst()
                .orElse(null);
    }
    
    /**
     * 获取所有可用通道
     * @return 通道列表
     */
    public List<OutputChannel> getAvailableChannels() {
        return new ArrayList<>(availableChannels);
    }
    
    /**
     * 获取活跃会话数量
     * @return 会话数量
     */
    public int getActiveSessionCount() {
        return sessionQueues.size();
    }
    
    /**
     * 获取分发器状态
     * @return 状态信息
     */
    public String getStatus() {
        return String.format("MultiChannelDispatcher{channelCount=%d, activeSessionCount=%d}", 
                           availableChannels.size(), getActiveSessionCount());
    }
}

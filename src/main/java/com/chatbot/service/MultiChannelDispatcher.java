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
 * 用户选择模式分发器（原多通道分发器简化版）
 * 根据用户选择的模式（纯文本、字符流+TTS、句级TTS）选择对应的输出通道
 */
@Service
public class MultiChannelDispatcher {
    
    private static final Logger logger = LoggerFactory.getLogger(MultiChannelDispatcher.class);
    
    private final List<OutputChannel> availableChannels;
    private final UserPreferencesService userPreferencesService;
    private final ChatService chatService;
    
    // 会话级别的队列管理
    private final Map<String, SharedSentenceQueue> sessionQueues = new ConcurrentHashMap<>();
    
    /**
     * 通道选择结果
     */
    private static class ChannelSelection {
        final OutputChannel channel;
        final String mode;
        
        ChannelSelection(OutputChannel channel, String mode) {
            this.channel = channel;
            this.mode = mode;
        }
    }
    
    public MultiChannelDispatcher(List<OutputChannel> availableChannels,
                                 UserPreferencesService userPreferencesService,
                                 ChatService chatService) {
        this.availableChannels = availableChannels;
        this.userPreferencesService = userPreferencesService;
        this.chatService = chatService;
        
        logger.info("初始化用户选择模式分发器，可用通道数: {}", availableChannels.size());
        availableChannels.forEach(channel -> 
            logger.info("注册输出通道: type={}, mode={}", channel.getChannelType(), channel.getMode()));
    }
    
    /**
     * 处理消息并根据用户选择的模式选择对应的输出通道
     * @param message 输入消息
     * @param responseCallback 响应回调
     * @return 任务ID
     */
    public String processMessage(ChatMessage message, Consumer<ChatMessage> responseCallback) {
        String sessionId = message.getSessionId();
        
        logger.info("开始用户选择模式的消息处理: sessionId={}", sessionId);
        
        try {
            // 获取用户偏好
            UserPreferences prefs = userPreferencesService.getUserPreferences(sessionId);
            
            // 根据用户选择的模式选择对应的输出通道
            ChannelSelection selection = selectChannelByUserChoice(prefs);
            
            if (selection.channel == null) {
                logger.warn("没有找到对应的输出通道，降级为基础处理: sessionId={}", sessionId);
                return chatService.processMessage(message, responseCallback);
            }
            
            logger.info("用户选择模式: sessionId={}, mode={}, channel={}", sessionId, 
                       selection.mode, selection.channel.getChannelType());
            
            // 创建用户选择模式的回调包装器
            Consumer<ChatMessage> userModeCallback = createUserModeCallback(
                selection, responseCallback, sessionId);
            
            // 使用ChatService处理消息，通过用户选择的通道输出
            return chatService.processMessage(message, userModeCallback);
            
        } catch (Exception e) {
            logger.error("用户选择模式消息处理失败: sessionId={}", sessionId, e);
            // 降级处理
            return chatService.processMessage(message, responseCallback);
        }
    }
    
    /**
     * 根据用户选择的模式选择对应的输出通道
     * @param prefs 用户偏好
     * @return 通道选择结果
     */
    private ChannelSelection selectChannelByUserChoice(UserPreferences prefs) {
        // 获取用户的TTS设置
        boolean chatTTSEnabled = prefs.getChatOutput().isEnabled() && prefs.getChatOutput().isAutoTTS();
        String chatTTSMode = prefs.getChatOutput().getMode(); // "text_only", "char_stream_tts"
        boolean live2dEnabled = prefs.getLive2dOutput().isEnabled();
        
        logger.debug("用户TTS设置: chatTTSEnabled={}, chatTTSMode={}, live2dEnabled={}", 
                    chatTTSEnabled, chatTTSMode, live2dEnabled);
        
        // 模式选择逻辑：
        // 1. 如果Live2D启用 → 句级TTS模式 (Live2D通道)
        // 2. 如果聊天TTS启用且模式为char_stream_tts → 字符流+TTS模式 (ChatWindow通道)
        // 3. 其他情况 → 纯文本模式 (ChatWindow通道)
        
        if (live2dEnabled) {
            // 句级TTS模式 - 使用Live2D通道
            OutputChannel live2dChannel = findChannelByType("live2d");
            if (live2dChannel != null) {
                logger.debug("选择句级TTS模式 (Live2D通道)");
                return new ChannelSelection(live2dChannel, "sentence_tts");
            }
        }
        
        if (chatTTSEnabled && "char_stream_tts".equals(chatTTSMode)) {
            // 字符流+TTS模式 - 使用ChatWindow通道的字符流模式
            OutputChannel chatChannel = findChannelByType("chat_window");
            if (chatChannel != null) {
                logger.debug("选择字符流+TTS模式 (ChatWindow通道)");
                return new ChannelSelection(chatChannel, "char_stream_tts");
            }
        }
        
        // 纯文本模式 - 使用ChatWindow通道的文本模式
        OutputChannel chatChannel = findChannelByType("chat_window");
        if (chatChannel != null) {
            logger.debug("选择纯文本模式 (ChatWindow通道)");
            return new ChannelSelection(chatChannel, "text_only");
        }
        
        logger.warn("没有找到任何可用的输出通道");
        return new ChannelSelection(null, "none");
    }
    
    /**
     * 根据类型查找通道
     * @param channelType 通道类型
     * @return 通道实例，如果不存在返回null
     */
    private OutputChannel findChannelByType(String channelType) {
        return availableChannels.stream()
                .filter(channel -> channel.getChannelType().equals(channelType))
                .findFirst()
                .orElse(null);
    }
    
    /**
     * 创建用户选择模式的回调包装器
     * @param selection 通道选择结果
     * @param originalCallback 原始回调
     * @param sessionId 会话ID
     * @return 用户模式回调包装器
     */
    private Consumer<ChatMessage> createUserModeCallback(
            ChannelSelection selection,
            Consumer<ChatMessage> originalCallback, 
            String sessionId) {
        
        return chatMessage -> {
            try {
                // 如果是思考内容，直接转发不进行特殊处理
                if (chatMessage.isThinking()) {
                    logger.debug("跳过思考内容的特殊处理: sessionId={}", sessionId);
                    originalCallback.accept(chatMessage);
                    return;
                }
                
                // 根据用户选择的模式处理消息
                switch (selection.mode) {
                    case "text_only":
                        // 纯文本模式：直接转发给ChatWindow
                        originalCallback.accept(chatMessage);
                        break;
                        
                    case "char_stream_tts":
                        // 字符流+TTS模式：设置ttsMode并转发给ChatWindow
                        if ("text".equals(chatMessage.getType()) && chatMessage.isStreaming()) {
                            chatMessage.setTtsMode("char_stream");
                        }
                        originalCallback.accept(chatMessage);
                        break;
                        
                    case "sentence_tts":
                        // 句级TTS模式：使用Live2D通道处理
                        handleLive2DMode(selection.channel, chatMessage, originalCallback, sessionId);
                        break;
                        
                    default:
                        // 未知模式，直接转发
                        logger.warn("未知的用户选择模式: {}, sessionId={}", selection.mode, sessionId);
                        originalCallback.accept(chatMessage);
                        break;
                }
            } catch (Exception e) {
                logger.error("用户模式回调处理失败: sessionId={}, mode={}", 
                           sessionId, selection.mode, e);
                // 错误时直接转发原始消息
                originalCallback.accept(chatMessage);
            }
        };
    }
    
    /**
     * 处理Live2D模式
     */
    private void handleLive2DMode(OutputChannel live2dChannel, ChatMessage chatMessage, 
                                 Consumer<ChatMessage> originalCallback, String sessionId) {
        if ("text".equals(chatMessage.getType()) && chatMessage.isStreaming()) {
            // 创建上下文
            SharedSentenceQueue queue = getOrCreateSharedQueue(sessionId);
            MultiChannelContext context = new MultiChannelContext(sessionId, originalCallback, queue);
            
            String content = chatMessage.getContent();
            if (content != null && !content.isEmpty()) {
                // 使用Live2D通道处理内容
                live2dChannel.onNewSentence(content, 0, context);
                live2dChannel.startProcessing(context);
            }
            
            // 如果流式完成，通知通道结束处理
            if (chatMessage.isStreamComplete()) {
                logger.debug("通知Live2D通道流式完成: sessionId={}", sessionId);
                live2dChannel.onProcessingComplete(context);
                // 转发完成信号给前端
                originalCallback.accept(chatMessage);
            }
        } else {
            // 非流式消息直接转发
            originalCallback.accept(chatMessage);
        }
    }
    
    /**
     * 清理会话资源
     * @param sessionId 会话ID
     */
    public void cleanupSession(String sessionId) {
        logger.info("清理用户选择模式会话资源: sessionId={}", sessionId);
        
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
     * 获取或创建共享句子队列
     * @param sessionId 会话ID
     * @return 共享句子队列
     */
    private SharedSentenceQueue getOrCreateSharedQueue(String sessionId) {
        return sessionQueues.computeIfAbsent(sessionId, SharedSentenceQueue::new);
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

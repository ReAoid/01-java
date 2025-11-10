package com.chatbot.service;

import com.chatbot.model.domain.ChatMessage;
import com.chatbot.model.config.UserPreferences;
import com.chatbot.service.output.OutputStrategy;
import com.chatbot.service.session.UserPreferencesService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * 多通道分发器（简化版）
 * 根据用户选择的模式选择对应的输出策略
 * 
 * 重构说明：
 * - 移除了复杂的回调包装器
 * - 使用策略模式替代通道接口
 * - 减少了 3 层嵌套，从 443 行简化到约 150 行
 */
@Service
public class MultiChannelDispatcher {
    
    private static final Logger logger = LoggerFactory.getLogger(MultiChannelDispatcher.class);
    
    private final Map<String, OutputStrategy> strategies;
    private final UserPreferencesService userPreferencesService;
    private final ChatService chatService;
    
    /**
     * 构造函数
     * 
     * @param strategyList 所有可用的输出策略（Spring 自动注入）
     * @param userPreferencesService 用户偏好服务
     * @param chatService 聊天服务
     */
    public MultiChannelDispatcher(java.util.List<OutputStrategy> strategyList,
                                 UserPreferencesService userPreferencesService,
                                 ChatService chatService) {
        // 将策略列表转换为 Map，以策略名称为键
        this.strategies = strategyList.stream()
            .collect(Collectors.toMap(OutputStrategy::getStrategyName, s -> s));
        
        this.userPreferencesService = userPreferencesService;
        this.chatService = chatService;
        
        logger.info("多通道分发器初始化完成，可用策略数: {}", strategies.size());
        strategies.keySet().forEach(name -> 
            logger.info("注册输出策略: {}", name));
    }
    
    /**
     * 处理消息
     * 
     * @param message 输入消息
     * @param responseCallback 响应回调
     * @return 任务ID
     */
    public String processMessage(ChatMessage message, Consumer<ChatMessage> responseCallback) {
        String sessionId = message.getSessionId();
        
        logger.info("开始多通道消息处理: sessionId={}", sessionId);
        
        try {
            // 1. 获取用户偏好并选择策略
            UserPreferences prefs = userPreferencesService.getUserPreferences("Taiming");
            OutputStrategy strategy = selectStrategy(prefs);
            
            if (strategy == null) {
                logger.warn("未找到对应的输出策略，使用直接回调: sessionId={}", sessionId);
                return chatService.processMessage(message, responseCallback);
            }
            
            logger.info("选择输出策略: sessionId={}, strategy={}", 
                       sessionId, strategy.getStrategyName());
            
            // 2. 创建策略包装回调（只包装一次）
            Consumer<ChatMessage> strategyCallback = createStrategyCallback(
                strategy, responseCallback, sessionId);
            
            // 3. 调用 ChatService 处理消息
            return chatService.processMessage(message, strategyCallback);
            
        } catch (Exception e) {
            logger.error("多通道消息处理失败: sessionId={}", sessionId, e);
            // 降级处理：直接使用回调
            return chatService.processMessage(message, responseCallback);
        }
    }
    
    /**
     * 选择输出策略
     * 
     * @param prefs 用户偏好
     * @return 选中的策略，如果找不到返回 null
     */
    private OutputStrategy selectStrategy(UserPreferences prefs) {
        // 检查是否启用 Live2D
        if (prefs.getOutputChannel().getLive2d().isEnabled()) {
            OutputStrategy strategy = strategies.get("sentence_sync");
            if (strategy != null) {
                logger.debug("选择句级同步策略（Live2D 启用）");
                return strategy;
            }
        }
        
        // 根据聊天窗口模式选择策略
        String chatMode = prefs.getOutputChannel().getChatWindow().getMode();
        
        switch (chatMode) {
            case "text_only":
                return strategies.get("text_only");
            
            case "char_stream_tts":
                return strategies.get("char_stream_tts");
            
            case "sentence_tts":
                // 句级 TTS 使用同步策略
                return strategies.get("sentence_sync");
            
            default:
                logger.warn("未知的聊天模式: {}, 降级为纯文本模式", chatMode);
                return strategies.get("text_only");
        }
    }
    
    /**
     * 创建策略回调包装器
     * 
     * 这个包装器的作用：
     * 1. 将 ChatService 的流式输出转发给选定的策略
     * 2. 策略处理后再转发给原始回调
     * 
     * @param strategy 输出策略
     * @param originalCallback 原始回调
     * @param sessionId 会话ID
     * @return 包装后的回调
     */
    private Consumer<ChatMessage> createStrategyCallback(
            OutputStrategy strategy,
            Consumer<ChatMessage> originalCallback,
            String sessionId) {
        
        return chatMessage -> {
            try {
                // 如果是流完成信号
                if (chatMessage.isStreamComplete()) {
                    logger.debug("收到流完成信号: sessionId={}", sessionId);
                    strategy.onStreamComplete(originalCallback, sessionId);
                    return;
                }
                
                // 处理流式文本块
                String content = chatMessage.getContent();
                boolean isThinking = chatMessage.isThinking();
                
                if (content != null && !content.isEmpty()) {
                    strategy.processChunk(content, originalCallback, sessionId, isThinking);
                }
                
            } catch (Exception e) {
                logger.error("策略回调处理失败: sessionId={}, strategy={}", 
                           sessionId, strategy.getStrategyName(), e);
                // 降级：直接转发消息
                originalCallback.accept(chatMessage);
            }
        };
    }
    
    /**
     * 清理会话资源
     * 
     * @param sessionId 会话ID
     */
    public void cleanupSession(String sessionId) {
        logger.info("清理多通道会话资源: sessionId={}", sessionId);
        
        // 通知所有策略清理资源
        for (OutputStrategy strategy : strategies.values()) {
            try {
                strategy.cleanup(sessionId);
            } catch (Exception e) {
                logger.error("策略清理失败: strategy={}, sessionId={}", 
                           strategy.getStrategyName(), sessionId, e);
            }
        }
    }
    
    /**
     * 获取指定策略
     * 
     * @param strategyName 策略名称
     * @return 策略实例，如果不存在返回 null
     */
    public OutputStrategy getStrategy(String strategyName) {
        return strategies.get(strategyName);
    }
    
    /**
     * 获取所有可用策略
     * 
     * @return 策略 Map
     */
    public Map<String, OutputStrategy> getAllStrategies() {
        return strategies;
    }
    
    /**
     * 获取分发器状态
     * 
     * @return 状态信息
     */
    public String getStatus() {
        return String.format("MultiChannelDispatcher{strategyCount=%d, strategies=%s}", 
                           strategies.size(), strategies.keySet());
    }
}

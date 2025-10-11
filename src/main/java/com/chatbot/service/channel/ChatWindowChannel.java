package com.chatbot.service.channel;

import com.chatbot.model.ChatMessage;
import com.chatbot.model.SentenceItem;
import com.chatbot.model.UserPreferences;
import com.chatbot.service.MultiChannelContext;
import com.chatbot.service.MultiModalService;
import com.chatbot.service.UserPreferencesService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 聊天窗口通道实现
 * 支持模式1（纯文本）和模式2（字符流+TTS）
 */
@Component
public class ChatWindowChannel implements OutputChannel {
    
    private static final Logger logger = LoggerFactory.getLogger(ChatWindowChannel.class);
    
    private final UserPreferencesService userPreferencesService;
    private final MultiModalService multiModalService;
    
    // 会话级别的句子存储
    private final Map<String, List<SentenceItem>> sessionSentences = new ConcurrentHashMap<>();
    
    @Autowired
    public ChatWindowChannel(UserPreferencesService userPreferencesService,
                           MultiModalService multiModalService) {
        this.userPreferencesService = userPreferencesService;
        this.multiModalService = multiModalService;
    }
    
    @Override
    public String getChannelType() {
        return "chat_window";
    }
    
    @Override
    public String getMode() {
        // 动态获取模式，基于用户偏好
        return "dynamic";
    }
    
    @Override
    public void onNewSentence(String sentence, int order, MultiChannelContext context) {
        String sessionId = context.getSessionId();
        
        try {
            UserPreferences prefs = userPreferencesService.getUserPreferences(sessionId);
            String chatMode = getChatMode(prefs);
            
            logger.debug("聊天窗口接收新句子: order={}, mode={}, sessionId={}", 
                        order, chatMode, sessionId);
            
            // 记录句子用于后续处理
            SentenceItem sentenceItem = new SentenceItem(sentence, order, sessionId);
            sessionSentences.computeIfAbsent(sessionId, k -> new ArrayList<>()).add(sentenceItem);
            
            switch (chatMode) {
                case "text_only":
                    processTextOnlyMode(sentence, order, context);
                    break;
                case "char_stream_tts":
                    processCharStreamMode(sentence, order, context);
                    break;
                default:
                    logger.warn("未知的聊天模式: {}, 降级为文本模式", chatMode);
                    processTextOnlyMode(sentence, order, context);
            }
            
        } catch (Exception e) {
            logger.error("聊天窗口处理新句子失败: order={}, sessionId={}", 
                        order, sessionId, e);
        }
    }
    
    @Override
    public void startProcessing(MultiChannelContext context) {
        // 聊天窗口通道在onNewSentence中已经处理了，这里不需要额外操作
        logger.debug("聊天窗口通道开始处理: sessionId={}", context.getSessionId());
    }
    
    @Override
    public void onProcessingComplete(MultiChannelContext context) {
        String sessionId = context.getSessionId();
        logger.debug("聊天窗口通道处理完成: sessionId={}", sessionId);
        
        // 发送流式完成消息
        ChatMessage completionMessage = new ChatMessage();
        completionMessage.setType("text");
        completionMessage.setChannelType(getChannelType());
        completionMessage.setStreaming(false);
        completionMessage.setStreamComplete(true);
        completionMessage.setSessionId(sessionId);
        
        context.sendMessage(completionMessage, getChannelType());
    }
    
    @Override
    public boolean isEnabled(MultiChannelContext context) {
        try {
            UserPreferences prefs = context.getSharedData("userPreferences", UserPreferences.class);
            if (prefs == null) {
                prefs = userPreferencesService.getUserPreferences(context.getSessionId());
            }
            
            // 聊天窗口通道默认总是启用的
            return true;
            
        } catch (Exception e) {
            logger.error("检查聊天窗口通道启用状态失败: sessionId={}", 
                        context.getSessionId(), e);
            return true; // 默认启用
        }
    }
    
    @Override
    public void cleanup(MultiChannelContext context) {
        String sessionId = context.getSessionId();
        List<SentenceItem> removed = sessionSentences.remove(sessionId);
        
        if (removed != null) {
            logger.debug("清理聊天窗口通道资源: sessionId={}, sentenceCount={}", 
                        sessionId, removed.size());
        }
    }
    
    /**
     * 获取聊天模式
     */
    private String getChatMode(UserPreferences prefs) {
        // 优先使用新的配置结构
        if (prefs.getChatOutput() != null) {
            return prefs.getChatOutput().getMode();
        }
        
        // 降级到旧的配置方式
        if (prefs.isEnableVoice()) {
            return "char_stream_tts";
        } else {
            return "text_only";
        }
    }
    
    /**
     * 处理纯文本模式（模式1）
     */
    private void processTextOnlyMode(String sentence, int order, MultiChannelContext context) {
        ChatMessage textMessage = new ChatMessage();
        textMessage.setType("text");
        textMessage.setChannelType(getChannelType());
        textMessage.setContent(sentence);
        textMessage.setStreaming(true);
        textMessage.setStreamComplete(false);
        textMessage.setSentenceOrder(order);
        
        context.sendMessage(textMessage, getChannelType());
        
        logger.trace("发送纯文本消息: order={}, sessionId={}", order, context.getSessionId());
    }
    
    /**
     * 处理字符流+TTS模式（模式2）
     */
    private void processCharStreamMode(String sentence, int order, MultiChannelContext context) {
        // 1. 立即发送文本
        ChatMessage textMessage = new ChatMessage();
        textMessage.setType("text");
        textMessage.setChannelType(getChannelType());
        textMessage.setTtsMode("char_stream");
        textMessage.setContent(sentence);
        textMessage.setStreaming(true);
        textMessage.setStreamComplete(false);
        textMessage.setSentenceOrder(order);
        
        context.sendMessage(textMessage, getChannelType());
        
        // 2. 异步生成TTS
        generateTTSAsync(sentence, order, context);
        
        logger.debug("发送字符流消息并启动TTS: order={}, sessionId={}", 
                    order, context.getSessionId());
    }
    
    /**
     * 异步生成TTS音频
     */
    @Async
    public void generateTTSAsync(String sentence, int order, MultiChannelContext context) {
        String sessionId = context.getSessionId();
        
        try {
            // 获取用户TTS偏好
            UserPreferences prefs = userPreferencesService.getUserPreferences(sessionId);
            String voice = prefs.getPreferredTtsVoice();
            
            logger.debug("开始生成TTS: order={}, voice={}, sessionId={}", order, voice, sessionId);
            
            // 调用TTS服务
            CompletableFuture<byte[]> ttsTask = multiModalService.textToSpeech(sentence, voice, "mp3");
            byte[] audioData = ttsTask.get(10, TimeUnit.SECONDS);
            
            // 发送音频消息
            ChatMessage audioMessage = new ChatMessage();
            audioMessage.setType("audio");
            audioMessage.setChannelType(getChannelType());
            audioMessage.setTtsMode("char_stream");
            audioMessage.setContent(sentence);
            audioMessage.setAudioData(audioData);
            audioMessage.setSentenceOrder(order);
            audioMessage.setSentenceId(generateSentenceId(sessionId, order));
            
            context.sendMessage(audioMessage, getChannelType());
            
            logger.debug("TTS生成完成: order={}, audioSize={}bytes, sessionId={}", 
                        order, audioData.length, sessionId);
            
        } catch (Exception e) {
            logger.error("TTS生成失败: order={}, sessionId={}", order, sessionId, e);
            
            // 发送TTS错误消息
            ChatMessage errorMessage = new ChatMessage();
            errorMessage.setType("tts_error");
            errorMessage.setChannelType(getChannelType());
            errorMessage.setContent("语音生成失败: " + sentence);
            errorMessage.setSentenceOrder(order);
            
            context.sendMessage(errorMessage, getChannelType());
        }
    }
    
    /**
     * 生成句子ID
     */
    private String generateSentenceId(String sessionId, int order) {
        return String.format("chat_sentence_%s_%d", sessionId, order);
    }
    
    @Override
    public String getStatus() {
        return String.format("ChatWindowChannel{activeSessions=%d}", sessionSentences.size());
    }
}

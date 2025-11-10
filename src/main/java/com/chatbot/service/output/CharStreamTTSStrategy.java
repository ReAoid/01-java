package com.chatbot.service.output;

import com.chatbot.config.AppConfig;
import com.chatbot.model.domain.ChatMessage;
import com.chatbot.model.config.UserPreferences;
import com.chatbot.service.MultiModalService;
import com.chatbot.service.session.UserPreferencesService;
import com.chatbot.service.processor.SentenceProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 字符流 + TTS 策略
 * 文字立即显示，TTS 异步生成
 * 
 * 适用场景：用户希望看到实时文字流，同时后台生成语音
 */
@Component
public class CharStreamTTSStrategy implements OutputStrategy {
    
    private static final Logger logger = LoggerFactory.getLogger(CharStreamTTSStrategy.class);
    
    private final UserPreferencesService userPreferencesService;
    private final MultiModalService multiModalService;
    private final AppConfig appConfig;
    
    // 会话级别的句子处理器（按需创建）
    private final Map<String, SentenceProcessor> sessionProcessors = new ConcurrentHashMap<>();
    // 会话级别的句子顺序计数器
    private final Map<String, AtomicInteger> sessionCounters = new ConcurrentHashMap<>();
    
    public CharStreamTTSStrategy(UserPreferencesService userPreferencesService,
                                MultiModalService multiModalService,
                                AppConfig appConfig) {
        this.userPreferencesService = userPreferencesService;
        this.multiModalService = multiModalService;
        this.appConfig = appConfig;
    }
    
    @Override
    public String getStrategyName() {
        return "char_stream_tts";
    }
    
    @Override
    public void processChunk(String chunk, Consumer<ChatMessage> callback, String sessionId, boolean isThinking) {
        if (chunk == null || chunk.isEmpty()) {
            return;
        }
        
        // 1. 立即发送文本消息（不等待 TTS）
        ChatMessage textMessage = new ChatMessage();
        textMessage.setType("text");
        textMessage.setContent(chunk);
        textMessage.setRole("assistant");
        textMessage.setSessionId(sessionId);
        textMessage.setStreaming(true);
        textMessage.setStreamComplete(false);
        textMessage.setThinking(isThinking);
        
        if (isThinking) {
            textMessage.setThinkingContent(chunk);
        }
        
        callback.accept(textMessage);
        
        // 2. 如果是思考内容，不生成 TTS
        if (isThinking) {
            logger.trace("跳过思考内容的TTS生成: sessionId={}", sessionId);
            return;
        }
        
        // 3. 将文本块添加到句子处理器
        SentenceProcessor processor = sessionProcessors.computeIfAbsent(
            sessionId, k -> SentenceProcessor.getOrCreate(sessionId));
        
        processor.addTextChunk(chunk);
        
        // 4. 检查是否有完整句子，如果有则异步生成 TTS
        while (processor.hasPendingSentence()) {
            String sentence = processor.extractSentence();
            if (sentence != null && !sentence.isEmpty()) {
                int order = sessionCounters.computeIfAbsent(sessionId, k -> new AtomicInteger(0))
                                          .getAndIncrement();
                generateTTSAsync(sentence, order, callback, sessionId);
            }
        }
        
        logger.trace("字符流TTS策略处理chunk: length={}, sessionId={}", chunk.length(), sessionId);
    }
    
    @Override
    public void onStreamComplete(Consumer<ChatMessage> callback, String sessionId) {
        // 1. 发送流完成信号
        ChatMessage finalMessage = new ChatMessage();
        finalMessage.setType("text");
        finalMessage.setContent("");
        finalMessage.setRole("assistant");
        finalMessage.setSessionId(sessionId);
        finalMessage.setStreaming(true);
        finalMessage.setStreamComplete(true);
        
        callback.accept(finalMessage);
        
        // 2. 处理缓冲区中剩余的文本（作为最后一个句子）
        SentenceProcessor processor = sessionProcessors.get(sessionId);
        if (processor != null) {
            String remaining = processor.getRemainingText();
            if (!remaining.isEmpty()) {
                int order = sessionCounters.computeIfAbsent(sessionId, k -> new AtomicInteger(0))
                                          .getAndIncrement();
                generateTTSAsync(remaining, order, callback, sessionId);
                logger.debug("处理剩余文本: length={}, sessionId={}", remaining.length(), sessionId);
            }
        }
        
        logger.debug("字符流TTS策略流完成: sessionId={}", sessionId);
    }
    
    @Override
    public void cleanup(String sessionId) {
        sessionProcessors.remove(sessionId);
        sessionCounters.remove(sessionId);
        SentenceProcessor.remove(sessionId);
        logger.debug("清理字符流TTS策略资源: sessionId={}", sessionId);
    }
    
    /**
     * 异步生成 TTS 音频
     */
    @Async
    public void generateTTSAsync(String sentence, int order, Consumer<ChatMessage> callback, String sessionId) {
        try {
            // 获取用户 TTS 偏好
            UserPreferences prefs = userPreferencesService.getUserPreferences("Taiming");
            String speakerId = prefs.getTts().getPreferredSpeaker();
            
            logger.debug("开始生成TTS: order={}, speakerId={}, sessionId={}", order, speakerId, sessionId);
            
            // 调用 TTS 服务
            CompletableFuture<byte[]> ttsTask = multiModalService.textToSpeech(sentence, speakerId, "wav");
            int timeoutSeconds = appConfig.getPython().getTimeout().getTtsTaskTimeoutSeconds();
            byte[] audioData = ttsTask.get(timeoutSeconds, TimeUnit.SECONDS);
            
            // 发送音频消息
            ChatMessage audioMessage = new ChatMessage();
            audioMessage.setType("audio");
            audioMessage.setChannelType("chat_window");
            audioMessage.setTtsMode("char_stream");
            audioMessage.setContent(sentence);
            audioMessage.setAudioData(audioData);
            audioMessage.setSentenceOrder(order);
            audioMessage.setSentenceId(generateSentenceId(sessionId, order));
            audioMessage.setSessionId(sessionId);
            
            callback.accept(audioMessage);
            
            logger.info("TTS生成完成: order={}, audioSize={}bytes, sessionId={}", 
                       order, audioData.length, sessionId);
            
        } catch (Exception e) {
            logger.error("TTS生成失败: order={}, sessionId={}", order, sessionId, e);
            
            // 发送 TTS 错误消息
            ChatMessage errorMessage = new ChatMessage();
            errorMessage.setType("tts_error");
            errorMessage.setChannelType("chat_window");
            errorMessage.setContent("语音生成失败: " + sentence);
            errorMessage.setSentenceOrder(order);
            errorMessage.setSessionId(sessionId);
            
            callback.accept(errorMessage);
        }
    }
    
    /**
     * 生成句子 ID
     */
    private String generateSentenceId(String sessionId, int order) {
        return String.format("char_stream_%s_%d", sessionId, order);
    }
}


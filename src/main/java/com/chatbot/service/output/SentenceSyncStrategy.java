package com.chatbot.service.output;

import com.chatbot.config.AppConfig;
import com.chatbot.model.domain.ChatMessage;
import com.chatbot.model.domain.SentenceItem;
import com.chatbot.model.config.UserPreferences;
import com.chatbot.service.MultiModalService;
import com.chatbot.service.session.UserPreferencesService;
import com.chatbot.service.processor.SentenceProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * 句级同步策略
 * 严格的句子级同步处理，等待前一句播放完成才处理下一句
 * 
 * 适用场景：Live2D 角色需要严格同步口型和文字显示
 */
@Component
public class SentenceSyncStrategy implements OutputStrategy {
    
    private static final Logger logger = LoggerFactory.getLogger(SentenceSyncStrategy.class);
    
    private final UserPreferencesService userPreferencesService;
    private final MultiModalService multiModalService;
    private final AppConfig appConfig;
    
    // 会话级别的同步队列
    private final Map<String, SyncQueue> sessionQueues = new ConcurrentHashMap<>();
    // 会话级别的句子处理器
    private final Map<String, SentenceProcessor> sessionProcessors = new ConcurrentHashMap<>();
    // 会话级别的句子顺序计数器
    private final Map<String, AtomicInteger> sessionCounters = new ConcurrentHashMap<>();
    
    public SentenceSyncStrategy(UserPreferencesService userPreferencesService,
                               MultiModalService multiModalService,
                               AppConfig appConfig) {
        this.userPreferencesService = userPreferencesService;
        this.multiModalService = multiModalService;
        this.appConfig = appConfig;
    }
    
    @Override
    public String getStrategyName() {
        return "sentence_sync";
    }
    
    @Override
    public void processChunk(String chunk, Consumer<ChatMessage> callback, String sessionId, boolean isThinking) {
        if (chunk == null || chunk.isEmpty()) {
            return;
        }
        
        // 思考内容也正常显示，但不生成 TTS
        if (isThinking) {
            ChatMessage thinkingMessage = new ChatMessage();
            thinkingMessage.setType("text");
            thinkingMessage.setContent(chunk);
            thinkingMessage.setRole("assistant");
            thinkingMessage.setSessionId(sessionId);
            thinkingMessage.setStreaming(true);
            thinkingMessage.setStreamComplete(false);
            thinkingMessage.setThinking(true);
            thinkingMessage.setThinkingContent(chunk);
            
            callback.accept(thinkingMessage);
            return;
        }
        
        // 将文本块添加到句子处理器
        SentenceProcessor processor = sessionProcessors.computeIfAbsent(
            sessionId, k -> SentenceProcessor.getOrCreate(sessionId));
        
        processor.addTextChunk(chunk);
        
        // 提取完整句子并添加到同步队列
        while (processor.hasPendingSentence()) {
            String sentence = processor.extractSentence();
            if (sentence != null && !sentence.isEmpty()) {
                int order = sessionCounters.computeIfAbsent(sessionId, k -> new AtomicInteger(0))
                                          .getAndIncrement();
                
                // 获取或创建同步队列
                SyncQueue queue = sessionQueues.computeIfAbsent(sessionId, 
                    k -> new SyncQueue(sessionId, callback, this));
                
                queue.addSentence(sentence, order);
                logger.debug("添加句子到同步队列: order={}, sessionId={}", order, sessionId);
            }
        }
        
        logger.trace("句级同步策略处理chunk: length={}, sessionId={}", chunk.length(), sessionId);
    }
    
    @Override
    public void onStreamComplete(Consumer<ChatMessage> callback, String sessionId) {
        // 处理缓冲区中剩余的文本
        SentenceProcessor processor = sessionProcessors.get(sessionId);
        if (processor != null) {
            String remaining = processor.getRemainingText();
            if (!remaining.isEmpty()) {
                int order = sessionCounters.computeIfAbsent(sessionId, k -> new AtomicInteger(0))
                                          .getAndIncrement();
                
                SyncQueue queue = sessionQueues.get(sessionId);
                if (queue != null) {
                    queue.addSentence(remaining, order);
                    logger.debug("添加剩余文本到同步队列: order={}, sessionId={}", order, sessionId);
                }
            }
        }
        
        // 标记流完成，开始同步处理
        SyncQueue queue = sessionQueues.get(sessionId);
        if (queue != null) {
            queue.markStreamComplete();
            logger.info("句级同步策略开始处理: sessionId={}, sentenceCount={}", 
                       sessionId, queue.size());
        }
        
        logger.debug("句级同步策略流完成: sessionId={}", sessionId);
    }
    
    @Override
    public void cleanup(String sessionId) {
        SyncQueue removed = sessionQueues.remove(sessionId);
        if (removed != null) {
            removed.cleanup();
        }
        sessionProcessors.remove(sessionId);
        sessionCounters.remove(sessionId);
        SentenceProcessor.remove(sessionId);
        
        logger.debug("清理句级同步策略资源: sessionId={}", sessionId);
    }
    
    /**
     * 处理音频播放完成事件（由前端通知）
     */
    public void onAudioCompleted(String sessionId, String sentenceId) {
        SyncQueue queue = sessionQueues.get(sessionId);
        if (queue != null) {
            queue.onAudioCompleted(sentenceId);
        } else {
            logger.warn("音频完成通知但未找到队列: sessionId={}, sentenceId={}", 
                       sessionId, sentenceId);
        }
    }
    
    /**
     * 异步生成同步 TTS 音频
     */
    @Async
    public void generateSyncTTS(SentenceItem sentence, Consumer<ChatMessage> callback, 
                               String sessionId, SyncQueue queue) {
        String sentenceId = sentence.getId();
        
        try {
            // 获取 Live2D 语音偏好
            UserPreferences prefs = userPreferencesService.getUserPreferences("Taiming");
            String speakerId = prefs.getOutputChannel().getLive2d().getSpeakerId();
            
            logger.debug("开始生成句级同步TTS: id={}, speakerId={}, sessionId={}", 
                        sentenceId, speakerId, sessionId);
            
            // 调用 TTS 服务
            CompletableFuture<byte[]> ttsTask = multiModalService.textToSpeech(
                sentence.getText(), speakerId, "wav");
            int timeoutSeconds = appConfig.getPython().getTimeout().getLive2dTtsTaskTimeoutSeconds();
            byte[] audioData = ttsTask.get(timeoutSeconds, TimeUnit.SECONDS);
            
            // 发送音频消息
            ChatMessage audioMessage = new ChatMessage();
            audioMessage.setType("live2d_sentence_audio");
            audioMessage.setChannelType("live2d");
            audioMessage.setTtsMode("sentence_sync");
            audioMessage.setSentenceId(sentenceId);
            audioMessage.setSentenceOrder(sentence.getOrder());
            audioMessage.setContent(sentence.getText());
            audioMessage.setAudioData(audioData);
            audioMessage.setAudioReady(true);
            audioMessage.setSessionId(sessionId);
            
            callback.accept(audioMessage);
            
            logger.info("句级同步TTS生成完成: id={}, audioSize={}bytes, sessionId={}", 
                       sentenceId, audioData.length, sessionId);
            
        } catch (Exception e) {
            logger.error("句级同步TTS生成失败: id={}, sessionId={}", sentenceId, sessionId, e);
            
            // 发送 TTS 失败消息
            ChatMessage errorMessage = new ChatMessage();
            errorMessage.setType("live2d_audio_failed");
            errorMessage.setChannelType("live2d");
            errorMessage.setSentenceId(sentenceId);
            errorMessage.setSentenceOrder(sentence.getOrder());
            errorMessage.setContent("TTS生成失败: " + sentence.getText());
            errorMessage.setSessionId(sessionId);
            
            callback.accept(errorMessage);
            
            // 通知队列 TTS 失败，跳过当前句子
            queue.onTTSError(sentenceId);
        }
    }
    
    /**
     * 同步队列
     * 严格控制句子的处理顺序
     */
    private static class SyncQueue {
        private final String sessionId;
        private final Consumer<ChatMessage> callback;
        private final SentenceSyncStrategy strategy;
        private final Queue<SentenceItem> sentences = new LinkedList<>();
        
        private boolean isProcessing = false;
        private boolean streamComplete = false;
        private String currentSentenceId = null;
        
        public SyncQueue(String sessionId, Consumer<ChatMessage> callback, SentenceSyncStrategy strategy) {
            this.sessionId = sessionId;
            this.callback = callback;
            this.strategy = strategy;
        }
        
        public void addSentence(String text, int order) {
            SentenceItem sentence = new SentenceItem(text, order, sessionId);
            sentences.offer(sentence);
        }
        
        public void markStreamComplete() {
            streamComplete = true;
            // 流完成后开始处理
            if (!isProcessing) {
                startProcessing();
            }
        }
        
        private void startProcessing() {
            if (isProcessing) {
                return;
            }
            isProcessing = true;
            processNextSentence();
        }
        
        private void processNextSentence() {
            if (!isProcessing || !streamComplete) {
                return;
            }
            
            SentenceItem sentence = sentences.poll();
            if (sentence == null) {
                // 所有句子处理完毕
                isProcessing = false;
                sendAllComplete();
                return;
            }
            
            currentSentenceId = sentence.getId();
            
            logger.debug("开始处理同步句子: order={}, id={}, sessionId={}", 
                        sentence.getOrder(), currentSentenceId, sessionId);
            
            // 1. 立即发送显示消息
            sendDisplayMessage(sentence);
            
            // 2. 异步生成 TTS
            strategy.generateSyncTTS(sentence, callback, sessionId, this);
        }
        
        private void sendDisplayMessage(SentenceItem sentence) {
            ChatMessage displayMessage = new ChatMessage();
            displayMessage.setType("live2d_sentence_display");
            displayMessage.setChannelType("live2d");
            displayMessage.setSentenceId(sentence.getId());
            displayMessage.setSentenceOrder(sentence.getOrder());
            displayMessage.setContent(sentence.getText());
            displayMessage.setTtsMode("sentence_sync");
            displayMessage.setSessionId(sessionId);
            
            callback.accept(displayMessage);
        }
        
        private void sendAllComplete() {
            ChatMessage completionMessage = new ChatMessage();
            completionMessage.setType("live2d_all_complete");
            completionMessage.setChannelType("live2d");
            completionMessage.setSessionId(sessionId);
            
            callback.accept(completionMessage);
            
            logger.info("同步队列处理完成: sessionId={}", sessionId);
        }
        
        public void onAudioCompleted(String sentenceId) {
            if (currentSentenceId != null && currentSentenceId.equals(sentenceId)) {
                logger.debug("音频播放完成，处理下一句: sentenceId={}, sessionId={}", 
                           sentenceId, sessionId);
                
                currentSentenceId = null;
                processNextSentence();
            } else {
                logger.warn("音频完成通知的句子ID不匹配: expected={}, actual={}, sessionId={}", 
                           currentSentenceId, sentenceId, sessionId);
            }
        }
        
        public void onTTSError(String sentenceId) {
            if (currentSentenceId != null && currentSentenceId.equals(sentenceId)) {
                logger.warn("TTS失败，跳过当前句子: sentenceId={}, sessionId={}", 
                           sentenceId, sessionId);
                // 跳过当前句子，继续下一句
                onAudioCompleted(sentenceId);
            }
        }
        
        public int size() {
            return sentences.size();
        }
        
        public void cleanup() {
            sentences.clear();
            isProcessing = false;
            streamComplete = false;
            currentSentenceId = null;
        }
    }
}


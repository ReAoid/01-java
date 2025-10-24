package com.chatbot.service.channel;

import com.chatbot.config.AppConfig;
import com.chatbot.model.ChatMessage;
import com.chatbot.model.SentenceItem;
import com.chatbot.model.UserPreferences;
import com.chatbot.service.MultiChannelContext;
import com.chatbot.service.MultiModalService;
import com.chatbot.service.UserPreferencesService;
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

/**
 * Live2D通道实现
 * 支持模式3（句级气泡+句级TTS）严格同步
 */
@Component
public class Live2DChannel implements OutputChannel {
    
    private static final Logger logger = LoggerFactory.getLogger(Live2DChannel.class);
    
    private final UserPreferencesService userPreferencesService;
    private final MultiModalService multiModalService;
    private final AppConfig appConfig;
    
    // 会话级别的同步队列管理
    private final Map<String, Live2DSyncQueue> sessionQueues = new ConcurrentHashMap<>();
    
    public Live2DChannel(UserPreferencesService userPreferencesService,
                        MultiModalService multiModalService,
                        AppConfig appConfig) {
        this.userPreferencesService = userPreferencesService;
        this.multiModalService = multiModalService;
        this.appConfig = appConfig;
    }
    
    @Override
    public String getChannelType() {
        return "live2d";
    }
    
    @Override
    public String getMode() {
        return "sentence_sync";
    }
    
    @Override
    public void onNewSentence(String sentence, int order, MultiChannelContext context) {
        String sessionId = context.getSessionId();
        
        logger.debug("Live2D通道接收新句子: order={}, sessionId={}", order, sessionId);
        
        // 获取或创建同步队列
        Live2DSyncQueue queue = sessionQueues.computeIfAbsent(sessionId, 
            k -> new Live2DSyncQueue(sessionId, context, this));
        
        // 添加句子到队列（但不立即处理）
        queue.addSentence(sentence, order);
    }
    
    @Override
    public void startProcessing(MultiChannelContext context) {
        String sessionId = context.getSessionId();
        Live2DSyncQueue queue = sessionQueues.get(sessionId);
        
        if (queue != null) {
            logger.info("Live2D通道开始同步处理: sessionId={}, sentenceCount={}", 
                       sessionId, queue.size());
            queue.startSyncProcessing();
        } else {
            logger.warn("Live2D通道未找到队列: sessionId={}", sessionId);
        }
    }
    
    @Override
    public void onProcessingComplete(MultiChannelContext context) {
        String sessionId = context.getSessionId();
        logger.debug("Live2D通道处理完成: sessionId={}", sessionId);
        
        // 可以在这里发送一个全局完成消息
        ChatMessage completionMessage = new ChatMessage();
        completionMessage.setType("live2d_all_complete");
        completionMessage.setChannelType(getChannelType());
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
            
            // 检查Live2D功能是否启用
            return prefs.getLive2dOutput() != null && prefs.getLive2dOutput().isEnabled();
            
        } catch (Exception e) {
            logger.error("检查Live2D通道启用状态失败: sessionId={}", 
                        context.getSessionId(), e);
            return false; // 默认不启用
        }
    }
    
    @Override
    public void cleanup(MultiChannelContext context) {
        String sessionId = context.getSessionId();
        Live2DSyncQueue removed = sessionQueues.remove(sessionId);
        
        if (removed != null) {
            removed.cleanup();
            logger.debug("清理Live2D通道资源: sessionId={}", sessionId);
        }
    }
    
    /**
     * 处理音频播放完成事件（由前端通知）
     */
    public void onAudioCompleted(String sessionId, String sentenceId) {
        Live2DSyncQueue queue = sessionQueues.get(sessionId);
        if (queue != null) {
            queue.onAudioCompleted(sentenceId);
        } else {
            logger.warn("音频完成通知但未找到队列: sessionId={}, sentenceId={}", 
                       sessionId, sentenceId);
        }
    }
    
    @Override
    public String getStatus() {
        return String.format("Live2DChannel{activeSessions=%d}", sessionQueues.size());
    }
    
    /**
     * Live2D同步队列
     * 负责严格的句子级同步处理
     */
    private static class Live2DSyncQueue {
        private final String sessionId;
        private final MultiChannelContext context;
        private final Live2DChannel channel;
        private final Queue<SentenceItem> sentences = new LinkedList<>();
        
        private boolean isProcessing = false;
        private String currentSentenceId = null;
        
        public Live2DSyncQueue(String sessionId, MultiChannelContext context, Live2DChannel channel) {
            this.sessionId = sessionId;
            this.context = context;
            this.channel = channel;
        }
        
        public void addSentence(String text, int order) {
            SentenceItem sentence = new SentenceItem(text, order, sessionId);
            sentences.offer(sentence);
            
            logger.trace("添加句子到Live2D队列: order={}, sessionId={}", order, sessionId);
        }
        
        public void startSyncProcessing() {
            if (isProcessing) {
                logger.warn("Live2D队列已在处理中: sessionId={}", sessionId);
                return;
            }
            
            isProcessing = true;
            processNextSentence();
        }
        
        private void processNextSentence() {
            if (!isProcessing) {
                return;
            }
            
            SentenceItem sentence = sentences.poll();
            if (sentence == null) {
                // 所有句子处理完毕
                isProcessing = false;
                logger.info("Live2D队列处理完成: sessionId={}", sessionId);
                return;
            }
            
            currentSentenceId = sentence.getId();
            
            logger.debug("Live2D开始处理句子: order={}, id={}, sessionId={}", 
                        sentence.getOrder(), currentSentenceId, sessionId);
            
            // 1. 立即发送显示消息
            sendDisplayMessage(sentence);
            
            // 2. 异步生成TTS
            channel.generateSyncTTS(sentence, context, this);
        }
        
        private void sendDisplayMessage(SentenceItem sentence) {
            ChatMessage displayMessage = new ChatMessage();
            displayMessage.setType("live2d_sentence_display");
            displayMessage.setChannelType("live2d");
            displayMessage.setSentenceId(sentence.getId());
            displayMessage.setSentenceOrder(sentence.getOrder());
            displayMessage.setContent(sentence.getText());
            displayMessage.setTtsMode("sentence_sync");
            
            context.sendMessage(displayMessage, "live2d");
            
            logger.trace("发送Live2D显示消息: id={}, sessionId={}", 
                        sentence.getId(), sessionId);
        }
        
        public void onAudioCompleted(String sentenceId) {
            if (currentSentenceId != null && currentSentenceId.equals(sentenceId)) {
                logger.debug("Live2D音频播放完成，处理下一句: sentenceId={}, sessionId={}", 
                           sentenceId, sessionId);
                
                currentSentenceId = null;
                
                // 处理下一个句子
                processNextSentence();
            } else {
                logger.warn("音频完成通知的句子ID不匹配: expected={}, actual={}, sessionId={}", 
                           currentSentenceId, sentenceId, sessionId);
            }
        }
        
        public void onTTSError(String sentenceId) {
            if (currentSentenceId != null && currentSentenceId.equals(sentenceId)) {
                logger.warn("Live2D TTS生成失败，跳过当前句子: sentenceId={}, sessionId={}", 
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
            currentSentenceId = null;
        }
    }
    
    /**
     * 异步生成同步TTS音频
     */
    @Async
    public void generateSyncTTS(SentenceItem sentence, MultiChannelContext context, Live2DSyncQueue queue) {
        String sessionId = context.getSessionId();
        String sentenceId = sentence.getId();
        
        try {
            // 获取Live2D语音偏好 - 使用default用户配置
            UserPreferences prefs = userPreferencesService.getUserPreferences("default");
            String speakerId = (prefs.getLive2dOutput() != null) ? 
                prefs.getLive2dOutput().getSpeakerId() : "派蒙";
            
            logger.debug("开始生成Live2D TTS: id={}, speakerId={}, sessionId={}", 
                        sentenceId, speakerId, sessionId);
            
            // 调用TTS服务
            CompletableFuture<byte[]> ttsTask = multiModalService.textToSpeech(
                sentence.getText(), speakerId, "wav");
            int timeoutSeconds = appConfig.getPython().getTimeout().getLive2dTtsTaskTimeoutSeconds();
            byte[] audioData = ttsTask.get(timeoutSeconds, TimeUnit.SECONDS);
            
            // 发送音频消息
            ChatMessage audioMessage = new ChatMessage();
            audioMessage.setType("live2d_sentence_audio");
            audioMessage.setChannelType(getChannelType());
            audioMessage.setTtsMode("sentence_sync");
            audioMessage.setSentenceId(sentenceId);
            audioMessage.setSentenceOrder(sentence.getOrder());
            audioMessage.setContent(sentence.getText());
            audioMessage.setAudioData(audioData);
            audioMessage.setAudioReady(true);
            
            context.sendMessage(audioMessage, getChannelType());
            
            logger.debug("Live2D TTS生成完成: id={}, audioSize={}bytes, sessionId={}", 
                        sentenceId, audioData.length, sessionId);
            
        } catch (Exception e) {
            logger.error("Live2D TTS生成失败: id={}, sessionId={}", sentenceId, sessionId, e);
            
            // 发送TTS失败消息
            ChatMessage errorMessage = new ChatMessage();
            errorMessage.setType("live2d_audio_failed");
            errorMessage.setChannelType(getChannelType());
            errorMessage.setSentenceId(sentenceId);
            errorMessage.setSentenceOrder(sentence.getOrder());
            errorMessage.setContent("TTS生成失败: " + sentence.getText());
            
            context.sendMessage(errorMessage, getChannelType());
            
            // 通知队列TTS失败
            queue.onTTSError(sentenceId);
        }
    }
}

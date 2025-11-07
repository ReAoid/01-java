package com.chatbot.service.channel;

import com.chatbot.model.domain.SentenceItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 句子处理器
 * 整合了 SharedSentenceQueue 和 SentenceBuffer 的功能
 * 负责流式文本的句子分割、缓冲和队列管理
 */
public class SentenceProcessor {
    
    private static final Logger logger = LoggerFactory.getLogger(SentenceProcessor.class);
    
    // 中英文句子结束标点的正则表达式
    private static final Pattern SENTENCE_END = Pattern.compile(
        "[。！？；…\\.\\!\\?;]+" +          // 基础标点符号
        "|[：:](?=\\s|$)" +                // 冒号(后面跟空格或结尾)
        "|[\"\"''】)）](?=[。！？；…\\.\\!\\?;])" + // 引号后跟标点
        "|\\n{2,}"                         // 连续换行也视为句子结束
    );
    
    private final String sessionId;
    
    // 句子队列：存储已分割的完整句子
    private final List<SentenceItem> sentenceQueue = new ArrayList<>();
    
    // 文本缓冲区：累积流式文本
    private final StringBuilder textBuffer = new StringBuilder();
    
    // 句子顺序计数器
    private final AtomicInteger orderCounter = new AtomicInteger(0);
    
    // 静态工厂：管理所有会话的处理器实例
    private static final ConcurrentHashMap<String, SentenceProcessor> processors = new ConcurrentHashMap<>();
    
    /**
     * 获取或创建句子处理器
     * @param sessionId 会话ID
     * @return 句子处理器实例
     */
    public static SentenceProcessor getOrCreate(String sessionId) {
        return processors.computeIfAbsent(sessionId, SentenceProcessor::new);
    }
    
    /**
     * 移除会话的句子处理器
     * @param sessionId 会话ID
     */
    public static void remove(String sessionId) {
        SentenceProcessor processor = processors.remove(sessionId);
        if (processor != null) {
            processor.clear();
        }
    }
    
    /**
     * 私有构造函数
     */
    private SentenceProcessor(String sessionId) {
        this.sessionId = sessionId;
    }
    
    // ==================== 核心API ====================
    
    /**
     * 添加文本块到缓冲区
     * @param chunk 文本块
     */
    public void addTextChunk(String chunk) {
        if (chunk != null && !chunk.isEmpty()) {
            textBuffer.append(chunk);
            logger.trace("添加文本块到缓冲区: '{}', sessionId: {}", chunk, sessionId);
        }
    }
    
    /**
     * 检查是否有待处理的完整句子
     * @return 是否有完整句子
     */
    public boolean hasPendingSentence() {
        String text = textBuffer.toString();
        Matcher matcher = SENTENCE_END.matcher(text);
        boolean hasSentence = matcher.find();
        
        if (hasSentence) {
            logger.trace("检测到完整句子，缓冲区长度: {}, sessionId: {}", 
                        text.length(), sessionId);
        }
        
        return hasSentence;
    }
    
    /**
     * 提取下一个完整句子并添加到队列
     * @return 提取的句子，如果没有返回null
     */
    public String extractSentence() {
        String text = textBuffer.toString();
        Matcher matcher = SENTENCE_END.matcher(text);
        
        if (matcher.find()) {
            int endIndex = matcher.end();
            String sentence = text.substring(0, endIndex).trim();
            
            // 更新缓冲区，保留剩余内容
            String remaining = text.substring(endIndex);
            textBuffer.setLength(0);
            textBuffer.append(remaining);
            
            // 清理句子内容
            sentence = cleanSentence(sentence);
            
            if (!sentence.isEmpty()) {
                // 添加到队列
                int order = orderCounter.getAndIncrement();
                SentenceItem item = new SentenceItem(sentence, order, sessionId);
                
                synchronized (sentenceQueue) {
                    sentenceQueue.add(item);
                }
                
                logger.debug("提取并入队句子: order={}, text='{}', sessionId={}", 
                            order, sentence, sessionId);
                return sentence;
            } else {
                // 如果清理后为空，继续尝试提取下一句
                return hasPendingSentence() ? extractSentence() : null;
            }
        }
        
        return null;
    }
    
    /**
     * 获取剩余文本内容（流结束时调用）
     * @return 剩余文本
     */
    public String getRemainingText() {
        String remaining = textBuffer.toString().trim();
        String cleaned = cleanSentence(remaining);
        
        if (!cleaned.isEmpty()) {
            logger.debug("获取剩余文本: '{}', sessionId: {}", cleaned, sessionId);
        }
        
        return cleaned;
    }
    
    /**
     * 添加句子到队列（直接入队，不经过缓冲区）
     * @param text 句子文本
     * @return 句子顺序号
     */
    public int addSentence(String text) {
        if (text == null || text.trim().isEmpty()) {
            logger.warn("尝试添加空句子，sessionId: {}", sessionId);
            return -1;
        }
        
        int order = orderCounter.getAndIncrement();
        SentenceItem sentence = new SentenceItem(text.trim(), order, sessionId);
        
        synchronized (sentenceQueue) {
            sentenceQueue.add(sentence);
        }
        
        logger.debug("直接入队句子: order={}, text={}, sessionId={}", 
                    order, text.substring(0, Math.min(text.length(), 50)) + "...", sessionId);
        
        return order;
    }
    
    /**
     * 根据顺序获取句子
     * @param order 句子顺序号
     * @return 句子项，如果不存在返回null
     */
    public SentenceItem getSentence(int order) {
        synchronized (sentenceQueue) {
            return sentenceQueue.stream()
                    .filter(s -> s.getOrder() == order)
                    .findFirst()
                    .orElse(null);
        }
    }
    
    /**
     * 获取所有句子的副本
     * @return 句子列表副本
     */
    public List<SentenceItem> getAllSentences() {
        synchronized (sentenceQueue) {
            return new ArrayList<>(sentenceQueue);
        }
    }
    
    /**
     * 获取队列中的句子总数
     * @return 句子数量
     */
    public int size() {
        synchronized (sentenceQueue) {
            return sentenceQueue.size();
        }
    }
    
    /**
     * 检查队列是否为空
     * @return 是否为空
     */
    public boolean isEmpty() {
        synchronized (sentenceQueue) {
            return sentenceQueue.isEmpty();
        }
    }
    
    /**
     * 获取缓冲区当前内容
     * @return 当前内容
     */
    public String getCurrentBuffer() {
        return textBuffer.toString();
    }
    
    /**
     * 获取缓冲区大小
     * @return 字符数
     */
    public int getBufferSize() {
        return textBuffer.length();
    }
    
    /**
     * 检查缓冲区是否为空
     * @return 是否为空
     */
    public boolean isBufferEmpty() {
        return textBuffer.length() == 0;
    }
    
    /**
     * 获取下一个句子的顺序号
     * @return 下一个顺序号
     */
    public int getNextOrder() {
        return orderCounter.get();
    }
    
    /**
     * 清空处理器（清空队列和缓冲区）
     */
    public void clear() {
        synchronized (sentenceQueue) {
            sentenceQueue.clear();
        }
        textBuffer.setLength(0);
        orderCounter.set(0);
        logger.debug("清空句子处理器，sessionId: {}", sessionId);
    }
    
    /**
     * 获取会话ID
     * @return 会话ID
     */
    public String getSessionId() {
        return sessionId;
    }
    
    // ==================== 内部实现 ====================
    
    /**
     * 清理句子内容
     * 移除多余的空白字符、特殊标记和思考内容
     * @param sentence 原始句子
     * @return 清理后的句子
     */
    private String cleanSentence(String sentence) {
        if (sentence == null) {
            return "";
        }
        
        // 1. 移除思考标记和其中的内容
        sentence = removeThinkingContent(sentence);
        
        // 2. 移除多余的空白字符
        sentence = sentence.replaceAll("\\s+", " ").trim();
        
        // 3. 移除空的标点符号行
        if (sentence.matches("^[。！？；…\\.\\!\\?;：:]+$")) {
            return "";
        }
        
        return sentence;
    }
    
    /**
     * 移除思考内容
     * @param text 原始文本
     * @return 移除思考内容后的文本
     */
    private String removeThinkingContent(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        
        // 如果不包含思考标签，直接返回
        if (!text.contains("<think>") && !text.contains("</think>")) {
            return text;
        }
        
        StringBuilder result = new StringBuilder();
        String[] lines = text.split("\n");
        boolean inThinkingBlock = false;
        
        for (String line : lines) {
            // 检查是否进入思考块
            if (line.contains("<think>")) {
                inThinkingBlock = true;
                // 如果这一行在<think>之前还有内容，保留它
                int thinkIndex = line.indexOf("<think>");
                if (thinkIndex > 0) {
                    String beforeThink = line.substring(0, thinkIndex).trim();
                    if (!beforeThink.isEmpty()) {
                        result.append(beforeThink).append("\n");
                    }
                }
                continue;
            }
            
            // 检查是否退出思考块
            if (line.contains("</think>")) {
                inThinkingBlock = false;
                // 如果这一行在</think>之后还有内容，保留它
                int endThinkIndex = line.indexOf("</think>");
                if (endThinkIndex + 8 < line.length()) {
                    String afterThink = line.substring(endThinkIndex + 8).trim();
                    if (!afterThink.isEmpty()) {
                        result.append(afterThink).append("\n");
                    }
                }
                continue;
            }
            
            // 如果不在思考块中，保留这一行
            if (!inThinkingBlock) {
                result.append(line).append("\n");
            }
        }
        
        return result.toString().trim();
    }
    
    @Override
    public String toString() {
        return String.format("SentenceProcessor{sessionId='%s', queueSize=%d, bufferSize=%d, nextOrder=%d}",
                           sessionId, size(), getBufferSize(), getNextOrder());
    }
}


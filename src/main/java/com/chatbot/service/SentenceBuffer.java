package com.chatbot.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 句子缓冲区
 * 用于流式文本处理中的句子边界检测和分割
 */
@Component
public class SentenceBuffer {
    
    private static final Logger logger = LoggerFactory.getLogger(SentenceBuffer.class);
    
    // 中英文句子结束标点的正则表达式
    private static final Pattern SENTENCE_END = Pattern.compile(
        "[。！？；…\\.\\!\\?;]+" +          // 基础标点符号
        "|[：:](?=\\s|$)" +                // 冒号(后面跟空格或结尾)
        "|[\"\"''】)）](?=[。！？；…\\.\\!\\?;])" + // 引号后跟标点
        "|\\n{2,}"                         // 连续换行也视为句子结束
    );
    
    private final StringBuilder currentBuffer = new StringBuilder();
    
    /**
     * 添加文本块到缓冲区
     * @param chunk 文本块
     */
    public void addChunk(String chunk) {
        if (chunk != null && !chunk.isEmpty()) {
            currentBuffer.append(chunk);
            logger.trace("添加文本块到缓冲区: {}", chunk);
        }
    }
    
    /**
     * 检查是否有待处理的完整句子
     * @return 是否有完整句子
     */
    public boolean hasPendingSentence() {
        String text = currentBuffer.toString();
        Matcher matcher = SENTENCE_END.matcher(text);
        boolean hasSentence = matcher.find();
        
        if (hasSentence) {
            logger.trace("检测到完整句子，当前缓冲区: {}", 
                        text.substring(0, Math.min(text.length(), 100)) + "...");
        }
        
        return hasSentence;
    }
    
    /**
     * 提取下一个完整句子
     * @return 完整句子，如果没有返回null
     */
    public String extractSentence() {
        String text = currentBuffer.toString();
        Matcher matcher = SENTENCE_END.matcher(text);
        
        if (matcher.find()) {
            int endIndex = matcher.end();
            String sentence = text.substring(0, endIndex).trim();
            
            // 更新缓冲区，保留剩余内容
            String remaining = text.substring(endIndex);
            currentBuffer.setLength(0);
            currentBuffer.append(remaining);
            
            // 清理句子内容
            sentence = cleanSentence(sentence);
            
            if (!sentence.isEmpty()) {
                logger.debug("提取句子: {}", sentence);
                return sentence;
            } else {
                // 如果清理后为空，继续尝试提取下一句
                return hasPendingSentence() ? extractSentence() : null;
            }
        }
        
        return null;
    }
    
    /**
     * 获取剩余文本内容
     * @return 剩余文本
     */
    public String getRemainingText() {
        String remaining = currentBuffer.toString().trim();
        return cleanSentence(remaining);
    }
    
    /**
     * 清空缓冲区
     */
    public void clear() {
        currentBuffer.setLength(0);
        logger.debug("清空句子缓冲区");
    }
    
    /**
     * 获取当前缓冲区内容
     * @return 当前内容
     */
    public String getCurrentContent() {
        return currentBuffer.toString();
    }
    
    /**
     * 检查缓冲区是否为空
     * @return 是否为空
     */
    public boolean isEmpty() {
        return currentBuffer.length() == 0;
    }
    
    /**
     * 清理句子内容
     * 移除多余的空白字符和特殊标记，过滤思考内容
     * @param sentence 原始句子
     * @return 清理后的句子
     */
    private String cleanSentence(String sentence) {
        if (sentence == null) {
            return "";
        }
        
        // 移除思考标记和其中的内容
        sentence = removeThinkingContent(sentence);
        
        // 移除多余的空白字符
        sentence = sentence.replaceAll("\\s+", " ").trim();
        
        // 移除空的标点符号行
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
    
    /**
     * 获取缓冲区大小
     * @return 字符数
     */
    public int size() {
        return currentBuffer.length();
    }
    
    @Override
    public String toString() {
        return "SentenceBuffer{" +
                "size=" + size() +
                ", content='" + (size() > 50 ? 
                    getCurrentContent().substring(0, 50) + "..." : 
                    getCurrentContent()) + '\'' +
                '}';
    }
}

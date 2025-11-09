package com.chatbot.service.chat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 聊天消息处理器
 * 负责消息的预处理、清理和过滤
 */
@Service
public class ChatMessageProcessor {
    
    private static final Logger logger = LoggerFactory.getLogger(ChatMessageProcessor.class);
    
    /**
     * 预处理用户输入
     * 清理特殊字符、空白符等
     * 
     * @param input 原始输入
     * @return 处理后的输入
     */
    public String preprocessInput(String input) {
        if (input == null) {
            logger.debug("输入为null，返回空字符串");
            return "";
        }
        
        // 清理特殊字符、合并空格、替换换行符
        String processed = input.trim()
                   .replaceAll("\\s+", " ")  // 合并多个空格
                   .replaceAll("[\\r\\n]+", " "); // 替换换行符
        
        return processed;
    }
    
    /**
     * 智能过滤思考内容，保留真正的回复
     * 移除 &lt;think&gt;...&lt;/think&gt; 标签及其内容
     * 
     * @param content 原始内容
     * @return 过滤后的内容
     */
    public String filterThinkingContent(String content) {
        if (content == null) {
            return null;
        }
        
        // 如果不包含思考标签，直接返回
        if (!content.contains("<think>") && !content.contains("</think>")) {
            return content;
        }
        
        StringBuilder result = new StringBuilder();
        String[] lines = content.split("\n");
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
        
        // 清理结果
        String filtered = result.toString().trim();
        
        // 只记录调试信息
        if (content.contains("<think>")) {
            logger.debug("过滤统计 - 原始长度: {}, 过滤后长度: {}", content.length(), filtered.length());
        }
        
        return filtered.isEmpty() ? null : filtered;
    }
    
    /**
     * 将发送者映射为角色
     * 
     * @param sender 发送者
     * @return 角色 (user/assistant/system)
     */
    public String mapSenderToRole(String sender) {
        if (sender == null) return "user";
        return switch (sender.toLowerCase()) {
            case "assistant", "ai", "bot" -> "assistant";
            case "system" -> "system";
            default -> "user";
        };
    }
}


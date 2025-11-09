package com.chatbot.service.channel;

import com.chatbot.model.domain.ChatMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;

/**
 * 纯文本输出策略
 * 直接转发文本块，不进行任何额外处理
 * 
 * 适用场景：用户只需要文本输出，不需要语音
 */
@Component
public class TextOnlyStrategy implements OutputStrategy {
    
    private static final Logger logger = LoggerFactory.getLogger(TextOnlyStrategy.class);
    
    @Override
    public String getStrategyName() {
        return "text_only";
    }
    
    @Override
    public void processChunk(String chunk, Consumer<ChatMessage> callback, String sessionId, boolean isThinking) {
        if (chunk == null || chunk.isEmpty()) {
            return;
        }
        
        // 创建消息并直接发送
        ChatMessage message = new ChatMessage();
        message.setType("text");
        message.setContent(chunk);
        message.setRole("assistant");
        message.setSessionId(sessionId);
        message.setStreaming(true);
        message.setStreamComplete(false);
        message.setThinking(isThinking);
        
        if (isThinking) {
            message.setThinkingContent(chunk);
        }
        
        callback.accept(message);
        
        logger.trace("纯文本策略发送消息: length={}, isThinking={}, sessionId={}", 
                    chunk.length(), isThinking, sessionId);
    }
    
    @Override
    public void onStreamComplete(Consumer<ChatMessage> callback, String sessionId) {
        // 发送流完成信号
        ChatMessage finalMessage = new ChatMessage();
        finalMessage.setType("text");
        finalMessage.setContent("");
        finalMessage.setRole("assistant");
        finalMessage.setSessionId(sessionId);
        finalMessage.setStreaming(true);
        finalMessage.setStreamComplete(true);
        
        callback.accept(finalMessage);
        
        logger.debug("纯文本策略流完成: sessionId={}", sessionId);
    }
}


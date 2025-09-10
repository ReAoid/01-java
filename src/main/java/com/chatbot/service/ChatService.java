package com.chatbot.service;

import com.chatbot.config.AIConfig;
import com.chatbot.model.ChatMessage;
import com.chatbot.model.ChatSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * 聊天服务
 * 实现AI对话引擎和流式处理
 */
@Service
public class ChatService {
    
    private static final Logger logger = LoggerFactory.getLogger(ChatService.class);
    
    private final SessionService sessionService;
    private final PersonaService personaService;
    private final MemoryService memoryService;
    private final MultiModalService multiModalService;
    private final AIConfig aiConfig;
    private final OllamaService ollamaService;
    
    public ChatService(SessionService sessionService, 
                      PersonaService personaService,
                      MemoryService memoryService,
                      MultiModalService multiModalService,
                      AIConfig aiConfig,
                      OllamaService ollamaService) {
        this.sessionService = sessionService;
        this.personaService = personaService;
        this.memoryService = memoryService;
        this.multiModalService = multiModalService;
        this.aiConfig = aiConfig;
        this.ollamaService = ollamaService;
    }
    
    /**
     * 处理用户消息并生成AI回复（流式处理）
     */
    public void processMessage(ChatMessage userMessage, Consumer<ChatMessage> responseCallback) {
        CompletableFuture.runAsync(() -> {
            try {
                // 1. 获取或创建会话
                ChatSession session = sessionService.getOrCreateSession(userMessage.getSessionId());
                
                // 2. 添加用户消息到会话历史
                userMessage.setSender("user");
                session.addMessage(userMessage);
                
                // 3. 预处理用户输入
                String processedInput = preprocessInput(userMessage.getContent());
                
                // 4. 获取上下文和记忆
                String context = buildContext(session);
                String longTermMemory = memoryService.retrieveRelevantMemory(
                    userMessage.getSessionId(), processedInput);
                
                // 5. 应用人设
                String personaPrompt = personaService.getPersonaPrompt(session.getCurrentPersonaId());
                
                // 6. 构建完整提示
                String fullPrompt = buildPrompt(personaPrompt, context, longTermMemory, processedInput);
                
                // 7. 调用AI模型生成回复（流式）
                generateStreamingResponse(fullPrompt, userMessage.getSessionId(), responseCallback);
                
            } catch (Exception e) {
                logger.error("处理消息时发生错误", e);
                
                ChatMessage errorResponse = new ChatMessage();
                errorResponse.setType("error");
                errorResponse.setContent("抱歉，处理您的消息时出现了问题，请稍后重试。");
                errorResponse.setSender("assistant");
                errorResponse.setSessionId(userMessage.getSessionId());
                
                responseCallback.accept(errorResponse);
            }
        });
    }
    
    /**
     * 预处理用户输入
     */
    private String preprocessInput(String input) {
        if (input == null) return "";
        
        // 清理特殊字符、纠正拼写等
        return input.trim()
                   .replaceAll("\\s+", " ")  // 合并多个空格
                   .replaceAll("[\\r\\n]+", " "); // 替换换行符
    }
    
    /**
     * 构建上下文
     */
    private String buildContext(ChatSession session) {
        List<ChatMessage> recentMessages = session.getRecentMessages(10);
        StringBuilder context = new StringBuilder();
        
        for (ChatMessage msg : recentMessages) {
            if (msg.getContent() != null) {
                context.append(msg.getSender()).append(": ").append(msg.getContent()).append("\n");
            }
        }
        
        return context.toString();
    }
    
    /**
     * 构建完整提示
     */
    private String buildPrompt(String personaPrompt, String context, String longTermMemory, String userInput) {
        StringBuilder prompt = new StringBuilder();
        
        // 系统指令
        prompt.append("你是一个智能AI助手。请根据以下信息回复用户：\n\n");
        
        // 人设信息
        if (personaPrompt != null && !personaPrompt.isEmpty()) {
            prompt.append("角色设定：\n").append(personaPrompt).append("\n\n");
        }
        
        // 长期记忆
        if (longTermMemory != null && !longTermMemory.isEmpty()) {
            prompt.append("相关记忆：\n").append(longTermMemory).append("\n\n");
        }
        
        // 对话历史
        if (!context.isEmpty()) {
            prompt.append("对话历史：\n").append(context).append("\n");
        }
        
        // 当前用户输入
        prompt.append("用户: ").append(userInput).append("\n");
        prompt.append("助手: ");
        
        return prompt.toString();
    }
    
    /**
     * 生成流式回复（使用Ollama）
     */
    private void generateStreamingResponse(String prompt, String sessionId, Consumer<ChatMessage> callback) {
        // 检查Ollama服务是否可用
        if (!ollamaService.isServiceAvailable()) {
            logger.warn("Ollama服务不可用，使用Mock响应");
            generateMockStreamingResponse(prompt, sessionId, callback);
            return;
        }
        
        StringBuilder completeResponse = new StringBuilder();
        
        // 使用Ollama服务生成流式响应
        ollamaService.generateStreamingResponse(
            prompt,
            // 成功处理每个chunk
            chunk -> {
                completeResponse.append(chunk);
                
                ChatMessage streamMessage = new ChatMessage();
                streamMessage.setType("text");
                streamMessage.setContent(chunk);
                streamMessage.setSender("assistant");
                streamMessage.setSessionId(sessionId);
                streamMessage.setStreaming(true);
                streamMessage.setStreamComplete(false);
                
                callback.accept(streamMessage);
            },
            // 错误处理
            error -> {
                logger.error("Ollama流式响应发生错误", error);
                
                // 发送完成消息
                if (completeResponse.length() > 0) {
                    ChatMessage finalMessage = new ChatMessage();
                    finalMessage.setType("text");
                    finalMessage.setContent("");
                    finalMessage.setSender("assistant");
                    finalMessage.setSessionId(sessionId);
                    finalMessage.setStreaming(true);
                    finalMessage.setStreamComplete(true);
                    
                    callback.accept(finalMessage);
                    
                    // 保存完整响应到会话历史
                    saveCompleteResponse(sessionId, completeResponse.toString());
                } else {
                    // 如果没有收到任何响应，发送错误消息
                    ChatMessage errorMessage = new ChatMessage();
                    errorMessage.setType("error");
                    errorMessage.setContent("抱歉，AI服务暂时不可用，请稍后重试。");
                    errorMessage.setSender("assistant");
                    errorMessage.setSessionId(sessionId);
                    
                    callback.accept(errorMessage);
                }
            }
        );
        
        // 在流式响应结束后发送完成信号
        // 注意：实际的完成处理在OllamaService的流式处理中进行
        CompletableFuture.runAsync(() -> {
            try {
                // 等待一小段时间确保所有chunk都被处理
                Thread.sleep(100);
                
                if (completeResponse.length() > 0) {
                    // 发送流完成信号
                    ChatMessage finalMessage = new ChatMessage();
                    finalMessage.setType("text");
                    finalMessage.setContent("");
                    finalMessage.setSender("assistant");
                    finalMessage.setSessionId(sessionId);
                    finalMessage.setStreaming(true);
                    finalMessage.setStreamComplete(true);
                    
                    callback.accept(finalMessage);
                    
                    // 保存完整响应到会话历史
                    saveCompleteResponse(sessionId, completeResponse.toString());
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }
    
    /**
     * 保存完整响应到会话历史
     */
    private void saveCompleteResponse(String sessionId, String completeResponse) {
        try {
            ChatMessage completeMessage = new ChatMessage();
            completeMessage.setType("text");
            completeMessage.setContent(completeResponse);
            completeMessage.setSender("assistant");
            completeMessage.setSessionId(sessionId);
            completeMessage.setStreaming(false);
            
            ChatSession session = sessionService.getSession(sessionId);
            if (session != null) {
                session.addMessage(completeMessage);
                
                // 更新长期记忆
                memoryService.updateMemory(sessionId, completeResponse);
            }
        } catch (Exception e) {
            logger.error("保存完整响应时发生错误", e);
        }
    }
    
    /**
     * 生成Mock流式回复（fallback方案）
     */
    private void generateMockStreamingResponse(String prompt, String sessionId, Consumer<ChatMessage> callback) {
        // Mock AI回复生成
        String mockResponse = generateMockResponse(prompt);
        
        // 模拟流式输出
        int chunkSize = aiConfig.getStreaming().getChunkSize();
        int delay = aiConfig.getStreaming().getDelayMs();
        
        StringBuilder currentChunk = new StringBuilder();
        
        for (int i = 0; i < mockResponse.length(); i++) {
            currentChunk.append(mockResponse.charAt(i));
            
            if (currentChunk.length() >= chunkSize || i == mockResponse.length() - 1) {
                ChatMessage streamMessage = new ChatMessage();
                streamMessage.setType("text");
                streamMessage.setContent(currentChunk.toString());
                streamMessage.setSender("assistant");
                streamMessage.setSessionId(sessionId);
                streamMessage.setStreaming(true);
                streamMessage.setStreamComplete(i == mockResponse.length() - 1);
                
                callback.accept(streamMessage);
                
                // 添加到会话历史（只在最后一块时添加完整消息）
                if (streamMessage.isStreamComplete()) {
                    ChatMessage completeMessage = new ChatMessage();
                    completeMessage.setType("text");
                    completeMessage.setContent(mockResponse);
                    completeMessage.setSender("assistant");
                    completeMessage.setSessionId(sessionId);
                    completeMessage.setStreaming(false);
                    
                    ChatSession session = sessionService.getSession(sessionId);
                    if (session != null) {
                        session.addMessage(completeMessage);
                        
                        // 更新长期记忆
                        memoryService.updateMemory(sessionId, mockResponse);
                    }
                }
                
                currentChunk.setLength(0);
                
                // 模拟延迟
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }
    
    /**
     * 生成Mock AI回复
     */
    private String generateMockResponse(String prompt) {
        // 简单的Mock回复逻辑
        String[] responses = {
            "我理解您的问题。这是一个很有趣的话题，让我来为您详细解答。",
            "根据您提供的信息，我认为最好的方法是...",
            "这确实是一个复杂的问题。从多个角度来看...",
            "我很乐意帮助您解决这个问题。首先，我们需要考虑...",
            "基于我的理解，我建议您可以尝试以下几种方法..."
        };
        
        // 根据提示内容选择合适的回复
        int index = Math.abs(prompt.hashCode()) % responses.length;
        return responses[index];
    }
    
    /**
     * 清理会话资源
     */
    public void cleanupSession(String sessionId) {
        // 清理会话相关的资源
        logger.info("清理会话资源: {}", sessionId);
        
        // 可以在这里添加其他清理逻辑
        // 例如：清理临时文件、取消正在进行的任务等
    }
}

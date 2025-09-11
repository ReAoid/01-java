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
        logger.info("初始化ChatService");
        this.sessionService = sessionService;
        this.personaService = personaService;
        this.memoryService = memoryService;
        this.multiModalService = multiModalService;
        this.aiConfig = aiConfig;
        this.ollamaService = ollamaService;
        logger.debug("ChatService初始化完成，已注入所有依赖服务");
    }
    
    /**
     * 处理用户消息并生成AI回复（流式处理）
     */
    public void processMessage(ChatMessage userMessage, Consumer<ChatMessage> responseCallback) {
        logger.info("开始处理用户消息，sessionId: {}, messageType: {}, contentLength: {}", 
                   userMessage.getSessionId(), userMessage.getType(), 
                   userMessage.getContent() != null ? userMessage.getContent().length() : 0);
        
        CompletableFuture.runAsync(() -> {
            long startTime = System.currentTimeMillis();
            String sessionId = userMessage.getSessionId();
            
            try {
                // 1. 获取或创建会话
                logger.debug("步骤1：获取或创建会话，sessionId: {}", sessionId);
                ChatSession session = sessionService.getOrCreateSession(sessionId);
                logger.debug("会话获取成功，当前会话消息数: {}", session.getMessageHistory().size());
                
                // 2. 添加用户消息到会话历史
                logger.debug("步骤2：添加用户消息到会话历史");
                userMessage.setSender("user");
                session.addMessage(userMessage);
                logger.debug("用户消息已添加到会话历史");
                
                // 3. 预处理用户输入
                logger.debug("步骤3：预处理用户输入");
                String processedInput = preprocessInput(userMessage.getContent());
                logger.debug("用户输入预处理完成，原始长度: {}, 处理后长度: {}", 
                           userMessage.getContent() != null ? userMessage.getContent().length() : 0,
                           processedInput.length());
                
                // 4. 获取上下文和记忆
                logger.debug("步骤4：获取上下文和记忆");
                String context = buildContext(session);
                String longTermMemory = memoryService.retrieveRelevantMemory(sessionId, processedInput);
                logger.debug("上下文构建完成，上下文长度: {}, 长期记忆长度: {}", 
                           context.length(), longTermMemory != null ? longTermMemory.length() : 0);
                
                // 5. 应用人设
                logger.debug("步骤5：应用人设，personaId: {}", session.getCurrentPersonaId());
                String personaPrompt = personaService.getPersonaPrompt(session.getCurrentPersonaId());
                logger.debug("人设提示获取完成，长度: {}", personaPrompt != null ? personaPrompt.length() : 0);
                
                // 6. 构建完整提示
                logger.debug("步骤6：构建完整提示");
                String fullPrompt = buildPrompt(personaPrompt, context, longTermMemory, processedInput);
                logger.debug("完整提示构建完成，总长度: {}", fullPrompt.length());
                
                // 7. 调用AI模型生成回复（流式）
                logger.debug("步骤7：调用AI模型生成回复");
                generateStreamingResponse(fullPrompt, sessionId, responseCallback);
                
                long processingTime = System.currentTimeMillis() - startTime;
                logger.info("消息处理完成，sessionId: {}, 处理时间: {}ms", sessionId, processingTime);
                
            } catch (Exception e) {
                long processingTime = System.currentTimeMillis() - startTime;
                logger.error("处理消息时发生错误，sessionId: {}, 处理时间: {}ms", sessionId, processingTime, e);
                
                ChatMessage errorResponse = new ChatMessage();
                errorResponse.setType("error");
                errorResponse.setContent("抱歉，处理您的消息时出现了问题，请稍后重试。");
                errorResponse.setSender("assistant");
                errorResponse.setSessionId(sessionId);
                
                responseCallback.accept(errorResponse);
            }
        });
    }
    
    /**
     * 预处理用户输入
     */
    private String preprocessInput(String input) {
        logger.debug("开始预处理用户输入");
        if (input == null) {
            logger.debug("输入为null，返回空字符串");
            return "";
        }
        
        // 清理特殊字符、纠正拼写等
        String processed = input.trim()
                   .replaceAll("\\s+", " ")  // 合并多个空格
                   .replaceAll("[\\r\\n]+", " "); // 替换换行符
        
        logger.debug("用户输入预处理完成，原始: '{}', 处理后: '{}'", input, processed);
        return processed;
    }
    
    /**
     * 构建上下文
     */
    private String buildContext(ChatSession session) {
        logger.debug("开始构建对话上下文，sessionId: {}", session.getSessionId());
        List<ChatMessage> recentMessages = session.getRecentMessages(10);
        StringBuilder context = new StringBuilder();
        
        int messageCount = 0;
        for (ChatMessage msg : recentMessages) {
            if (msg.getContent() != null) {
                context.append(msg.getSender() + ": " + msg.getContent() + "\n");
                messageCount++;
            }
        }
        
        logger.debug("对话上下文构建完成，包含{}条消息，总长度: {}", messageCount, context.length());
        return context.toString();
    }
    
    /**
     * 构建完整提示
     */
    private String buildPrompt(String personaPrompt, String context, String longTermMemory, String userInput) {
        logger.debug("开始构建完整提示");
        StringBuilder prompt = new StringBuilder();
        
        // 系统指令
        String systemInstruction = "你是一个智能AI助手。请根据以下信息回复用户：\n\n";
        prompt.append(systemInstruction);
        logger.debug("添加系统指令: '{}'", systemInstruction.replace("\n", "\\n"));
        
        // 人设信息
        if (personaPrompt != null && !personaPrompt.isEmpty()) {
            String personaSection = "角色设定：\n" + personaPrompt + "\n\n";
            prompt.append(personaSection);
            logger.debug("添加人设信息: '{}' (长度: {})", 
                        personaPrompt.replace("\n", "\\n"), personaPrompt.length());
        } else {
            logger.debug("无人设信息");
        }
        
        // 长期记忆
        if (longTermMemory != null && !longTermMemory.isEmpty()) {
            String memorySection = "相关记忆：\n" + longTermMemory + "\n\n";
            prompt.append(memorySection);
            logger.debug("添加长期记忆: '{}' (长度: {})", 
                        longTermMemory.replace("\n", "\\n"), longTermMemory.length());
        } else {
            logger.debug("无长期记忆");
        }
        
        // 对话历史
        if (!context.isEmpty()) {
            String contextSection = "对话历史：\n" + context + "\n";
            prompt.append(contextSection);
            logger.debug("添加对话历史: '{}' (长度: {})", 
                        context.replace("\n", "\\n"), context.length());
        } else {
            logger.debug("无对话历史");
        }
        
        // 当前用户输入
        String userSection = "用户: " + userInput + "\n助手: ";
        prompt.append(userSection);
        logger.debug("添加用户输入: '{}'", userInput.replace("\n", "\\n"));
        
        String finalPrompt = prompt.toString();
        logger.info("完整提示构建完成，总长度: {}", finalPrompt.length());
        logger.debug("最终完整提示内容:\n{}", finalPrompt);
        
        return finalPrompt;
    }
    
    /**
     * 生成流式回复（使用Ollama）
     */
    private void generateStreamingResponse(String prompt, String sessionId, Consumer<ChatMessage> callback) {
        logger.info("开始生成流式响应，sessionId: {}, 提示长度: {}", sessionId, prompt.length());
        
        // 检查Ollama服务是否可用
        if (!ollamaService.isServiceAvailable()) {
            logger.warn("Ollama服务不可用，使用Mock响应，sessionId: {}", sessionId);
            generateMockStreamingResponse(prompt, sessionId, callback);
            return;
        }
        
        StringBuilder completeResponse = new StringBuilder();
        final int[] chunkCounter = {0}; // 使用数组来在lambda中修改值
        
        logger.debug("调用Ollama服务生成流式响应，sessionId: {}", sessionId);
        
        // 使用Ollama服务生成流式响应
        ollamaService.generateStreamingResponse(
            prompt,
            // 成功处理每个chunk
            chunk -> {
                chunkCounter[0]++;
                completeResponse.append(chunk);
                
                logger.debug("ChatService接收流式数据块#{}: '{}' (块长度: {})", 
                           chunkCounter[0], chunk.replace("\n", "\\n"), chunk.length());
                logger.debug("ChatService累积响应文本: '{}' (总长度: {})", 
                           completeResponse.toString().replace("\n", "\\n"), completeResponse.length());
                
                ChatMessage streamMessage = new ChatMessage();
                streamMessage.setType("text");
                streamMessage.setContent(chunk);
                streamMessage.setSender("assistant");
                streamMessage.setSessionId(sessionId);
                streamMessage.setStreaming(true);
                streamMessage.setStreamComplete(false);
                
                logger.debug("发送流式消息到WebSocket，sessionId: {}, 块#{}", sessionId, chunkCounter[0]);
                callback.accept(streamMessage);
            },
            // 错误处理
            error -> {
                logger.error("Ollama流式响应发生错误，sessionId: {}, 已接收{}个数据块，累积长度: {}", 
                           sessionId, chunkCounter[0], completeResponse.length(), error);
                
                // 发送完成消息
                if (completeResponse.length() > 0) {
                    logger.info("流式响应异常但有部分内容，发送完成信号，sessionId: {}, 最终响应长度: {}", 
                               sessionId, completeResponse.length());
                    
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
                    logger.warn("流式响应异常且无任何内容，发送错误消息，sessionId: {}", sessionId);
                    
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
                    logger.info("流式响应正常完成，sessionId: {}, 总数据块: {}, 最终响应长度: {}", 
                               sessionId, chunkCounter[0], completeResponse.length());
                    logger.debug("最终完整响应内容: '{}'", 
                               completeResponse.toString().replace("\n", "\\n"));
                    
                    // 发送流完成信号
                    ChatMessage finalMessage = new ChatMessage();
                    finalMessage.setType("text");
                    finalMessage.setContent("");
                    finalMessage.setSender("assistant");
                    finalMessage.setSessionId(sessionId);
                    finalMessage.setStreaming(true);
                    finalMessage.setStreamComplete(true);
                    
                    logger.debug("发送流式响应完成信号，sessionId: {}", sessionId);
                    callback.accept(finalMessage);
                    
                    // 保存完整响应到会话历史
                    saveCompleteResponse(sessionId, completeResponse.toString());
                } else {
                    logger.warn("流式响应完成但无内容，sessionId: {}", sessionId);
                }
            } catch (InterruptedException e) {
                logger.warn("流式响应完成处理被中断，sessionId: {}", sessionId);
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
        logger.info("开始生成Mock流式响应，sessionId: {}", sessionId);
        
        // Mock AI回复生成
        String mockResponse = generateMockResponse(prompt);
        logger.debug("Mock响应内容: '{}' (长度: {})", mockResponse.replace("\n", "\\n"), mockResponse.length());
        
        // 模拟流式输出
        int chunkSize = aiConfig.getStreaming().getChunkSize();
        int delay = aiConfig.getStreaming().getDelayMs();
        
        logger.debug("Mock流式配置 - 块大小: {}, 延迟: {}ms", chunkSize, delay);
        
        StringBuilder currentChunk = new StringBuilder();
        StringBuilder processedText = new StringBuilder();
        int chunkCount = 0;
        
        for (int i = 0; i < mockResponse.length(); i++) {
            currentChunk.append(mockResponse.charAt(i));
            processedText.append(mockResponse.charAt(i));
            
            if (currentChunk.length() >= chunkSize || i == mockResponse.length() - 1) {
                chunkCount++;
                boolean isLastChunk = (i == mockResponse.length() - 1);
                
                logger.debug("Mock流式数据块#{}: '{}' (块长度: {}, 是否最后一块: {})", 
                           chunkCount, currentChunk.toString().replace("\n", "\\n"), 
                           currentChunk.length(), isLastChunk);
                logger.debug("Mock累积响应文本: '{}' (总长度: {})", 
                           processedText.toString().replace("\n", "\\n"), processedText.length());
                
                ChatMessage streamMessage = new ChatMessage();
                streamMessage.setType("text");
                streamMessage.setContent(currentChunk.toString());
                streamMessage.setSender("assistant");
                streamMessage.setSessionId(sessionId);
                streamMessage.setStreaming(true);
                streamMessage.setStreamComplete(isLastChunk);
                
                logger.debug("发送Mock流式消息到WebSocket，sessionId: {}, 块#{}", sessionId, chunkCount);
                callback.accept(streamMessage);
                
                // 添加到会话历史（只在最后一块时添加完整消息）
                if (streamMessage.isStreamComplete()) {
                    logger.info("Mock流式响应完成，sessionId: {}, 总数据块: {}, 最终响应长度: {}", 
                               sessionId, chunkCount, mockResponse.length());
                    
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
                        logger.debug("Mock响应已保存到会话历史和长期记忆，sessionId: {}", sessionId);
                    }
                }
                
                currentChunk.setLength(0);
                
                // 模拟延迟
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException e) {
                    logger.warn("Mock流式响应被中断，sessionId: {}", sessionId);
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

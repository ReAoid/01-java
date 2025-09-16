package com.chatbot.service;

import com.chatbot.config.AIConfig;
import com.chatbot.model.ChatMessage;
import com.chatbot.model.ChatSession;
import com.chatbot.model.OllamaMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
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
    private final ConversationHistoryService conversationHistoryService;
    
    public ChatService(SessionService sessionService, 
                      PersonaService personaService,
                      MemoryService memoryService,
                      MultiModalService multiModalService,
                      AIConfig aiConfig,
                      OllamaService ollamaService,
                      ConversationHistoryService conversationHistoryService) {
        logger.info("初始化ChatService");
        this.sessionService = sessionService;
        this.personaService = personaService;
        this.memoryService = memoryService;
        this.multiModalService = multiModalService;
        this.aiConfig = aiConfig;
        this.ollamaService = ollamaService;
        this.conversationHistoryService = conversationHistoryService;
        logger.debug("ChatService初始化完成，已注入所有依赖服务");
    }
    
    /**
     * 处理用户消息并生成AI回复（流式处理）
     */
    public void processMessage(ChatMessage userMessage, Consumer<ChatMessage> responseCallback) {
        long messageStartTime = System.currentTimeMillis();
        String sessionId = userMessage.getSessionId();
        
        CompletableFuture.runAsync(() -> {
            
            try {
                // 1. 获取或创建会话
                logger.debug("步骤1：获取或创建会话，sessionId: {}", sessionId);
                long step1Start = System.currentTimeMillis();
                ChatSession session = sessionService.getOrCreateSession(sessionId);
                long step1Time = System.currentTimeMillis() - step1Start;
                logger.debug("会话获取成功，耗时: {}ms，sessionId: {}，当前会话消息数: {}", step1Time, session.getSessionId(), session.getMessageHistory().size());
                
                // 2. 添加用户消息到会话历史和对话记录
                logger.debug("步骤2：添加用户消息到会话历史和对话记录");
                long step2Start = System.currentTimeMillis();
                userMessage.setSender("user");
                session.addMessage(userMessage);
                
                // 添加到对话历史记录
                conversationHistoryService.addMessage(sessionId, userMessage);
                
                long step2Time = System.currentTimeMillis() - step2Start;
                logger.debug("用户消息已添加到会话历史和对话记录，耗时: {}ms", step2Time);
                
                // 3. 预处理用户输入
                logger.debug("步骤3：预处理用户输入");
                long step3Start = System.currentTimeMillis();
                String processedInput = preprocessInput(userMessage.getContent());
                long step3Time = System.currentTimeMillis() - step3Start;
                logger.debug("用户输入预处理完成，耗时: {}ms，原始长度: {}, 处理后长度: {}, 原始: '{}', 处理后: '{}'",
                           step3Time,
                           userMessage.getContent() != null ? userMessage.getContent().length() : 0,
                           processedInput.length(),
                           userMessage.getContent(),
                           processedInput);
                
                // 4. 获取上下文和记忆
                logger.debug("步骤4：获取上下文和记忆");
                long step4Start = System.currentTimeMillis();
                String context = buildContext(session);
                String longTermMemory = memoryService.retrieveRelevantMemory(sessionId, processedInput);
                long step4Time = System.currentTimeMillis() - step4Start;
                logger.debug("上下文构建完成，耗时: {}ms，上下文长度: {}, 长期记忆长度: {}", 
                           step4Time, context.length(), longTermMemory != null ? longTermMemory.length() : 0);
                
                // 5. 应用人设
                logger.debug("步骤5：应用人设，personaId: {}", session.getCurrentPersonaId());
                long step5Start = System.currentTimeMillis();
                String personaPrompt = personaService.getPersonaPrompt(session.getCurrentPersonaId());
                long step5Time = System.currentTimeMillis() - step5Start;
                logger.debug("人设提示获取完成，耗时: {}ms，长度: {}", step5Time, personaPrompt != null ? personaPrompt.length() : 0);
                
                // 6. 构建完整的消息列表
                logger.debug("步骤6：构建完整的消息列表");
                long step6Start = System.currentTimeMillis();
                List<OllamaMessage> messages = buildMessagesList(personaPrompt, session, longTermMemory, processedInput);
                long step6Time = System.currentTimeMillis() - step6Start;
                logger.debug("消息列表构建完成，耗时: {}ms，消息数量: {}", step6Time, messages.size());
                
                // 记录预处理完成时间
                long preprocessingTime = System.currentTimeMillis() - messageStartTime;
                logger.info("📊 预处理阶段完成，sessionId: {}, 总预处理时间: {}ms (步骤1: {}ms, 步骤2: {}ms, 步骤3: {}ms, 步骤4: {}ms, 步骤5: {}ms, 步骤6: {}ms)", 
                           sessionId, preprocessingTime, step1Time, step2Time, step3Time, step4Time, step5Time, step6Time);
                
                // 7. 调用AI模型生成回复（流式）
                logger.debug("步骤7：调用AI模型生成回复");
                long aiCallStartTime = System.currentTimeMillis();
                generateStreamingResponse(messages, sessionId, responseCallback, messageStartTime, aiCallStartTime);
                
                long totalProcessingTime = System.currentTimeMillis() - messageStartTime;
                logger.info("消息处理启动完成，sessionId: {}, 总启动时间: {}ms", sessionId, totalProcessingTime);
                
            } catch (Exception e) {
                long processingTime = System.currentTimeMillis() - messageStartTime;
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
        if (input == null) {
            logger.debug("输入为null，返回空字符串");
            return "";
        }
        
        // 清理特殊字符、纠正拼写等
        String processed = input.trim()
                   .replaceAll("\\s+", " ")  // 合并多个空格
                   .replaceAll("[\\r\\n]+", " "); // 替换换行符

        return processed;
    }
    
    /**
     * 构建上下文
     */
    private String buildContext(ChatSession session) {
        List<ChatMessage> recentMessages = session.getRecentMessages(10);
        StringBuilder context = new StringBuilder();

        for (ChatMessage msg : recentMessages) {
            if (msg.getContent() != null) {
                context.append(msg.getSender() + ": " + msg.getContent() + "\n");
            }
        }

        return context.toString();
    }
    
    /**
     * 构建完整的消息列表
     */
    private List<OllamaMessage> buildMessagesList(String personaPrompt, ChatSession session, String longTermMemory, String userInput) {
        List<OllamaMessage> messages = new ArrayList<>();
        
        // 1. 添加系统消息（人设和基础指令）
        String systemContent = buildSystemContent(personaPrompt);
        if (systemContent != null && !systemContent.isEmpty()) {
            messages.add(new OllamaMessage("system", systemContent));
        }
        
        // 2. 添加历史对话消息（排除最新的用户消息，因为我们会单独处理）
        List<ChatMessage> recentMessages = session.getRecentMessages(10);
        // 排除最后一条消息，因为它是刚刚添加的当前用户消息
        int historyCount = Math.max(0, recentMessages.size() - 1);
        for (int i = 0; i < historyCount; i++) {
            ChatMessage msg = recentMessages.get(i);
            if (msg.getContent() != null && !msg.getContent().trim().isEmpty()) {
                String role = mapSenderToRole(msg.getSender());
                messages.add(new OllamaMessage(role, msg.getContent()));
                logger.debug("添加历史消息: role={}, contentLength={}", role, msg.getContent().length());
            }
        }

        // 3. 添加当前用户输入
        StringBuilder currentUserContent = new StringBuilder();
        currentUserContent.append(userInput);

        // 4. 添加长期记忆（如果有的话，作为用户消息的上下文）
        if (longTermMemory != null && !longTermMemory.isEmpty()) {
            currentUserContent.append("[相关记忆]：\n").append(longTermMemory).append("\n\n");
            logger.debug("添加长期记忆到当前用户消息，长度: {}", longTermMemory.length());
        }

        // 5.其他额外设置（qwen系列的不思考模式）
//        currentUserContent.append("\no_think");

        messages.add(new OllamaMessage("user", currentUserContent.toString()));
        
        logger.info("消息列表构建完成，总消息数: {}, 系统消息: {}, 历史消息: {}, 当前用户消息: 1", 
                   messages.size(), 
                   systemContent != null && !systemContent.isEmpty() ? 1 : 0,
                   historyCount);
        
        return messages;
    }
    
    /**
     * 构建系统消息内容
     */
    private String buildSystemContent(String personaPrompt) {
        StringBuilder systemContent = new StringBuilder();
        
        // 基础系统指令
        String baseInstruction = "你是一个智能AI助手。";
        systemContent.append(baseInstruction);
        
        // 人设信息
        if (personaPrompt != null && !personaPrompt.isEmpty()) {
            systemContent.append("\n\n").append(personaPrompt);
        }
        
        return systemContent.toString();
    }
    
    /**
     * 将发送者映射为角色
     */
    private String mapSenderToRole(String sender) {
        if (sender == null) return "user";
        return switch (sender.toLowerCase()) {
            case "assistant", "ai", "bot" -> "assistant";
            case "system" -> "system";
            default -> "user";
        };
    }
    
    
    /**
     * 智能过滤思考内容，保留真正的回复
     */
    private String filterThinkingContent(String content) {
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
        
        // 只记录调试信息，不在这里打印完整内容
        if (content.contains("<think>")) {
            logger.debug("过滤统计 - 原始长度: {}, 过滤后长度: {}", content.length(), filtered.length());
        }
        
        return filtered.isEmpty() ? null : filtered;
    }
    
    
    /**
     * 生成流式回复（使用Ollama）
     */
    private void generateStreamingResponse(List<OllamaMessage> messages, String sessionId, Consumer<ChatMessage> callback, 
                                         long messageStartTime, long aiCallStartTime) {
        
        // 检查Ollama服务是否可用
        if (!ollamaService.isServiceAvailable()) {
            logger.error("Ollama服务不可用，无法生成响应，sessionId: {}", sessionId);
            
            ChatMessage errorMessage = new ChatMessage();
            errorMessage.setType("error");
            errorMessage.setContent("抱歉，AI服务当前不可用，请稍后重试。");
            errorMessage.setSender("assistant");
            errorMessage.setSessionId(sessionId);
            
            callback.accept(errorMessage);
            return;
        }
        
        StringBuilder completeResponse = new StringBuilder();
        StringBuilder thinkingContent = new StringBuilder(); // 存储思考内容
        StringBuilder userVisibleContent = new StringBuilder(); // 存储用户实际看到的内容
        final int[] chunkCounter = {0}; // 使用数组来在lambda中修改值
        final boolean[] isFirstChunk = {true}; // 跟踪是否是第一个数据块
        final boolean[] inThinkingMode = {false}; // 跟踪是否在思考模式中
        
        // 使用Ollama服务生成流式响应
        ollamaService.generateStreamingResponse(
            messages,
            // 成功处理每个chunk
            chunk -> {
                chunkCounter[0]++;
                completeResponse.append(chunk);
                
                // 记录第一个数据块的接收时间
                if (isFirstChunk[0]) {
                    long firstChunkTime = System.currentTimeMillis();
                    long timeToFirstChunk = firstChunkTime - messageStartTime;
                    long aiResponseTime = firstChunkTime - aiCallStartTime;
                    
                    logger.info("🎯 AI首次响应时间统计 - sessionId: {}, 从用户消息到AI首次响应: {}ms, AI处理时间: {}ms",
                               sessionId, timeToFirstChunk, aiResponseTime);
                    
                    isFirstChunk[0] = false;
                }
                
                // 检查当前块是否包含思考标记
                boolean chunkContainsThinkStart = chunk.contains("<think>");
                boolean chunkContainsThinkEnd = chunk.contains("</think>");
                
                // 处理思考模式的状态转换和内容存储
                if (chunkContainsThinkStart) {
                    inThinkingMode[0] = true;
//                    logger.debug("检测到思考开始标记，进入思考模式，sessionId: {}", sessionId);
                }
                
                // 如果在思考模式中，存储思考内容
                if (inThinkingMode[0]) {
                    thinkingContent.append(chunk);
                }
                
                if (chunkContainsThinkEnd) {
                    inThinkingMode[0] = false;
//                    logger.debug("检测到思考结束标记，退出思考模式，sessionId: {}", sessionId);
                    
                    // 打印完整的思考内容到日志
                    logger.info("🧠 完整思考内容 - sessionId: {}\n{}", sessionId, thinkingContent.toString());
                }
                
                // 只有不在思考模式中的内容才发送给用户
                if (!inThinkingMode[0] && !chunkContainsThinkStart && !chunkContainsThinkEnd) {
                    if (chunk != null && !chunk.trim().isEmpty()) {
                        userVisibleContent.append(chunk); // 累积用户可见内容
                        
                        ChatMessage streamMessage = new ChatMessage();
                        streamMessage.setType("text");
                        streamMessage.setContent(chunk);
                        streamMessage.setSender("assistant");
                        streamMessage.setSessionId(sessionId);
                        streamMessage.setStreaming(true);
                        streamMessage.setStreamComplete(false);
                        
                        callback.accept(streamMessage);
                    }
                } else if (chunkContainsThinkEnd) {
                    // 如果当前块包含思考结束标记，需要提取结束标记后的内容
                    int endThinkIndex = chunk.indexOf("</think>");
                    if (endThinkIndex + 8 < chunk.length()) {
                        String afterThink = chunk.substring(endThinkIndex + 8);
                        if (!afterThink.trim().isEmpty()) {
                            userVisibleContent.append(afterThink); // 累积用户可见内容
                            
                            ChatMessage streamMessage = new ChatMessage();
                            streamMessage.setType("text");
                            streamMessage.setContent(afterThink);
                            streamMessage.setSender("assistant");
                            streamMessage.setSessionId(sessionId);
                            streamMessage.setStreaming(true);
                            streamMessage.setStreamComplete(false);
                            
                            callback.accept(streamMessage);
                        }
                    }
                } else if (chunkContainsThinkStart) {
                    // 如果当前块包含思考开始标记，需要提取开始标记前的内容
                    int thinkIndex = chunk.indexOf("<think>");
                    if (thinkIndex > 0) {
                        String beforeThink = chunk.substring(0, thinkIndex);
                        if (!beforeThink.trim().isEmpty()) {
                            userVisibleContent.append(beforeThink); // 累积用户可见内容
                            
                            ChatMessage streamMessage = new ChatMessage();
                            streamMessage.setType("text");
                            streamMessage.setContent(beforeThink);
                            streamMessage.setSender("assistant");
                            streamMessage.setSessionId(sessionId);
                            streamMessage.setStreaming(true);
                            streamMessage.setStreamComplete(false);
                            
                            callback.accept(streamMessage);
                        }
                    }
                }
            },
            // 错误处理
            error -> {
                logger.error("Ollama流式响应发生错误，sessionId: {}, 已接收{}个数据块，累积长度: {}", 
                           sessionId, chunkCounter[0], completeResponse.length(), error);
                
                // 如果有思考内容，也要打印出来
                if (thinkingContent.length() > 0) {
                    logger.info("🧠 异常情况下的思考内容 - sessionId: {}\n{}", sessionId, thinkingContent.toString());
                }
                
                // 发送完成消息
                if (completeResponse.length() > 0) {
                    logger.info("流式响应异常但有部分内容，发送完成信号，sessionId: {}, 最终响应长度: {}", 
                               sessionId, completeResponse.length());
                    
                    // 输出完整的AI返回内容（包含异常情况下的部分内容）
                    logger.info("🤖 异常情况下的完整AI输出内容 - sessionId: {}\n{}", sessionId, completeResponse.toString());
                    
                    // 输出用户实际接收到的内容（异常情况下）
                    logger.info("📺 异常情况下用户实际接收到的内容 - sessionId: {}\n{}", sessionId, 
                               userVisibleContent.length() > 0 ? userVisibleContent.toString() : "无有效内容");
                    
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
        
        // 添加完成处理
        CompletableFuture.runAsync(() -> {
            try {
                // 等待流式响应完成
                Thread.sleep(2000);
                
                if (completeResponse.length() > 0) {
                    logger.info("流式响应正常完成，sessionId: {}, 总数据块: {}, 最终响应长度: {}", 
                               sessionId, chunkCounter[0], completeResponse.length());
                    
                    // 输出完整的AI返回内容（原始内容，包含思考内容）
                    logger.info("🤖 完整AI原始输出内容 - sessionId: {}\n{}", sessionId, completeResponse.toString());
                    
                    // 输出用户实际接收到的完整内容（流式发送的累积）
                    logger.info("📺 用户实际接收到的完整内容 - sessionId: {}\n{}", sessionId, 
                               userVisibleContent.length() > 0 ? userVisibleContent.toString() : "无有效内容");
                    
                    // 输出过滤后的内容（通过filterThinkingContent方法处理的结果）
                    String filteredResponse = filterThinkingContent(completeResponse.toString());
                    logger.info("🔄 过滤方法处理后的内容 - sessionId: {}\n{}", sessionId, 
                               filteredResponse != null ? filteredResponse : "无有效内容");
                    
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
                logger.warn("流式响应完成处理被中断，sessionId: {}", sessionId);
                Thread.currentThread().interrupt();
            }
        });
        
        // 注意：流式响应的完成处理在OllamaService的回调中进行
        // 这里不需要额外的完成检查逻辑
    }
    
    /**
     * 保存完整响应到会话历史
     */
    private void saveCompleteResponse(String sessionId, String completeResponse) {
        try {
            // 在日志中打印完整的流式响应汇总
            logger.info("🔄 流式响应完成汇总 - sessionId: {}", sessionId);
            logger.info("📄 完整流式响应内容:\n{}", completeResponse);
            
            // 过滤思考内容，获取干净的回答用于保存
            String filteredResponse = filterThinkingContent(completeResponse);
            String finalResponse = (filteredResponse != null && !filteredResponse.trim().isEmpty()) 
                                 ? filteredResponse : completeResponse;
            
            logger.info("💾 保存到历史记录的内容:\n{}", finalResponse);
            
            ChatMessage completeMessage = new ChatMessage();
            completeMessage.setType("text");
            completeMessage.setContent(finalResponse);  // 保存过滤后的内容
            completeMessage.setSender("assistant");
            completeMessage.setSessionId(sessionId);
            completeMessage.setStreaming(false);
            
            ChatSession session = sessionService.getSession(sessionId);
            if (session != null) {
                session.addMessage(completeMessage);
                
                // 添加到对话历史记录
                conversationHistoryService.addMessage(sessionId, completeMessage);
                
                // 更新长期记忆（使用过滤后的内容）
                memoryService.updateMemory(sessionId, finalResponse);
            }
        } catch (Exception e) {
            logger.error("保存完整响应时发生错误", e);
        }
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

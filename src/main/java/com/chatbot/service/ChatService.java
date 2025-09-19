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
    @SuppressWarnings("unused")
    private final MultiModalService multiModalService;
    @SuppressWarnings("unused")
    private final AIConfig aiConfig;
    private final OllamaService ollamaService;
    private final ConversationHistoryService conversationHistoryService;
    private final SessionHistoryService sessionHistoryService;
    
    public ChatService(SessionService sessionService, 
                      PersonaService personaService,
                      MemoryService memoryService,
                      MultiModalService multiModalService,
                      AIConfig aiConfig,
                      OllamaService ollamaService,
                      ConversationHistoryService conversationHistoryService,
                      SessionHistoryService sessionHistoryService) {
        logger.info("初始化ChatService");
        this.sessionService = sessionService;
        this.personaService = personaService;
        this.memoryService = memoryService;
        this.multiModalService = multiModalService;
        this.aiConfig = aiConfig;
        this.ollamaService = ollamaService;
        this.conversationHistoryService = conversationHistoryService;
        this.sessionHistoryService = sessionHistoryService;
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
                
                // 1.5. 加载历史记录到会话中（如果会话是新创建的或内存中没有历史）
                logger.debug("步骤1.5：加载历史记录到会话中");
                long step1_5Start = System.currentTimeMillis();
                loadHistoryToSession(session);
                long step1_5Time = System.currentTimeMillis() - step1_5Start;
                logger.debug("历史记录加载完成，耗时: {}ms，当前会话消息数: {}", step1_5Time, session.getMessageHistory().size());
                
                // 2. 添加用户消息到会话历史和对话记录
                logger.debug("步骤2：添加用户消息到会话历史和对话记录");
                long step2Start = System.currentTimeMillis();
                userMessage.setRole("user");
                session.addMessage(userMessage);
                
                // 添加到对话历史记录和会话历史文件
                conversationHistoryService.addMessage(sessionId, userMessage);
                sessionHistoryService.addMessageAndSave(sessionId, userMessage);
                
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
                errorResponse.setRole("assistant");
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
                context.append(msg.getRole() + ": " + msg.getContent() + "\n");
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
                String role = mapSenderToRole(msg.getRole());
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
     * 生成流式回复（使用Ollama）- 优化版
     */
    private void generateStreamingResponse(List<OllamaMessage> messages, String sessionId, Consumer<ChatMessage> callback, 
                                         long messageStartTime, long aiCallStartTime) {
        
        // 检查Ollama服务是否可用
        if (!ollamaService.isServiceAvailable()) {
            logger.error("Ollama服务不可用，无法生成响应，sessionId: {}", sessionId);
            
            ChatMessage errorMessage = new ChatMessage();
            errorMessage.setType("error");
            errorMessage.setContent("抱歉，AI服务当前不可用，请稍后重试。");
            errorMessage.setRole("assistant");
            errorMessage.setSessionId(sessionId);
            
            callback.accept(errorMessage);
            return;
        }
        
        // 流式处理状态管理
        StreamingState state = new StreamingState();
        
        // 使用Ollama服务生成流式响应
        ollamaService.generateStreamingResponse(
            messages,
            // 成功处理每个chunk
            chunk -> {
                handleStreamChunk(chunk, sessionId, callback, state, messageStartTime, aiCallStartTime);
            },
            // 错误处理
            error -> {
                handleStreamError(error, sessionId, callback, state);
            }
        );
        
        // 添加完成处理回调 - 使用更短的延迟
        CompletableFuture.runAsync(() -> {
            try {
                // 等待流式响应完成 - 减少延迟时间
                Thread.sleep(500); // 从3000ms减少到500ms
                
                // 发送流完成信号
                ChatMessage finalMessage = new ChatMessage();
                finalMessage.setType("text");
                finalMessage.setContent("");
                finalMessage.setRole("assistant");
                finalMessage.setSessionId(sessionId);
                finalMessage.setStreaming(true);
                finalMessage.setStreamComplete(true);
                
                callback.accept(finalMessage);
                
                // 保存完整响应
                if (state.completeResponse.length() > 0) {
                    logger.info("💾 触发AI回答保存 - sessionId: {}, 响应长度: {}", 
                               sessionId, state.completeResponse.length());
                    saveCompleteResponse(sessionId, state.completeResponse.toString());
                } else {
                    logger.warn("⚠️ 没有AI回答内容需要保存 - sessionId: {}", sessionId);
                }
                
            } catch (InterruptedException e) {
                logger.warn("流式响应完成处理被中断，sessionId: {}", sessionId);
                Thread.currentThread().interrupt();
            }
        });
    }
    
    /**
     * 流式处理状态类
     */
    private static class StreamingState {
        final StringBuilder completeResponse = new StringBuilder();
        final StringBuilder thinkingContent = new StringBuilder();
        final StringBuilder userVisibleContent = new StringBuilder();
        int chunkCounter = 0;
        boolean isFirstChunk = true;
        boolean inThinkingMode = false;
    }
    
    /**
     * 处理流式数据块
     */
    private void handleStreamChunk(String chunk, String sessionId, Consumer<ChatMessage> callback, 
                                 StreamingState state, long messageStartTime, long aiCallStartTime) {
        state.chunkCounter++;
        state.completeResponse.append(chunk);
        
        // 记录第一个数据块的接收时间
        if (state.isFirstChunk) {
            long firstChunkTime = System.currentTimeMillis();
            long timeToFirstChunk = firstChunkTime - messageStartTime;
            long aiResponseTime = firstChunkTime - aiCallStartTime;
            
            logger.info("🎯 AI首次响应时间统计 - sessionId: {}, 从用户消息到AI首次响应: {}ms, AI处理时间: {}ms",
                       sessionId, timeToFirstChunk, aiResponseTime);
            
            state.isFirstChunk = false;
        }
        
        // 获取用户的思考显示偏好
        boolean showThinking = getUserThinkingPreference(sessionId);
        
        // 处理思考模式和内容过滤
        ThinkingProcessResult result = processThinkingContentWithToggle(chunk, state, sessionId, showThinking);
        
        // 发送思考内容（如果用户开启了显示）
        if (result.thinkingContent != null && !result.thinkingContent.isEmpty()) {
            state.userVisibleContent.append(result.thinkingContent);
            
            ChatMessage thinkingMessage = new ChatMessage();
            thinkingMessage.setType("text");
            thinkingMessage.setContent(result.thinkingContent);
            thinkingMessage.setRole("assistant");
            thinkingMessage.setSessionId(sessionId);
            thinkingMessage.setStreaming(true);
            thinkingMessage.setStreamComplete(false);
            thinkingMessage.setThinking(true);
            thinkingMessage.setThinkingContent(result.thinkingContent);
            
            callback.accept(thinkingMessage);
        }
        
        // 发送可见内容给用户
        if (result.visibleContent != null && !result.visibleContent.isEmpty()) {
            state.userVisibleContent.append(result.visibleContent);
            
            ChatMessage streamMessage = new ChatMessage();
            streamMessage.setType("text");
            streamMessage.setContent(result.visibleContent);
            streamMessage.setRole("assistant");
            streamMessage.setSessionId(sessionId);
            streamMessage.setStreaming(true);
            streamMessage.setStreamComplete(false);
            streamMessage.setThinking(false);
            
            callback.accept(streamMessage);
        }
    }
    
    /**
     * 思考处理结果类
     */
    private static class ThinkingProcessResult {
        String visibleContent;
        String thinkingContent;
        
        ThinkingProcessResult(String visibleContent, String thinkingContent) {
            this.visibleContent = visibleContent;
            this.thinkingContent = thinkingContent;
        }
    }
    
    /**
     * 处理思考内容和过滤（支持切换显示）
     */
    private ThinkingProcessResult processThinkingContentWithToggle(String chunk, StreamingState state, String sessionId, boolean showThinking) {
        boolean chunkContainsThinkStart = chunk.contains("<think>");
        boolean chunkContainsThinkEnd = chunk.contains("</think>");
        
        String visibleContent = null;
        String thinkingContent = null;
        
        // 处理思考模式状态转换
        if (chunkContainsThinkStart) {
            state.inThinkingMode = true;
        }
        
        if (state.inThinkingMode) {
            state.thinkingContent.append(chunk);
            if (showThinking) {
                // 如果用户选择显示思考过程，则返回思考内容
                thinkingContent = chunk;
            }
        }
        
        if (chunkContainsThinkEnd) {
            state.inThinkingMode = false;
            // 记录思考内容
            logger.debug("🧠 思考内容片段 - sessionId: {}, 内容: {}", sessionId, state.thinkingContent.toString());
        }
        
        // 处理可见内容
        if (!state.inThinkingMode && !chunkContainsThinkStart && !chunkContainsThinkEnd) {
            visibleContent = chunk;
        } else if (chunkContainsThinkEnd) {
            // 提取思考结束后的内容
            int endThinkIndex = chunk.indexOf("</think>");
            if (endThinkIndex + 8 < chunk.length()) {
                visibleContent = chunk.substring(endThinkIndex + 8);
            }
            // 如果用户选择显示思考过程，也要显示思考部分
            if (showThinking) {
                thinkingContent = chunk.substring(0, endThinkIndex + 8);
            }
        } else if (chunkContainsThinkStart) {
            // 提取思考开始前的内容
            int thinkIndex = chunk.indexOf("<think>");
            if (thinkIndex > 0) {
                visibleContent = chunk.substring(0, thinkIndex);
            }
            // 如果用户选择显示思考过程，也要显示思考部分
            if (showThinking) {
                thinkingContent = chunk.substring(thinkIndex);
            }
        }
        
        return new ThinkingProcessResult(visibleContent, thinkingContent);
    }
    
    /**
     * 获取用户的思考显示偏好（默认不显示）
     */
    private boolean getUserThinkingPreference(String sessionId) {
        try {
            ChatSession session = sessionService.getSession(sessionId);
            if (session != null && session.getMetadata() != null) {
                Object showThinking = session.getMetadata().get("showThinking");
                if (showThinking instanceof Boolean) {
                    return (Boolean) showThinking;
                }
            }
        } catch (Exception e) {
            logger.debug("获取用户思考显示偏好失败", e);
        }
        return false; // 默认不显示思考过程
    }
    
    /**
     * 设置用户的思考显示偏好
     */
    public void setUserThinkingPreference(String sessionId, boolean showThinking) {
        try {
            ChatSession session = sessionService.getOrCreateSession(sessionId);
            if (session.getMetadata() == null) {
                session.setMetadata(new java.util.HashMap<>());
            }
            session.getMetadata().put("showThinking", showThinking);
            logger.info("设置用户思考显示偏好 - sessionId: {}, showThinking: {}", sessionId, showThinking);
        } catch (Exception e) {
            logger.error("设置用户思考显示偏好失败", e);
        }
    }
    
    /**
     * 获取用户思考过程保存偏好
     */
    private boolean getUserThinkingSavePreference(String sessionId) {
        try {
            ChatSession session = sessionService.getSession(sessionId);
            if (session != null && session.getMetadata() != null) {
                Object saveThinking = session.getMetadata().get("saveThinking");
                if (saveThinking instanceof Boolean) {
                    return (Boolean) saveThinking;
                }
            }
        } catch (Exception e) {
            logger.error("获取用户思考过程保存偏好失败", e);
        }
        // 默认不保存思考过程
        return false;
    }
    
    /**
     * 设置用户思考过程保存偏好
     */
    public void setUserThinkingSavePreference(String sessionId, boolean saveThinking) {
        try {
            ChatSession session = sessionService.getOrCreateSession(sessionId);
            if (session.getMetadata() == null) {
                session.setMetadata(new java.util.HashMap<>());
            }
            session.getMetadata().put("saveThinking", saveThinking);
            logger.info("设置用户思考过程保存偏好 - sessionId: {}, saveThinking: {}", sessionId, saveThinking);
        } catch (Exception e) {
            logger.error("设置用户思考过程保存偏好失败", e);
        }
    }
    
    /**
     * 处理流式错误
     */
    private void handleStreamError(Throwable error, String sessionId, Consumer<ChatMessage> callback, StreamingState state) {
        logger.error("Ollama流式响应发生错误，sessionId: {}, 已接收{}个数据块，累积长度: {}", 
                   sessionId, state.chunkCounter, state.completeResponse.length(), error);
        
        // 记录思考内容
        if (state.thinkingContent.length() > 0) {
            logger.info("🧠 异常情况下的思考内容 - sessionId: {}\n{}", sessionId, state.thinkingContent.toString());
        }
        
        // 发送错误或部分完成消息
        if (state.completeResponse.length() > 0) {
            // 发送流完成信号
            ChatMessage finalMessage = new ChatMessage();
            finalMessage.setType("text");
            finalMessage.setContent("");
            finalMessage.setRole("assistant");
            finalMessage.setSessionId(sessionId);
            finalMessage.setStreaming(true);
            finalMessage.setStreamComplete(true);
            
            callback.accept(finalMessage);
            
            // 保存部分响应
            saveCompleteResponse(sessionId, state.completeResponse.toString());
        } else {
            // 发送错误消息
            ChatMessage errorMessage = new ChatMessage();
            errorMessage.setType("error");
            errorMessage.setContent("抱歉，AI服务暂时不可用，请稍后重试。");
            errorMessage.setRole("assistant");
            errorMessage.setSessionId(sessionId);
            
            callback.accept(errorMessage);
        }
    }
    
    /**
     * 保存完整响应到会话历史
     */
    private void saveCompleteResponse(String sessionId, String completeResponse) {
        try {
            logger.info("🔄 流式响应完成汇总 - sessionId: {}, 内容长度: {}, 内容预览: {}", 
                       sessionId, completeResponse.length(), 
                       completeResponse.length() > 100 ? completeResponse.substring(0, 100) + "..." : completeResponse);
            
            // 获取用户的思考过程保存偏好
            boolean saveThinking = getUserThinkingSavePreference(sessionId);
            
            String finalResponse;
            if (saveThinking) {
                // 如果用户选择保存思考过程，保存完整内容
                finalResponse = completeResponse;
                logger.debug("💾 保存完整内容（包含思考过程）到历史记录，长度: {}", finalResponse.length());
            } else {
                // 过滤思考内容，获取干净的回答用于保存
                String filteredResponse = filterThinkingContent(completeResponse);
                finalResponse = (filteredResponse != null && !filteredResponse.trim().isEmpty()) 
                               ? filteredResponse : completeResponse;
                logger.debug("💾 保存过滤后的内容到历史记录，长度: {}", finalResponse.length());
            }
            
            ChatMessage completeMessage = new ChatMessage();
            completeMessage.setType("text");
            completeMessage.setContent(finalResponse);
            completeMessage.setRole("assistant");
            completeMessage.setSessionId(sessionId);
            completeMessage.setStreaming(false);
            
            // 如果保存了思考过程，标记消息包含思考内容
            if (saveThinking && completeResponse.contains("<think>")) {
                completeMessage.setThinking(true);
            }
            
            ChatSession session = sessionService.getSession(sessionId);
            if (session != null) {
                session.addMessage(completeMessage);
                conversationHistoryService.addMessage(sessionId, completeMessage);
                
                // 同时保存到会话历史文件
                logger.debug("💾 开始保存AI回答到历史文件 - sessionId: {}, role: {}, contentLength: {}", 
                           sessionId, completeMessage.getRole(), completeMessage.getContent().length());
                sessionHistoryService.addMessageAndSave(sessionId, completeMessage);
                logger.debug("💾 AI回答保存完成 - sessionId: {}", sessionId);
                
                // 对于记忆更新，始终使用过滤后的内容
                String memoryContent = filterThinkingContent(completeResponse);
                String finalMemoryContent = (memoryContent != null && !memoryContent.trim().isEmpty()) 
                                          ? memoryContent : completeResponse;
                memoryService.updateMemory(sessionId, finalMemoryContent);
            }
        } catch (Exception e) {
            logger.error("保存完整响应时发生错误", e);
        }
    }
    
    /**
     * 加载历史记录到会话中
     */
    private void loadHistoryToSession(ChatSession session) {
        String sessionId = session.getSessionId();
        
        // 如果内存中已有消息历史，且不是只有系统消息，则不需要重新加载
        List<ChatMessage> currentHistory = new ArrayList<>(session.getMessageHistory());
        if (currentHistory != null && currentHistory.size() > 0) {
            // 检查是否有用户或助手的消息（非系统消息）
            boolean hasUserOrAssistantMessages = currentHistory.stream()
                .anyMatch(msg -> "user".equals(msg.getRole()) || "assistant".equals(msg.getRole()));
            
            if (hasUserOrAssistantMessages) {
                logger.debug("会话内存中已有对话历史，跳过加载，sessionId: {}, 消息数: {}", 
                           sessionId, currentHistory.size());
                return;
            }
        }
        
        // 从文件加载历史记录
        List<ChatMessage> historyMessages = sessionHistoryService.loadSessionHistory(sessionId);
        
        if (historyMessages != null && !historyMessages.isEmpty()) {
            logger.info("从文件加载历史记录到会话，sessionId: {}, 历史消息数: {}", 
                       sessionId, historyMessages.size());
            
            // 将历史消息添加到会话中
            for (ChatMessage msg : historyMessages) {
                session.addMessage(msg);
            }
            
            logger.debug("历史记录加载完成，会话当前消息数: {}", session.getMessageHistory().size());
        } else {
            logger.debug("没有找到历史记录文件或文件为空，sessionId: {}", sessionId);
        }
    }
    
    /**
     * 结束会话并保存历史记录
     */
    public void endSession(String sessionId) {
        try {
            logger.info("结束会话并保存历史记录，sessionId: {}", sessionId);
            
            ChatSession session = sessionService.getSession(sessionId);
            if (session != null) {
                // 获取会话中的所有消息
                List<ChatMessage> allMessages = new ArrayList<>(session.getMessageHistory());
                
                if (!allMessages.isEmpty()) {
                    // 保存完整的会话历史到文件
                    sessionHistoryService.saveSessionHistory(sessionId, allMessages);
                    logger.info("会话历史已保存到文件，sessionId: {}, 消息数量: {}", sessionId, allMessages.size());
                } else {
                    logger.debug("会话没有消息，跳过保存，sessionId: {}", sessionId);
                }
            } else {
                logger.warn("未找到会话，无法保存历史记录，sessionId: {}", sessionId);
            }
            
        } catch (Exception e) {
            logger.error("结束会话并保存历史记录时发生错误，sessionId: {}", sessionId, e);
        }
    }
    
    /**
     * 清理会话资源
     */
    public void cleanupSession(String sessionId) {
        // 先结束会话并保存历史记录
        endSession(sessionId);
        
        // 清理会话相关的资源
        logger.info("清理会话资源: {}", sessionId);
        
        // 可以在这里添加其他清理逻辑
        // 例如：清理临时文件、取消正在进行的任务等
    }
}
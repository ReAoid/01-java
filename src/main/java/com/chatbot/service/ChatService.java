package com.chatbot.service;

import com.chatbot.config.AppConfig;
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
    private final WorldBookService worldBookService;
    @SuppressWarnings("unused")
    private final MultiModalService multiModalService;
    private final AppConfig.AIConfig aiConfig;
    private final OllamaService ollamaService;
    private final ConversationHistoryService conversationHistoryService;
    private final SessionHistoryService sessionHistoryService;
    
    public ChatService(SessionService sessionService, 
                      PersonaService personaService,
                      MemoryService memoryService,
                      WorldBookService worldBookService,
                      MultiModalService multiModalService,
                      AppConfig appConfig,
                      OllamaService ollamaService,
                      ConversationHistoryService conversationHistoryService,
                      SessionHistoryService sessionHistoryService) {
        logger.info("初始化ChatService");
        this.sessionService = sessionService;
        this.personaService = personaService;
        this.memoryService = memoryService;
        this.worldBookService = worldBookService;
        this.multiModalService = multiModalService;
        this.aiConfig = appConfig.getAi();
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
                
                // 2. 获取系统提示词和人设提示词
                logger.debug("步骤2：获取系统提示词和人设提示词");
                long step2Start = System.currentTimeMillis();
                List<ChatMessage> systemPrompts = getSystemPrompts(session);
                long step2Time = System.currentTimeMillis() - step2Start;
                logger.debug("系统提示词获取完成，耗时: {}ms，提示词数量: {}", step2Time, systemPrompts.size());
                
                // 3. 获取历史对话记录（去掉系统提示词部分，只保留AI和用户对话历史）
                logger.debug("步骤3：获取历史对话记录");
                long step3Start = System.currentTimeMillis();
                List<ChatMessage> dialogueHistory = getDialogueHistory(session);
                long step3Time = System.currentTimeMillis() - step3Start;
                logger.debug("历史对话记录获取完成，耗时: {}ms，对话消息数: {}", step3Time, dialogueHistory.size());
                
                // 4. 预处理用户输入
                logger.debug("步骤4：预处理用户输入");
                long step4Start = System.currentTimeMillis();
                String processedInput = preprocessInput(userMessage.getContent());
                long step4Time = System.currentTimeMillis() - step4Start;
                logger.debug("用户输入预处理完成，耗时: {}ms，原始长度: {}, 处理后长度: {}, 原始: '{}', 处理后: '{}'",
                           step4Time,
                           userMessage.getContent() != null ? userMessage.getContent().length() : 0,
                           processedInput.length(),
                           userMessage.getContent(),
                           processedInput);
                
                // 5. 获取世界书设定（长期记忆）
                logger.debug("步骤5：获取世界书设定");
                long step5Start = System.currentTimeMillis();
                ChatMessage worldBookSetting = getWorldBookSetting(session, processedInput);
                long step5Time = System.currentTimeMillis() - step5Start;
                logger.debug("世界书设定获取完成，耗时: {}ms，是否有设定: {}", step5Time, worldBookSetting != null);
                
                // 6. 准备用户消息（不提前保存，等AI回答完成后一起保存）
                logger.debug("步骤6：准备用户消息");
                long step6Start = System.currentTimeMillis();
                userMessage.setRole("user");
                userMessage.setContent(processedInput); // 使用预处理后的输入
                
                long step6Time = System.currentTimeMillis() - step6Start;
                logger.debug("用户消息准备完成，耗时: {}ms", step6Time);
                
                // 7. 构建完整的消息列表（带 token 限制）
                logger.debug("步骤7：构建完整的消息列表");
                long step7Start = System.currentTimeMillis();
                List<OllamaMessage> messages = buildMessagesListWithTokenLimit(
                    systemPrompts, dialogueHistory, worldBookSetting, userMessage);
                long step7Time = System.currentTimeMillis() - step7Start;
                logger.debug("消息列表构建完成，耗时: {}ms，消息数量: {}", step7Time, messages.size());
                
                // 记录预处理完成时间
                long preprocessingTime = System.currentTimeMillis() - messageStartTime;
                logger.info("📊 预处理阶段完成，sessionId: {}, 总预处理时间: {}ms (步骤1: {}ms, 步骤2: {}ms, 步骤3: {}ms, 步骤4: {}ms, 步骤5: {}ms, 步骤6: {}ms, 步骤7: {}ms)", 
                           sessionId, preprocessingTime, step1Time, step2Time, step3Time, step4Time, step5Time, step6Time, step7Time);
                
                // 8. 调用AI模型生成回复（流式）
                logger.debug("步骤8：调用AI模型生成回复");
                long aiCallStartTime = System.currentTimeMillis();
                generateStreamingResponse(messages, sessionId, responseCallback, messageStartTime, aiCallStartTime, userMessage);
                
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
                                         long messageStartTime, long aiCallStartTime, ChatMessage userMessage) {
        
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
                handleStreamError(error, sessionId, callback, state, userMessage);
            },
            // 完成处理回调 - 在流式响应真正完成时调用
            () -> {
                logger.debug("收到流式响应完成通知，sessionId: {}", sessionId);
                
                // 发送流完成信号
                ChatMessage finalMessage = new ChatMessage();
                finalMessage.setType("text");
                finalMessage.setContent("");
                finalMessage.setRole("assistant");
                finalMessage.setSessionId(sessionId);
                finalMessage.setStreaming(true);
                finalMessage.setStreamComplete(true);
                
                callback.accept(finalMessage);
                
                // 保存完整响应（同时保存用户消息和AI回答）
                if (state.completeResponse.length() > 0) {
//                    logger.info("💾 触发对话保存 - sessionId: {}, AI响应长度: {}",
//                               sessionId, state.completeResponse.length());
                    saveCompleteConversation(sessionId, userMessage, state.completeResponse.toString());
                } else {
                    logger.warn("⚠️ 没有AI回答内容需要保存 - sessionId: {}", sessionId);
                }
            }
        );
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
     * 获取用户的思考显示偏好（默认显示）
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
        return true; // 默认显示思考过程
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
     * 处理流式错误
     */
    private void handleStreamError(Throwable error, String sessionId, Consumer<ChatMessage> callback, StreamingState state, ChatMessage userMessage) {
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
            if (userMessage != null) {
                saveCompleteConversation(sessionId, userMessage, state.completeResponse.toString());
            }
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
     * 保存完整对话到会话历史（用户消息 + AI回答）
     */
    private void saveCompleteConversation(String sessionId, ChatMessage userMessage, String aiResponse) {
        try {
            logger.info("🔄 对话保存开始 - sessionId: {}, AI响应长度: {}, 内容预览: {}", 
                       sessionId, aiResponse.length(), 
                       aiResponse.length() > 100 ? aiResponse.substring(0, 100) + "..." : aiResponse);
            
            // 过滤AI回答中的思考内容，只保存干净的回答
            String filteredResponse = filterThinkingContent(aiResponse);
            String finalResponse = (filteredResponse != null && !filteredResponse.trim().isEmpty()) 
                                  ? filteredResponse : aiResponse;
//            logger.debug("💾 过滤思考内容后，AI回答长度: {}", finalResponse.length());
            
            // 创建AI回答消息
            ChatMessage aiMessage = new ChatMessage();
            aiMessage.setType("text");
            aiMessage.setContent(finalResponse);
            aiMessage.setRole("assistant");
            aiMessage.setSessionId(sessionId);
            aiMessage.setStreaming(false);
            
            ChatSession session = sessionService.getSession(sessionId);
            if (session != null) {
                // 1. 先保存用户消息
                logger.debug("💾 保存用户消息 - sessionId: {}, 内容长度: {}", 
                           sessionId, userMessage.getContent().length());
                session.addMessage(userMessage);
                conversationHistoryService.addMessage(sessionId, userMessage);
                sessionHistoryService.addMessageAndSave(sessionId, userMessage);
                
                // 2. 再保存AI回答
                logger.debug("💾 保存AI回答 - sessionId: {}, 内容长度: {}", 
                           sessionId, aiMessage.getContent().length());
                session.addMessage(aiMessage);
                conversationHistoryService.addMessage(sessionId, aiMessage);
                sessionHistoryService.addMessageAndSave(sessionId, aiMessage);
                
                // 3. 更新记忆和世界书（使用用户输入内容，而不是AI回答）
                memoryService.updateMemory(sessionId, userMessage.getContent());
                worldBookService.extractAndAddEntry(sessionId, userMessage.getContent());
                
                logger.info("💾 对话保存完成 - sessionId: {}, 用户消息和AI回答已保存", sessionId);
            }
        } catch (Exception e) {
            logger.error("保存完整对话时发生错误", e);
        }
    }
    
    /**
     * 获取系统提示词和人设提示词
     */
    private List<ChatMessage> getSystemPrompts(ChatSession session) {
        List<ChatMessage> systemPrompts = new ArrayList<>();
        
        // 1. 添加通用系统提示词（从配置文件读取）
        String baseSystemPrompt = aiConfig.getSystemPrompt().getBase();
        if (baseSystemPrompt == null || baseSystemPrompt.trim().isEmpty()) {
            baseSystemPrompt = aiConfig.getSystemPrompt().getFallback();
        }
        
        ChatMessage systemMessage = new ChatMessage();
        systemMessage.setRole("system");
        systemMessage.setContent(baseSystemPrompt);
        systemMessage.setSessionId(session.getSessionId());
        systemMessage.setType("text");
        systemPrompts.add(systemMessage);
        logger.debug("创建通用系统提示词，内容: {}", baseSystemPrompt);
        
        // 2. 如果启用人设且有人设，添加人设提示词
        if (aiConfig.getSystemPrompt().isEnablePersona()) {
            String personaId = session.getCurrentPersonaId();
            if (personaId != null) {
                String personaPrompt = personaService.getPersonaPrompt(personaId);
                if (personaPrompt != null && !personaPrompt.isEmpty()) {
                    ChatMessage personaMessage = new ChatMessage();
                    personaMessage.setRole("system");
                    personaMessage.setContent(personaPrompt);
                    personaMessage.setSessionId(session.getSessionId());
                    personaMessage.setType("text");
                    systemPrompts.add(personaMessage);
                    logger.debug("创建人设提示词，personaId: {}, 内容长度: {}", 
                               personaId, personaPrompt.length());
                }
            }
        } else {
            logger.debug("人设系统提示词已禁用");
        }
        
        return systemPrompts;
    }
    
    /**
     * 获取历史对话记录（去掉系统提示词部分，只保留AI和用户对话历史）
     */
    private List<ChatMessage> getDialogueHistory(ChatSession session) {
        String sessionId = session.getSessionId();
        List<ChatMessage> dialogueMessages = new ArrayList<>();
        
        // 首先检查内存中是否已有对话历史
        List<ChatMessage> currentHistory = new ArrayList<>(session.getMessageHistory());
        List<ChatMessage> existingDialogue = currentHistory.stream()
            .filter(msg -> "user".equals(msg.getRole()) || "assistant".equals(msg.getRole()))
            .toList();
            
        if (!existingDialogue.isEmpty()) {
            logger.debug("从会话内存中获取对话历史，sessionId: {}, 消息数: {}", 
                       sessionId, existingDialogue.size());
            dialogueMessages.addAll(existingDialogue);
        } else {
            // 从文件加载历史记录
            List<ChatMessage> historyMessages = sessionHistoryService.loadSessionHistory(sessionId);
            
            if (historyMessages != null && !historyMessages.isEmpty()) {
                // 过滤掉系统提示词部分，只保留AI和用户的对话历史
                List<ChatMessage> filteredDialogue = historyMessages.stream()
                    .filter(msg -> "user".equals(msg.getRole()) || "assistant".equals(msg.getRole()))
                    .toList();
                    
                logger.info("从文件加载对话历史，sessionId: {}, 原始消息数: {}, 过滤后对话消息数: {}", 
                           sessionId, historyMessages.size(), filteredDialogue.size());
                
                dialogueMessages.addAll(filteredDialogue);
            } else {
                logger.debug("没有找到历史记录文件或文件为空，sessionId: {}", sessionId);
            }
        }
        
        return dialogueMessages;
    }
    
    /**
     * 获取世界书设定（按相关性排序并设置阈值）
     */
    private ChatMessage getWorldBookSetting(ChatSession session, String userInput) {
        try {
            // 从世界书中获取相关设定
            String worldBookContent = retrieveRelevantWorldBook(session.getSessionId(), userInput);
            
            if (worldBookContent != null && !worldBookContent.trim().isEmpty()) {
                // 创建世界书消息
                ChatMessage worldBookMessage = new ChatMessage();
                worldBookMessage.setRole("system");
                worldBookMessage.setContent("为了回答用户的问题，你需要知道：\n" + worldBookContent);
                worldBookMessage.setSessionId(session.getSessionId());
                worldBookMessage.setType("text");
                
                logger.debug("创建世界书设定消息，内容长度: {}", worldBookContent.length());
                return worldBookMessage;
            } else {
                logger.debug("没有找到相关的世界书设定");
                return null;
            }
        } catch (Exception e) {
            logger.error("获取世界书设定时发生错误", e);
            return null;
        }
    }
    
    /**
     * 从世界书中检索相关内容（基于相关性阈值）
     */
    private String retrieveRelevantWorldBook(String sessionId, String userInput) {
        try {
            // 使用WorldBookService获取相关内容（包含手动配置和自动提取的内容）
            String worldBookContent = worldBookService.retrieveRelevantContent(sessionId, userInput);
            
            if (worldBookContent != null && !worldBookContent.trim().isEmpty()) {
                logger.debug("检索到世界书内容，长度: {}", worldBookContent.length());
                return worldBookContent;
            }
            
            logger.debug("未找到相关的世界书内容");
            return null;
        } catch (Exception e) {
            logger.error("检索世界书内容时发生错误", e);
            return null;
        }
    }
    
    /**
     * 构建完整的消息列表（带 token 限制和智能删除）
     */
    private List<OllamaMessage> buildMessagesListWithTokenLimit(
            List<ChatMessage> systemPrompts,
            List<ChatMessage> dialogueHistory, 
            ChatMessage worldBookSetting,
            ChatMessage userMessage) {
        
        List<OllamaMessage> messages = new ArrayList<>();
        
        // 配置参数（可以从配置文件或环境变量读取）
        final int MAX_TOKENS = getMaxTokenLimit(); // 最大 token 数量限制
        final int ESTIMATED_TOKENS_PER_CHAR = getTokensPerCharEstimate(); // 估算每个字符的 token 数（中文通常更高）
        
        int currentTokens = 0;
        
        // 1. 首先添加系统提示词（这些不能删除）
        for (ChatMessage systemMsg : systemPrompts) {
            if (systemMsg.getContent() != null && !systemMsg.getContent().trim().isEmpty()) {
                String role = mapSenderToRole(systemMsg.getRole());
                messages.add(new OllamaMessage(role, systemMsg.getContent()));
                currentTokens += estimateTokens(systemMsg.getContent(), ESTIMATED_TOKENS_PER_CHAR);
                logger.debug("添加系统消息: role={}, tokens={}", role, estimateTokens(systemMsg.getContent(), ESTIMATED_TOKENS_PER_CHAR));
            }
        }
        
        // 2. 添加世界书设定（如果有的话）
        if (worldBookSetting != null && worldBookSetting.getContent() != null && !worldBookSetting.getContent().trim().isEmpty()) {
            String role = mapSenderToRole(worldBookSetting.getRole());
            int worldBookTokens = estimateTokens(worldBookSetting.getContent(), ESTIMATED_TOKENS_PER_CHAR);
            
            if (currentTokens + worldBookTokens <= MAX_TOKENS) {
                messages.add(new OllamaMessage(role, worldBookSetting.getContent()));
                currentTokens += worldBookTokens;
                logger.debug("添加世界书设定: tokens={}", worldBookTokens);
            } else {
                logger.warn("世界书设定超过 token 限制，跳过添加");
            }
        }
        
        // 3. 添加当前用户消息（这个必须包含）
        if (userMessage != null && userMessage.getContent() != null && !userMessage.getContent().trim().isEmpty()) {
            String role = mapSenderToRole(userMessage.getRole());
            int userTokens = estimateTokens(userMessage.getContent(), ESTIMATED_TOKENS_PER_CHAR);
            messages.add(new OllamaMessage(role, userMessage.getContent()));
            currentTokens += userTokens;
            logger.debug("添加用户消息: tokens={}", userTokens);
        }
        
        // 4. 智能添加对话历史（从最新的开始，向前添加，直到达到 token 限制）
        List<ChatMessage> filteredHistory = filterDialogueHistoryByTokens(
            dialogueHistory, MAX_TOKENS - currentTokens, ESTIMATED_TOKENS_PER_CHAR);
        
        // 将过滤后的历史消息插入到系统消息之后、用户消息之前
        int insertIndex = systemPrompts.size();
        if (worldBookSetting != null) {
            insertIndex++;
        }
        
        for (ChatMessage historyMsg : filteredHistory) {
            if (historyMsg.getContent() != null && !historyMsg.getContent().trim().isEmpty()) {
                String role = mapSenderToRole(historyMsg.getRole());
                messages.add(insertIndex++, new OllamaMessage(role, historyMsg.getContent()));
                logger.debug("添加历史对话: role={}, contentLength={}", role, historyMsg.getContent().length());
            }
        }
        
        // 计算最终的 token 数量
        int finalTokens = messages.stream()
            .mapToInt(msg -> estimateTokens(msg.getContent(), ESTIMATED_TOKENS_PER_CHAR))
            .sum();
            
        logger.info("消息列表构建完成 - 总消息数: {}, 估算 tokens: {}/{}, 系统消息: {}, 历史消息: {}, 世界书: {}, 用户消息: 1", 
                   messages.size(), finalTokens, MAX_TOKENS, systemPrompts.size(), 
                   filteredHistory.size(), worldBookSetting != null ? 1 : 0);
        
        return messages;
    }
    
    /**
     * 估算文本的 token 数量
     */
    private int estimateTokens(String text, int tokensPerChar) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        return text.length() / tokensPerChar;
    }
    
    /**
     * 根据 token 限制过滤对话历史（从最远的开始删除）
     */
    private List<ChatMessage> filterDialogueHistoryByTokens(List<ChatMessage> dialogueHistory, int maxTokens, int tokensPerChar) {
        if (dialogueHistory == null || dialogueHistory.isEmpty()) {
            return new ArrayList<>();
        }
        
        List<ChatMessage> result = new ArrayList<>();
        int currentTokens = 0;
        
        // 从最新的消息开始向前检查（倒序遍历）
        for (int i = dialogueHistory.size() - 1; i >= 0; i--) {
            ChatMessage msg = dialogueHistory.get(i);
            if (msg.getContent() != null && !msg.getContent().trim().isEmpty()) {
                int msgTokens = estimateTokens(msg.getContent(), tokensPerChar);
                
                if (currentTokens + msgTokens <= maxTokens) {
                    result.add(0, msg); // 插入到列表开头以保持原有顺序
                    currentTokens += msgTokens;
                } else {
                    // 超过限制，停止添加更早的消息
                    logger.debug("历史消息超过 token 限制，丢弃 {} 条更早的消息", i + 1);
                    break;
                }
            }
        }
        
        logger.debug("过滤对话历史完成，保留 {}/{} 条消息，使用 tokens: {}/{}", 
                   result.size(), dialogueHistory.size(), currentTokens, maxTokens);
        
        return result;
    }
    
    /**
     * 获取最大 token 限制（可配置）
     */
    private int getMaxTokenLimit() {
        // 这里可以从配置文件或环境变量读取
        // 目前使用默认值 4000
        return 4000;
    }
    
    /**
     * 获取每个字符的 token 估算值（可配置）
     */
    private int getTokensPerCharEstimate() {
        // 中文字符通常比英文占用更多 token
        // 这里使用保守估算值 4
        return 4;
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
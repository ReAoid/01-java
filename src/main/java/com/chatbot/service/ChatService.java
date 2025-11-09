package com.chatbot.service;

import com.chatbot.config.AppConfig;
import com.chatbot.model.domain.ChatMessage;
import com.chatbot.model.domain.ChatSession;
import com.chatbot.model.dto.llm.Message;
import com.chatbot.model.config.UserPreferences;
import com.chatbot.service.chat.ChatContextBuilder;
import com.chatbot.service.chat.ChatMessageProcessor;
import com.chatbot.service.llm.impl.OllamaLLMServiceImpl;
import com.chatbot.util.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 聊天服务
 * 实现AI对话引擎和流式处理
 */
@Service
public class ChatService {
    
    private static final Logger logger = LoggerFactory.getLogger(ChatService.class);
    
    private final SessionService sessionService;
    private final KnowledgeService knowledgeService;  // Phase 2：统一知识管理
    private final AppConfig.AIConfig aiConfig;
    private final OllamaLLMServiceImpl llmService;  // 使用新的 LLM 服务
    private final ChatHistoryService chatHistoryService;  // 统一历史服务（替代 ConversationHistoryService 和 SessionHistoryService）
    private final WebSearchService webSearchService;
    private final TaskManager taskManager;
    private final UserPreferencesService userPreferencesService;
    
    // Phase 1 重构：新增的子服务
    private final ChatMessageProcessor messageProcessor;
    private final ChatContextBuilder contextBuilder;
    
    public ChatService(SessionService sessionService, 
                      KnowledgeService knowledgeService,  // Phase 2：使用统一的知识服务
                      AppConfig appConfig,
                      @Qualifier("ollamaLLMService") OllamaLLMServiceImpl llmService,
                      ChatHistoryService chatHistoryService,  // 使用统一的历史服务
                      WebSearchService webSearchService,
                      TaskManager taskManager,
                      UserPreferencesService userPreferencesService,
                      ChatMessageProcessor messageProcessor,  // Phase 1：消息处理
                      ChatContextBuilder contextBuilder) {    // Phase 1：上下文构建
        this.sessionService = sessionService;
        this.knowledgeService = knowledgeService;
        this.aiConfig = appConfig.getAi();
        this.llmService = llmService;  // 使用新的 LLM 服务
        this.chatHistoryService = chatHistoryService;  // 使用统一的历史服务
        this.webSearchService = webSearchService;
        this.taskManager = taskManager;
        this.userPreferencesService = userPreferencesService;
        this.messageProcessor = messageProcessor;
        this.contextBuilder = contextBuilder;
        
        logger.info("ChatService 初始化完成 - Phase 2 统一知识管理完成");
    }
    
    /**
     * 处理用户消息并生成AI回复（流式处理）
     */
    public String processMessage(ChatMessage userMessage, Consumer<ChatMessage> responseCallback) {
        long messageStartTime = System.currentTimeMillis();
        String sessionId = userMessage.getSessionId();
        
        // 生成任务ID
        String taskId = taskManager.generateTaskId(sessionId);
        logger.info("开始处理消息，sessionId: {}, taskId: {}", sessionId, taskId);
        
        // 检查是否有活跃的任务需要中断
        int activeTasks = taskManager.getSessionActiveTaskCount(sessionId);
        if (activeTasks > 0) {
            logger.info("检测到会话有 {} 个活跃任务正在运行，进行中断处理", activeTasks);
            int cancelledTasks = taskManager.cancelSessionTasks(sessionId);
            if (cancelledTasks > 0) {
                logger.info("中断了 {} 个之前的任务，sessionId: {}", cancelledTasks, sessionId);
                
                // 发送中断通知给前端
                ChatMessage interruptNotification = new ChatMessage();
                interruptNotification.setType("system");
                interruptNotification.setContent("AI回复已被中断 (中断了 " + cancelledTasks + " 个任务)");
                interruptNotification.setSessionId(sessionId);
                interruptNotification.setMetadata(Map.of(
                    "interrupt_confirmed", true,
                    "interrupted_tasks", cancelledTasks,
                    "interrupt_type", "new_message"
                ));
                responseCallback.accept(interruptNotification);
            }
        } else {
            logger.debug("会话没有活跃任务，直接处理新消息");
        }
        
        // 提交新任务
        taskManager.submitTask(taskId, () -> {
            
            try {
                // 1. 获取或创建会话
                ChatSession session = sessionService.getOrCreateSession(sessionId);
                logger.debug("会话准备完成，sessionId: {}，消息数: {}", session.getSessionId(), session.getMessageHistory().size());
                
                // 2. 获取系统提示词和人设提示词（使用重构后的 contextBuilder）
                List<ChatMessage> systemPrompts = contextBuilder.getSystemPrompts(session);
                
                // 3. 获取历史对话记录（使用重构后的 contextBuilder）
                List<ChatMessage> dialogueHistory = contextBuilder.getDialogueHistory(session);
                
                // 4. 预处理用户输入（使用重构后的 messageProcessor）
                String processedInput = messageProcessor.preprocessInput(userMessage.getContent());
                
                // 5. 获取世界书设定（使用重构后的 contextBuilder）
                ChatMessage worldBookSetting = contextBuilder.getWorldBookSetting(session, processedInput);
                
                // 6. 智能判断是否需要联网搜索并准备用户消息
                long step6Start = System.currentTimeMillis();
                
                // 检查用户是否启用了联网搜索
                boolean userEnabledWebSearch = getUserWebSearchPreference(sessionId);
                ChatMessage webSearchMessage = null;
                
                if (userEnabledWebSearch) {
                    logger.info("用户启用了联网搜索功能，开始智能判断搜索需求");
                    
                    // 使用AI判断是否需要联网搜索并提取搜索关键词
                    WebSearchDecision searchDecision = intelligentWebSearchDecision(
                        processedInput, dialogueHistory, worldBookSetting, sessionId);
                    
                    if (searchDecision.needsWebSearch()) {
                        logger.info("联网搜索决策: 需要搜索 | 来源: {} | 关键词: '{}' | 原因: {}", 
                                  searchDecision.getSource(), 
                                  searchDecision.getSearchQuery(), 
                                  searchDecision.getReason());
                        webSearchMessage = performWebSearch(searchDecision.getSearchQuery(), sessionId);
                    } else {
                        logger.info("联网搜索决策: 无需搜索 | 来源: {} | 原因: {}", 
                                  searchDecision.getSource(), 
                                  searchDecision.getReason());
                    }
                } else {
                    logger.debug("用户未启用联网搜索功能");
                }
                
                userMessage.setRole("user");
                userMessage.setContent(processedInput); // 使用预处理后的输入
                
                long step6Time = System.currentTimeMillis() - step6Start;
                logger.debug("用户消息准备完成（含智能联网搜索），耗时: {}ms", step6Time);
                
                // 7. 构建完整的消息列表（使用重构后的 contextBuilder）
                logger.debug("步骤7：构建完整的消息列表");
                long step7Start = System.currentTimeMillis();
                List<Message> messages = contextBuilder.buildMessagesListWithTokenLimit(
                    systemPrompts, dialogueHistory, worldBookSetting, webSearchMessage, userMessage);
                long step7Time = System.currentTimeMillis() - step7Start;
                logger.debug("消息列表构建完成，耗时: {}ms，消息数量: {}", step7Time, messages.size());
                
                // 记录预处理完成时间
                long preprocessingTime = System.currentTimeMillis() - messageStartTime;
                logger.debug("预处理完成，sessionId: {}, 耗时: {}ms", sessionId, preprocessingTime);
                
                // 8. 调用AI模型生成回复（流式）
                long aiCallStartTime = System.currentTimeMillis();
                
                // 在任务内部调用流式响应，这样可以立即注册HTTP调用
                generateStreamingResponseInTask(messages, sessionId, taskId, responseCallback, messageStartTime, aiCallStartTime, userMessage);
                
                long totalProcessingTime = System.currentTimeMillis() - messageStartTime;
                logger.debug("消息处理启动完成，sessionId: {}, 耗时: {}ms", sessionId, totalProcessingTime);
                
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
        
        return taskId;
    }
    
    /**
     * 中断指定任务
     */
    public boolean interruptTask(String taskId) {
        logger.info("收到中断任务请求，taskId: {}", taskId);
        return taskManager.cancelTask(taskId);
    }
    
    /**
     * 中断会话的所有任务
     */
    public int interruptSessionTasks(String sessionId) {
        logger.info("收到中断会话任务请求，sessionId: {}", sessionId);
        return taskManager.cancelSessionTasks(sessionId);
    }
    
    // Phase 1 重构：以下方法已被 ChatMessageProcessor 替代
    // - preprocessInput() → messageProcessor.preprocessInput()
    // - mapSenderToRole() → messageProcessor.mapSenderToRole()
    // - filterThinkingContent() → messageProcessor.filterThinkingContent()
    
    
    /**
     * 生成流式回复（使用Ollama）- 优化版
     */
    private void generateStreamingResponse(List<Message> messages, String sessionId, String taskId, Consumer<ChatMessage> callback, 
                                         long messageStartTime, long aiCallStartTime, ChatMessage userMessage) {
        
        // 检查LLM服务是否可用
        if (!llmService.isServiceAvailable()) {
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
        
        // 获取用户配置
        UserPreferences userPrefs = userPreferencesService.getUserPreferences("Taiming");
        
        // 构建 LLMRequest
        String model = (userPrefs != null && userPrefs.getLlm().getModel() != null)
                ? userPrefs.getLlm().getModel()
                : "yi:6b"; // 默认模型
        
        Double temperature = 0.7; // 可以从配置读取
        
        com.chatbot.model.dto.llm.LLMRequest llmRequest = new com.chatbot.model.dto.llm.LLMRequest.Builder()
                .messages(messages)
                .model(model)
                .temperature(temperature)
                .stream(true)
                .build();
        
        // 打印 LLM 请求报文（用于调试）
        try {
            String requestJson = JsonUtil.toJson(llmRequest);
            logger.info("=== LLM 请求 [generateStreamingResponse] ===");
            logger.info("SessionId: {}, TaskId: {}", sessionId, taskId);
            logger.info("请求 JSON:\n{}", requestJson);
            logger.info("==========================================");
        } catch (Exception e) {
            logger.warn("无法序列化 LLM 请求为 JSON: {}", e.getMessage());
        }
        
        // 使用新的统一接口生成流式响应
        Object callObj = llmService.generateStreamWithInterruptCheck(
            llmRequest,
            // 成功处理每个chunk
            chunk -> {
                // 检查任务是否被取消
                if (taskManager.isTaskCancelled(taskId)) {
                    logger.info("任务已被取消，停止处理流式响应，taskId: {}", taskId);
                    return;
                }
                handleStreamChunk(chunk.getContent(), sessionId, taskId, callback, state, messageStartTime, aiCallStartTime);
            },
            // 错误处理
            error -> {
                handleStreamError(error, sessionId, callback, state, userMessage);
            },
            // 完成处理回调 - 在流式响应真正完成时调用
            () -> {
                // 检查任务是否被取消
                if (taskManager.isTaskCancelled(taskId)) {
                    logger.info("任务已被取消，跳过完成处理，taskId: {}", taskId);
                    return;
                }
                
                logger.debug("收到流式响应完成通知，sessionId: {}", sessionId);
                
                // 打印完整的 LLM 响应（用于调试）
                String completeResponse = state.completeResponse.toString();
                logger.info("=== LLM 完整响应 [generateStreamingResponse] ===");
                logger.info("SessionId: {}, TaskId: {}", sessionId, taskId);
                logger.info("响应长度: {} 字符", completeResponse.length());
                logger.info("完整内容:\n{}", completeResponse);
                logger.info("===============================================");
                
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
                    saveCompleteConversation(sessionId, userMessage, state.completeResponse.toString());
                } else {
                    logger.warn("⚠️ 没有AI回答内容需要保存 - sessionId: {}", sessionId);
                }
            },
            // 中断检查器
            () -> taskManager.isTaskCancelled(taskId)
        );
        
        // 注册HTTP调用以便可以取消（强制转换为okhttp3.Call）
        if (callObj instanceof okhttp3.Call) {
            okhttp3.Call ollamaCall = (okhttp3.Call) callObj;
            taskManager.registerHttpCall(taskId, ollamaCall);
        } else {
            logger.warn("返回的Call对象类型不匹配，无法注册HTTP调用，taskId: {}", taskId);
        }
    }
    
    /**
     * 在任务内部生成流式回复，确保HTTP调用被正确注册
     */
    private void generateStreamingResponseInTask(List<Message> messages, String sessionId, String taskId, Consumer<ChatMessage> callback, 
                                               long messageStartTime, long aiCallStartTime, ChatMessage userMessage) {
        
        // 检查LLM服务是否可用
        if (!llmService.isServiceAvailable()) {
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
        
        // 获取用户配置
        UserPreferences userPrefs = userPreferencesService.getUserPreferences("Taiming");
        
        // 构建 LLMRequest
        String model = (userPrefs != null && userPrefs.getLlm().getModel() != null)
                ? userPrefs.getLlm().getModel()
                : "yi:6b"; // 默认模型
        
        Double temperature = 0.7; // 可以从配置读取
        
        com.chatbot.model.dto.llm.LLMRequest llmRequest = new com.chatbot.model.dto.llm.LLMRequest.Builder()
                .messages(messages)
                .model(model)
                .temperature(temperature)
                .stream(true)
                .build();
        
        // 打印 LLM 请求报文（用于调试）
        try {
            String requestJson = JsonUtil.toJson(llmRequest);
            logger.info("=== LLM 请求 [generateStreamingResponseInTask] ===");
            logger.info("SessionId: {}, TaskId: {}", sessionId, taskId);
            logger.info("请求 JSON:\n{}", requestJson);
            logger.info("==================================================");
        } catch (Exception e) {
            logger.warn("无法序列化 LLM 请求为 JSON: {}", e.getMessage());
        }
        
        // 使用新的统一接口生成流式响应
        Object callObj = llmService.generateStreamWithInterruptCheck(
            llmRequest,
            // 成功处理每个chunk
            chunk -> {
                // 检查任务是否被取消
                if (taskManager.isTaskCancelled(taskId)) {
                    logger.info("任务已被取消，停止处理流式响应，taskId: {}", taskId);
                    return;
                }
                handleStreamChunk(chunk.getContent(), sessionId, taskId, callback, state, messageStartTime, aiCallStartTime);
            },
            // 错误处理
            error -> {
                handleStreamError(error, sessionId, callback, state, userMessage);
            },
            // 完成处理回调 - 在流式响应真正完成时调用
            () -> {
                // 检查任务是否被取消
                if (taskManager.isTaskCancelled(taskId)) {
                    logger.info("任务已被取消，跳过完成处理，taskId: {}", taskId);
                    return;
                }
                
                logger.debug("收到流式响应完成通知，sessionId: {}", sessionId);
                
                // 打印完整的 LLM 响应（用于调试）
                String completeResponse = state.completeResponse.toString();
                logger.info("=== LLM 完整响应 [generateStreamingResponseInTask] ===");
                logger.info("SessionId: {}, TaskId: {}", sessionId, taskId);
                logger.info("响应长度: {} 字符", completeResponse.length());
                logger.info("完整内容:\n{}", completeResponse);
                logger.info("======================================================");
                
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
                    saveCompleteConversation(sessionId, userMessage, state.completeResponse.toString());
                } else {
                    logger.warn("⚠️ 没有AI回答内容需要保存 - sessionId: {}", sessionId);
                }
            },
            // 中断检查器
            () -> taskManager.isTaskCancelled(taskId)
        );
        
        // 立即注册HTTP调用以便可以取消（强制转换为okhttp3.Call）
        if (callObj instanceof okhttp3.Call) {
            okhttp3.Call ollamaCall = (okhttp3.Call) callObj;
            taskManager.registerHttpCall(taskId, ollamaCall);
            logger.info("✅ 在任务内部注册HTTP调用: {}", taskId);
        } else {
            logger.warn("❌ 返回的Call对象类型不匹配，无法注册HTTP调用，taskId: {}", taskId);
        }
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
    private void handleStreamChunk(String chunk, String sessionId, String taskId, Consumer<ChatMessage> callback, 
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
     * 获取用户的联网搜索偏好（默认关闭）
     */
    private boolean getUserWebSearchPreference(String sessionId) {
        try {
            ChatSession session = sessionService.getSession(sessionId);
            if (session != null && session.getMetadata() != null) {
                Object useWebSearch = session.getMetadata().get("useWebSearch");
                if (useWebSearch instanceof Boolean) {
                    return (Boolean) useWebSearch;
                }
            }
        } catch (Exception e) {
            logger.debug("获取用户联网搜索偏好失败", e);
        }
        return false; // 默认关闭联网搜索
    }
    
    /**
     * 设置用户的联网搜索偏好
     */
    public void setUserWebSearchPreference(String sessionId, boolean useWebSearch) {
        try {
            ChatSession session = sessionService.getOrCreateSession(sessionId);
            if (session.getMetadata() == null) {
                session.setMetadata(new java.util.HashMap<>());
            }
            session.getMetadata().put("useWebSearch", useWebSearch);
            logger.info("设置用户联网搜索偏好 - sessionId: {}, useWebSearch: {}", sessionId, useWebSearch);
        } catch (Exception e) {
            logger.error("设置用户联网搜索偏好失败", e);
        }
    }
    
    /**
     * 联网搜索决策结果类
     */
    private static class WebSearchDecision {
        private final boolean needsWebSearch;
        private final String searchQuery;
        private final String reason;
        private final boolean isTimeout; // 标记是否由于超时导致的判断
        private final DecisionSource source; // 判断来源
        
        // 判断来源枚举
        public enum DecisionSource {
            AI_DECISION,    // AI正常返回的判断
            TIMEOUT_FALLBACK, // 超时后的备选策略
            ERROR_FALLBACK   // 错误后的备选策略
        }
        
        public WebSearchDecision(boolean needsWebSearch, String searchQuery, String reason) {
            this(needsWebSearch, searchQuery, reason, false, DecisionSource.AI_DECISION);
        }
        
        public WebSearchDecision(boolean needsWebSearch, String searchQuery, String reason, 
                               boolean isTimeout, DecisionSource source) {
            this.needsWebSearch = needsWebSearch;
            this.searchQuery = searchQuery;
            this.reason = reason;
            this.isTimeout = isTimeout;
            this.source = source;
        }
        
        // 创建超时备选决策的静态方法
        public static WebSearchDecision createTimeoutFallback(boolean enableTimeoutFallback) {
            return new WebSearchDecision(
                !enableTimeoutFallback, // 如果启用超时备选，则不搜索；否则搜索
                "",
                "AI判断超时，采用" + (enableTimeoutFallback ? "保守策略（不搜索）" : "积极策略（搜索）"),
                true,
                DecisionSource.TIMEOUT_FALLBACK
            );
        }
        
        // 创建错误备选决策的静态方法
        public static WebSearchDecision createErrorFallback() {
            return new WebSearchDecision(
                false,
                "",
                "AI判断过程发生异常，采用保守策略（不搜索）",
                false,
                DecisionSource.ERROR_FALLBACK
            );
        }
        
        public boolean needsWebSearch() { return needsWebSearch; }
        public String getSearchQuery() { return searchQuery; }
        public String getReason() { return reason; }
        public boolean isTimeout() { return isTimeout; }
        public DecisionSource getSource() { return source; }
        
        // 判断是否为正常AI决策
        public boolean isNormalAIDecision() {
            return source == DecisionSource.AI_DECISION;
        }
    }
    
    /**
     * 智能判断是否需要联网搜索并提取搜索关键词
     */
    private WebSearchDecision intelligentWebSearchDecision(
            String userInput, 
            List<ChatMessage> dialogueHistory, 
            ChatMessage worldBookSetting, 
            String sessionId) {
        
        try {
            logger.debug("开始AI智能判断联网搜索需求");
            
            // 构建判断提示词
            String decisionPrompt = buildWebSearchDecisionPrompt(userInput, dialogueHistory, worldBookSetting);
            
            // 调用AI进行判断
            List<Message> decisionMessages = List.of(
                Message.system(decisionPrompt),
                Message.user(userInput)
            );
            
            // 使用同步方式获取AI判断结果
            AIDecisionResult aiResult = getAIDecisionSync(decisionMessages, sessionId);
            
            // 处理不同的结果情况
            if (aiResult.hasError()) {
                logger.error("AI判断过程发生错误");
                return WebSearchDecision.createErrorFallback();
            } else if (aiResult.isTimeout()) {
                logger.warn("AI判断超时，采用备选策略");
                boolean enableTimeoutFallback = aiConfig.getWebSearchDecision().isEnableTimeoutFallback();
                return WebSearchDecision.createTimeoutFallback(enableTimeoutFallback);
            } else {
                // 解析AI的正常判断结果
                return parseWebSearchDecision(aiResult.getResponse(), userInput);
            }
            
        } catch (Exception e) {
            logger.error("AI智能判断联网搜索需求失败", e);
            // 发生异常时，采用保守策略：不搜索
            return WebSearchDecision.createErrorFallback();
        }
    }
    
    /**
     * 构建联网搜索判断的提示词
     */
    private String buildWebSearchDecisionPrompt(
            String userInput, 
            List<ChatMessage> dialogueHistory, 
            ChatMessage worldBookSetting) {
        
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("你是一个智能搜索决策助手。请根据用户的问题和现有信息，判断是否需要进行联网搜索来获取更多信息。\n\n");
        
        prompt.append("判断标准：\n");
        prompt.append("1. 需要联网搜索的情况：\n");
        prompt.append("   - 询问具体的概念、术语、人物、事件、地点等百科知识\n");
        prompt.append("   - 需要权威、准确的定义或解释\n");
        prompt.append("   - 询问历史事件、科学原理、技术概念等\n");
        prompt.append("   - 现有对话历史和世界书中没有相关信息\n\n");
        
        prompt.append("2. 不需要联网搜索的情况：\n");
        prompt.append("   - 纯聊天、问候、情感交流\n");
        prompt.append("   - 询问个人观点、建议、推荐\n");
        prompt.append("   - 数学计算、逻辑推理等可以直接回答的问题\n");
        prompt.append("   - 现有信息已经足够回答的问题\n");
        prompt.append("   - 询问操作方法、使用技巧等实用性问题\n\n");
        
        // 添加现有信息上下文
        if (dialogueHistory != null && !dialogueHistory.isEmpty()) {
            prompt.append("对话历史摘要：\n");
            int historyCount = Math.min(3, dialogueHistory.size());
            for (int i = dialogueHistory.size() - historyCount; i < dialogueHistory.size(); i++) {
                ChatMessage msg = dialogueHistory.get(i);
                prompt.append("- ").append(msg.getRole()).append(": ").append(
                    msg.getContent().length() > 100 ? msg.getContent().substring(0, 100) + "..." : msg.getContent()
                ).append("\n");
            }
            prompt.append("\n");
        }
        
        if (worldBookSetting != null && worldBookSetting.getContent() != null && !worldBookSetting.getContent().trim().isEmpty()) {
            prompt.append("世界书相关信息：\n");
            String worldBookContent = worldBookSetting.getContent();
            prompt.append(worldBookContent.length() > 200 ? worldBookContent.substring(0, 200) + "..." : worldBookContent);
            prompt.append("\n\n");
        }
        
        prompt.append("请按照以下格式回复：\n");
        prompt.append("判断：需要搜索 / 不需要搜索\n");
        prompt.append("关键词：[如果需要搜索，提取1-3个最核心的搜索关键词，用逗号分隔]\n");
        prompt.append("原因：[简要说明判断理由]\n\n");
        
        prompt.append("注意：\n");
        prompt.append("- 搜索关键词应该是名词或名词短语，适合在维基百科中查找\n");
        prompt.append("- 移除疑问词、语气词，只保留核心概念\n");
        prompt.append("- 优先选择更通用、更可能有百科条目的词汇\n");
        
        return prompt.toString();
    }
    
    /**
     * 同步获取AI判断结果
     * 返回结果包装类，包含响应内容和是否超时信息
     */
    private static class AIDecisionResult {
        private final String response;
        private final boolean isTimeout;
        private final boolean hasError;
        
        public AIDecisionResult(String response, boolean isTimeout, boolean hasError) {
            this.response = response;
            this.isTimeout = isTimeout;
            this.hasError = hasError;
        }
        
        public String getResponse() { return response; }
        public boolean isTimeout() { return isTimeout; }
        public boolean hasError() { return hasError; }
    }
    
    /**
     * 同步获取AI判断结果
     */
    private AIDecisionResult getAIDecisionSync(List<Message> messages, String sessionId) {
        StringBuilder result = new StringBuilder();
        
        try {
            // 获取配置的超时时间
            long timeoutMillis = aiConfig.getWebSearchDecision().getTimeoutMillis();
            logger.debug("AI判断超时设置: {}毫秒", timeoutMillis);
            
            // 使用一个简单的同步机制来获取AI响应
            final Object lock = new Object();
            final boolean[] completed = {false};
            final boolean[] hasError = {false};
            
            // 获取用户配置
            UserPreferences userPrefs = userPreferencesService.getUserPreferences("Taiming");
            
            // 构建 LLMRequest
            String model = (userPrefs != null && userPrefs.getLlm().getModel() != null)
                    ? userPrefs.getLlm().getModel()
                    : "yi:6b"; // 默认模型
            
            Double temperature = 0.7; // 可以从配置读取
            
            com.chatbot.model.dto.llm.LLMRequest llmRequest = new com.chatbot.model.dto.llm.LLMRequest.Builder()
                    .messages(messages)
                    .model(model)
                    .temperature(temperature)
                    .stream(true)
                    .build();
            
            // 打印 LLM 请求报文（网络搜索决策）
            try {
                String requestJson = JsonUtil.toJson(llmRequest);
                logger.info("=== LLM 请求 [WebSearch Decision] ===");
                logger.info("SessionId: {}, 超时: {}ms", sessionId, timeoutMillis);
                logger.info("请求 JSON:\n{}", requestJson);
                logger.info("=====================================");
            } catch (Exception e) {
                logger.warn("无法序列化 LLM 请求为 JSON: {}", e.getMessage());
            }
            
            // 使用新的统一接口
            llmService.generateStream(
                llmRequest,
                // 成功处理每个chunk
                chunk -> {
                    result.append(chunk.getContent());
                },
                // 错误处理
                error -> {
                    logger.error("AI判断请求失败", error);
                    synchronized (lock) {
                        hasError[0] = true;
                        completed[0] = true;
                        lock.notify();
                    }
                },
                // 完成处理
                () -> {
                    synchronized (lock) {
                        completed[0] = true;
                        lock.notify();
                    }
                }
            );
            
            // 等待响应完成，使用配置的超时时间
            synchronized (lock) {
                if (!completed[0]) {
                    lock.wait(timeoutMillis);
                }
            }
            
            boolean isTimeout = !completed[0];
            if (isTimeout) {
                logger.warn("AI判断请求超时，超时时间: {}毫秒", timeoutMillis);
            }
            
            // 打印完整的 AI 决策响应（用于调试）
            String aiResponse = result.toString();
            if (!hasError[0] && !isTimeout && !aiResponse.isEmpty()) {
                logger.info("=== LLM 完整响应 [WebSearch Decision] ===");
                logger.info("SessionId: {}", sessionId);
                logger.info("响应长度: {} 字符", aiResponse.length());
                logger.info("完整内容:\n{}", aiResponse);
                logger.info("==========================================");
            }
            
            return new AIDecisionResult(result.toString(), isTimeout, hasError[0]);
            
        } catch (Exception e) {
            logger.error("同步获取AI判断结果失败", e);
            return new AIDecisionResult("", false, true);
        }
    }
    
    /**
     * 解析AI的联网搜索判断结果
     */
    private WebSearchDecision parseWebSearchDecision(String aiResponse, String originalQuery) {
        if (aiResponse == null || aiResponse.trim().isEmpty()) {
            return new WebSearchDecision(false, "", "AI响应为空");
        }
        
        try {
            String response = aiResponse.toLowerCase();
            boolean needsSearch = response.contains("需要搜索") && !response.contains("不需要搜索");
            
            String searchQuery = "";
            String reason = "基于AI判断";
            
            // 提取关键词
            if (needsSearch) {
                searchQuery = extractSearchKeywords(aiResponse, originalQuery);
            }
            
            // 提取原因
            if (aiResponse.contains("原因：")) {
                int reasonStart = aiResponse.indexOf("原因：") + 3;
                int reasonEnd = aiResponse.indexOf("\n", reasonStart);
                if (reasonEnd == -1) reasonEnd = aiResponse.length();
                if (reasonStart < aiResponse.length()) {
                    reason = aiResponse.substring(reasonStart, reasonEnd).trim();
                }
            }
            
            logger.debug("AI判断结果解析：需要搜索={}, 关键词='{}', 原因='{}'", needsSearch, searchQuery, reason);
            return new WebSearchDecision(needsSearch, searchQuery, reason);
            
        } catch (Exception e) {
            logger.error("解析AI判断结果失败", e);
            return new WebSearchDecision(false, "", "解析AI判断结果失败");
        }
    }
    
    /**
     * 从AI响应中提取搜索关键词
     */
    private String extractSearchKeywords(String aiResponse, String originalQuery) {
        // 尝试从AI响应中提取关键词
        if (aiResponse.contains("关键词：")) {
            int keywordStart = aiResponse.indexOf("关键词：") + 4;
            int keywordEnd = aiResponse.indexOf("\n", keywordStart);
            if (keywordEnd == -1) keywordEnd = aiResponse.length();
            
            if (keywordStart < aiResponse.length()) {
                String keywords = aiResponse.substring(keywordStart, keywordEnd).trim();
                // 移除方括号
                keywords = keywords.replaceAll("[\\[\\]]", "").trim();
                if (!keywords.isEmpty() && !keywords.equals("无")) {
                    return keywords;
                }
            }
        }
        
        // 如果AI没有提供关键词，使用原始查询的简化版本
        return simplifyQuery(originalQuery);
    }
    
    /**
     * 简化查询，提取核心关键词
     */
    private String simplifyQuery(String query) {
        if (query == null || query.trim().isEmpty()) {
            return query;
        }
        
        String processed = query.trim();
        
        // 移除常见的疑问词和语气词
        String[] questionWords = {
            "你知道", "你了解", "什么是", "是什么", "吗？", "呢？", "吗", "呢", "？", "?",
            "请问", "能告诉我", "我想知道", "帮我查一下", "搜索一下", "查找",
            "的信息", "相关信息", "的内容", "有关", "关于", "怎么", "如何", "为什么"
        };
        
        for (String word : questionWords) {
            processed = processed.replace(word, "");
        }
        
        // 移除多余的空格
        processed = processed.replaceAll("\\s+", " ").trim();
        
        // 如果处理后为空，返回原查询
        if (processed.isEmpty()) {
            return query;
        }
        
        return processed;
    }
    
    /**
     * 执行联网搜索
     */
    private ChatMessage performWebSearch(String query, String sessionId) {
        try {
            logger.info("开始执行联网搜索 - sessionId: {}, query: '{}'", sessionId, query);
            
            // 检查搜索服务是否可用
            if (!webSearchService.isSearchAvailable()) {
                logger.warn("联网搜索服务不可用 - sessionId: {}", sessionId);
                return createWebSearchUnavailableMessage(sessionId);
            }
            
            // 执行搜索
            var searchResults = webSearchService.search(query);
            
            if (searchResults.isEmpty()) {
                logger.info("联网搜索无结果 - sessionId: {}, query: '{}'", sessionId, query);
                return createNoSearchResultsMessage(sessionId, query);
            }
            
            // 格式化搜索结果
            String formattedResults = webSearchService.formatSearchResults(searchResults);
            
            // 创建搜索结果消息
            ChatMessage webSearchMessage = new ChatMessage();
            webSearchMessage.setRole("system");
            webSearchMessage.setContent(formattedResults);
            webSearchMessage.setSessionId(sessionId);
            webSearchMessage.setType("text");
            
            logger.info("联网搜索完成 - sessionId: {}, 找到{}个结果", sessionId, searchResults.size());
            return webSearchMessage;
            
        } catch (Exception e) {
            logger.error("执行联网搜索时发生错误 - sessionId: {}, query: '{}'", sessionId, query, e);
            return createWebSearchErrorMessage(sessionId, e.getMessage());
        }
    }
    
    /**
     * 创建搜索服务不可用消息
     */
    private ChatMessage createWebSearchUnavailableMessage(String sessionId) {
        ChatMessage message = new ChatMessage();
        message.setRole("system");
        message.setContent("联网搜索服务暂时不可用，请基于已有知识回答用户问题。");
        message.setSessionId(sessionId);
        message.setType("text");
        return message;
    }
    
    /**
     * 创建无搜索结果消息
     */
    private ChatMessage createNoSearchResultsMessage(String sessionId, String query) {
        ChatMessage message = new ChatMessage();
        message.setRole("system");
        message.setContent("联网搜索未找到相关结果（搜索关键词：" + query + "），请基于已有知识回答用户问题。");
        message.setSessionId(sessionId);
        message.setType("text");
        return message;
    }
    
    /**
     * 创建搜索错误消息
     */
    private ChatMessage createWebSearchErrorMessage(String sessionId, String errorMessage) {
        ChatMessage message = new ChatMessage();
        message.setRole("system");
        message.setContent("联网搜索时发生错误（" + errorMessage + "），请基于已有知识回答用户问题。");
        message.setSessionId(sessionId);
        message.setType("text");
        return message;
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
            
            // 过滤AI回答中的思考内容（使用重构后的 messageProcessor）
            String filteredResponse = messageProcessor.filterThinkingContent(aiResponse);
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
                chatHistoryService.addMessage(sessionId, userMessage);
                chatHistoryService.addMessageAndSave(sessionId, userMessage);
                
                // 2. 再保存AI回答
                logger.debug("💾 保存AI回答 - sessionId: {}, 内容长度: {}", 
                           sessionId, aiMessage.getContent().length());
                session.addMessage(aiMessage);
                chatHistoryService.addMessage(sessionId, aiMessage);
                chatHistoryService.addMessageAndSave(sessionId, aiMessage);
                
                // 3. 使用 KnowledgeService 统一更新知识库（包括短期记忆和长期知识）
                knowledgeService.updateKnowledge(sessionId, userMessage.getContent());
                
                logger.info("💾 对话保存完成 - sessionId: {}, 用户消息、AI回答和知识库已更新", sessionId);
            }
        } catch (Exception e) {
            logger.error("保存完整对话时发生错误", e);
        }
    }
    
    // Phase 1 重构：以下方法已被 ChatContextBuilder 替代
    // - getSystemPrompts() → contextBuilder.getSystemPrompts()
    // - getDialogueHistory() → contextBuilder.getDialogueHistory()
    // - getWorldBookSetting() → contextBuilder.getWorldBookSetting()
    // - retrieveRelevantWorldBook() → 内部由 contextBuilder 调用
    
    // Phase 1 重构：以下方法已被 ChatContextBuilder 替代
    // - buildMessagesListWithTokenLimit() → contextBuilder.buildMessagesListWithTokenLimit()
    // - estimateTokens() → contextBuilder 内部使用
    // - filterDialogueHistoryByTokens() → contextBuilder 内部使用
    // - getMaxTokenLimit() → contextBuilder 内部配置
    // - getTokensPerCharEstimate() → contextBuilder 内部配置
    
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
                    chatHistoryService.saveSessionHistory(sessionId, allMessages);
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
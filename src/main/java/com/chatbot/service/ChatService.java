package com.chatbot.service;

import com.chatbot.config.AppConfig;
import com.chatbot.model.domain.ChatMessage;
import com.chatbot.model.domain.ChatSession;
import com.chatbot.model.dto.llm.Message;
import com.chatbot.model.config.UserPreferences;
import com.chatbot.service.chat.ChatContextBuilder;
import com.chatbot.service.chat.ChatMessageProcessor;
import com.chatbot.service.llm.impl.OllamaLLMServiceImpl;
import com.chatbot.service.search.WebSearchDecisionService;
import com.chatbot.service.search.WebSearchDecisionService.WebSearchDecision;
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
    private final WebSearchDecisionService webSearchDecisionService;  // Phase 2：联网搜索决策
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
                      WebSearchDecisionService webSearchDecisionService,  // Phase 2：联网搜索决策
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
        this.webSearchDecisionService = webSearchDecisionService;
        this.taskManager = taskManager;
        this.userPreferencesService = userPreferencesService;
        this.messageProcessor = messageProcessor;
        this.contextBuilder = contextBuilder;
        
        logger.info("ChatService 初始化完成 - Phase 2 优化完成（统一知识管理 + 联网搜索决策）");
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
                    
                    // 使用 WebSearchDecisionService 判断是否需要联网搜索并提取搜索关键词
                    WebSearchDecision searchDecision = webSearchDecisionService.makeDecision(
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
    
    // Phase 2 重构：联网搜索决策逻辑已迁移到 WebSearchDecisionService
    // - intelligentWebSearchDecision() → webSearchDecisionService.makeDecision()
    // - buildWebSearchDecisionPrompt() → WebSearchDecisionService 内部
    // - getAIDecisionSync() → WebSearchDecisionService 内部
    // - parseWebSearchDecision() → WebSearchDecisionService 内部
    // - extractSearchKeywords() → WebSearchDecisionService 内部
    // - simplifyQuery() → WebSearchDecisionService 内部
    // - WebSearchDecision 类 → WebSearchDecisionService.WebSearchDecision
    // - AIDecisionResult 类 → WebSearchDecisionService 内部
    
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
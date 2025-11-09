package com.chatbot.service.search;

import com.chatbot.config.AppConfig;
import com.chatbot.model.domain.ChatMessage;
import com.chatbot.model.dto.llm.LLMRequest;
import com.chatbot.model.dto.llm.Message;
import com.chatbot.model.config.UserPreferences;
import com.chatbot.service.UserPreferencesService;
import com.chatbot.service.llm.impl.OllamaLLMServiceImpl;
import com.chatbot.util.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 联网搜索决策服务
 * 负责智能判断是否需要进行联网搜索，以及提取搜索关键词
 */
@Service
public class WebSearchDecisionService {
    
    private static final Logger logger = LoggerFactory.getLogger(WebSearchDecisionService.class);
    
    private final OllamaLLMServiceImpl llmService;
    private final UserPreferencesService userPreferencesService;
    private final AppConfig.AIConfig aiConfig;
    
    public WebSearchDecisionService(OllamaLLMServiceImpl llmService,
                                   UserPreferencesService userPreferencesService,
                                   AppConfig appConfig) {
        this.llmService = llmService;
        this.userPreferencesService = userPreferencesService;
        this.aiConfig = appConfig.getAi();
        
        logger.info("WebSearchDecisionService 初始化完成");
    }
    
    /**
     * 智能判断是否需要联网搜索并提取搜索关键词
     * 
     * @param userInput 用户输入
     * @param dialogueHistory 对话历史
     * @param worldBookSetting 世界书设定
     * @param sessionId 会话ID
     * @return 搜索决策结果
     */
    public WebSearchDecision makeDecision(
            String userInput, 
            List<ChatMessage> dialogueHistory, 
            ChatMessage worldBookSetting, 
            String sessionId) {
        
        try {
            logger.debug("开始AI智能判断联网搜索需求 - sessionId: {}", sessionId);
            
            // 构建判断提示词
            String decisionPrompt = buildDecisionPrompt(userInput, dialogueHistory, worldBookSetting);
            
            // 调用AI进行判断
            List<Message> decisionMessages = List.of(
                Message.system(decisionPrompt),
                Message.user(userInput)
            );
            
            // 使用同步方式获取AI判断结果
            AIDecisionResult aiResult = getAIDecisionSync(decisionMessages, sessionId);
            
            // 处理不同的结果情况
            if (aiResult.hasError()) {
                logger.error("AI判断过程发生错误 - sessionId: {}", sessionId);
                return WebSearchDecision.createErrorFallback();
            } else if (aiResult.isTimeout()) {
                logger.warn("AI判断超时，采用备选策略 - sessionId: {}", sessionId);
                boolean enableTimeoutFallback = aiConfig.getWebSearchDecision().isEnableTimeoutFallback();
                return WebSearchDecision.createTimeoutFallback(enableTimeoutFallback);
            } else {
                // 解析AI的正常判断结果
                return parseDecisionResult(aiResult.getResponse(), userInput);
            }
            
        } catch (Exception e) {
            logger.error("AI智能判断联网搜索需求失败 - sessionId: {}", sessionId, e);
            // 发生异常时，采用保守策略：不搜索
            return WebSearchDecision.createErrorFallback();
        }
    }
    
    /**
     * 构建联网搜索判断的提示词
     */
    private String buildDecisionPrompt(
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
     */
    private AIDecisionResult getAIDecisionSync(List<Message> messages, String sessionId) {
        StringBuilder result = new StringBuilder();
        
        try {
            // 获取配置的超时时间
            long timeoutMillis = aiConfig.getWebSearchDecision().getTimeoutMillis();
            logger.debug("AI判断超时设置: {}毫秒 - sessionId: {}", timeoutMillis, sessionId);
            
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
            
            LLMRequest llmRequest = new LLMRequest.Builder()
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
                    logger.error("AI判断请求失败 - sessionId: {}", sessionId, error);
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
                logger.warn("AI判断请求超时 - sessionId: {}, 超时时间: {}毫秒", sessionId, timeoutMillis);
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
            logger.error("同步获取AI判断结果失败 - sessionId: {}", sessionId, e);
            return new AIDecisionResult("", false, true);
        }
    }
    
    /**
     * 解析AI的联网搜索判断结果
     */
    private WebSearchDecision parseDecisionResult(String aiResponse, String originalQuery) {
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
    
    // ==================== 内部类 ====================
    
    /**
     * AI决策结果包装类
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
     * 联网搜索决策结果类
     */
    public static class WebSearchDecision {
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
        
        @Override
        public String toString() {
            return String.format("WebSearchDecision{needsSearch=%s, query='%s', reason='%s', source=%s}",
                               needsWebSearch, searchQuery, reason, source);
        }
    }
}


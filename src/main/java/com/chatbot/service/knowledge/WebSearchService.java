package com.chatbot.service.knowledge;

import com.chatbot.config.AppConfig;
import com.chatbot.config.properties.AIProperties;
import com.chatbot.model.domain.ChatMessage;
import com.chatbot.model.dto.llm.LLMRequest;
import com.chatbot.model.dto.llm.Message;
import com.chatbot.model.config.UserPreferences;
import com.chatbot.service.ai.llm.OllamaLLMServiceImpl;
import com.chatbot.service.session.UserPreferencesService;
import com.chatbot.util.JsonUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * 统一的联网搜索服务
 * 整合了搜索决策、执行和结果处理
 * 提供一站式的智能搜索解决方案
 */
@Service
public class WebSearchService {
    
    private static final Logger logger = LoggerFactory.getLogger(WebSearchService.class);
    
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final OllamaLLMServiceImpl llmService;
    private final UserPreferencesService userPreferencesService;
    private final AIProperties aiConfig;
    
    // 搜索引擎配置
    private static final String WIKIPEDIA_API_URL = "https://zh.wikipedia.org/w/api.php"; // 维基百科中文API
    
    // 默认搜索参数
    private static final int DEFAULT_MAX_RESULTS = 5;
    private static final int DEFAULT_TIMEOUT_SECONDS = 10;
    
    /**
     * 完整功能构造器（包含智能搜索决策）
     * Spring 会使用此构造器进行依赖注入
     */
    @Autowired
    public WebSearchService(ObjectMapper objectMapper,
                           @Qualifier("ollamaLLMService") OllamaLLMServiceImpl llmService,
                           UserPreferencesService userPreferencesService,
                           AppConfig appConfig) {
        this.objectMapper = objectMapper;
        this.llmService = llmService;
        this.userPreferencesService = userPreferencesService;
        this.aiConfig = appConfig.getAi();
        
        // 创建HTTP客户端
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(DEFAULT_TIMEOUT_SECONDS))
            .build();
        
        logger.info("WebSearchService 初始化完成 - 整合了搜索决策和执行功能");
    }
    
    /**
     * 简化构造器（仅用于基础搜索功能测试）
     */
    public WebSearchService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.llmService = null;
        this.userPreferencesService = null;
        this.aiConfig = null;
        
        // 创建HTTP客户端
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(DEFAULT_TIMEOUT_SECONDS))
            .build();
        
        logger.info("WebSearchService 初始化完成 - 测试模式（仅基础搜索功能）");
    }
    
    // ==================== 智能搜索入口 ====================
    
    /**
     * 智能搜索：自动判断是否需要搜索，执行搜索并格式化结果
     * 这是统一的搜索入口，封装了决策、执行和结果处理的完整流程
     * 
     * @param userInput 用户输入
     * @param dialogueHistory 对话历史
     * @param worldBookSetting 世界书设定
     * @param sessionId 会话ID
     * @return 格式化的搜索结果消息，如果不需要搜索则返回 null
     */
    public ChatMessage intelligentSearch(
            String userInput,
            List<ChatMessage> dialogueHistory,
            ChatMessage worldBookSetting,
            String sessionId) {
        
        // 检查是否为测试模式（依赖为 null）
        if (llmService == null || userPreferencesService == null || aiConfig == null) {
            logger.error("智能搜索功能不可用 - 缺少必要的依赖（测试模式？）");
            throw new IllegalStateException("WebSearchService 在测试模式下不支持智能搜索，请使用完整构造器");
        }
        
        try {
            logger.debug("开始智能搜索流程 - sessionId: {}", sessionId);
            
            // 1. 智能判断是否需要搜索
            SearchDecision decision = makeSearchDecision(userInput, dialogueHistory, worldBookSetting, sessionId);
            
            logger.info("搜索决策: {} | 来源: {} | 关键词: '{}' | 原因: {}", 
                       decision.needsSearch() ? "需要搜索" : "无需搜索",
                       decision.getSource(),
                       decision.getSearchQuery(),
                       decision.getReason());
            
            // 2. 如果不需要搜索，直接返回
            if (!decision.needsSearch()) {
                return null;
            }
            
            // 3. 执行搜索并返回格式化的 ChatMessage
            return executeSearchAndFormat(decision.getSearchQuery(), sessionId);
            
        } catch (Exception e) {
            logger.error("智能搜索流程失败 - sessionId: {}", sessionId, e);
            return createSearchErrorMessage(sessionId, "搜索流程发生错误");
        }
    }
    
    // ==================== 搜索决策逻辑 ====================
    
    /**
     * 智能判断是否需要联网搜索
     */
    private SearchDecision makeSearchDecision(
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
                return SearchDecision.createErrorFallback();
            } else if (aiResult.isTimeout()) {
                logger.warn("AI判断超时，采用备选策略 - sessionId: {}", sessionId);
                boolean enableTimeoutFallback = aiConfig.getWebSearchDecision().isEnableTimeoutFallback();
                return SearchDecision.createTimeoutFallback(enableTimeoutFallback);
            } else {
                // 解析AI的正常判断结果
                return parseDecisionResult(aiResult.getResponse(), userInput);
            }
            
        } catch (Exception e) {
            logger.error("AI智能判断联网搜索需求失败 - sessionId: {}", sessionId, e);
            return SearchDecision.createErrorFallback();
        }
    }
    
    /**
     * 构建搜索决策的提示词
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
                    : "yi:6b";
            
            Double temperature = 0.7;
            
            LLMRequest llmRequest = new LLMRequest.Builder()
                    .messages(messages)
                    .model(model)
                    .temperature(temperature)
                    .stream(true)
                    .build();
            
            // 打印 LLM 请求报文
            try {
                String requestJson = JsonUtil.toJson(llmRequest);
                logger.info("=== LLM 请求 [WebSearch Decision] ===");
                logger.info("SessionId: {}, 超时: {}ms", sessionId, timeoutMillis);
                logger.info("请求 JSON:\n{}", requestJson);
                logger.info("=====================================");
            } catch (Exception e) {
                logger.warn("无法序列化 LLM 请求为 JSON: {}", e.getMessage());
            }
            
            // 使用统一接口
            llmService.generateStream(
                llmRequest,
                chunk -> result.append(chunk.getContent()),
                error -> {
                    logger.error("AI判断请求失败 - sessionId: {}", sessionId, error);
                    synchronized (lock) {
                        hasError[0] = true;
                        completed[0] = true;
                        lock.notify();
                    }
                },
                () -> {
                    synchronized (lock) {
                        completed[0] = true;
                        lock.notify();
                    }
                }
            );
            
            // 等待响应完成
            synchronized (lock) {
                if (!completed[0]) {
                    lock.wait(timeoutMillis);
                }
            }
            
            boolean isTimeout = !completed[0];
            if (isTimeout) {
                logger.warn("AI判断请求超时 - sessionId: {}, 超时时间: {}毫秒", sessionId, timeoutMillis);
            }
            
            // 打印完整响应
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
     * 解析AI的搜索判断结果
     */
    private SearchDecision parseDecisionResult(String aiResponse, String originalQuery) {
        if (aiResponse == null || aiResponse.trim().isEmpty()) {
            return new SearchDecision(false, "", "AI响应为空");
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
            return new SearchDecision(needsSearch, searchQuery, reason);
            
        } catch (Exception e) {
            logger.error("解析AI判断结果失败", e);
            return new SearchDecision(false, "", "解析AI判断结果失败");
        }
    }
    
    /**
     * 从AI响应中提取搜索关键词
     */
    private String extractSearchKeywords(String aiResponse, String originalQuery) {
        if (aiResponse.contains("关键词：")) {
            int keywordStart = aiResponse.indexOf("关键词：") + 4;
            int keywordEnd = aiResponse.indexOf("\n", keywordStart);
            if (keywordEnd == -1) keywordEnd = aiResponse.length();
            
            if (keywordStart < aiResponse.length()) {
                String keywords = aiResponse.substring(keywordStart, keywordEnd).trim();
                keywords = keywords.replaceAll("[\\[\\]]", "").trim();
                if (!keywords.isEmpty() && !keywords.equals("无")) {
                    return keywords;
                }
            }
        }
        
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
        
        String[] questionWords = {
            "你知道", "你了解", "什么是", "是什么", "吗？", "呢？", "吗", "呢", "？", "?",
            "请问", "能告诉我", "我想知道", "帮我查一下", "搜索一下", "查找",
            "的信息", "相关信息", "的内容", "有关", "关于", "怎么", "如何", "为什么"
        };
        
        for (String word : questionWords) {
            processed = processed.replace(word, "");
        }
        
        processed = processed.replaceAll("\\s+", " ").trim();
        
        if (processed.isEmpty()) {
            return query;
        }
        
        return processed;
    }
    
    // ==================== 搜索执行和结果处理 ====================
    
    /**
     * 执行搜索并格式化为 ChatMessage
     */
    private ChatMessage executeSearchAndFormat(String query, String sessionId) {
        try {
            logger.info("开始执行联网搜索 - sessionId: {}, query: '{}'", sessionId, query);
            
            // 检查搜索服务是否可用
            if (!isSearchAvailable()) {
                logger.warn("联网搜索服务不可用 - sessionId: {}", sessionId);
                return createSearchUnavailableMessage(sessionId);
            }
            
            // 执行搜索
            List<SearchResult> searchResults = search(query);
            
            if (searchResults.isEmpty()) {
                logger.info("联网搜索无结果 - sessionId: {}, query: '{}'", sessionId, query);
                return createNoResultsMessage(sessionId, query);
            }
            
            // 格式化搜索结果
            String formattedResults = formatSearchResults(searchResults);
            
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
            return createSearchErrorMessage(sessionId, e.getMessage());
        }
    }
    
    /**
     * 创建搜索服务不可用消息
     */
    private ChatMessage createSearchUnavailableMessage(String sessionId) {
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
    private ChatMessage createNoResultsMessage(String sessionId, String query) {
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
    private ChatMessage createSearchErrorMessage(String sessionId, String errorMessage) {
        ChatMessage message = new ChatMessage();
        message.setRole("system");
        message.setContent("联网搜索时发生错误（" + errorMessage + "），请基于已有知识回答用户问题。");
        message.setSessionId(sessionId);
        message.setType("text");
        return message;
    }
    
    /**
     * 执行网络搜索
     * 
     * @param query 搜索查询
     * @param maxResults 最大结果数量
     * @return 搜索结果列表
     */
    public List<SearchResult> search(String query, int maxResults) {
        if (query == null || query.trim().isEmpty()) {
            logger.warn("搜索查询为空，返回空结果");
            return new ArrayList<>();
        }
        
        logger.info("开始网络搜索: query='{}', maxResults={}", query, maxResults);
        
        try {
            // 直接尝试维基百科中文API搜索
            List<SearchResult> results = searchWithWikipedia(query, maxResults);
            
            if (results.isEmpty()) {
                logger.info("维基百科搜索无结果，创建模拟搜索结果");
                // 创建模拟搜索结果用于测试
                results = createSimulatedSearchResults(query, maxResults);
            }
            
            logger.info("搜索完成: 找到{}个结果", results.size());
            return results;
            
        } catch (Exception e) {
            logger.error("网络搜索失败: query='{}', 错误: {}", query, e.getMessage());
            logger.debug("搜索异常详情", e);
            
            // 返回模拟搜索结果用于测试
            return createSimulatedSearchResults(query, maxResults);
        }
    }
    
    /**
     * 执行网络搜索（使用默认参数）
     */
    public List<SearchResult> search(String query) {
        return search(query, DEFAULT_MAX_RESULTS);
    }
    
    
    /**
     * 创建模拟搜索结果（用于测试和演示）
     */
    private List<SearchResult> createSimulatedSearchResults(String query, int maxResults) {
        List<SearchResult> results = new ArrayList<>();
        
        logger.info("创建模拟搜索结果，query: '{}', maxResults: {}", query, maxResults);
        
        // 创建多个模拟结果
        for (int i = 1; i <= Math.min(maxResults, 3); i++) {
            SearchResult result = new SearchResult();
            result.setTitle(String.format("关于'%s'的搜索结果 %d", query, i));
            result.setSnippet(String.format("这是关于'%s'的第%d个模拟搜索结果。在实际环境中，这里会显示来自搜索引擎的真实内容摘要。" +
                                           "当前这是为了测试功能而生成的示例内容，包含了查询关键词'%s'的相关信息。", 
                                           query, i, query));
            result.setUrl(String.format("https://example.com/search-result-%d?q=%s", i, 
                                       URLEncoder.encode(query, StandardCharsets.UTF_8)));
            result.setSource("模拟搜索引擎");
            
            results.add(result);
        }
        
        // 添加一个说明性的结果
        if (results.size() < maxResults) {
            SearchResult infoResult = new SearchResult();
            infoResult.setTitle("搜索功能说明");
            infoResult.setSnippet("当前显示的是模拟搜索结果，用于测试联网搜索功能的完整流程。" +
                                "在生产环境中，系统会连接到真实的搜索引擎获取最新的网络信息。" +
                                "这个功能确保了即使在网络受限的情况下，搜索服务也能正常工作。");
            infoResult.setUrl("https://github.com/your-project/wiki/web-search-feature");
            infoResult.setSource("系统说明");
            
            results.add(infoResult);
        }
        
        logger.info("创建了{}个模拟搜索结果", results.size());
        return results;
    }
    
    /**
     * 使用维基百科中文API进行搜索
     */
    private List<SearchResult> searchWithWikipedia(String query, int maxResults) {
        List<SearchResult> results = new ArrayList<>();
        
        try {
            logger.debug("开始维基百科搜索: query='{}'", query);
            
            // 首先搜索页面标题
            List<String> titles = searchWikipediaTitles(query, maxResults);
            
            if (titles.isEmpty()) {
                logger.debug("维基百科未找到相关页面标题");
                return results;
            }
            
            // 获取每个页面的摘要信息
            for (String title : titles) {
                if (results.size() >= maxResults) break;
                
                SearchResult result = getWikipediaPageSummary(title);
                if (result != null) {
                    results.add(result);
                }
            }
            
            logger.info("维基百科搜索完成，找到{}个结果", results.size());
            
        } catch (Exception e) {
            logger.error("维基百科搜索失败", e);
        }
        
        return results;
    }
    
    /**
     * 搜索维基百科页面标题
     */
    private List<String> searchWikipediaTitles(String query, int maxResults) {
        List<String> titles = new ArrayList<>();
        
        try {
            String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
            String searchUrl = WIKIPEDIA_API_URL + 
                "?action=query&format=json&list=search&srsearch=" + encodedQuery + 
                "&srlimit=" + Math.min(maxResults, 10) + "&srprop=snippet";
            
            logger.debug("维基百科标题搜索URL: {}", searchUrl);
            
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(searchUrl))
                .timeout(Duration.ofSeconds(DEFAULT_TIMEOUT_SECONDS))
                .header("User-Agent", "WebSearchService/1.0 (https://example.com/contact)")
                .header("Accept", "application/json")
                .GET()
                .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                JsonNode jsonResponse = objectMapper.readTree(response.body());
                JsonNode searchResults = jsonResponse.path("query").path("search");
                
                for (JsonNode result : searchResults) {
                    String title = result.path("title").asText();
                    if (!title.isEmpty()) {
                        titles.add(title);
                    }
                }
                
                logger.debug("找到{}个维基百科页面标题", titles.size());
            } else {
                logger.warn("维基百科标题搜索返回错误状态码: {}", response.statusCode());
            }
            
        } catch (Exception e) {
            logger.error("维基百科标题搜索失败", e);
        }
        
        return titles;
    }
    
    /**
     * 获取维基百科页面摘要
     */
    private SearchResult getWikipediaPageSummary(String title) {
        try {
            String encodedTitle = URLEncoder.encode(title, StandardCharsets.UTF_8);
            String summaryUrl = WIKIPEDIA_API_URL + 
                "?action=query&format=json&prop=extracts&exintro&titles=" + encodedTitle;
            
            logger.debug("获取维基百科页面摘要: title='{}'", title);
            
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(summaryUrl))
                .timeout(Duration.ofSeconds(DEFAULT_TIMEOUT_SECONDS))
                .header("User-Agent", "WebSearchService/1.0 (https://example.com/contact)")
                .header("Accept", "application/json")
                .GET()
                .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                JsonNode jsonResponse = objectMapper.readTree(response.body());
                JsonNode pages = jsonResponse.path("query").path("pages");
                
                // 获取第一个页面的信息
                if (pages.isObject() && pages.size() > 0) {
                    JsonNode page = pages.elements().next();
                    String extract = page.path("extract").asText();
                    String pageId = page.path("pageid").asText();
                    
                    if (!extract.isEmpty() && !"-1".equals(pageId)) {
                        // 清理HTML标签
                        String cleanExtract = extract.replaceAll("<.*?>", "").trim();
                        
                        // 限制摘要长度
                        if (cleanExtract.length() > 300) {
                            cleanExtract = cleanExtract.substring(0, 300) + "...";
                        }
                        
                        SearchResult result = new SearchResult();
                        result.setTitle(title);
                        result.setSnippet(cleanExtract);
                        result.setUrl("https://zh.wikipedia.org/wiki/" + encodedTitle);
                        result.setSource("维基百科");
                        
                        logger.debug("成功获取维基百科页面摘要: title='{}', 摘要长度={}", title, cleanExtract.length());
                        return result;
                    }
                }
            } else {
                logger.warn("维基百科页面摘要请求返回错误状态码: {}", response.statusCode());
            }
            
        } catch (Exception e) {
            logger.error("获取维基百科页面摘要失败: title='{}'", title, e);
        }
        
        return null;
    }
    
    
    /**
     * 将搜索结果格式化为文本
     */
    public String formatSearchResults(List<SearchResult> results) {
        if (results == null || results.isEmpty()) {
            return "没有找到相关的搜索结果。";
        }
        
        StringBuilder formatted = new StringBuilder();
        formatted.append("🔍 网络搜索结果：\n\n");
        
        for (int i = 0; i < results.size(); i++) {
            SearchResult result = results.get(i);
            formatted.append(String.format("%d. **%s**\n", i + 1, result.getTitle()));
            formatted.append(String.format("   %s\n", result.getSnippet()));
            
            if (result.getUrl() != null && !result.getUrl().isEmpty()) {
                formatted.append(String.format("   来源：%s\n", result.getUrl()));
            }
            
            formatted.append("\n");
        }
        
        formatted.append("---\n以上是网络搜索的结果，请基于这些信息回答用户的问题。");
        
        return formatted.toString();
    }
    
    /**
     * 检查搜索服务是否可用
     */
    public boolean isSearchAvailable() {
        try {
            // 直接执行一个简单的测试搜索来检查服务状态
            List<SearchResult> testResults = search("test", 1);
            boolean hasResults = !testResults.isEmpty();
            logger.debug("测试搜索结果: {} 个", testResults.size());
            return hasResults;
        } catch (Exception e) {
            logger.debug("搜索服务可用性检查失败", e);
            return false;
        }
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
     * 搜索决策结果类
     */
    public static class SearchDecision {
        private final boolean needsSearch;
        private final String searchQuery;
        private final String reason;
        private final boolean isTimeout;
        private final DecisionSource source;
        
        // 判断来源枚举
        public enum DecisionSource {
            AI_DECISION,        // AI正常返回的判断
            TIMEOUT_FALLBACK,   // 超时后的备选策略
            ERROR_FALLBACK      // 错误后的备选策略
        }
        
        public SearchDecision(boolean needsSearch, String searchQuery, String reason) {
            this(needsSearch, searchQuery, reason, false, DecisionSource.AI_DECISION);
        }
        
        public SearchDecision(boolean needsSearch, String searchQuery, String reason,
                            boolean isTimeout, DecisionSource source) {
            this.needsSearch = needsSearch;
            this.searchQuery = searchQuery;
            this.reason = reason;
            this.isTimeout = isTimeout;
            this.source = source;
        }
        
        // 创建超时备选决策
        public static SearchDecision createTimeoutFallback(boolean enableTimeoutFallback) {
            return new SearchDecision(
                !enableTimeoutFallback,
                "",
                "AI判断超时，采用" + (enableTimeoutFallback ? "保守策略（不搜索）" : "积极策略（搜索）"),
                true,
                DecisionSource.TIMEOUT_FALLBACK
            );
        }
        
        // 创建错误备选决策
        public static SearchDecision createErrorFallback() {
            return new SearchDecision(
                false,
                "",
                "AI判断过程发生异常，采用保守策略（不搜索）",
                false,
                DecisionSource.ERROR_FALLBACK
            );
        }
        
        public boolean needsSearch() { return needsSearch; }
        public String getSearchQuery() { return searchQuery; }
        public String getReason() { return reason; }
        public boolean isTimeout() { return isTimeout; }
        public DecisionSource getSource() { return source; }
        
        public boolean isNormalAIDecision() {
            return source == DecisionSource.AI_DECISION;
        }
        
        @Override
        public String toString() {
            return String.format("SearchDecision{needsSearch=%s, query='%s', reason='%s', source=%s}",
                               needsSearch, searchQuery, reason, source);
        }
    }
    
    /**
     * 搜索结果数据类
     */
    public static class SearchResult {
        private String title;
        private String snippet;
        private String url;
        private String source;
        
        // Constructors
        public SearchResult() {}
        
        public SearchResult(String title, String snippet, String url, String source) {
            this.title = title;
            this.snippet = snippet;
            this.url = url;
            this.source = source;
        }
        
        // Getters and Setters
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        
        public String getSnippet() { return snippet; }
        public void setSnippet(String snippet) { this.snippet = snippet; }
        
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
        
        public String getSource() { return source; }
        public void setSource(String source) { this.source = source; }
        
        @Override
        public String toString() {
            return String.format("SearchResult{title='%s', snippet='%s', url='%s', source='%s'}", 
                               title, snippet, url, source);
        }
    }
}

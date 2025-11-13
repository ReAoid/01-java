package com.chatbot.service;

import com.chatbot.service.knowledge.WebSearchService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * 智能联网搜索功能测试
 */
class IntelligentWebSearchTest {

    private static final Logger logger = LoggerFactory.getLogger(IntelligentWebSearchTest.class);

    private WebSearchService webSearchService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        webSearchService = new WebSearchService(objectMapper);
        logger.info("智能联网搜索测试环境初始化完成");
    }

    @Test
    @DisplayName("测试需要搜索的问题类型")
    void testQuestionsNeedingSearch() {
        logger.info("开始测试需要搜索的问题类型");

        // 测试各种需要搜索的问题
        String[] searchQueries = {
            "什么是量子计算？",
            "你知道爱因斯坦是谁吗？", 
            "请告诉我关于人工智能的信息",
            "深度学习的原理是什么？",
            "区块链技术怎么工作的？",
            "马克思主义的基本观点",
            "中国的首都在哪里？",
            "COVID-19疫苗的工作原理"
        };

        for (String query : searchQueries) {
            logger.info("测试查询: '{}'", query);
            
            // 测试搜索功能
            List<WebSearchService.SearchResult> results = webSearchService.search(query, 3);
            
            // 验证结果
            if (results != null && !results.isEmpty()) {
                logger.info("  ✅ 搜索成功，返回{}个结果", results.size());
                for (int i = 0; i < Math.min(2, results.size()); i++) {
                    WebSearchService.SearchResult result = results.get(i);
                    logger.info("    结果{}: 标题='{}', 来源='{}'", 
                              i+1, result.getTitle(), result.getSource());
                }
            } else {
                logger.warn("  ❌ 搜索无结果");
            }
        }

        logger.info("需要搜索的问题类型测试完成");
    }

    @Test
    @DisplayName("测试不需要搜索的问题类型")
    void testQuestionsNotNeedingSearch() {
        logger.info("开始测试不需要搜索的问题类型");

        // 这些问题通常不需要联网搜索
        String[] chatQueries = {
            "你好",
            "今天天气怎么样？",
            "我心情不好",
            "你觉得哪个更好？",
            "帮我计算一下 2 + 3 = ?",
            "我应该选择哪种颜色？",
            "你能给我一些建议吗？",
            "谢谢你的帮助"
        };

        for (String query : chatQueries) {
            logger.info("测试聊天查询: '{}'", query);
            // 这里主要是展示这些查询的特点，实际的AI判断逻辑在ChatService中
        }

        logger.info("不需要搜索的问题类型测试完成");
    }

    @Test
    @DisplayName("测试关键词提取效果")
    void testKeywordExtraction() {
        logger.info("开始测试关键词提取效果");

        // 测试从复杂问题中提取关键词的效果
        String[] complexQueries = {
            "你能告诉我关于机器学习算法的基本原理吗？",
            "我想了解一下区块链技术在金融领域的应用",
            "请帮我查找有关新冠疫情对全球经济影响的信息",
            "什么是深度学习？它与传统机器学习有什么区别？",
            "你知道量子计算机的工作原理是什么样的吗？"
        };

        for (String query : complexQueries) {
            logger.info("原始查询: '{}'", query);
            
            // 使用简化的关键词提取（模拟ChatService中的逻辑）
            String simplified = simplifyQueryForTest(query);
            logger.info("提取关键词: '{}'", simplified);
            
            // 测试搜索效果
            List<WebSearchService.SearchResult> results = webSearchService.search(simplified, 2);
            if (results != null && !results.isEmpty()) {
                logger.info("  ✅ 关键词搜索成功，返回{}个结果", results.size());
            }
        }

        logger.info("关键词提取效果测试完成");
    }

    @Test
    @DisplayName("测试搜索结果质量")
    void testSearchResultQuality() {
        logger.info("开始测试搜索结果质量");

        String[] qualityQueries = {
            "人工智能",
            "量子物理",
            "区块链",
            "深度学习",
            "机器学习"
        };

        for (String query : qualityQueries) {
            logger.info("测试查询: '{}'", query);
            
            List<WebSearchService.SearchResult> results = webSearchService.search(query, 3);
            
            if (results != null && !results.isEmpty()) {
                logger.info("  搜索结果数量: {}", results.size());
                
                for (WebSearchService.SearchResult result : results) {
                    // 检查结果质量
                    boolean hasTitle = result.getTitle() != null && !result.getTitle().trim().isEmpty();
                    boolean hasSnippet = result.getSnippet() != null && !result.getSnippet().trim().isEmpty();
                    boolean hasSource = result.getSource() != null && !result.getSource().trim().isEmpty();
                    
                    logger.info("    标题: {} | 摘要: {} | 来源: {} - '{}'", 
                              hasTitle ? "✅" : "❌",
                              hasSnippet ? "✅" : "❌", 
                              hasSource ? "✅" : "❌",
                              result.getTitle());
                    
                    if (hasSnippet) {
                        String snippet = result.getSnippet();
                        logger.info("      摘要预览: {}", 
                                  snippet.length() > 80 ? snippet.substring(0, 80) + "..." : snippet);
                    }
                }
            } else {
                logger.warn("  ❌ 无搜索结果");
            }
        }

        logger.info("搜索结果质量测试完成");
    }

    @Test
    @DisplayName("测试多语言查询")
    void testMultiLanguageQueries() {
        logger.info("开始测试多语言查询");

        String[][] languageQueries = {
            {"中文", "计算机科学", "数据结构", "算法设计"},
            {"英文", "computer science", "artificial intelligence", "machine learning"},
            {"混合", "AI人工智能", "deep learning深度学习", "neural network神经网络"}
        };

        for (String[] queryGroup : languageQueries) {
            String language = queryGroup[0];
            logger.info("测试{}查询:", language);
            
            for (int i = 1; i < queryGroup.length; i++) {
                String query = queryGroup[i];
                logger.info("  查询: '{}'", query);
                
                List<WebSearchService.SearchResult> results = webSearchService.search(query, 2);
                if (results != null && !results.isEmpty()) {
                    logger.info("    ✅ 成功返回{}个结果", results.size());
                    WebSearchService.SearchResult first = results.get(0);
                    logger.info("    首个结果: '{}'", first.getTitle());
                } else {
                    logger.warn("    ❌ 无结果");
                }
            }
        }

        logger.info("多语言查询测试完成");
    }

    @Test
    @DisplayName("综合智能搜索测试")
    void testComprehensiveIntelligentSearch() {
        logger.info("开始综合智能搜索测试");

        // 模拟完整的智能搜索流程
        String[] testScenarios = {
            "我想了解人工智能的发展历史",
            "区块链技术在医疗行业有什么应用？",
            "量子计算对密码学会产生什么影响？",
            "深度学习和传统机器学习的主要区别是什么？"
        };

        for (String scenario : testScenarios) {
            logger.info("\n=== 测试场景: '{}' ===", scenario);
            
            // 1. 关键词提取
            String keywords = simplifyQueryForTest(scenario);
            logger.info("1. 关键词提取: '{}'", keywords);
            
            // 2. 执行搜索
            List<WebSearchService.SearchResult> results = webSearchService.search(keywords, 3);
            logger.info("2. 搜索执行: 返回{}个结果", results != null ? results.size() : 0);
            
            // 3. 结果格式化
            if (results != null && !results.isEmpty()) {
                String formatted = webSearchService.formatSearchResults(results);
                logger.info("3. 结果格式化: 长度{}字符", formatted.length());
                
                // 显示格式化结果的开头部分
                String preview = formatted.length() > 200 ? formatted.substring(0, 200) + "..." : formatted;
                logger.info("   格式化预览:\n{}", preview);
            } else {
                logger.warn("3. 无搜索结果可格式化");
            }
        }

        logger.info("综合智能搜索测试完成");
    }

    /**
     * 简化查询的测试版本（模拟ChatService中的逻辑）
     */
    private String simplifyQueryForTest(String query) {
        if (query == null || query.trim().isEmpty()) {
            return query;
        }
        
        String processed = query.trim();
        
        // 移除常见的疑问词和语气词
        String[] questionWords = {
            "你知道", "你了解", "什么是", "是什么", "吗？", "呢？", "吗", "呢", "？", "?",
            "请问", "能告诉我", "我想知道", "帮我查一下", "搜索一下", "查找", "我想了解",
            "的信息", "相关信息", "的内容", "有关", "关于", "怎么", "如何", "为什么",
            "会产生", "有什么", "主要", "基本"
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
}

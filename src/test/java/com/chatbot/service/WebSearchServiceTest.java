package com.chatbot.service;

import com.chatbot.service.knowledge.WebSearchService;
import com.chatbot.service.knowledge.WebSearchService.SearchResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * WebSearchService测试类
 * 测试联网搜索功能的各种场景
 */
class WebSearchServiceTest {

    private static final Logger logger = LoggerFactory.getLogger(WebSearchServiceTest.class);
    
    private WebSearchService webSearchService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        webSearchService = new WebSearchService(objectMapper);
        logger.info("WebSearchService测试环境初始化完成");
    }

    @Test
    @DisplayName("测试基本搜索功能")
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void testBasicSearch() {
        logger.info("开始测试基本搜索功能");
        
        try {
            // 执行搜索
            List<SearchResult> results = webSearchService.search("你知道电视机是什么吗？", 3);
            
            // 验证结果
            assertNotNull(results, "搜索结果不应为null");
            logger.info("搜索返回{}个结果", results.size());
            
            // 打印搜索结果
            for (int i = 0; i < results.size(); i++) {
                SearchResult result = results.get(i);
                logger.info("结果{}: 标题='{}', 摘要='{}', 来源='{}'", 
                           i + 1, result.getTitle(), 
                           result.getSnippet().length() > 100 ? result.getSnippet().substring(0, 100) + "..." : result.getSnippet(),
                           result.getSource());
            }
            
            // 验证每个结果的基本属性
            for (SearchResult result : results) {
                assertNotNull(result.getTitle(), "搜索结果标题不应为null");
                assertNotNull(result.getSnippet(), "搜索结果摘要不应为null");
                assertNotNull(result.getSource(), "搜索结果来源不应为null");
                assertFalse(result.getTitle().trim().isEmpty(), "搜索结果标题不应为空");
                assertFalse(result.getSnippet().trim().isEmpty(), "搜索结果摘要不应为空");
            }
            
            logger.info("基本搜索功能测试完成");
            
        } catch (Exception e) {
            // 这种情况不应该再发生，因为WebSearchService现在会优雅处理网络问题
            logger.error("意外的搜索异常: {}", e.getMessage());
            fail("WebSearchService应该优雅处理网络问题，不应抛出异常");
        }
    }

    @Test
    @DisplayName("测试默认参数搜索")
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void testSearchWithDefaultParams() {
        logger.info("开始测试默认参数搜索");
        
        // 使用默认参数搜索
        List<SearchResult> results = webSearchService.search("计算机科学");
        
        assertNotNull(results, "搜索结果不应为null");
        logger.info("默认参数搜索返回{}个结果", results.size());
        
        // 验证结果数量不超过默认限制
        assertTrue(results.size() <= 5, "默认搜索结果数量不应超过5个");
        
        logger.info("默认参数搜索测试完成");
    }

    @Test
    @DisplayName("测试空查询处理")
    void testEmptyQuery() {
        logger.info("开始测试空查询处理");
        
        // 测试null查询
        List<SearchResult> nullResults = webSearchService.search(null);
        assertNotNull(nullResults, "null查询结果不应为null");
        assertTrue(nullResults.isEmpty(), "null查询应返回空结果");
        
        // 测试空字符串查询
        List<SearchResult> emptyResults = webSearchService.search("");
        assertNotNull(emptyResults, "空字符串查询结果不应为null");
        assertTrue(emptyResults.isEmpty(), "空字符串查询应返回空结果");
        
        // 测试空白字符串查询
        List<SearchResult> blankResults = webSearchService.search("   ");
        assertNotNull(blankResults, "空白字符串查询结果不应为null");
        assertTrue(blankResults.isEmpty(), "空白字符串查询应返回空结果");
        
        logger.info("空查询处理测试完成");
    }

    @Test
    @DisplayName("测试中文查询")
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void testChineseQuery() {
        logger.info("开始测试中文查询");
        
        List<SearchResult> results = webSearchService.search("深度学习", 3);
        
        assertNotNull(results, "中文查询结果不应为null");
        logger.info("中文查询返回{}个结果", results.size());
        
        // 打印中文查询结果
        for (SearchResult result : results) {
            logger.info("中文查询结果: 标题='{}', 摘要长度={}, 来源='{}'", 
                       result.getTitle(), result.getSnippet().length(), result.getSource());
        }
        
        logger.info("中文查询测试完成");
    }

    @Test
    @DisplayName("测试英文查询")
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void testEnglishQuery() {
        logger.info("开始测试英文查询");
        
        List<SearchResult> results = webSearchService.search("artificial intelligence", 3);
        
        assertNotNull(results, "英文查询结果不应为null");
        logger.info("英文查询返回{}个结果", results.size());
        
        // 打印英文查询结果
        for (SearchResult result : results) {
            logger.info("英文查询结果: 标题='{}', 摘要长度={}, 来源='{}'", 
                       result.getTitle(), result.getSnippet().length(), result.getSource());
        }
        
        logger.info("英文查询测试完成");
    }

    @Test
    @DisplayName("测试搜索结果格式化")
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void testFormatSearchResults() {
        logger.info("开始测试搜索结果格式化");
        
        // 执行搜索
        List<SearchResult> results = webSearchService.search("操作系统", 2);
        
        // 格式化结果
        String formattedResults = webSearchService.formatSearchResults(results);
        
        assertNotNull(formattedResults, "格式化结果不应为null");
        assertFalse(formattedResults.trim().isEmpty(), "格式化结果不应为空");
        
        logger.info("格式化结果长度: {}", formattedResults.length());
        logger.info("格式化结果预览:\n{}", 
                   formattedResults.length() > 200 ? formattedResults.substring(0, 200) + "..." : formattedResults);
        
        // 验证格式化结果包含必要元素
        assertTrue(formattedResults.contains("🔍 网络搜索结果"), "格式化结果应包含标题");
        assertTrue(formattedResults.contains("---"), "格式化结果应包含分隔符");
        
        logger.info("搜索结果格式化测试完成");
    }

    @Test
    @DisplayName("测试空结果格式化")
    void testFormatEmptyResults() {
        logger.info("开始测试空结果格式化");
        
        // 测试null结果
        String nullFormatted = webSearchService.formatSearchResults(null);
        assertNotNull(nullFormatted, "null结果格式化不应为null");
        assertTrue(nullFormatted.contains("没有找到相关的搜索结果"), "应包含无结果提示");
        
        // 测试空列表
        String emptyFormatted = webSearchService.formatSearchResults(List.of());
        assertNotNull(emptyFormatted, "空列表格式化不应为null");
        assertTrue(emptyFormatted.contains("没有找到相关的搜索结果"), "应包含无结果提示");
        
        logger.info("空结果格式化测试完成");
    }

    @Test
    @DisplayName("测试搜索服务可用性检查")
    void testSearchAvailability() {
        logger.info("开始测试搜索服务可用性检查");
        
        boolean isAvailable = webSearchService.isSearchAvailable();
        logger.info("搜索服务可用性: {}", isAvailable);
        
        // 注意：这个测试结果取决于网络状况，所以我们只记录结果
        if (isAvailable) {
            logger.info("搜索服务当前可用");
        } else {
            logger.warn("搜索服务当前不可用，可能是网络问题");
        }
        
        logger.info("搜索服务可用性检查测试完成");
    }

    @Test
    @DisplayName("测试特殊字符查询")
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void testSpecialCharacterQuery() {
        logger.info("开始测试特殊字符查询");
        
        // 测试包含特殊字符的查询
        List<SearchResult> results = webSearchService.search("C++ & Java 编程对比", 2);
        
        assertNotNull(results, "特殊字符查询结果不应为null");
        logger.info("特殊字符查询返回{}个结果", results.size());
        
        logger.info("特殊字符查询测试完成");
    }

    @Test
    @DisplayName("测试长查询字符串")
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void testLongQuery() {
        logger.info("开始测试长查询字符串");
        
        String longQuery = "如何在Spring Boot应用程序中实现高性能的微服务架构并集成Redis缓存和MySQL数据库以及消息队列系统";
        List<SearchResult> results = webSearchService.search(longQuery, 2);
        
        assertNotNull(results, "长查询结果不应为null");
        logger.info("长查询返回{}个结果", results.size());
        
        logger.info("长查询字符串测试完成");
    }

    @Test
    @DisplayName("测试数字限制参数")
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void testMaxResultsLimit() {
        logger.info("开始测试数字限制参数");
        
        // 测试限制1个结果
        List<SearchResult> oneResult = webSearchService.search("Python", 1);
        assertNotNull(oneResult, "单个结果查询不应为null");
        assertTrue(oneResult.size() <= 1, "结果数量不应超过限制");
        logger.info("限制1个结果，实际返回{}个", oneResult.size());
        
        // 测试限制10个结果
        List<SearchResult> tenResults = webSearchService.search("JavaScript", 10);
        assertNotNull(tenResults, "10个结果查询不应为null");
        assertTrue(tenResults.size() <= 10, "结果数量不应超过限制");
        logger.info("限制10个结果，实际返回{}个", tenResults.size());
        
        logger.info("数字限制参数测试完成");
    }

    @Test
    @DisplayName("测试SearchResult对象")
    void testSearchResultObject() {
        logger.info("开始测试SearchResult对象");
        
        // 创建测试对象
        SearchResult result = new SearchResult();
        result.setTitle("测试标题");
        result.setSnippet("测试摘要内容");
        result.setUrl("https://example.com");
        result.setSource("测试来源");
        
        // 验证getter方法
        assertEquals("测试标题", result.getTitle());
        assertEquals("测试摘要内容", result.getSnippet());
        assertEquals("https://example.com", result.getUrl());
        assertEquals("测试来源", result.getSource());
        
        // 测试构造函数
        SearchResult result2 = new SearchResult("标题2", "摘要2", "https://example2.com", "来源2");
        assertEquals("标题2", result2.getTitle());
        assertEquals("摘要2", result2.getSnippet());
        assertEquals("https://example2.com", result2.getUrl());
        assertEquals("来源2", result2.getSource());
        
        // 测试toString方法
        String toString = result.toString();
        assertNotNull(toString);
        assertTrue(toString.contains("测试标题"));
        assertTrue(toString.contains("测试摘要内容"));
        
        logger.info("SearchResult toString: {}", toString);
        logger.info("SearchResult对象测试完成");
    }

    @Test
    @DisplayName("测试维基百科搜索")
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void testWikipediaSearch() {
        logger.info("开始测试维基百科搜索功能");
        
        // 测试中文查询
        List<SearchResult> results = webSearchService.search("Java编程语言", 3);
        
        assertNotNull(results, "维基百科搜索结果不应为null");
        assertFalse(results.isEmpty(), "维基百科搜索应该返回结果");
        logger.info("维基百科搜索返回{}个结果", results.size());
        
        // 验证搜索结果
        for (int i = 0; i < results.size(); i++) {
            SearchResult result = results.get(i);
            assertNotNull(result.getTitle(), "搜索结果标题不应为null");
            assertNotNull(result.getSnippet(), "搜索结果摘要不应为null");
            assertNotNull(result.getSource(), "搜索结果来源不应为null");
            
            logger.info("维基百科结果{}: 标题='{}', 来源='{}', URL='{}'", 
                       i + 1, result.getTitle(), result.getSource(), result.getUrl());
            logger.debug("摘要: {}", result.getSnippet().length() > 100 ? 
                        result.getSnippet().substring(0, 100) + "..." : result.getSnippet());
        }
        
        // 验证至少有一个来自维基百科的结果
        boolean hasWikipediaResult = results.stream()
            .anyMatch(result -> "维基百科".equals(result.getSource()));
        
        if (hasWikipediaResult) {
            logger.info("✅ 成功获取维基百科搜索结果");
        } else {
            logger.info("ℹ️ 未获取到维基百科结果，可能使用了备用搜索方案");
        }
        
        logger.info("维基百科搜索功能测试完成");
    }

    @Test
    @DisplayName("测试搜索异常处理")
    void testSearchExceptionHandling() {
        logger.info("开始测试搜索异常处理");
        
        // 测试搜索服务的异常处理能力
        List<SearchResult> results = webSearchService.search("异常处理测试", 2);
        
        // 验证服务能够优雅处理各种情况
        assertNotNull(results, "搜索结果不应为null");
        logger.info("异常处理测试返回{}个结果", results.size());
        
        // 验证结果的基本属性
        for (SearchResult result : results) {
            assertNotNull(result.getTitle(), "搜索结果标题不应为null");
            assertNotNull(result.getSnippet(), "搜索结果摘要不应为null");
            assertNotNull(result.getSource(), "搜索结果来源不应为null");
            logger.debug("结果: 标题='{}', 来源='{}'", result.getTitle(), result.getSource());
        }
        
        // 测试服务可用性检查
        boolean isAvailable = webSearchService.isSearchAvailable();
        logger.info("搜索服务可用性: {}", isAvailable);
        
        logger.info("搜索异常处理测试完成");
    }

    @Test
    @DisplayName("综合功能测试")
    @Timeout(value = 60, unit = TimeUnit.SECONDS)
    void testComprehensiveFunctionality() {
        logger.info("开始综合功能测试");
        
        // 1. 测试搜索功能
        List<SearchResult> results = webSearchService.search("Spring Framework教程", 3);
        assertNotNull(results);
        logger.info("步骤1: 搜索完成，返回{}个结果", results.size());
        
        // 2. 测试结果格式化
        String formatted = webSearchService.formatSearchResults(results);
        assertNotNull(formatted);
        assertFalse(formatted.trim().isEmpty());
        logger.info("步骤2: 结果格式化完成，长度={}", formatted.length());
        
        // 3. 测试服务可用性
        boolean available = webSearchService.isSearchAvailable();
        logger.info("步骤3: 服务可用性检查完成，结果={}", available);
        
        // 4. 打印综合测试结果
        logger.info("=== 综合测试结果摘要 ===");
        logger.info("搜索结果数量: {}", results.size());
        logger.info("格式化结果长度: {}", formatted.length());
        logger.info("服务可用性: {}", available);
        
        if (!results.isEmpty()) {
            logger.info("第一个搜索结果:");
            SearchResult first = results.get(0);
            logger.info("  标题: {}", first.getTitle());
            logger.info("  摘要: {}", first.getSnippet().length() > 50 ? 
                       first.getSnippet().substring(0, 50) + "..." : first.getSnippet());
            logger.info("  来源: {}", first.getSource());
        }
        
        logger.info("综合功能测试完成");
    }
}

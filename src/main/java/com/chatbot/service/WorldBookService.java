package com.chatbot.service;

import com.chatbot.model.WorldBookEntry;
import com.chatbot.util.FileUtil;
import com.chatbot.util.JsonUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 世界书服务
 * 管理手动配置和自动提取的世界书内容
 */
@Service
public class WorldBookService {
    
    private static final Logger logger = LoggerFactory.getLogger(WorldBookService.class);
    
    // 手动配置的世界书文件路径
    @Value("${app.resources.data-paths.worldbook-manual:src/main/resources/data/worldbook/manual_worldbook.json}")
    private String manualWorldBookPath;
    
    // 自动提取的世界书文件路径
    @Value("${app.resources.data-paths.worldbook-extracted:src/main/resources/data/worldbook/extracted_worldbook.json}")
    private String extractedWorldBookPath;
    
    // 内存中的世界书条目
    private final ConcurrentHashMap<String, WorldBookEntry> manualEntries;
    private final ConcurrentHashMap<String, WorldBookEntry> extractedEntries;
    
    // 按会话ID索引的提取条目
    private final ConcurrentHashMap<String, List<WorldBookEntry>> sessionExtractedEntries;
    
    // 配置参数
    private static final int MAX_EXTRACTED_ENTRIES_PER_SESSION = 50;
    private static final int MAX_RETRIEVED_ENTRIES = 3;
    
    public WorldBookService() {
        this.manualEntries = new ConcurrentHashMap<>();
        this.extractedEntries = new ConcurrentHashMap<>();
        this.sessionExtractedEntries = new ConcurrentHashMap<>();
    }
    
    /**
     * 初始化：加载本地JSON文件
     */
    @PostConstruct
    public void initialize() {
        loadManualWorldBook();
        loadExtractedWorldBook();
        logger.info("🌍 世界书服务初始化完成 - 手动条目: {}, 自动提取条目: {}", 
                   manualEntries.size(), extractedEntries.size());
    }
    
    /**
     * 加载手动配置的世界书
     */
    private void loadManualWorldBook() {
        try {
            File file = new File(manualWorldBookPath);
            if (!file.exists()) {
                logger.info("手动世界书文件不存在，创建默认文件: {}", manualWorldBookPath);
                createDefaultManualWorldBook();
                return;
            }
            
            String content = FileUtil.readString(manualWorldBookPath);
            if (content == null || content.trim().isEmpty()) {
                logger.warn("手动世界书文件为空: {}", manualWorldBookPath);
                return;
            }
            
            List<WorldBookEntry> entries = JsonUtil.fromJson(content, new TypeReference<List<WorldBookEntry>>() {});
            if (entries != null) {
                manualEntries.clear();
                for (WorldBookEntry entry : entries) {
                    if (entry.getEntryId() != null) {
                        manualEntries.put(entry.getEntryId(), entry);
                    }
                }
            }
            
        } catch (Exception e) {
            logger.error("加载手动世界书失败: {}", manualWorldBookPath, e);
        }
    }
    
    /**
     * 加载自动提取的世界书
     */
    private void loadExtractedWorldBook() {
        try {
            File file = new File(extractedWorldBookPath);
            if (!file.exists()) {
                logger.info("自动提取世界书文件不存在，创建空文件: {}", extractedWorldBookPath);
                saveExtractedWorldBook();
                return;
            }
            
            String content = FileUtil.readString(extractedWorldBookPath);
            if (content == null || content.trim().isEmpty()) {
                logger.info("自动提取世界书文件为空: {}", extractedWorldBookPath);
                return;
            }
            
            List<WorldBookEntry> entries = JsonUtil.fromJson(content, new TypeReference<List<WorldBookEntry>>() {});
            if (entries != null) {
                extractedEntries.clear();
                sessionExtractedEntries.clear();
                
                for (WorldBookEntry entry : entries) {
                    if (entry.getEntryId() != null) {
                        extractedEntries.put(entry.getEntryId(), entry);
                        
                        // 按会话ID索引
                        if (entry.getSessionId() != null) {
                            sessionExtractedEntries.computeIfAbsent(entry.getSessionId(), k -> new ArrayList<>())
                                                  .add(entry);
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("加载自动提取世界书失败: {}", extractedWorldBookPath, e);
        }
    }
    
    /**
     * 创建默认的手动世界书
     */
    private void createDefaultManualWorldBook() {
        try {
            List<WorldBookEntry> defaultEntries = new ArrayList<>();
            
            // 示例条目1：创作偏好
            WorldBookEntry entry1 = new WorldBookEntry("创作偏好", 
                "用户在创作诗歌时，喜欢古风和现代诗两种风格。如果用户没有特别指定风格，可以询问用户的偏好。", 
                "manual");
            entry1.setKeywords(Arrays.asList("诗", "诗歌", "创作", "写诗", "古风", "现代诗"));
            entry1.setImportance(7);
            entry1.setRelevanceThreshold(0.2);
            defaultEntries.add(entry1);
            
            // 示例条目2：对话风格
            WorldBookEntry entry2 = new WorldBookEntry("对话风格", 
                "与用户对话时保持友好、耐心的态度，避免过于正式的语言，可以适当使用表情符号增加亲和力。", 
                "manual");
            entry2.setKeywords(Arrays.asList("对话", "聊天", "交流", "风格", "态度"));
            entry2.setImportance(5);
            entry2.setRelevanceThreshold(0.4);
            defaultEntries.add(entry2);
            
            // 保存到文件（使用格式化JSON）
            String jsonContent = JsonUtil.toPrettyJson(defaultEntries);
            FileUtil.createDirectories(new File(manualWorldBookPath).getParent());
            FileUtil.writeString(manualWorldBookPath, jsonContent);
            
            // 加载到内存
            for (WorldBookEntry entry : defaultEntries) {
                manualEntries.put(entry.getEntryId(), entry);
            }
            
            logger.info("创建默认手动世界书完成，条目数: {}", defaultEntries.size());
            
        } catch (Exception e) {
            logger.error("创建默认手动世界书失败", e);
        }
    }
    
    /**
     * 检索相关的世界书内容
     */
    public String retrieveRelevantContent(String sessionId, String userInput) {
        try {
            List<WorldBookEntry> relevantEntries = new ArrayList<>();
            
            // 1. 检索手动配置的条目
            relevantEntries.addAll(findRelevantEntries(manualEntries.values(), userInput));
            
            // 2. 检索当前会话的自动提取条目
            List<WorldBookEntry> sessionEntries = sessionExtractedEntries.get(sessionId);
            if (sessionEntries != null) {
                relevantEntries.addAll(findRelevantEntries(sessionEntries, userInput));
            }
            
            // 3. 检索其他会话的高重要性自动提取条目
            relevantEntries.addAll(findRelevantEntries(
                extractedEntries.values().stream()
                    .filter(entry -> !sessionId.equals(entry.getSessionId()) && entry.getImportance() >= 7)
                    .collect(Collectors.toList()), 
                userInput));
            
            // 4. 按相关性和重要性排序
            relevantEntries = relevantEntries.stream()
                .distinct()
                .sorted((e1, e2) -> {
                    double score1 = calculateRelevanceScore(e1, userInput) * e1.getImportance();
                    double score2 = calculateRelevanceScore(e2, userInput) * e2.getImportance();
                    return Double.compare(score2, score1);
                })
                .limit(MAX_RETRIEVED_ENTRIES)
                .collect(Collectors.toList());
            
            // 5. 更新使用信息
            relevantEntries.forEach(WorldBookEntry::updateUsage);
            
            // 6. 构建内容
            return buildWorldBookContent(relevantEntries);
            
        } catch (Exception e) {
            logger.error("检索世界书内容时发生错误", e);
            return "";
        }
    }
    
    /**
     * 从对话中提取并添加世界书条目
     */
    public void extractAndAddEntry(String sessionId, String userInput) {
        try {
            // 提取关键信息
            List<String> keyInfo = extractKeyInformation(userInput);
            
            for (String info : keyInfo) {
                if (isImportantInformation(info) && !isDuplicate(info, sessionId)) {
                    WorldBookEntry entry = createExtractedEntry(sessionId, info);
                    addExtractedEntry(entry);
                    logger.debug("提取新的世界书条目: {}", entry.getTitle());
                }
            }
            
            // 清理过期条目
            cleanupOldExtractedEntries(sessionId);
            
            // 异步保存到文件
            saveExtractedWorldBookAsync();
            
        } catch (Exception e) {
            logger.error("提取世界书条目时发生错误", e);
        }
    }
    
    /**
     * 查找相关条目
     */
    private List<WorldBookEntry> findRelevantEntries(Collection<WorldBookEntry> entries, String userInput) {
        return entries.stream()
            .filter(WorldBookEntry::isActive)
            .filter(entry -> {
                double score = calculateRelevanceScore(entry, userInput);
                return score >= entry.getRelevanceThreshold();
            })
            .collect(Collectors.toList());
    }
    
    /**
     * 计算相关性评分
     */
    private double calculateRelevanceScore(WorldBookEntry entry, String userInput) {
        if (entry.getKeywords() == null || entry.getKeywords().isEmpty()) {
            return 0.0;
        }
        
        String lowerInput = userInput.toLowerCase();
        int matchCount = 0;
        
        for (String keyword : entry.getKeywords()) {
            if (lowerInput.contains(keyword.toLowerCase())) {
                matchCount++;
            }
        }
        
        return (double) matchCount / entry.getKeywords().size();
    }
    
    /**
     * 构建世界书内容
     */
    private String buildWorldBookContent(List<WorldBookEntry> entries) {
        if (entries.isEmpty()) {
            return "";
        }
        
        StringBuilder content = new StringBuilder();
        for (WorldBookEntry entry : entries) {
            if (content.length() > 0) {
                content.append("\n");
            }
            content.append("- ").append(entry.getContent());
        }
        
        return content.toString();
    }
    
    /**
     * 提取关键信息
     */
    private List<String> extractKeyInformation(String content) {
        List<String> keyInfo = new ArrayList<>();
        
        if (content == null || content.trim().isEmpty()) {
            return keyInfo;
        }
        
        // 按句子分割
        String[] sentences = content.split("[。！？.!?]");
        
        for (String sentence : sentences) {
            sentence = sentence.trim();
            if (sentence.length() > 10 && containsKeywords(sentence)) {
                keyInfo.add(sentence);
            }
        }
        
        return keyInfo;
    }
    
    /**
     * 检查是否包含关键词
     */
    private boolean containsKeywords(String sentence) {
        String[] keywordPatterns = {
            "我是", "我叫", "我的名字", "我喜欢", "我不喜欢", "我需要", "我想要",
            "重要", "记住", "提醒", "偏好", "习惯", "经常", "总是", "从不",
            "我的", "我在", "我会", "我希望", "我觉得", "我认为"
        };
        
        String lowerSentence = sentence.toLowerCase();
        return Arrays.stream(keywordPatterns)
                .anyMatch(lowerSentence::contains);
    }
    
    /**
     * 判断是否为重要信息
     */
    private boolean isImportantInformation(String info) {
        return info.length() > 5 && info.length() < 200;
    }
    
    /**
     * 检查是否重复
     */
    private boolean isDuplicate(String content, String sessionId) {
        List<WorldBookEntry> sessionEntries = sessionExtractedEntries.get(sessionId);
        if (sessionEntries == null) {
            return false;
        }
        
        return sessionEntries.stream()
            .anyMatch(entry -> entry.getContent().equals(content));
    }
    
    /**
     * 创建提取的条目
     */
    private WorldBookEntry createExtractedEntry(String sessionId, String content) {
        WorldBookEntry entry = new WorldBookEntry(
            content.length() > 20 ? content.substring(0, 20) + "..." : content,
            content,
            "extracted"
        );
        entry.setSessionId(sessionId);
        entry.setKeywords(extractKeywords(content));
        entry.setImportance(calculateImportance(content));
        return entry;
    }
    
    /**
     * 添加提取的条目
     */
    private void addExtractedEntry(WorldBookEntry entry) {
        extractedEntries.put(entry.getEntryId(), entry);
        sessionExtractedEntries.computeIfAbsent(entry.getSessionId(), k -> new ArrayList<>())
                              .add(entry);
    }
    
    /**
     * 提取关键词
     */
    private List<String> extractKeywords(String content) {
        String[] words = content.replaceAll("[^\\w\\s]", "").split("\\s+");
        return Arrays.stream(words)
            .filter(word -> word.length() > 1)
            .distinct()
            .limit(5)
            .collect(Collectors.toList());
    }
    
    /**
     * 计算重要性
     */
    private int calculateImportance(String content) {
        int score = 5; // 基础分数
        
        if (content.contains("重要") || content.contains("记住")) {
            score += 2;
        }
        if (content.contains("我") || content.contains("我的")) {
            score += 1;
        }
        if (content.length() > 50) {
            score += 1;
        }
        
        return Math.max(1, Math.min(10, score));
    }
    
    /**
     * 清理过期的提取条目
     */
    private void cleanupOldExtractedEntries(String sessionId) {
        List<WorldBookEntry> sessionEntries = sessionExtractedEntries.get(sessionId);
        if (sessionEntries == null || sessionEntries.size() <= MAX_EXTRACTED_ENTRIES_PER_SESSION) {
            return;
        }
        
        // 按创建时间排序，移除最旧的条目
        sessionEntries.sort(Comparator.comparing(WorldBookEntry::getCreatedAt));
        while (sessionEntries.size() > MAX_EXTRACTED_ENTRIES_PER_SESSION) {
            WorldBookEntry oldEntry = sessionEntries.remove(0);
            extractedEntries.remove(oldEntry.getEntryId());
        }
    }
    
    /**
     * 保存自动提取的世界书到文件
     */
    private void saveExtractedWorldBook() {
        try {
            List<WorldBookEntry> allExtracted = new ArrayList<>(extractedEntries.values());
            String jsonContent = JsonUtil.toPrettyJson(allExtracted);
            
            FileUtil.createDirectories(new File(extractedWorldBookPath).getParent());
            FileUtil.writeString(extractedWorldBookPath, jsonContent);
            
            logger.debug("自动提取世界书已保存，条目数: {}", allExtracted.size());
            
        } catch (Exception e) {
            logger.error("保存自动提取世界书失败", e);
        }
    }
    
    /**
     * 异步保存自动提取的世界书
     */
    private void saveExtractedWorldBookAsync() {
        // 简单的异步实现，实际可以使用@Async
        new Thread(this::saveExtractedWorldBook).start();
    }
    
    /**
     * 获取统计信息
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("manualEntries", manualEntries.size());
        stats.put("extractedEntries", extractedEntries.size());
        stats.put("activeSessions", sessionExtractedEntries.size());
        
        int totalUsage = extractedEntries.values().stream()
            .mapToInt(WorldBookEntry::getUsageCount)
            .sum();
        stats.put("totalUsage", totalUsage);
        
        return stats;
    }
}

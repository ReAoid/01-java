package com.chatbot.service;

import com.chatbot.model.domain.Memory;
import com.chatbot.model.record.MemoryStats;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 记忆管理服务
 * 实现长期记忆的存储、检索和管理
 */
@Service
public class MemoryService {
    
    private static final Logger logger = LoggerFactory.getLogger(MemoryService.class);
    
    // 按会话ID存储记忆
    private final ConcurrentHashMap<String, List<Memory>> sessionMemories;
    
    // 全局记忆索引（用于快速检索）
    private final ConcurrentHashMap<String, Memory> memoryIndex;
    
    // 配置参数
    private static final int MAX_MEMORIES_PER_SESSION = 1000; // 每个会话最多存储的记忆数量
    private static final int MAX_RETRIEVED_MEMORIES = 5; // 每次检索最多返回的记忆数量
    
    public MemoryService() {
        this.sessionMemories = new ConcurrentHashMap<>();
        this.memoryIndex = new ConcurrentHashMap<>();
    }
    
    /**
     * 更新记忆（从对话中提取重要信息）
     */
    public void updateMemory(String sessionId, String content) {
        try {
            // 提取关键信息
            List<String> keyInfo = extractKeyInformation(content);
            
            for (String info : keyInfo) {
                if (isImportantInformation(info)) {
                    Memory memory = createMemory(sessionId, info);
                    storeMemory(memory);
                }
            }
            
            // 清理过期记忆
            cleanupOldMemories(sessionId);
            
        } catch (Exception e) {
            logger.error("更新记忆时发生错误，会话ID: {}", sessionId, e);
        }
    }
    
    /**
     * 检索相关记忆
     */
    public String retrieveRelevantMemory(String sessionId, String query) {
        try {
            List<Memory> sessionMems = sessionMemories.get(sessionId);
            if (sessionMems == null || sessionMems.isEmpty()) {
                return "";
            }
            
            // 根据相关性评分排序记忆
            List<Memory> relevantMemories = sessionMems.stream()
                    .filter(Memory::isActive)
                    .sorted((m1, m2) -> calculateRelevanceScore(m2, query) - calculateRelevanceScore(m1, query))
                    .limit(MAX_RETRIEVED_MEMORIES)
                    .collect(Collectors.toList());
            
            // 更新访问信息
            relevantMemories.forEach(Memory::updateAccess);
            
            // 构建记忆摘要
            return buildMemorySummary(relevantMemories);
            
        } catch (Exception e) {
            logger.error("检索记忆时发生错误，会话ID: {}", sessionId, e);
            return "";
        }
    }
    
    /**
     * 提取关键信息（简化版实现）
     */
    private List<String> extractKeyInformation(String content) {
        List<String> keyInfo = new ArrayList<>();
        
        if (content == null || content.trim().isEmpty()) {
            return keyInfo;
        }
        
        // 简单的关键信息提取逻辑
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
            "重要", "记住", "提醒", "偏好", "习惯", "经常", "总是", "从不"
        };
        
        String lowerSentence = sentence.toLowerCase();
        return Arrays.stream(keywordPatterns)
                .anyMatch(lowerSentence::contains);
    }
    
    /**
     * 判断是否为重要信息
     */
    private boolean isImportantInformation(String info) {
        // 简单的重要性判断逻辑
        return info.length() > 5 && info.length() < 200;
    }
    
    /**
     * 创建记忆对象
     */
    private Memory createMemory(String sessionId, String content) {
        Memory memory = new Memory(sessionId, content, determineMemoryType(content));
        memory.setImportance(calculateImportance(content));
        memory.setKeywords(extractKeywords(content));
        return memory;
    }
    
    /**
     * 确定记忆类型
     */
    private String determineMemoryType(String content) {
        String lowerContent = content.toLowerCase();
        
        if (lowerContent.contains("喜欢") || lowerContent.contains("不喜欢") || 
            lowerContent.contains("偏好") || lowerContent.contains("习惯")) {
            return "preference";
        } else if (lowerContent.contains("我是") || lowerContent.contains("我叫") || 
                   lowerContent.contains("我的")) {
            return "fact";
        } else if (lowerContent.contains("朋友") || lowerContent.contains("家人") || 
                   lowerContent.contains("同事")) {
            return "relationship";
        } else {
            return "event";
        }
    }
    
    /**
     * 计算重要性评分
     */
    private int calculateImportance(String content) {
        int score = 5; // 基础分数
        
        // 根据内容特征调整分数
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
     * 提取关键词
     */
    private String[] extractKeywords(String content) {
        // 简单的关键词提取
        String[] words = content.replaceAll("[^\\w\\s]", "")
                               .split("\\s+"); // 将内容中的非字母数字字符替换为空格，然后按空格分割
        
        return Arrays.stream(words)
                .filter(word -> word.length() > 1)
                .distinct()
                .limit(5)
                .toArray(String[]::new);
    }
    
    /**
     * 存储记忆
     */
    private void storeMemory(Memory memory) {
        sessionMemories.computeIfAbsent(memory.getSessionId(), k -> new ArrayList<>())
                      .add(memory);
        
        memoryIndex.put(memory.getMemoryId(), memory);
        
        logger.debug("存储新记忆: {} - {}", memory.getMemoryId(), 
                    memory.getContent().substring(0, Math.min(50, memory.getContent().length())));
    }
    
    /**
     * 计算相关性评分
     */
    private int calculateRelevanceScore(Memory memory, String query) {
        int score = 0;
        
        String lowerContent = memory.getContent().toLowerCase();
        String lowerQuery = query.toLowerCase();
        
        // 直接匹配
        if (lowerContent.contains(lowerQuery)) {
            score += 10;
        }
        
        // 关键词匹配
        if (memory.getKeywords() != null) {
            for (String keyword : memory.getKeywords()) {
                if (lowerQuery.contains(keyword.toLowerCase())) {
                    score += 5;
                }
            }
        }
        
        // 重要性加分
        score += memory.getImportance();
        
        // 访问频率加分
        score += Math.min(memory.getAccessCount(), 5);
        
        // 时间衰减（越新的记忆分数越高）
        long daysSinceCreated = java.time.Duration.between(memory.getCreatedAt(), LocalDateTime.now()).toDays();
        score += Math.max(0, 10 - daysSinceCreated);
        
        return score;
    }
    
    /**
     * 构建记忆摘要
     */
    private String buildMemorySummary(List<Memory> memories) {
        if (memories.isEmpty()) {
            return "";
        }
        
        StringBuilder summary = new StringBuilder();
        
        for (Memory memory : memories) {
            summary.append("- ").append(memory.getContent()).append("\n");
        }
        
        return summary.toString();
    }
    
    /**
     * 清理过期记忆
     */
    private void cleanupOldMemories(String sessionId) {
        List<Memory> memories = sessionMemories.get(sessionId);
        if (memories != null && memories.size() > MAX_MEMORIES_PER_SESSION) {
            // 保留最重要和最新的记忆
            memories.sort((m1, m2) -> {
                int importanceCompare = Integer.compare(m2.getImportance(), m1.getImportance());
                if (importanceCompare != 0) {
                    return importanceCompare;
                }
                return m2.getCreatedAt().compareTo(m1.getCreatedAt());
            });
            
            // 移除超出限制的记忆
            List<Memory> toRemove = memories.subList(MAX_MEMORIES_PER_SESSION, memories.size());
            toRemove.forEach(memory -> {
                memory.setActive(false);
                memoryIndex.remove(memory.getMemoryId());
            });
            
            memories.removeAll(toRemove);
            
            logger.info("清理了 {} 个过期记忆，会话ID: {}", toRemove.size(), sessionId);
        }
    }
    
    /**
     * 获取会话记忆统计
     */
    public MemoryStats getMemoryStats(String sessionId) {
        List<Memory> memories = sessionMemories.get(sessionId);
        if (memories == null) {
            return new MemoryStats(0, 0);
        }
        
        long activeCount = memories.stream().filter(Memory::isActive).count();
        return new MemoryStats(memories.size(), (int) activeCount);
    }
    
}

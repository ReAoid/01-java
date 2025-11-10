package com.chatbot.service.knowledge;

import com.chatbot.model.domain.Persona;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 统一的知识管理服务
 * 整合了 MemoryService（短期记忆）、WorldBookService（长期知识）和 PersonaService（人设管理）
 * 提供统一的知识检索和管理接口
 */
@Service
public class KnowledgeService {
    
    private static final Logger logger = LoggerFactory.getLogger(KnowledgeService.class);
    
    private final MemoryService memoryService;
    private final WorldBookService worldBookService;
    private final PersonaService personaService;
    
    public KnowledgeService(MemoryService memoryService,
                           WorldBookService worldBookService,
                           PersonaService personaService) {
        this.memoryService = memoryService;
        this.worldBookService = worldBookService;
        this.personaService = personaService;
        
        logger.info("知识管理服务初始化完成");
    }
    
    // ==================== 统一检索API ====================
    
    /**
     * 统一的知识上下文检索
     * 整合人设、短期记忆和长期知识
     * @param sessionId 会话ID
     * @param query 查询内容
     * @return 相关知识上下文
     */
    public KnowledgeContext retrieveRelevantContext(String sessionId, String query) {
        KnowledgeContext context = new KnowledgeContext();
        
        try {
            // 1. 获取人设提示词（最高优先级）
            String personaId = getCurrentPersonaId(sessionId);
            String personaPrompt = personaService.getPersonaPrompt(personaId);
            if (personaPrompt != null && !personaPrompt.isEmpty()) {
                context.setPersonaPrompt(personaPrompt);
                logger.debug("获取人设提示词: personaId={}, length={}", personaId, personaPrompt.length());
            }
            
            // 2. 获取短期记忆（会话内记忆）
            String shortTermMemory = memoryService.retrieveRelevantMemory(sessionId, query);
            if (!shortTermMemory.isEmpty()) {
                context.setShortTermMemory(shortTermMemory);
                logger.debug("获取短期记忆: sessionId={}, length={}", sessionId, shortTermMemory.length());
            }
            
            // 3. 获取长期知识（世界书）
            String longTermKnowledge = worldBookService.retrieveRelevantContent(sessionId, query);
            if (!longTermKnowledge.isEmpty()) {
                context.setLongTermKnowledge(longTermKnowledge);
                logger.debug("获取长期知识: sessionId={}, length={}", sessionId, longTermKnowledge.length());
            }
            
            logger.info("知识上下文检索完成: sessionId={}, 人设={}, 短期记忆={}, 长期知识={}", 
                       sessionId, 
                       context.hasPersonaPrompt(), 
                       context.hasShortTermMemory(), 
                       context.hasLongTermKnowledge());
            
        } catch (Exception e) {
            logger.error("检索知识上下文时发生错误: sessionId={}", sessionId, e);
        }
        
        return context;
    }
    
    /**
     * 获取格式化的知识上下文（用于AI提示词）
     * @param sessionId 会话ID
     * @param query 查询内容
     * @return 格式化的上下文文本
     */
    public String getFormattedContext(String sessionId, String query) {
        KnowledgeContext context = retrieveRelevantContext(sessionId, query);
        return context.toFormattedString();
    }
    
    // ==================== 知识更新API ====================
    
    /**
     * 从用户输入更新知识库
     * 同时更新短期记忆和长期知识
     * @param sessionId 会话ID
     * @param userInput 用户输入
     */
    public void updateKnowledge(String sessionId, String userInput) {
        try {
            // 1. 更新短期记忆
            memoryService.updateMemory(sessionId, userInput);
            logger.debug("更新短期记忆: sessionId={}", sessionId);
            
            // 2. 提取并更新长期知识
            worldBookService.extractAndAddEntry(sessionId, userInput);
            logger.debug("更新长期知识: sessionId={}", sessionId);
            
        } catch (Exception e) {
            logger.error("更新知识库时发生错误: sessionId={}", sessionId, e);
        }
    }
    
    /**
     * 批量更新知识库（用于对话历史导入）
     * @param sessionId 会话ID
     * @param contents 内容列表
     */
    public void batchUpdateKnowledge(String sessionId, List<String> contents) {
        if (contents == null || contents.isEmpty()) {
            return;
        }
        
        logger.info("批量更新知识库: sessionId={}, count={}", sessionId, contents.size());
        
        for (String content : contents) {
            updateKnowledge(sessionId, content);
        }
    }
    
    // ==================== 人设管理API ====================
    
    /**
     * 设置会话的人设
     * @param sessionId 会话ID
     * @param personaId 人设ID
     */
    public void setPersona(String sessionId, String personaId) {
        // 这里可以实现会话级别的人设存储
        // 目前简化处理：直接返回指定人设
        logger.info("设置会话人设: sessionId={}, personaId={}", sessionId, personaId);
    }
    
    /**
     * 获取当前会话的人设ID
     * @param sessionId 会话ID
     * @return 人设ID
     */
    public String getCurrentPersonaId(String sessionId) {
        // 简化处理：返回默认人设
        // 实际可以从SessionService中获取
        return personaService.getDefaultPersonaId();
    }
    
    /**
     * 获取所有可用人设列表
     * @return 人设列表
     */
    public List<Persona> getAllPersonas() {
        return personaService.getAllPersonas();
    }
    
    /**
     * 获取活跃人设列表
     * @return 活跃人设列表
     */
    public List<Persona> getActivePersonas() {
        return personaService.getActivePersonas();
    }
    
    // ==================== 统计与管理API ====================
    
    /**
     * 获取知识库统计信息
     * @param sessionId 会话ID
     * @return 统计信息
     */
    public KnowledgeStats getStatistics(String sessionId) {
        KnowledgeStats stats = new KnowledgeStats();
        
        try {
            // 短期记忆统计
            var memoryStats = memoryService.getMemoryStats(sessionId);
            stats.shortTermMemoryCount = memoryStats.getActiveMemories();
            
            // 长期知识统计
            var worldBookStats = worldBookService.getStatistics();
            stats.longTermKnowledgeCount = (int) worldBookStats.get("manualEntries") + 
                                          (int) worldBookStats.get("extractedEntries");
            
            // 人设统计
            stats.personaCount = personaService.getAllPersonas().size();
            
            logger.debug("知识库统计: sessionId={}, stats={}", sessionId, stats);
            
        } catch (Exception e) {
            logger.error("获取知识库统计时发生错误: sessionId={}", sessionId, e);
        }
        
        return stats;
    }
    
    /**
     * 清理会话知识（清理短期记忆，保留长期知识）
     * @param sessionId 会话ID
     */
    public void cleanupSessionKnowledge(String sessionId) {
        try {
            logger.info("清理会话知识: sessionId={}", sessionId);
            // 短期记忆会自动过期，无需手动清理
            // 长期知识保留在世界书中
        } catch (Exception e) {
            logger.error("清理会话知识时发生错误: sessionId={}", sessionId, e);
        }
    }
    
    // ==================== 内部类 ====================
    
    /**
     * 知识上下文
     * 包含人设、短期记忆和长期知识
     */
    public static class KnowledgeContext {
        private String personaPrompt;
        private String shortTermMemory;
        private String longTermKnowledge;
        
        public boolean hasPersonaPrompt() {
            return personaPrompt != null && !personaPrompt.isEmpty();
        }
        
        public boolean hasShortTermMemory() {
            return shortTermMemory != null && !shortTermMemory.isEmpty();
        }
        
        public boolean hasLongTermKnowledge() {
            return longTermKnowledge != null && !longTermKnowledge.isEmpty();
        }
        
        public boolean isEmpty() {
            return !hasPersonaPrompt() && !hasShortTermMemory() && !hasLongTermKnowledge();
        }
        
        /**
         * 转换为格式化的提示词文本
         */
        public String toFormattedString() {
            if (isEmpty()) {
                return "";
            }
            
            StringBuilder formatted = new StringBuilder();
            
            // 1. 人设提示词（作为系统提示）
            if (hasPersonaPrompt()) {
                formatted.append(personaPrompt);
            }
            
            // 2. 短期记忆（最近对话中的记忆）
            if (hasShortTermMemory()) {
                if (formatted.length() > 0) {
                    formatted.append("\n\n");
                }
                formatted.append("【近期记忆】\n");
                formatted.append(shortTermMemory);
            }
            
            // 3. 长期知识（世界书设定）
            if (hasLongTermKnowledge()) {
                if (formatted.length() > 0) {
                    formatted.append("\n\n");
                }
                formatted.append("【相关知识】\n");
                formatted.append(longTermKnowledge);
            }
            
            return formatted.toString();
        }
        
        // Getters and Setters
        public String getPersonaPrompt() { return personaPrompt; }
        public void setPersonaPrompt(String personaPrompt) { this.personaPrompt = personaPrompt; }
        
        public String getShortTermMemory() { return shortTermMemory; }
        public void setShortTermMemory(String shortTermMemory) { this.shortTermMemory = shortTermMemory; }
        
        public String getLongTermKnowledge() { return longTermKnowledge; }
        public void setLongTermKnowledge(String longTermKnowledge) { this.longTermKnowledge = longTermKnowledge; }
        
        @Override
        public String toString() {
            return String.format("KnowledgeContext{persona=%s, shortTerm=%s, longTerm=%s}",
                               hasPersonaPrompt(), hasShortTermMemory(), hasLongTermKnowledge());
        }
    }
    
    /**
     * 知识库统计信息
     */
    public static class KnowledgeStats {
        private int shortTermMemoryCount = 0;
        private int longTermKnowledgeCount = 0;
        private int personaCount = 0;
        
        public int getShortTermMemoryCount() { return shortTermMemoryCount; }
        public int getLongTermKnowledgeCount() { return longTermKnowledgeCount; }
        public int getPersonaCount() { return personaCount; }
        
        @Override
        public String toString() {
            return String.format("KnowledgeStats{shortTerm=%d, longTerm=%d, persona=%d}",
                               shortTermMemoryCount, longTermKnowledgeCount, personaCount);
        }
    }
}


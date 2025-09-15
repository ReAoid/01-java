package com.chatbot.service;

import com.chatbot.config.ResourceConfig;
import com.chatbot.model.ChatMessage;
import com.chatbot.model.ConversationRecord;
import com.chatbot.model.ConversationStats;
import com.chatbot.util.FileUtil;
import com.chatbot.util.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * 对话历史存储服务
 * 负责将对话记录保存为本地 JSON 文件
 */
@Service
public class ConversationHistoryService {
    
    private static final Logger logger = LoggerFactory.getLogger(ConversationHistoryService.class);
    
    private final ResourceConfig resourceConfig;
    
    // 内存中的对话记录缓存
    private final ConcurrentHashMap<String, ConversationRecord> activeConversations = new ConcurrentHashMap<>();
    
    // 文件名时间格式
    private static final DateTimeFormatter FILE_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private static final DateTimeFormatter DIR_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy/MM/dd");
    
    public ConversationHistoryService(ResourceConfig resourceConfig) {
        this.resourceConfig = resourceConfig;
        initializeHistoryDirectory();
    }
    
    /**
     * 初始化历史记录目录
     */
    private void initializeHistoryDirectory() {
        try {
            String historyDir = getHistoryBaseDir();
            Path historyPath = Paths.get(historyDir);
            if (!Files.exists(historyPath)) {
                Files.createDirectories(historyPath);
                logger.info("创建对话历史目录: {}", historyDir);
            }
        } catch (IOException e) {
            logger.error("初始化对话历史目录失败", e);
        }
    }
    
    /**
     * 开始记录新对话
     */
    public void startConversation(String sessionId) {
        logger.debug("开始记录对话，sessionId: {}", sessionId);
        ConversationRecord record = new ConversationRecord(sessionId);
        activeConversations.put(sessionId, record);
    }
    
    /**
     * 添加消息到对话记录
     */
    public void addMessage(String sessionId, ChatMessage message) {
        ConversationRecord record = activeConversations.get(sessionId);
        if (record == null) {
            logger.debug("会话记录不存在，创建新记录，sessionId: {}", sessionId);
            startConversation(sessionId);
            record = activeConversations.get(sessionId);
        }
        
        // 添加消息到记录
        record.addMessage(
            message.getTimestamp() != null ? message.getTimestamp() : LocalDateTime.now(),
            message.getSender() != null ? message.getSender() : "unknown",
            message.getContent()
        );
        
        logger.debug("添加消息到对话记录，sessionId: {}, role: {}, contentLength: {}", 
                    sessionId, message.getSender(), 
                    message.getContent() != null ? message.getContent().length() : 0);
    }
    
    /**
     * 结束对话并保存到文件
     */
    public String endConversation(String sessionId) {
        ConversationRecord record = activeConversations.remove(sessionId);
        if (record == null) {
            logger.warn("尝试结束不存在的对话记录，sessionId: {}", sessionId);
            return null;
        }
        
        try {
            String filePath = saveConversationToFile(record);
            logger.info("对话记录已保存，sessionId: {}, filePath: {}, messageCount: {}", 
                       sessionId, filePath, record.getMessages().size());
            return filePath;
        } catch (Exception e) {
            logger.error("保存对话记录失败，sessionId: {}", sessionId, e);
            return null;
        }
    }
    
    /**
     * 保存对话记录到文件
     */
    private String saveConversationToFile(ConversationRecord record) throws IOException {
        LocalDateTime now = LocalDateTime.now();
        
        // 生成文件路径：conversations/YYYY/MM/DD/sessionId_YYYYMMDD_HHmmss.json
        String dateDir = now.format(DIR_DATE_FORMAT);
        String fileName = String.format("%s_%s.json", 
                                       record.getSessionId(), 
                                       now.format(FILE_DATE_FORMAT));
        
        String fullDir = getHistoryBaseDir() + File.separator + dateDir;
        String fullPath = fullDir + File.separator + fileName;
        
        // 确保目录存在
        Path dirPath = Paths.get(fullDir);
        if (!Files.exists(dirPath)) {
            Files.createDirectories(dirPath);
        }
        
        // 将记录转换为JSON并保存
        String jsonContent = JsonUtil.toJson(record);
        FileUtil.writeString(fullPath, jsonContent);
        
        return fullPath;
    }
    
    /**
     * 查询指定日期的对话记录
     */
    public List<ConversationRecord> getConversationsByDate(LocalDateTime date) {
        List<ConversationRecord> conversations = new ArrayList<>();
        
        String dateDir = date.format(DIR_DATE_FORMAT);
        String fullDir = getHistoryBaseDir() + File.separator + dateDir;
        Path dirPath = Paths.get(fullDir);
        
        if (!Files.exists(dirPath)) {
            logger.debug("指定日期的对话记录目录不存在: {}", fullDir);
            return conversations;
        }
        
        try (Stream<Path> files = Files.list(dirPath)) {
            files.filter(path -> path.toString().endsWith(".json"))
                 .forEach(path -> {
                     try {
                         String content = Files.readString(path);
                         ConversationRecord record = JsonUtil.fromJson(content, ConversationRecord.class);
                         if (record != null) {
                             conversations.add(record);
                         }
                     } catch (Exception e) {
                         logger.warn("读取对话记录文件失败: {}", path, e);
                     }
                 });
        } catch (IOException e) {
            logger.error("列出对话记录目录失败: {}", fullDir, e);
        }
        
        return conversations;
    }
    
    /**
     * 根据sessionId查询对话记录
     */
    public ConversationRecord getConversationBySessionId(String sessionId) {
        // 首先检查活跃对话
        ConversationRecord activeRecord = activeConversations.get(sessionId);
        if (activeRecord != null) {
            return activeRecord;
        }
        
        // 在历史文件中搜索
        return searchConversationInFiles(sessionId);
    }
    
    /**
     * 在文件中搜索指定sessionId的对话记录
     */
    private ConversationRecord searchConversationInFiles(String sessionId) {
        String historyDir = getHistoryBaseDir();
        Path historyPath = Paths.get(historyDir);
        
        if (!Files.exists(historyPath)) {
            return null;
        }
        
        try (Stream<Path> paths = Files.walk(historyPath)) {
            return paths.filter(path -> path.toString().endsWith(".json"))
                       .filter(path -> path.getFileName().toString().startsWith(sessionId + "_"))
                       .findFirst()
                       .map(path -> {
                           try {
                               String content = Files.readString(path);
                               return JsonUtil.fromJson(content, ConversationRecord.class);
                           } catch (Exception e) {
                               logger.warn("读取对话记录文件失败: {}", path, e);
                               return null;
                           }
                       })
                       .orElse(null);
        } catch (IOException e) {
            logger.error("搜索对话记录失败，sessionId: {}", sessionId, e);
            return null;
        }
    }
    
    /**
     * 获取对话统计信息
     */
    public ConversationStats getConversationStats() {
        ConversationStats stats = new ConversationStats();
        String historyDir = getHistoryBaseDir();
        Path historyPath = Paths.get(historyDir);
        
        if (!Files.exists(historyPath)) {
            return stats;
        }
        
        try (Stream<Path> paths = Files.walk(historyPath)) {
            paths.filter(path -> path.toString().endsWith(".json"))
                 .forEach(path -> {
                     try {
                         String content = Files.readString(path);
                         ConversationRecord record = JsonUtil.fromJson(content, ConversationRecord.class);
                        if (record != null) {
                            stats.setTotalConversations(stats.getTotalConversations() + 1);
                            stats.setTotalMessages(stats.getTotalMessages() + record.getMessages().size());
                            stats.setTotalFileSize(stats.getTotalFileSize() + Files.size(path));
                        }
                     } catch (Exception e) {
                         logger.warn("读取统计信息时文件解析失败: {}", path, e);
                     }
                 });
        } catch (IOException e) {
            logger.error("获取对话统计信息失败", e);
        }
        
        return stats;
    }
    
    /**
     * 获取历史记录基础目录
     */
    private String getHistoryBaseDir() {
        return resourceConfig.getFullPath("data/conversations");
    }
    
    /**
     * 获取当前活跃对话数量
     */
    public int getActiveConversationCount() {
        return activeConversations.size();
    }
    
}

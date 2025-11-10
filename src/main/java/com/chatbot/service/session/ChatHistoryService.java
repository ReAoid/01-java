package com.chatbot.service.session;

import com.chatbot.config.AppConfig;
import com.chatbot.config.properties.ResourceProperties;
import com.chatbot.model.domain.ChatMessage;
import com.chatbot.model.record.ConversationRecord;
import com.chatbot.model.record.ConversationStats;
import com.chatbot.model.record.SimpleChatMessageRecord;
import com.chatbot.util.FileUtil;
import com.chatbot.util.JsonUtil;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
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
 * 统一的聊天历史管理服务
 * 整合了 ConversationHistoryService 和 SessionHistoryService 的功能
 * 支持多种存储策略：按会话ID存储、按日期存储
 */
@Service
public class ChatHistoryService {
    
    private static final Logger logger = LoggerFactory.getLogger(ChatHistoryService.class);
    
    private final ResourceProperties resourceConfig;
    
    // 内存中的活跃对话缓存
    private final ConcurrentHashMap<String, List<ChatMessage>> activeConversations = new ConcurrentHashMap<>();
    
    // 文件名时间格式
    private static final DateTimeFormatter FILE_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private static final DateTimeFormatter DIR_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy/MM/dd");
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    /**
     * 存储策略枚举
     */
    public enum StorageStrategy {
        BY_SESSION,  // 按会话ID存储（默认）：sessions/{sessionId}_history.json
        BY_DATE      // 按日期存储（归档）：conversations/YYYY/MM/DD/*.json
    }
    
    public ChatHistoryService(AppConfig appConfig) {
        this.resourceConfig = appConfig.getResource();
        initializeHistoryDirectories();
    }
    
    /**
     * 初始化历史记录目录
     */
    private void initializeHistoryDirectories() {
        try {
            // 初始化按会话存储的目录
            String sessionsDir = getSessionsDir();
            Path sessionsPath = Paths.get(sessionsDir);
            if (!Files.exists(sessionsPath)) {
                Files.createDirectories(sessionsPath);
                logger.info("创建会话历史目录: {}", sessionsDir);
            }
            
            // 初始化按日期存储的目录
            String conversationsDir = getConversationsBaseDir();
            Path conversationsPath = Paths.get(conversationsDir);
            if (!Files.exists(conversationsPath)) {
                Files.createDirectories(conversationsPath);
                logger.info("创建对话历史目录: {}", conversationsDir);
            }
        } catch (IOException e) {
            logger.error("初始化历史记录目录失败", e);
        }
    }
    
    // ==================== 核心API ====================
    
    /**
     * 开始记录新对话（在内存中）
     * @param sessionId 会话ID
     */
    public void startConversation(String sessionId) {
        activeConversations.put(sessionId, new ArrayList<>());
        logger.debug("开始记录对话，sessionId: {}", sessionId);
    }
    
    /**
     * 添加消息到对话记录（内存中）
     * @param sessionId 会话ID
     * @param message 消息
     */
    public void addMessage(String sessionId, ChatMessage message) {
        List<ChatMessage> messages = activeConversations.get(sessionId);
        if (messages == null) {
            logger.debug("会话记录不存在，创建新记录，sessionId: {}", sessionId);
            startConversation(sessionId);
            messages = activeConversations.get(sessionId);
        }
        
        messages.add(message);
        logger.trace("添加消息到会话，sessionId: {}, role: {}", sessionId, message.getRole());
    }
    
    /**
     * 添加消息并立即保存到文件
     * @param sessionId 会话ID
     * @param message 消息
     */
    public void addMessageAndSave(String sessionId, ChatMessage message) {
        List<ChatMessage> history = loadHistory(sessionId);
        history.add(message);
        saveHistory(sessionId, history);
    }
    
    /**
     * 保存历史记录（默认策略：按会话ID存储）
     * @param sessionId 会话ID
     * @param messages 消息列表
     */
    public void saveHistory(String sessionId, List<ChatMessage> messages) {
        saveHistory(sessionId, messages, StorageStrategy.BY_SESSION);
    }
    
    /**
     * 保存历史记录（指定存储策略）
     * @param sessionId 会话ID
     * @param messages 消息列表
     * @param strategy 存储策略
     */
    public void saveHistory(String sessionId, List<ChatMessage> messages, StorageStrategy strategy) {
        try {
            switch (strategy) {
                case BY_SESSION:
                    saveBySession(sessionId, messages);
                    break;
                case BY_DATE:
                    saveByDate(sessionId, messages);
                    break;
            }
            logger.info("保存会话历史成功，sessionId: {}, 消息数量: {}, 策略: {}", 
                       sessionId, messages.size(), strategy);
        } catch (Exception e) {
            logger.error("保存会话历史失败，sessionId: {}, 策略: {}", sessionId, strategy, e);
        }
    }
    
    /**
     * 加载历史记录（优先从BY_SESSION加载）
     * @param sessionId 会话ID
     * @return 消息列表
     */
    public List<ChatMessage> loadHistory(String sessionId) {
        // 1. 先检查内存缓存
        List<ChatMessage> cached = activeConversations.get(sessionId);
        if (cached != null && !cached.isEmpty()) {
            logger.debug("从内存缓存加载历史，sessionId: {}, 消息数: {}", sessionId, cached.size());
            return new ArrayList<>(cached);
        }
        
        // 2. 从按会话存储的文件加载
        List<ChatMessage> sessionHistory = loadBySession(sessionId);
        if (!sessionHistory.isEmpty()) {
            logger.debug("从会话文件加载历史，sessionId: {}, 消息数: {}", sessionId, sessionHistory.size());
            return sessionHistory;
        }
        
        // 3. 如果找不到，尝试从按日期存储的文件加载（最近7天）
        List<ChatMessage> dateHistory = searchByDate(sessionId, 7);
        if (!dateHistory.isEmpty()) {
            logger.debug("从日期文件加载历史，sessionId: {}, 消息数: {}", sessionId, dateHistory.size());
            return dateHistory;
        }
        
        logger.debug("未找到历史记录，sessionId: {}", sessionId);
        return new ArrayList<>();
    }
    
    /**
     * 获取最近N条消息
     * @param sessionId 会话ID
     * @param limit 数量限制
     * @return 消息列表
     */
    public List<ChatMessage> getRecentMessages(String sessionId, int limit) {
        List<ChatMessage> history = loadHistory(sessionId);
        
        if (history.size() <= limit) {
            return history;
        }
        
        return history.subList(history.size() - limit, history.size());
    }
    
    /**
     * 结束对话并保存（从内存清除，保存到两种格式）
     * @param sessionId 会话ID
     * @return 保存的文件路径
     */
    public String endConversation(String sessionId) {
        List<ChatMessage> messages = activeConversations.remove(sessionId);
        if (messages == null || messages.isEmpty()) {
            logger.warn("尝试结束不存在或为空的对话记录，sessionId: {}", sessionId);
            return null;
        }
        
        try {
            // 1. 按会话ID保存（主要存储）
            saveBySession(sessionId, messages);
            
            // 2. 按日期保存（归档备份）
            String archivePath = saveByDate(sessionId, messages);
            
            logger.info("对话记录已保存，sessionId: {}, 消息数量: {}", sessionId, messages.size());
            return archivePath;
        } catch (Exception e) {
            logger.error("保存对话记录失败，sessionId: {}", sessionId, e);
            return null;
        }
    }
    
    /**
     * 删除历史记录
     * @param sessionId 会话ID
     * @return 是否删除成功
     */
    public boolean deleteHistory(String sessionId) {
        try {
            // 1. 从内存删除
            activeConversations.remove(sessionId);
            
            // 2. 删除会话文件
            String sessionFilePath = getSessionFilePath(sessionId);
            Path sessionPath = Paths.get(sessionFilePath);
            if (Files.exists(sessionPath)) {
                Files.delete(sessionPath);
                logger.info("删除会话历史文件成功，sessionId: {}", sessionId);
            }
            
            return true;
        } catch (Exception e) {
            logger.error("删除会话历史失败，sessionId: {}", sessionId, e);
            return false;
        }
    }
    
    /**
     * 检查历史记录是否存在
     * @param sessionId 会话ID
     * @return 是否存在
     */
    public boolean hasHistory(String sessionId) {
        // 1. 检查内存
        if (activeConversations.containsKey(sessionId)) {
            return true;
        }
        
        // 2. 检查文件
        String filePath = getSessionFilePath(sessionId);
        return Files.exists(Paths.get(filePath));
    }
    
    /**
     * 获取活跃对话数量
     * @return 数量
     */
    public int getActiveConversationCount() {
        return activeConversations.size();
    }
    
    /**
     * 获取历史统计信息
     * @return 统计信息
     */
    public HistoryStats getStatistics() {
        HistoryStats stats = new HistoryStats();
        
        try {
            // 统计会话文件
            String sessionsDir = getSessionsDir();
            Path sessionsPath = Paths.get(sessionsDir);
            if (Files.exists(sessionsPath)) {
                Files.walk(sessionsPath, 1)
                     .filter(path -> path.toString().endsWith(".json"))
                     .forEach(path -> {
                         try {
                             stats.totalSessions++;
                             stats.totalFileSize += Files.size(path);
                             
                             String content = Files.readString(path);
                             List<SimpleMessage> messages = JsonUtil.fromJsonToList(content, SimpleMessage.class);
                             if (messages != null) {
                                 stats.totalMessages += messages.size();
                             }
                         } catch (Exception e) {
                             logger.warn("读取统计信息失败: {}", path, e);
                         }
                     });
            }
            
            // 添加活跃对话统计
            stats.activeConversations = activeConversations.size();
            activeConversations.values().forEach(messages -> 
                stats.totalMessages += messages.size()
            );
            
        } catch (Exception e) {
            logger.error("获取历史统计信息失败", e);
        }
        
        return stats;
    }
    
    // ==================== 内部实现 ====================
    
    /**
     * 按会话ID存储
     */
    private void saveBySession(String sessionId, List<ChatMessage> messages) throws IOException {
        String filePath = getSessionFilePath(sessionId);
        
        // 转换为简化格式
        List<SimpleMessage> simpleMessages = convertToSimpleMessages(messages);
        
        // 保存为格式化JSON
        String jsonContent = JsonUtil.toPrettyJson(simpleMessages);
        FileUtil.writeString(filePath, jsonContent);
        
        logger.debug("按会话保存历史，sessionId: {}, 文件: {}, 消息数: {}", 
                    sessionId, filePath, messages.size());
    }
    
    /**
     * 按日期存储
     */
    private String saveByDate(String sessionId, List<ChatMessage> messages) throws IOException {
        LocalDateTime now = LocalDateTime.now();
        
        // 生成文件路径：conversations/YYYY/MM/DD/sessionId_HHmmss.json
        String dateDir = now.format(DIR_DATE_FORMAT);
        String fileName = String.format("%s_%s.json", sessionId, now.format(FILE_DATE_FORMAT));
        
        String fullDir = getConversationsBaseDir() + File.separator + dateDir;
        String fullPath = fullDir + File.separator + fileName;
        
        // 确保目录存在
        Path dirPath = Paths.get(fullDir);
        if (!Files.exists(dirPath)) {
            Files.createDirectories(dirPath);
        }
        
        // 转换为简化格式
        List<SimpleMessage> simpleMessages = convertToSimpleMessages(messages);
        
        // 保存为格式化JSON
        String jsonContent = JsonUtil.toPrettyJson(simpleMessages);
        FileUtil.writeString(fullPath, jsonContent);
        
        logger.debug("按日期保存历史，sessionId: {}, 文件: {}, 消息数: {}", 
                    sessionId, fullPath, messages.size());
        
        return fullPath;
    }
    
    /**
     * 从会话文件加载
     */
    private List<ChatMessage> loadBySession(String sessionId) {
        try {
            String filePath = getSessionFilePath(sessionId);
            Path path = Paths.get(filePath);
            
            if (!Files.exists(path)) {
                return new ArrayList<>();
            }
            
            String jsonContent = FileUtil.readString(filePath);
            List<SimpleMessage> simpleMessages = JsonUtil.fromJsonToList(jsonContent, SimpleMessage.class);
            
            if (simpleMessages != null) {
                return convertToChatMessages(simpleMessages, sessionId);
            }
        } catch (Exception e) {
            logger.error("从会话文件加载历史失败，sessionId: {}", sessionId, e);
        }
        
        return new ArrayList<>();
    }
    
    /**
     * 从日期文件搜索并加载（搜索最近N天）
     */
    private List<ChatMessage> searchByDate(String sessionId, int recentDays) {
        try {
            String conversationsDir = getConversationsBaseDir();
            Path basePath = Paths.get(conversationsDir);
            
            if (!Files.exists(basePath)) {
                return new ArrayList<>();
            }
            
            // 搜索最近N天的文件
            LocalDateTime startDate = LocalDateTime.now().minusDays(recentDays);
            
            return Files.walk(basePath)
                        .filter(path -> path.toString().endsWith(".json"))
                        .filter(path -> path.getFileName().toString().startsWith(sessionId + "_"))
                        .filter(path -> {
                            try {
                                return Files.getLastModifiedTime(path).toInstant()
                                        .isAfter(startDate.atZone(java.time.ZoneId.systemDefault()).toInstant());
                            } catch (IOException e) {
                                return false;
                            }
                        })
                        .findFirst()
                        .map(path -> {
                            try {
                                String content = Files.readString(path);
                                List<SimpleMessage> messages = JsonUtil.fromJsonToList(content, SimpleMessage.class);
                                return convertToChatMessages(messages, sessionId);
                            } catch (Exception e) {
                                logger.warn("读取日期文件失败: {}", path, e);
                                return new ArrayList<ChatMessage>();
                            }
                        })
                        .orElse(new ArrayList<>());
                        
        } catch (Exception e) {
            logger.error("从日期文件搜索历史失败，sessionId: {}", sessionId, e);
            return new ArrayList<>();
        }
    }
    
    /**
     * 转换为简化消息格式
     */
    private List<SimpleMessage> convertToSimpleMessages(List<ChatMessage> messages) {
        List<SimpleMessage> simpleMessages = new ArrayList<>();
        
        for (ChatMessage msg : messages) {
            if (msg.getContent() != null && !msg.getContent().trim().isEmpty()) {
                String timestamp = msg.getTimestamp() != null 
                    ? msg.getTimestamp().format(TIMESTAMP_FORMAT)
                    : LocalDateTime.now().format(TIMESTAMP_FORMAT);
                
                SimpleMessage simple = new SimpleMessage(
                    msg.getType() != null ? msg.getType() : "text",
                    msg.getContent(),
                    msg.getRole(),
                    timestamp
                );
                
                simpleMessages.add(simple);
            }
        }
        
        return simpleMessages;
    }
    
    /**
     * 转换为ChatMessage格式
     */
    private List<ChatMessage> convertToChatMessages(List<SimpleMessage> simpleMessages, String sessionId) {
        List<ChatMessage> chatMessages = new ArrayList<>();
        
        if (simpleMessages == null) {
            return chatMessages;
        }
        
        for (SimpleMessage simple : simpleMessages) {
            ChatMessage chatMessage = new ChatMessage();
            chatMessage.setType(simple.getType());
            chatMessage.setContent(simple.getContent());
            chatMessage.setRole(simple.getRole());
            chatMessage.setSessionId(sessionId);
            
            try {
                chatMessage.setTimestamp(LocalDateTime.parse(simple.getTimestamp(), TIMESTAMP_FORMAT));
            } catch (Exception e) {
                chatMessage.setTimestamp(LocalDateTime.now());
            }
            
            chatMessages.add(chatMessage);
        }
        
        return chatMessages;
    }
    
    /**
     * 获取会话文件路径
     */
    private String getSessionFilePath(String sessionId) {
        return getSessionsDir() + File.separator + sessionId + "_history.json";
    }
    
    /**
     * 获取sessions目录
     */
    private String getSessionsDir() {
        String sessionsPath = resourceConfig.getSessionsPath();
        
        // 强制检查并修正异常路径
        if (sessionsPath == null || sessionsPath.contains("null")) {
            logger.warn("检测到sessions路径异常: {}, 使用默认路径", sessionsPath);
            sessionsPath = "src/main/resources/data/sessions";
        }
        
        return sessionsPath;
    }
    
    /**
     * 获取conversations基础目录
     */
    private String getConversationsBaseDir() {
        return resourceConfig.getFullPath("data/conversations");
    }
    
    // ==================== 内部类 ====================
    
    /**
     * 简化的消息模型 - 用于JSON序列化
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class SimpleMessage {
        private String type;
        private String content;
        private String role;
        
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private String timestamp;
        
        public SimpleMessage() {}
        
        public SimpleMessage(String type, String content, String role, String timestamp) {
            this.type = type;
            this.content = content;
            this.role = role;
            this.timestamp = timestamp;
        }
        
        // Getters and Setters
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        
        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
        
        public String getTimestamp() { return timestamp; }
        public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
    }
    
    /**
     * 历史统计信息
     */
    public static class HistoryStats {
        private int totalSessions = 0;
        private int totalMessages = 0;
        private long totalFileSize = 0;
        private int activeConversations = 0;
        
        public int getTotalSessions() { return totalSessions; }
        public int getTotalMessages() { return totalMessages; }
        public long getTotalFileSize() { return totalFileSize; }
        public int getActiveConversations() { return activeConversations; }
        
        @Override
        public String toString() {
            return String.format("HistoryStats{sessions=%d, messages=%d, fileSize=%d bytes, active=%d}",
                               totalSessions, totalMessages, totalFileSize, activeConversations);
        }
    }
    
    // ==================== ConversationHistoryService 兼容API ====================
    
    /**
     * 根据sessionId查询对话记录（ConversationHistoryService兼容）
     * @param sessionId 会话ID
     * @return 对话记录
     */
    public ConversationRecord getConversationBySessionId(String sessionId) {
        try {
            // 首先检查活跃对话
            List<ChatMessage> activeMessages = activeConversations.get(sessionId);
            if (activeMessages != null && !activeMessages.isEmpty()) {
                return convertToConversationRecord(sessionId, activeMessages);
            }
            
            // 在历史文件中搜索
            List<ChatMessage> historyMessages = loadHistory(sessionId);
            if (!historyMessages.isEmpty()) {
                return convertToConversationRecord(sessionId, historyMessages);
            }
            
            return null;
        } catch (Exception e) {
            logger.error("查询对话记录失败，sessionId: {}", sessionId, e);
            return null;
        }
    }
    
    /**
     * 查询指定日期的对话记录列表（ConversationHistoryService兼容）
     * @param date 日期
     * @return 对话记录列表
     */
    public List<ConversationRecord> getConversationsByDate(LocalDateTime date) {
        List<ConversationRecord> conversations = new ArrayList<>();
        
        String dateDir = date.format(DIR_DATE_FORMAT);
        String fullDir = getConversationsBaseDir() + File.separator + dateDir;
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
                         List<SimpleMessage> messages = JsonUtil.fromJsonToList(content, SimpleMessage.class);
                         if (messages != null && !messages.isEmpty()) {
                             // 从文件名提取sessionId
                             String fileName = path.getFileName().toString();
                             String sessionId = fileName.substring(0, fileName.indexOf('_'));
                             
                             ConversationRecord record = new ConversationRecord(sessionId);
                             for (SimpleMessage msg : messages) {
                                 SimpleChatMessageRecord chatMsg = new SimpleChatMessageRecord();
                                 try {
                                     chatMsg.setTimestamp(LocalDateTime.parse(msg.getTimestamp(), TIMESTAMP_FORMAT));
                                 } catch (Exception e) {
                                     chatMsg.setTimestamp(LocalDateTime.now());
                                 }
                                 chatMsg.setRole(msg.getRole());
                                 chatMsg.setContent(msg.getContent());
                                 record.getMessages().add(chatMsg);
                             }
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
     * 获取对话统计信息（ConversationHistoryService兼容）
     * @return 统计信息
     */
    public ConversationStats getConversationStats() {
        ConversationStats stats = new ConversationStats();
        
        try {
            // 统计会话文件
            String sessionsDir = getSessionsDir();
            Path sessionsPath = Paths.get(sessionsDir);
            if (Files.exists(sessionsPath)) {
                Files.walk(sessionsPath, 1)
                     .filter(path -> path.toString().endsWith(".json"))
                     .forEach(path -> {
                         try {
                             stats.setTotalConversations(stats.getTotalConversations() + 1);
                             stats.setTotalFileSize(stats.getTotalFileSize() + Files.size(path));
                             
                             String content = Files.readString(path);
                             List<SimpleMessage> messages = JsonUtil.fromJsonToList(content, SimpleMessage.class);
                             if (messages != null) {
                                 stats.setTotalMessages(stats.getTotalMessages() + messages.size());
                             }
                         } catch (Exception e) {
                             logger.warn("读取统计信息失败: {}", path, e);
                         }
                     });
            }
        } catch (Exception e) {
            logger.error("获取对话统计信息失败", e);
        }
        
        return stats;
    }
    
    // ==================== SessionHistoryService 兼容API ====================
    
    /**
     * 加载指定会话的历史记录（SessionHistoryService兼容）
     * @param sessionId 会话ID
     * @return 消息列表
     */
    public List<ChatMessage> loadSessionHistory(String sessionId) {
        return loadHistory(sessionId);
    }
    
    /**
     * 保存会话历史到文件（SessionHistoryService兼容）
     * @param sessionId 会话ID
     * @param messages 消息列表
     */
    public void saveSessionHistory(String sessionId, List<ChatMessage> messages) {
        saveHistory(sessionId, messages, StorageStrategy.BY_SESSION);
    }
    
    /**
     * 检查会话历史文件是否存在（SessionHistoryService兼容）
     * @param sessionId 会话ID
     * @return 是否存在
     */
    public boolean hasSessionHistory(String sessionId) {
        return hasHistory(sessionId);
    }
    
    /**
     * 删除会话历史文件（SessionHistoryService兼容）
     * @param sessionId 会话ID
     * @return 是否删除成功
     */
    public boolean deleteSessionHistory(String sessionId) {
        return deleteHistory(sessionId);
    }
    
    // ==================== 辅助方法 ====================
    
    /**
     * 将ChatMessage列表转换为ConversationRecord
     */
    private ConversationRecord convertToConversationRecord(String sessionId, List<ChatMessage> messages) {
        ConversationRecord record = new ConversationRecord(sessionId);
        
        for (ChatMessage msg : messages) {
            if (msg.getContent() != null && !msg.getContent().trim().isEmpty()) {
                SimpleChatMessageRecord chatMsg = new SimpleChatMessageRecord();
                chatMsg.setTimestamp(msg.getTimestamp() != null ? msg.getTimestamp() : LocalDateTime.now());
                chatMsg.setRole(msg.getRole() != null ? msg.getRole() : "unknown");
                chatMsg.setContent(msg.getContent());
                record.getMessages().add(chatMsg);
            }
        }
        
        return record;
    }
}


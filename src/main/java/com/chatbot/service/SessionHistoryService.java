package com.chatbot.service;

import com.chatbot.config.ResourceConfig;
import com.chatbot.model.ChatMessage;
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

/**
 * 会话历史管理服务
 * 负责将对话保存到 sessions/{sessionId}_history.json 文件
 * 并在用户提问时读取历史记录作为上下文
 */
@Service
public class SessionHistoryService {
    
    private static final Logger logger = LoggerFactory.getLogger(SessionHistoryService.class);
    
    private final ResourceConfig resourceConfig;
    
    public SessionHistoryService(ResourceConfig resourceConfig) {
        this.resourceConfig = resourceConfig;
        initializeSessionsDirectory();
    }
    
    /**
     * 初始化sessions目录
     */
    private void initializeSessionsDirectory() {
        try {
            String sessionsDir = getSessionsDir();
            Path sessionsPath = Paths.get(sessionsDir);
            if (!Files.exists(sessionsPath)) {
                Files.createDirectories(sessionsPath);
                logger.info("创建会话历史目录: {}", sessionsDir);
            }
        } catch (IOException e) {
            logger.error("初始化会话历史目录失败", e);
        }
    }
    
    /**
     * 获取sessions目录路径
     */
    private String getSessionsDir() {
        return resourceConfig.getSessionsPath();
    }
    
    /**
     * 获取指定会话的历史文件路径
     */
    private String getHistoryFilePath(String sessionId) {
        return getSessionsDir() + File.separator + sessionId + "_history.json";
    }
    
    /**
     * 加载指定会话的历史记录
     */
    public List<ChatMessage> loadSessionHistory(String sessionId) {
        String filePath = getHistoryFilePath(sessionId);
        
        try {
            if (!Files.exists(Paths.get(filePath))) {
                logger.debug("会话历史文件不存在，返回空列表，sessionId: {}", sessionId);
                return new ArrayList<>();
            }
            
            String jsonContent = FileUtil.readString(filePath);
            List<SimpleMessage> simpleMessages = JsonUtil.fromJsonToList(jsonContent, SimpleMessage.class);
            
            if (simpleMessages != null) {
                List<ChatMessage> chatMessages = new ArrayList<>();
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                
                for (SimpleMessage simple : simpleMessages) {
                    ChatMessage chatMessage = new ChatMessage();
                    chatMessage.setType(simple.getType());
                    chatMessage.setContent(simple.getContent());
                    chatMessage.setRole(simple.getRole());
                    chatMessage.setSessionId(sessionId);
                    
                    try {
                        chatMessage.setTimestamp(LocalDateTime.parse(simple.getTimestamp(), formatter));
                    } catch (Exception e) {
                        chatMessage.setTimestamp(LocalDateTime.now());
                    }
                    
                    chatMessages.add(chatMessage);
                }
                
                logger.info("加载会话历史成功，sessionId: {}, 消息数量: {}", sessionId, chatMessages.size());
                return chatMessages;
            }
            
        } catch (Exception e) {
            logger.error("加载会话历史失败，sessionId: {}", sessionId, e);
        }
        
        return new ArrayList<>();
    }
    
    /**
     * 保存会话历史到文件
     */
    public void saveSessionHistory(String sessionId, List<ChatMessage> messages) {
        String filePath = getHistoryFilePath(sessionId);
        
        try {
            List<SimpleMessage> simpleMessages = new ArrayList<>();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            
            for (ChatMessage msg : messages) {
                if (msg.getContent() != null && !msg.getContent().trim().isEmpty()) {
                    String content = msg.getContent();
                    
                    // 注意：不在这里过滤思考内容，因为在saveCompleteResponse中已经根据用户偏好处理过了
                    // 直接使用传入的内容，避免双重过滤
                    
                    String timestamp = msg.getTimestamp() != null 
                        ? msg.getTimestamp().format(formatter)
                        : LocalDateTime.now().format(formatter);
                    
                    SimpleMessage simple = new SimpleMessage(
                        msg.getType() != null ? msg.getType() : "text",
                        content,
                        msg.getRole(),
                        timestamp
                    );
                    
                    simpleMessages.add(simple);
                    
                    logger.debug("添加消息到历史记录: role={}, contentLength={}, timestamp={}", 
                               msg.getRole(), content.length(), timestamp);
                }
            }
            
            String jsonContent = JsonUtil.toJson(simpleMessages);
            FileUtil.writeString(filePath, jsonContent);
            
            logger.info("保存会话历史成功，sessionId: {}, 文件路径: {}, 消息数量: {}", 
                       sessionId, filePath, simpleMessages.size());
            
        } catch (Exception e) {
            logger.error("保存会话历史失败，sessionId: {}, 文件路径: {}", sessionId, filePath, e);
        }
    }
    
    /**
     * 添加消息到会话历史并保存
     */
    public void addMessageAndSave(String sessionId, ChatMessage message) {
        List<ChatMessage> history = loadSessionHistory(sessionId);
        history.add(message);
        saveSessionHistory(sessionId, history);
    }
    
    /**
     * 获取会话历史的最后N条消息
     */
    public List<ChatMessage> getRecentMessages(String sessionId, int limit) {
        List<ChatMessage> history = loadSessionHistory(sessionId);
        
        if (history.size() <= limit) {
            return history;
        }
        
        return history.subList(history.size() - limit, history.size());
    }
    
    /**
     * 检查会话历史文件是否存在
     */
    public boolean hasSessionHistory(String sessionId) {
        String filePath = getHistoryFilePath(sessionId);
        return Files.exists(Paths.get(filePath));
    }
    
    /**
     * 删除会话历史文件
     */
    public boolean deleteSessionHistory(String sessionId) {
        String filePath = getHistoryFilePath(sessionId);
        
        try {
            Path path = Paths.get(filePath);
            if (Files.exists(path)) {
                Files.delete(path);
                logger.info("删除会话历史文件成功，sessionId: {}, 文件路径: {}", sessionId, filePath);
                return true;
            }
        } catch (IOException e) {
            logger.error("删除会话历史文件失败，sessionId: {}, 文件路径: {}", sessionId, filePath, e);
        }
        
        return false;
    }
    
    /**
     * 过滤思考内容，只保留AI回答的正文部分
     */
    private String filterThinkingContent(String content) {
        if (content == null) {
            return null;
        }
        
        // 如果不包含思考标签，直接返回
        if (!content.contains("<think>") && !content.contains("</think>")) {
            return content;
        }
        
        StringBuilder result = new StringBuilder();
        String[] lines = content.split("\n");
        boolean inThinkingBlock = false;
        
        for (String line : lines) {
            // 检查是否进入思考块
            if (line.contains("<think>")) {
                inThinkingBlock = true;
                // 如果这一行在<think>之前还有内容，保留它
                int thinkIndex = line.indexOf("<think>");
                if (thinkIndex > 0) {
                    String beforeThink = line.substring(0, thinkIndex).trim();
                    if (!beforeThink.isEmpty()) {
                        result.append(beforeThink).append("\n");
                    }
                }
                continue;
            }
            
            // 检查是否退出思考块
            if (line.contains("</think>")) {
                inThinkingBlock = false;
                // 如果这一行在</think>之后还有内容，保留它
                int endThinkIndex = line.indexOf("</think>");
                if (endThinkIndex + 8 < line.length()) {
                    String afterThink = line.substring(endThinkIndex + 8).trim();
                    if (!afterThink.isEmpty()) {
                        result.append(afterThink).append("\n");
                    }
                }
                continue;
            }
            
            // 如果不在思考块中，保留这一行
            if (!inThinkingBlock) {
                result.append(line).append("\n");
            }
        }
        
        // 清理结果
        String filtered = result.toString().trim();
        return filtered.isEmpty() ? null : filtered;
    }
    
    /**
     * 简化的消息模型 - 只保留核心信息
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
}

package com.chatbot.controller;

import com.chatbot.model.record.ConversationRecord;
import com.chatbot.model.record.ConversationStats;
import com.chatbot.service.ChatHistoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 对话历史控制器
 * 提供对话记录的查询和统计功能
 */
@RestController
@RequestMapping("/api/conversation-history")
public class ChatHistoryController {
    
    private static final Logger logger = LoggerFactory.getLogger(ChatHistoryController.class);
    
    private final ChatHistoryService chatHistoryService;
    
    public ChatHistoryController(ChatHistoryService chatHistoryService) {
        this.chatHistoryService = chatHistoryService;
    }
    
    /**
     * 根据sessionId查询对话记录
     */
    @GetMapping("/session/{sessionId}")
    public ResponseEntity<Map<String, Object>> getConversationBySessionId(@PathVariable String sessionId) {
        logger.info("查询对话记录，sessionId: {}", sessionId);
        
        try {
            ConversationRecord record = chatHistoryService.getConversationBySessionId(sessionId);
            
            Map<String, Object> response = new HashMap<>();
            if (record != null) {
                response.put("success", true);
                response.put("data", record);
                response.put("message", "对话记录查询成功");
                logger.debug("找到对话记录，sessionId: {}, messageCount: {}", sessionId, record.getMessages().size());
            } else {
                response.put("success", false);
                response.put("data", null);
                response.put("message", "未找到指定的对话记录");
                logger.debug("未找到对话记录，sessionId: {}", sessionId);
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("查询对话记录失败，sessionId: {}", sessionId, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("data", null);
            errorResponse.put("message", "查询对话记录时发生错误: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    /**
     * 根据日期查询对话记录列表
     */
    @GetMapping("/date")
    public ResponseEntity<Map<String, Object>> getConversationsByDate(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime date) {
        logger.info("查询指定日期的对话记录，date: {}", date);
        
        try {
            List<ConversationRecord> records = chatHistoryService.getConversationsByDate(date);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", records);
            response.put("count", records.size());
            response.put("message", String.format("找到 %d 条对话记录", records.size()));
            
            logger.debug("查询日期对话记录完成，date: {}, count: {}", date, records.size());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("查询日期对话记录失败，date: {}", date, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("data", null);
            errorResponse.put("message", "查询日期对话记录时发生错误: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    /**
     * 获取对话统计信息
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getConversationStats() {
        logger.info("获取对话统计信息");
        
        try {
            ConversationStats stats = chatHistoryService.getConversationStats();
            int activeCount = chatHistoryService.getActiveConversationCount();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            
            Map<String, Object> statsData = new HashMap<>();
            statsData.put("total_conversations", stats.getTotalConversations());
            statsData.put("total_messages", stats.getTotalMessages());
            statsData.put("total_file_size", stats.getTotalFileSize());
            statsData.put("formatted_file_size", stats.getFormattedFileSize());
            statsData.put("active_conversations", activeCount);
            statsData.put("avg_messages_per_conversation", 
                         stats.getTotalConversations() > 0 ? 
                         (double) stats.getTotalMessages() / stats.getTotalConversations() : 0);
            
            response.put("data", statsData);
            response.put("message", "统计信息获取成功");
            
            logger.debug("对话统计信息: 总对话数={}, 总消息数={}, 文件大小={}, 活跃对话数={}", 
                        stats.getTotalConversations(), stats.getTotalMessages(), 
                        stats.getFormattedFileSize(), activeCount);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("获取对话统计信息失败", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("data", null);
            errorResponse.put("message", "获取统计信息时发生错误: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    /**
     * 结束指定会话的对话记录（手动保存）
     */
    @PostMapping("/session/{sessionId}/end")
    public ResponseEntity<Map<String, Object>> endConversation(@PathVariable String sessionId) {
        logger.info("手动结束对话记录，sessionId: {}", sessionId);
        
        try {
            String filePath = chatHistoryService.endConversation(sessionId);
            
            Map<String, Object> response = new HashMap<>();
            if (filePath != null) {
                response.put("success", true);
                response.put("file_path", filePath);
                response.put("message", "对话记录已保存");
                logger.info("对话记录保存成功，sessionId: {}, filePath: {}", sessionId, filePath);
            } else {
                response.put("success", false);
                response.put("file_path", null);
                response.put("message", "未找到活跃的对话记录或保存失败");
                logger.warn("对话记录保存失败，sessionId: {}", sessionId);
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("结束对话记录失败，sessionId: {}", sessionId, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("file_path", null);
            errorResponse.put("message", "结束对话记录时发生错误: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
}

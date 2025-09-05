package com.chatbot.controller;

import com.chatbot.service.SessionService;
import com.chatbot.service.MemoryService;
import com.chatbot.service.PersonaService;
import com.chatbot.websocket.ChatWebSocketHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 系统管理控制器
 * 提供系统状态查询和管理接口
 */
@RestController
@RequestMapping("/api/system")
public class SystemController {
    
    private final SessionService sessionService;
    private final MemoryService memoryService;
    private final PersonaService personaService;
    private final ChatWebSocketHandler webSocketHandler;
    
    public SystemController(SessionService sessionService,
                           MemoryService memoryService,
                           PersonaService personaService,
                           ChatWebSocketHandler webSocketHandler) {
        this.sessionService = sessionService;
        this.memoryService = memoryService;
        this.personaService = personaService;
        this.webSocketHandler = webSocketHandler;
    }
    
    /**
     * 系统健康检查
     */
    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("timestamp", LocalDateTime.now());
        health.put("service", "AI Chatbot System");
        return health;
    }
    
    /**
     * 系统信息
     */
    @GetMapping("/info")
    public Map<String, Object> systemInfo() {
        Map<String, Object> info = new HashMap<>();
        
        // 基本信息
        info.put("name", "AI聊天机器人系统");
        info.put("version", "1.0.0");
        info.put("description", "基于Java21的智能聊天机器人系统");
        info.put("timestamp", LocalDateTime.now());
        
        // 功能特性
        Map<String, Boolean> features = new HashMap<>();
        features.put("websocket_communication", true);
        features.put("streaming_response", true);
        features.put("persona_management", true);
        features.put("long_term_memory", true);
        features.put("multimodal_processing", true);
        features.put("session_management", true);
        info.put("features", features);
        
        return info;
    }
    
    /**
     * 系统统计信息
     */
    @GetMapping("/stats")
    public Map<String, Object> systemStats() {
        Map<String, Object> stats = new HashMap<>();
        
        // 会话统计
        stats.put("active_sessions", sessionService.getActiveSessionCount());
        stats.put("websocket_connections", webSocketHandler.getActiveSessionCount());
        
        // 人设统计
        stats.put("total_personas", personaService.getAllPersonas().size());
        stats.put("active_personas", personaService.getActivePersonas().size());
        
        // 系统运行时间
        long uptime = java.lang.management.ManagementFactory.getRuntimeMXBean().getUptime();
        stats.put("uptime_ms", uptime);
        stats.put("uptime_readable", formatUptime(uptime));
        
        return stats;
    }
    
    /**
     * 格式化运行时间
     */
    private String formatUptime(long uptimeMs) {
        long seconds = uptimeMs / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        
        if (days > 0) {
            return String.format("%d天 %d小时 %d分钟", days, hours % 24, minutes % 60);
        } else if (hours > 0) {
            return String.format("%d小时 %d分钟", hours, minutes % 60);
        } else if (minutes > 0) {
            return String.format("%d分钟 %d秒", minutes, seconds % 60);
        } else {
            return String.format("%d秒", seconds);
        }
    }
}

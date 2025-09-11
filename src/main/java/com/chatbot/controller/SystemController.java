package com.chatbot.controller;

import com.chatbot.service.SessionService;
import com.chatbot.service.MemoryService;
import com.chatbot.service.PersonaService;
import com.chatbot.websocket.ChatWebSocketHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    
    private static final Logger logger = LoggerFactory.getLogger(SystemController.class);
    
    private final SessionService sessionService;
    private final MemoryService memoryService;
    private final PersonaService personaService;
    private final ChatWebSocketHandler webSocketHandler;
    
    public SystemController(SessionService sessionService,
                           MemoryService memoryService,
                           PersonaService personaService,
                           ChatWebSocketHandler webSocketHandler) {
        logger.info("初始化SystemController");
        this.sessionService = sessionService;
        this.memoryService = memoryService;
        this.personaService = personaService;
        this.webSocketHandler = webSocketHandler;
        logger.debug("SystemController初始化完成");
    }
    
    /**
     * 系统健康检查
     */
    @GetMapping("/health")
    public Map<String, Object> health() {
        logger.debug("接收到健康检查请求");
        long startTime = System.currentTimeMillis();
        
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("timestamp", LocalDateTime.now());
        health.put("service", "AI Chatbot System");
        
        long responseTime = System.currentTimeMillis() - startTime;
        logger.info("健康检查完成，响应时间: {}ms", responseTime);
        return health;
    }
    
    /**
     * 系统信息
     */
    @GetMapping("/info")
    public Map<String, Object> systemInfo() {
        logger.debug("接收到系统信息查询请求");
        long startTime = System.currentTimeMillis();
        
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
        
        long responseTime = System.currentTimeMillis() - startTime;
        logger.info("系统信息查询完成，响应时间: {}ms", responseTime);
        return info;
    }
    
    /**
     * 系统统计信息
     */
    @GetMapping("/stats")
    public Map<String, Object> systemStats() {
        logger.debug("接收到系统统计信息查询请求");
        long startTime = System.currentTimeMillis();
        
        Map<String, Object> stats = new HashMap<>();
        
        // 会话统计
        int activeSessions = sessionService.getActiveSessionCount();
        int wsConnections = webSocketHandler.getActiveSessionCount();
        stats.put("active_sessions", activeSessions);
        stats.put("websocket_connections", wsConnections);
        logger.debug("会话统计 - 活跃会话: {}, WebSocket连接: {}", activeSessions, wsConnections);
        
        // 人设统计
        int totalPersonas = personaService.getAllPersonas().size();
        int activePersonas = personaService.getActivePersonas().size();
        stats.put("total_personas", totalPersonas);
        stats.put("active_personas", activePersonas);
        logger.debug("人设统计 - 总人设: {}, 活跃人设: {}", totalPersonas, activePersonas);
        
        // 系统运行时间
        long uptime = java.lang.management.ManagementFactory.getRuntimeMXBean().getUptime();
        stats.put("uptime_ms", uptime);
        stats.put("uptime_readable", formatUptime(uptime));
        
        long responseTime = System.currentTimeMillis() - startTime;
        logger.info("系统统计信息查询完成，响应时间: {}ms", responseTime);
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

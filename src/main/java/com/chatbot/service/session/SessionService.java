package com.chatbot.service.session;

import com.chatbot.config.AppConfig;
import com.chatbot.config.properties.SystemProperties;
import com.chatbot.model.domain.ChatSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 会话管理服务
 * 负责创建、维护和清理聊天会话
 */
@Service
public class SessionService {
    
    private static final Logger logger = LoggerFactory.getLogger(SessionService.class);
    
    private final SystemProperties systemConfig;
    private final ConcurrentHashMap<String, ChatSession> activeSessions;
    private final ScheduledExecutorService scheduler;
    
    public SessionService(AppConfig appConfig) {
        this.systemConfig = appConfig.getSystem();
        this.activeSessions = new ConcurrentHashMap<>();
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
        
        // 启动会话清理任务，每分钟执行一次
        scheduler.scheduleAtFixedRate(this::cleanupExpiredSessions, 1, 1, TimeUnit.MINUTES);
        logger.info("SessionService初始化完成，会话超时时间: {}秒", systemConfig.getSessionTimeout());
    }
    
    /**
     * 创建新会话
     */
    public ChatSession createSession(String sessionId) {
        ChatSession session = new ChatSession(sessionId);
        logger.info("创建新会话: {}", sessionId);
        return session;
    }
    
    /**
     * 获取会话，如果不存在则创建
     */
    public ChatSession getOrCreateSession(String sessionId) {
        boolean sessionExists = activeSessions.containsKey(sessionId);
        
        ChatSession session = activeSessions.computeIfAbsent(sessionId, this::createSession);
        
        if (!sessionExists) {
            logger.info("创建了新会话，sessionId: {}, 当前活跃会话数: {}", sessionId, activeSessions.size());
        } else {
            logger.debug("使用现有会话，sessionId: {}", sessionId);
        }
        
        return session;
    }
    
    /**
     * 获取现有会话
     */
    public ChatSession getSession(String sessionId) {
        ChatSession session = activeSessions.get(sessionId);
        if (session != null) {
            session.updateLastActiveTime();
        } else {
            logger.debug("会话不存在，sessionId: {}", sessionId);
        }
        return session;
    }
    
    /**
     * 清理指定会话
     */
    public void cleanupSession(String sessionId) {
        ChatSession session = activeSessions.remove(sessionId);
        if (session != null) {
            logger.info("清理会话: {}", sessionId);
        }
    }
    
    /**
     * 清理过期会话
     */
    private void cleanupExpiredSessions() {
        int timeoutSeconds = systemConfig.getSessionTimeout();
        
        activeSessions.entrySet().removeIf(entry -> {
            ChatSession session = entry.getValue();
            if (session.isExpired(timeoutSeconds)) {
                logger.info("清理过期会话: {}", entry.getKey());
                return true;
            }
            return false;
        });
    }
    
    /**
     * 获取活跃会话数量
     */
    public int getActiveSessionCount() {
        return activeSessions.size();
    }
}

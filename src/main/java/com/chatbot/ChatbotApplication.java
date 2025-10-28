package com.chatbot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * AI聊天机器人系统主启动类
 * 
 * 系统功能特性：
 * - 支持WebSocket实时通信
 * - 流式AI对话响应
 * - 多人设管理
 * - 长期记忆系统
 * - 多模态处理（语音、图像）
 * - 会话管理和状态维护
 */
@SpringBootApplication
@EnableConfigurationProperties
@org.springframework.boot.context.properties.ConfigurationPropertiesScan
public class ChatbotApplication {
    
    private static final Logger logger = LoggerFactory.getLogger(ChatbotApplication.class);
    
    public static void main(String[] args) {
        try {
            logger.info("正在启动AI聊天机器人系统...");
            
            SpringApplication app = new SpringApplication(ChatbotApplication.class);
            
            // 设置应用属性
            System.setProperty("spring.application.name", "ai-chatbot-system");
            
            app.run(args);
            
            logger.info("AI聊天机器人系统启动成功！");
            logger.info("WebSocket端点: ws://localhost:8080/ws/chat");
            logger.info("健康检查: http://localhost:8080/api/health");
            logger.info("系统信息: http://localhost:8080/api/system/info");
            
        } catch (Exception e) {
            logger.error("应用启动失败", e);
            System.exit(1);
        }
    }
}

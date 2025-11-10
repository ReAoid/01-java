package com.chatbot.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 资源配置
 */
@Component
@ConfigurationProperties(prefix = "app.resource")
public class ResourceProperties {
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ResourceProperties.class);
    
    private String basePath = "src/main/resources";  // 提供默认值
    private String logPath = "logs";                 // 提供默认值
    private DataPaths data = new DataPaths();
    
    // 辅助方法
    public String getFullPath(String relativePath) {
        String actualBasePath = basePath;
        
        // 确保basePath不为null且不包含null字符串
        if (actualBasePath == null || actualBasePath.contains("null")) {
            System.err.println("⚠️ WARNING: basePath异常 (" + actualBasePath + ")! 强制使用正确路径.");
            actualBasePath = "src/main/resources";
        }
        
        // 确保relativePath不为null
        if (relativePath == null || relativePath.contains("null")) {
            System.err.println("⚠️ WARNING: relativePath异常 (" + relativePath + ")! 使用默认值.");
            relativePath = "data";
        }
        
        return actualBasePath + "/" + relativePath;
    }
    
    public String getMemoriesPath() {
        return getFullPath(data.getMemories());
    }
    
    public String getPersonasPath() {
        return getFullPath(data.getPersonas());
    }
    
    public String getSessionsPath() {
        return getFullPath(data.getSessions());
    }
    
    public String getLogPath() {
        return getFullPath(logPath);
    }
    
    public String getLogPath(String fileName) {
        return getFullPath(logPath + "/" + fileName);
    }
    
    // Getters and Setters
    public String getBasePath() { return basePath; }
    public void setBasePath(String basePath) { 
        this.basePath = basePath;
        logger.info("🔧 设置basePath: {}", basePath);
    }
    
    public void setLogPath(String logPath) { this.logPath = logPath; }
    
    public DataPaths getData() { return data; }
    public void setData(DataPaths data) { this.data = data; }
    
    public static class DataPaths {
        private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(DataPaths.class);
        
        private String memories = "data/memories";   // 提供默认值
        private String personas = "data/personas";   // 提供默认值
        private String sessions = "data/sessions";   // 提供默认值
        
        public String getMemories() { return memories; }
        public void setMemories(String memories) { this.memories = memories; }
        
        public String getPersonas() { return personas; }
        public void setPersonas(String personas) { this.personas = personas; }
        
        public String getSessions() { return sessions; }
        public void setSessions(String sessions) { 
            this.sessions = sessions;
            logger.info("🔧 设置sessions路径: {}", sessions);
        }
    }
}


package com.chatbot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 资源配置类
 * 管理项目中各种资源文件的路径
 */
@Component
@ConfigurationProperties(prefix = "resource")
public class ResourceConfig {
    
    // 基础资源路径
    private String basePath = "src/main/resources";
    
    // 日志文件路径
    private String logPath = "logs";
    
    // 数据文件路径
    private DataPaths data = new DataPaths();
    
    /**
     * 数据文件路径配置
     */
    public static class DataPaths {
        private String memories = "data/memories";
        private String personas = "data/personas";
        private String sessions = "data/sessions";
        
        // Getters and setters
        public String getMemories() {
            return memories;
        }
        
        public void setMemories(String memories) {
            this.memories = memories;
        }
        
        public String getPersonas() {
            return personas;
        }
        
        public void setPersonas(String personas) {
            this.personas = personas;
        }
        
        public String getSessions() {
            return sessions;
        }
        
        public void setSessions(String sessions) {
            this.sessions = sessions;
        }
    }
    
    /**
     * 获取完整的文件路径
     */
    public String getFullPath(String relativePath) {
        return basePath + "/" + relativePath;
    }
    
    /**
     * 获取记忆文件完整路径
     */
    public String getMemoriesPath() {
        return getFullPath(data.getMemories());
    }
    
    /**
     * 获取人设文件完整路径
     */
    public String getPersonasPath() {
        return getFullPath(data.getPersonas());
    }
    
    /**
     * 获取会话文件完整路径
     */
    public String getSessionsPath() {
        return getFullPath(data.getSessions());
    }
    
    /**
     * 获取日志文件完整路径
     */
    public String getLogPath() {
        return getFullPath(logPath);
    }
    
    // Getters and setters
    public String getBasePath() {
        return basePath;
    }
    
    public void setBasePath(String basePath) {
        this.basePath = basePath;
    }
    
    public String getLogPath(String fileName) {
        return getFullPath(logPath + "/" + fileName);
    }
    
    public void setLogPath(String logPath) {
        this.logPath = logPath;
    }
    
    public DataPaths getData() {
        return data;
    }
    
    public void setData(DataPaths data) {
        this.data = data;
    }
}

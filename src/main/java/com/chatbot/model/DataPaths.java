package com.chatbot.model;

/**
 * 数据文件路径配置
 */
public class DataPaths {
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

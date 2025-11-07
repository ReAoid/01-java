package com.chatbot.model.domain;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * AI人设模型
 * 定义机器人的性格、知识范围和表达风格
 */
public class Persona {
    
    /**
     * 人设ID
     */
    private String personaId;
    
    /**
     * 人设名称
     */
    private String name;
    
    /**
     * 人设描述
     */
    private String description;
    
    /**
     * 性格特征
     */
    private String personality;
    
    /**
     * 知识范围
     */
    private String knowledgeScope;
    
    /**
     * 表达风格
     */
    private String communicationStyle;
    
    /**
     * 系统提示词
     */
    private String systemPrompt;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    
    /**
     * 是否激活
     */
    private boolean active;
    
    /**
     * 额外配置参数
     */
    private Map<String, Object> parameters;
    
    public Persona() {
        this.createdAt = LocalDateTime.now();
        this.active = true;
    }
    
    public Persona(String personaId, String name) {
        this();
        this.personaId = personaId;
        this.name = name;
    }
    
    // Getters and Setters
    public String getPersonaId() {
        return personaId;
    }
    
    public void setPersonaId(String personaId) {
        this.personaId = personaId;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getPersonality() {
        return personality;
    }
    
    public void setPersonality(String personality) {
        this.personality = personality;
    }
    
    public String getKnowledgeScope() {
        return knowledgeScope;
    }
    
    public void setKnowledgeScope(String knowledgeScope) {
        this.knowledgeScope = knowledgeScope;
    }
    
    public String getCommunicationStyle() {
        return communicationStyle;
    }
    
    public void setCommunicationStyle(String communicationStyle) {
        this.communicationStyle = communicationStyle;
    }
    
    public String getSystemPrompt() {
        return systemPrompt;
    }
    
    public void setSystemPrompt(String systemPrompt) {
        this.systemPrompt = systemPrompt;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public boolean isActive() {
        return active;
    }
    
    public void setActive(boolean active) {
        this.active = active;
    }
    
    public Map<String, Object> getParameters() {
        return parameters;
    }
    
    public void setParameters(Map<String, Object> parameters) {
        this.parameters = parameters;
    }
}

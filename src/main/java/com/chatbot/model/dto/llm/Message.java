package com.chatbot.model.dto.llm;

import java.io.Serializable;
import java.util.Objects;

/**
 * 通用消息类（统一接口层）
 * 用于LLM服务的统一消息格式，与具体LLM实现解耦
 * 
 * 设计原则：
 * - 这是统一接口层的类，不依赖任何具体LLM实现
 * - 支持多种LLM（Ollama、OpenAI、Claude等）
 * - 在服务层转换为具体LLM的消息格式
 */
public class Message implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private final String role;      // 角色：system, user, assistant
    private final String content;   // 消息内容
    
    /**
     * 构造函数
     * @param role 角色（system/user/assistant）
     * @param content 消息内容
     */
    public Message(String role, String content) {
        this.role = role;
        this.content = content;
    }
    
    // ========== Getters ==========
    
    public String getRole() {
        return role;
    }
    
    public String getContent() {
        return content;
    }
    
    // ========== 工具方法 ==========
    
    /**
     * 创建系统消息
     */
    public static Message system(String content) {
        return new Message("system", content);
    }
    
    /**
     * 创建用户消息
     */
    public static Message user(String content) {
        return new Message("user", content);
    }
    
    /**
     * 创建助手消息
     */
    public static Message assistant(String content) {
        return new Message("assistant", content);
    }
    
    // ========== Object 方法 ==========
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Message message = (Message) o;
        return Objects.equals(role, message.role) && 
               Objects.equals(content, message.content);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(role, content);
    }
    
    @Override
    public String toString() {
        return "Message{" +
                "role='" + role + '\'' +
                ", contentLength=" + (content != null ? content.length() : 0) +
                '}';
    }
}


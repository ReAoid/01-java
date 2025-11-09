package com.chatbot.model.dto.llm;

import java.util.List;

/**
 * Ollama聊天请求数据类
 * 支持从提示词或消息列表创建请求
 */
public class OllamaChatRequest {
    public final String model;
    public final OllamaMessage[] messages;
    public final boolean stream;
    public final OllamaOptions options;
    
    /**
     * 私有构造函数 - 使用静态工厂方法创建实例
     */
    private OllamaChatRequest(String model, OllamaMessage[] messages, boolean stream, double temperature) {
        this.model = model;
        this.messages = messages;
        this.stream = stream;
        this.options = new OllamaOptions(temperature);
    }
    
    /**
     * 从系统提示词和用户提示词创建请求
     * @param model Ollama模型名称
     * @param systemPrompt 系统提示词（可为null或空）
     * @param userPrompt 用户提示词
     * @param stream 是否使用流式输出
     * @param temperature 温度参数
     * @return OllamaChatRequest实例
     */
    public static OllamaChatRequest fromPrompts(String model, String systemPrompt, String userPrompt, 
                                                  boolean stream, double temperature) {
        OllamaMessage[] messages;
        
        // 构建消息数组
        if (systemPrompt != null && !systemPrompt.trim().isEmpty()) {
            messages = new OllamaMessage[] { 
                new OllamaMessage("system", systemPrompt.trim()),
                new OllamaMessage("user", userPrompt) 
            };
        } else {
            messages = new OllamaMessage[] { 
                new OllamaMessage("user", userPrompt) 
            };
        }
        
        return new OllamaChatRequest(model, messages, stream, temperature);
    }
    
    /**
     * 从消息列表创建请求
     * @param model Ollama模型名称
     * @param messageList 消息列表
     * @param stream 是否使用流式输出
     * @param temperature 温度参数
     * @return OllamaChatRequest实例
     */
    public static OllamaChatRequest fromMessages(String model, List<OllamaMessage> messageList, 
                                                   boolean stream, double temperature) {
        OllamaMessage[] messages = messageList.toArray(new OllamaMessage[0]);
        return new OllamaChatRequest(model, messages, stream, temperature);
    }
    
    // 废弃的构造函数已删除，请使用 fromPrompts() 或 fromMessages() 静态工厂方法
    
    // Getters for Jackson serialization
    public String getModel() {
        return model;
    }
    
    public OllamaMessage[] getMessages() {
        return messages;
    }
    
    public boolean isStream() {
        return stream;
    }
    
    public OllamaOptions getOptions() {
        return options;
    }
}


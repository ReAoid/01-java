package com.chatbot.model;

/**
 * Ollama聊天请求数据类
 */
public class OllamaChatRequest {
    public final String model;
    public final OllamaMessageData[] messages;
    public final boolean stream;
    public final OllamaOptions options;
    
    public OllamaChatRequest(String model, String systemPrompt, String userPrompt, boolean stream, double temperature) {
        this.model = model;
        
        // 构建消息数组
        if (systemPrompt != null && !systemPrompt.trim().isEmpty()) {
            this.messages = new OllamaMessageData[] { 
                new OllamaMessageData("system", systemPrompt.trim()),
                new OllamaMessageData("user", userPrompt) 
            };
        } else {
            this.messages = new OllamaMessageData[] { 
                new OllamaMessageData("user", userPrompt) 
            };
        }
        
        this.stream = stream;
        this.options = new OllamaOptions(temperature);
    }
    
    // Getters for Jackson serialization
    public String getModel() {
        return model;
    }
    
    public OllamaMessageData[] getMessages() {
        return messages;
    }
    
    public boolean isStream() {
        return stream;
    }
    
    public OllamaOptions getOptions() {
        return options;
    }
}

package com.chatbot.model;

import java.util.List;

/**
 * 从消息列表构建的Ollama聊天请求数据类
 */
public class OllamaChatRequestFromMessages {
    public final String model;
    public final OllamaMessageData[] messages;
    public final boolean stream;
    public final OllamaOptions options;
    
    public OllamaChatRequestFromMessages(String model, List<OllamaMessage> messageList, boolean stream, double temperature) {
        this.model = model;
        
        // 将 OllamaMessage 列表转换为 OllamaMessageData 数组
        this.messages = messageList.stream()
                .map(msg -> new OllamaMessageData(msg.getRole(), msg.getContent()))
                .toArray(OllamaMessageData[]::new);
        
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

package com.chatbot.model.dto.llm;

/**
 * Ollama选项数据类
 */
public class OllamaOptions {
    public final double temperature;
    
    public OllamaOptions(double temperature) {
        this.temperature = temperature;
    }
    
    public double getTemperature() {
        return temperature;
    }
}


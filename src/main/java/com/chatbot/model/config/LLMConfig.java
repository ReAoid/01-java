package com.chatbot.model.config;

/**
 * LLM (大语言模型) 配置
 * 支持多种模型提供商，不绑定特定实现
 * 
 * 支持的提供商：Ollama (本地部署)、OpenAI、Anthropic等
 */
public class LLMConfig {
    
    // ========== 提供商设置 ==========
    private String provider = "ollama";
    private String baseUrl = "http://localhost:11434";
    // private String apiKey = null;  // 新增字段，暂时注释
    
    // ========== 模型设置 ==========
    private String model = "qwen3:4b";
    // private String modelVersion = null;  // 新增字段，暂时注释
    
    // ========== 生成参数 ==========
    // private double temperature = 0.7;  // 新增字段，暂时注释
    // private double topP = 0.9;  // 新增字段，暂时注释
    private int maxTokens = 4096;
    // private double frequencyPenalty = 0.0;  // 新增字段，暂时注释
    // private double presencePenalty = 0.0;  // 新增字段，暂时注释
    
    // ========== 请求设置 ==========
    private boolean stream = true;
    private int timeout = 30000;
    // private int maxRetries = 3;  // 新增字段，暂时注释
    // private int retryDelay = 1000;  // 新增字段，暂时注释
    
    // ========== 上下文管理 ==========
    // private int contextWindowSize = 8192;  // 新增字段，暂时注释
    // private boolean enableContextCompression = false;  // 新增字段，暂时注释
    // private int maxHistoryMessages = 20;  // 新增字段，暂时注释
    
    // ========== 高级设置 ==========
    // private String systemPrompt = null;  // 新增字段，暂时注释
    // private boolean enableThinking = false;  // 新增字段，暂时注释
    // private boolean enableFunctionCalling = false;  // 新增字段，暂时注释
    
    /**
     * 验证配置有效性
     */
    public boolean validate() {
        if (baseUrl == null || baseUrl.isEmpty()) return false;
        if (model == null || model.isEmpty()) return false;
        if (maxTokens <= 0) return false;
        if (timeout <= 0) return false;
        return true;
    }
    
    // ========== Getters & Setters ==========
    
    public String getProvider() {
        return provider;
    }
    
    public void setProvider(String provider) {
        this.provider = provider;
    }
    
    public String getBaseUrl() {
        return baseUrl;
    }
    
    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }
    
    public String getModel() {
        return model;
    }
    
    public void setModel(String model) {
        this.model = model;
    }
    
    public int getMaxTokens() {
        return maxTokens;
    }
    
    public void setMaxTokens(int maxTokens) {
        this.maxTokens = maxTokens;
    }
    
    public boolean isStream() {
        return stream;
    }
    
    public void setStream(boolean stream) {
        this.stream = stream;
    }
    
    public int getTimeout() {
        return timeout;
    }
    
    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }
}


package com.chatbot.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * 统一应用配置类
 * 包含所有系统配置项
 */
@Configuration
@ConfigurationProperties(prefix = "app")
public class AppConfig {
    
    // ========== 系统配置 ==========
    private SystemConfig system = new SystemConfig();
    
    // ========== AI配置 ==========
    private AIConfig ai = new AIConfig();
    
    // ========== Ollama配置 ==========
    private OllamaConfig ollama = new OllamaConfig();
    
    // ========== Python API配置 ==========
    private PythonApiConfig python = new PythonApiConfig();
    
    // ========== 资源配置 ==========
    private ResourceConfig resource = new ResourceConfig();
    
    /**
     * Jackson配置 - 配置ObjectMapper支持Java 21时间类型
     */
    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        
        // 注册Java时间模块
        mapper.registerModule(new JavaTimeModule());
        
        // 禁用将日期写为时间戳
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        
        // 配置其他序列化特性
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        
        return mapper;
    }
    
    // ========== 内部配置类 ==========
    
    /**
     * 系统核心配置
     */
    public static class SystemConfig {
        private int maxContextTokens = 8192;
        private int sessionTimeout = 3600;
        private WebSocketConfig websocket = new WebSocketConfig();
        
        // Getters and Setters
        public int getMaxContextTokens() { return maxContextTokens; }
        public void setMaxContextTokens(int maxContextTokens) { this.maxContextTokens = maxContextTokens; }
        
        public int getSessionTimeout() { return sessionTimeout; }
        public void setSessionTimeout(int sessionTimeout) { this.sessionTimeout = sessionTimeout; }
        
        public WebSocketConfig getWebsocket() { return websocket; }
        public void setWebsocket(WebSocketConfig websocket) { this.websocket = websocket; }
        
        public static class WebSocketConfig {
            private int pingInterval = 30;
            private int maxReconnectAttempts = 5;
            
            public int getPingInterval() { return pingInterval; }
            public void setPingInterval(int pingInterval) { this.pingInterval = pingInterval; }
            
            public int getMaxReconnectAttempts() { return maxReconnectAttempts; }
            public void setMaxReconnectAttempts(int maxReconnectAttempts) { this.maxReconnectAttempts = maxReconnectAttempts; }
        }
    }
    
    /**
     * AI相关配置
     */
    public static class AIConfig {
        private StreamingConfig streaming = new StreamingConfig();
        private VoiceConfig voice = new VoiceConfig();
        
        public StreamingConfig getStreaming() { return streaming; }
        public void setStreaming(StreamingConfig streaming) { this.streaming = streaming; }
        
        public VoiceConfig getVoice() { return voice; }
        public void setVoice(VoiceConfig voice) { this.voice = voice; }
        
        public static class StreamingConfig {
            private int chunkSize = 16;
            private int delayMs = 50;
            
            public int getChunkSize() { return chunkSize; }
            public void setChunkSize(int chunkSize) { this.chunkSize = chunkSize; }
            
            public int getDelayMs() { return delayMs; }
            public void setDelayMs(int delayMs) { this.delayMs = delayMs; }
        }
        
        public static class VoiceConfig {
            private String asrModel = "whisper-medium";
            private String ttsVoice = "zh-CN-XiaoxiaoNeural";
            
            public String getAsrModel() { return asrModel; }
            public void setAsrModel(String asrModel) { this.asrModel = asrModel; }
            
            public String getTtsVoice() { return ttsVoice; }
            public void setTtsVoice(String ttsVoice) { this.ttsVoice = ttsVoice; }
        }
    }
    
    /**
     * Ollama配置
     */
    public static class OllamaConfig {
        private String baseUrl = "http://localhost:11434";
        private String model = "qwen3:4b";
        private int timeout = 30000;
        private int maxTokens = 4096;
        private double temperature = 0.7;
        private boolean stream = true;
        
        // Getters and Setters
        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
        
        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }
        
        public int getTimeout() { return timeout; }
        public void setTimeout(int timeout) { this.timeout = timeout; }
        
        public int getMaxTokens() { return maxTokens; }
        public void setMaxTokens(int maxTokens) { this.maxTokens = maxTokens; }
        
        public double getTemperature() { return temperature; }
        public void setTemperature(double temperature) { this.temperature = temperature; }
        
        public boolean isStream() { return stream; }
        public void setStream(boolean stream) { this.stream = stream; }
        
        // 辅助方法
        public String getGenerateUrl() { return baseUrl + "/api/generate"; }
        public String getChatUrl() { return baseUrl + "/api/chat"; }
        public String getModelsUrl() { return baseUrl + "/api/tags"; }
    }
    
    /**
     * Python API配置
     */
    public static class PythonApiConfig {
        private String baseUrl = "http://localhost:5000";
        private EndpointsConfig endpoints = new EndpointsConfig();
        
        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
        
        public EndpointsConfig getEndpoints() { return endpoints; }
        public void setEndpoints(EndpointsConfig endpoints) { this.endpoints = endpoints; }
        
        public static class EndpointsConfig {
            private String asr = "/api/asr";
            private String tts = "/api/tts";
            private String vad = "/api/vad";
            private String ocr = "/api/ocr";
            
            public String getAsr() { return asr; }
            public void setAsr(String asr) { this.asr = asr; }
            
            public String getTts() { return tts; }
            public void setTts(String tts) { this.tts = tts; }
            
            public String getVad() { return vad; }
            public void setVad(String vad) { this.vad = vad; }
            
            public String getOcr() { return ocr; }
            public void setOcr(String ocr) { this.ocr = ocr; }
        }
    }
    
    /**
     * 资源配置
     */
    public static class ResourceConfig {
        private String basePath = "src/main/resources";
        private String logPath = "logs";
        private DataPaths data = new DataPaths();
        
        // 辅助方法
        public String getFullPath(String relativePath) {
            return basePath + "/" + relativePath;
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
        public void setBasePath(String basePath) { this.basePath = basePath; }
        
        public void setLogPath(String logPath) { this.logPath = logPath; }
        
        public DataPaths getData() { return data; }
        public void setData(DataPaths data) { this.data = data; }
        
        public static class DataPaths {
            private String memories = "data/memories";
            private String personas = "data/personas";
            private String sessions = "data/sessions";
            
            public String getMemories() { return memories; }
            public void setMemories(String memories) { this.memories = memories; }
            
            public String getPersonas() { return personas; }
            public void setPersonas(String personas) { this.personas = personas; }
            
            public String getSessions() { return sessions; }
            public void setSessions(String sessions) { this.sessions = sessions; }
        }
    }
    
    // ========== 主配置Getters and Setters ==========
    
    public SystemConfig getSystem() { return system; }
    public void setSystem(SystemConfig system) { this.system = system; }
    
    public AIConfig getAi() { return ai; }
    public void setAi(AIConfig ai) { this.ai = ai; }
    
    public OllamaConfig getOllama() { return ollama; }
    public void setOllama(OllamaConfig ollama) { this.ollama = ollama; }
    
    public PythonApiConfig getPython() { return python; }
    public void setPython(PythonApiConfig python) { this.python = python; }
    
    public ResourceConfig getResource() { return resource; }
    public void setResource(ResourceConfig resource) { this.resource = resource; }
}

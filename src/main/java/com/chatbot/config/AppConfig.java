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
    
    // ========== 联网搜索配置 ==========
    private WebSearchConfig webSearch = new WebSearchConfig();
    
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
        private SystemPromptConfig systemPrompt = new SystemPromptConfig();
        private WebSearchDecisionConfig webSearchDecision = new WebSearchDecisionConfig();
        private StreamingConfig streaming = new StreamingConfig();
        private VoiceConfig voice = new VoiceConfig();
        
        public SystemPromptConfig getSystemPrompt() { return systemPrompt; }
        public void setSystemPrompt(SystemPromptConfig systemPrompt) { this.systemPrompt = systemPrompt; }
        
        public WebSearchDecisionConfig getWebSearchDecision() { return webSearchDecision; }
        public void setWebSearchDecision(WebSearchDecisionConfig webSearchDecision) { this.webSearchDecision = webSearchDecision; }
        
        public StreamingConfig getStreaming() { return streaming; }
        public void setStreaming(StreamingConfig streaming) { this.streaming = streaming; }
        
        public VoiceConfig getVoice() { return voice; }
        public void setVoice(VoiceConfig voice) { this.voice = voice; }
        
        /**
         * 系统提示词配置
         */
        public static class SystemPromptConfig {
            private String base = "你是一个智能AI助手，专注于提供准确、有用的信息和建议。";
            private String fallback = "你是一个友善的AI助手，请帮助用户解决问题。";
            private boolean enablePersona = true;
            
            public String getBase() { return base; }
            public void setBase(String base) { this.base = base; }
            
            public String getFallback() { return fallback; }
            public void setFallback(String fallback) { this.fallback = fallback; }
            
            public boolean isEnablePersona() { return enablePersona; }
            public void setEnablePersona(boolean enablePersona) { this.enablePersona = enablePersona; }
        }
        
        /**
         * 联网搜索判断配置
         */
        public static class WebSearchDecisionConfig {
            private int timeoutSeconds = 5; // AI判断超时时间（秒）
            private boolean enableTimeoutFallback = true; // 超时时是否采用保守策略
            
            public int getTimeoutSeconds() { return timeoutSeconds; }
            public void setTimeoutSeconds(int timeoutSeconds) { this.timeoutSeconds = timeoutSeconds; }
            
            public boolean isEnableTimeoutFallback() { return enableTimeoutFallback; }
            public void setEnableTimeoutFallback(boolean enableTimeoutFallback) { this.enableTimeoutFallback = enableTimeoutFallback; }
            
            // 辅助方法：获取毫秒超时时间
            public long getTimeoutMillis() { return timeoutSeconds * 1000L; }
        }
        
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
     * 每个服务都配置独立的完整URL，支持不同的域名和端口
     */
    public static class PythonApiConfig {
        private ServicesConfig services = new ServicesConfig();
        private TimeoutConfig timeout = new TimeoutConfig();
        
        public ServicesConfig getServices() { return services; }
        public void setServices(ServicesConfig services) { this.services = services; }
        
        public TimeoutConfig getTimeout() { return timeout; }
        public void setTimeout(TimeoutConfig timeout) { this.timeout = timeout; }
        
        /**
         * 各个服务的独立URL配置
         */
        public static class ServicesConfig {
            // ASR (语音识别) 服务
            private String asrUrl = "http://localhost:5000/api/asr";
            
            // TTS (文本转语音) 服务 - CosyVoice默认端口
            private String ttsUrl = "http://localhost:50000";
            
            // VAD (语音活动检测) 服务
            private String vadUrl = "http://localhost:5000/api/vad";
            
            // OCR (图像识别) 服务
            private String ocrUrl = "http://localhost:5000/api/ocr";
            
            // Getters and Setters
            public String getAsrUrl() { return asrUrl; }
            public void setAsrUrl(String asrUrl) { this.asrUrl = asrUrl; }
            
            public String getTtsUrl() { return ttsUrl; }
            public void setTtsUrl(String ttsUrl) { this.ttsUrl = ttsUrl; }
            
            public String getVadUrl() { return vadUrl; }
            public void setVadUrl(String vadUrl) { this.vadUrl = vadUrl; }
            
            public String getOcrUrl() { return ocrUrl; }
            public void setOcrUrl(String ocrUrl) { this.ocrUrl = ocrUrl; }
            
            // 便捷方法：获取TTS健康检查URL
            public String getTtsHealthUrl() {
                return ttsUrl + (ttsUrl.endsWith("/") ? "health" : "/health");
            }
            
            // 便捷方法：获取TTS注册说话人URL
            public String getTtsRegisterSpeakerUrl() {
                return ttsUrl + (ttsUrl.endsWith("/") ? "register_speaker" : "/register_speaker");
            }
            
            // 便捷方法：获取TTS自定义说话人合成URL
            public String getTtsCustomSpeakerUrl() {
                return ttsUrl + (ttsUrl.endsWith("/") ? "inference_custom_speaker" : "/inference_custom_speaker");
            }
            
            // 便捷方法：获取TTS删除说话人URL
            public String getTtsDeleteSpeakerUrl(String speakerName) {
                return ttsUrl + (ttsUrl.endsWith("/") ? "speaker/" : "/speaker/") + speakerName;
            }
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
    
    public WebSearchConfig getWebSearch() { return webSearch; }
    public void setWebSearch(WebSearchConfig webSearch) { this.webSearch = webSearch; }
    
    /**
     * 联网搜索配置
     */
    public static class WebSearchConfig {
        private boolean enabled = false; // 默认关闭
        private int maxResults = 5;
        private int timeoutSeconds = 10;
        private String defaultEngine = "duckduckgo";
        private boolean enableFallback = true;
        
        // API Keys (如果需要)
        private String serpApiKey = "";
        private String bingApiKey = "";
        
        // Getters and Setters
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        
        public int getMaxResults() { return maxResults; }
        public void setMaxResults(int maxResults) { this.maxResults = maxResults; }
        
        public int getTimeoutSeconds() { return timeoutSeconds; }
        public void setTimeoutSeconds(int timeoutSeconds) { this.timeoutSeconds = timeoutSeconds; }
        
        public String getDefaultEngine() { return defaultEngine; }
        public void setDefaultEngine(String defaultEngine) { this.defaultEngine = defaultEngine; }
        
        public boolean isEnableFallback() { return enableFallback; }
        public void setEnableFallback(boolean enableFallback) { this.enableFallback = enableFallback; }
        
        public String getSerpApiKey() { return serpApiKey; }
        public void setSerpApiKey(String serpApiKey) { this.serpApiKey = serpApiKey; }
        
        public String getBingApiKey() { return bingApiKey; }
        public void setBingApiKey(String bingApiKey) { this.bingApiKey = bingApiKey; }
    }
    
    /**
     * 超时配置
     */
    public static class TimeoutConfig {
        // HTTP连接超时（秒）
        private int connectTimeoutSeconds = 60;
        
        // HTTP读取超时（秒）
        private int readTimeoutSeconds = 60;
        
        // HTTP写入超时（秒）
        private int writeTimeoutSeconds = 60;
        
        // TTS任务等待超时（秒）
        private int ttsTaskTimeoutSeconds = 60;
        
        // Live2D TTS任务等待超时（秒）
        private int live2dTtsTaskTimeoutSeconds = 60;
        
        public int getConnectTimeoutSeconds() { return connectTimeoutSeconds; }
        public void setConnectTimeoutSeconds(int connectTimeoutSeconds) { this.connectTimeoutSeconds = connectTimeoutSeconds; }
        
        public int getReadTimeoutSeconds() { return readTimeoutSeconds; }
        public void setReadTimeoutSeconds(int readTimeoutSeconds) { this.readTimeoutSeconds = readTimeoutSeconds; }
        
        public int getWriteTimeoutSeconds() { return writeTimeoutSeconds; }
        public void setWriteTimeoutSeconds(int writeTimeoutSeconds) { this.writeTimeoutSeconds = writeTimeoutSeconds; }
        
        public int getTtsTaskTimeoutSeconds() { return ttsTaskTimeoutSeconds; }
        public void setTtsTaskTimeoutSeconds(int ttsTaskTimeoutSeconds) { this.ttsTaskTimeoutSeconds = ttsTaskTimeoutSeconds; }
        
        public int getLive2dTtsTaskTimeoutSeconds() { return live2dTtsTaskTimeoutSeconds; }
        public void setLive2dTtsTaskTimeoutSeconds(int live2dTtsTaskTimeoutSeconds) { this.live2dTtsTaskTimeoutSeconds = live2dTtsTaskTimeoutSeconds; }
    }
}

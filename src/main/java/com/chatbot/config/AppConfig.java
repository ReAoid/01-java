package com.chatbot.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.InitializingBean;

/**
 * 统一应用配置类
 * 包含所有系统配置项
 */
@Configuration
@ConfigurationProperties(prefix = "app")
@Component
public class AppConfig implements InitializingBean {
    
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(AppConfig.class);
    
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
     * 配置加载后的验证
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        logger.info("🔍 AppConfig配置验证开始...");
        logger.info("Resource对象: {}", resource);
        logger.info("Resource basePath: {}", resource != null ? resource.getBasePath() : "null");
        logger.info("Resource logPath: {}", resource != null ? resource.logPath : "null");
        logger.info("Resource data对象: {}", resource != null ? resource.getData() : "null");
        logger.info("Resource data sessions: {}", resource != null && resource.getData() != null ? resource.getData().getSessions() : "null");
        logger.info("System config: {}", system != null ? "已加载" : "null");
        logger.info("AI config: {}", ai != null ? "已加载" : "null");
        logger.info("Ollama config: {}", ollama != null ? "已加载" : "null");
        
        if (resource == null) {
            logger.error("❌ ResourceConfig未加载！");
        } else if (resource.getBasePath() == null) {
            logger.error("❌ basePath未配置！当前resource对象: {}", resource);
        } else {
            logger.info("✅ 配置验证通过 - basePath: {}", resource.getBasePath());
        }
    }
    
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
        private int maxContextTokens;
        private int sessionTimeout;
        private WebSocketConfig websocket = new WebSocketConfig();
        
        // Getters and Setters
        public int getMaxContextTokens() { return maxContextTokens; }
        public void setMaxContextTokens(int maxContextTokens) { this.maxContextTokens = maxContextTokens; }
        
        public int getSessionTimeout() { return sessionTimeout; }
        public void setSessionTimeout(int sessionTimeout) { this.sessionTimeout = sessionTimeout; }
        
        public WebSocketConfig getWebsocket() { return websocket; }
        public void setWebsocket(WebSocketConfig websocket) { this.websocket = websocket; }
        
        public static class WebSocketConfig {
            private int pingInterval;
            private int maxReconnectAttempts;
            
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
        
        // Streaming配置 - 直接使用基本类型，避免与model包中的StreamingConfig重复
        private int streamingChunkSize = 16;
        private int streamingDelayMs = 50;
        
        // Voice配置 - 直接使用基本类型，避免与model包中的VoiceConfig重复
        private String voiceAsrModel = "whisper-medium";
        private String voiceTtsVoice = "派蒙";
        
        public SystemPromptConfig getSystemPrompt() { return systemPrompt; }
        public void setSystemPrompt(SystemPromptConfig systemPrompt) { this.systemPrompt = systemPrompt; }
        
        public WebSearchDecisionConfig getWebSearchDecision() { return webSearchDecision; }
        public void setWebSearchDecision(WebSearchDecisionConfig webSearchDecision) { this.webSearchDecision = webSearchDecision; }
        
        // Streaming配置的getter/setter
        public int getStreamingChunkSize() { return streamingChunkSize; }
        public void setStreamingChunkSize(int streamingChunkSize) { this.streamingChunkSize = streamingChunkSize; }
        
        public int getStreamingDelayMs() { return streamingDelayMs; }
        public void setStreamingDelayMs(int streamingDelayMs) { this.streamingDelayMs = streamingDelayMs; }
        
        // Voice配置的getter/setter
        public String getVoiceAsrModel() { return voiceAsrModel; }
        public void setVoiceAsrModel(String voiceAsrModel) { this.voiceAsrModel = voiceAsrModel; }
        
        public String getVoiceTtsVoice() { return voiceTtsVoice; }
        public void setVoiceTtsVoice(String voiceTtsVoice) { this.voiceTtsVoice = voiceTtsVoice; }
        
        /**
         * 系统提示词配置
         */
        public static class SystemPromptConfig {
            private String base;
            private String fallback;
            private boolean enablePersona;
            
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
            private int timeoutSeconds; // AI判断超时时间（秒）
            private boolean enableTimeoutFallback; // 超时时是否采用保守策略
            
            public int getTimeoutSeconds() { return timeoutSeconds; }
            public void setTimeoutSeconds(int timeoutSeconds) { this.timeoutSeconds = timeoutSeconds; }
            
            public boolean isEnableTimeoutFallback() { return enableTimeoutFallback; }
            public void setEnableTimeoutFallback(boolean enableTimeoutFallback) { this.enableTimeoutFallback = enableTimeoutFallback; }
            
            // 辅助方法：获取毫秒超时时间
            public long getTimeoutMillis() { return timeoutSeconds * 1000L; }
        }
    }
    
    /**
     * Ollama配置
     */
    public static class OllamaConfig {
        private String baseUrl;
        private String model;
        private int timeout;
        private int maxTokens;
        private double temperature;
        private boolean stream;
        
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
            private String asrUrl;
            
            // TTS (文本转语音) 服务 - CosyVoice默认端口
            private String ttsUrl;
            
            // VAD (语音活动检测) 服务
            private String vadUrl;
            
            // OCR (图像识别) 服务
            private String ocrUrl;
            
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
        private String basePath = "src/main/resources";  // 提供默认值
        private String logPath = "logs";                 // 提供默认值
        private DataPaths data = new DataPaths();
        
        // 辅助方法
        public String getFullPath(String relativePath) {
            String actualBasePath = basePath;
            
            // 确保basePath不为null且不包含null字符串
            if (actualBasePath == null || actualBasePath.contains("null")) {
                System.err.println("⚠️ WARNING: basePath异常 (" + actualBasePath + ")! 强制使用正确路径.");
                actualBasePath = "src/main/resources";
            }
            
            // 确保relativePath不为null
            if (relativePath == null || relativePath.contains("null")) {
                System.err.println("⚠️ WARNING: relativePath异常 (" + relativePath + ")! 使用默认值.");
                relativePath = "data";
            }
            
            return actualBasePath + "/" + relativePath;
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
        public void setBasePath(String basePath) { 
            this.basePath = basePath;
            logger.info("🔧 设置basePath: {}", basePath);
        }
        
        public void setLogPath(String logPath) { this.logPath = logPath; }
        
        public DataPaths getData() { return data; }
        public void setData(DataPaths data) { this.data = data; }
        
        public static class DataPaths {
            private String memories = "data/memories";   // 提供默认值
            private String personas = "data/personas";   // 提供默认值
            private String sessions = "data/sessions";   // 提供默认值
            
            public String getMemories() { return memories; }
            public void setMemories(String memories) { this.memories = memories; }
            
            public String getPersonas() { return personas; }
            public void setPersonas(String personas) { this.personas = personas; }
            
            public String getSessions() { return sessions; }
            public void setSessions(String sessions) { 
                this.sessions = sessions;
                logger.info("🔧 设置sessions路径: {}", sessions);
            }
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
        private boolean enabled;
        private int maxResults;
        private int timeoutSeconds;
        private String defaultEngine;
        private boolean enableFallback;
        
        // API Keys (如果需要)
        private String serpApiKey;
        private String bingApiKey;
        
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
        private int connectTimeoutSeconds;
        
        // HTTP读取超时（秒）
        private int readTimeoutSeconds;
        
        // HTTP写入超时（秒）
        private int writeTimeoutSeconds;
        
        // TTS任务等待超时（秒）
        private int ttsTaskTimeoutSeconds;
        
        // Live2D TTS任务等待超时（秒）
        private int live2dTtsTaskTimeoutSeconds;
        
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

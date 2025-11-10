package com.chatbot.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.chatbot.config.properties.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.beans.factory.InitializingBean;

/**
 * 统一应用配置类
 * 整合所有系统配置项
 */
@Configuration
public class AppConfig implements InitializingBean {
    
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(AppConfig.class);
    
    // ========== 注入各个独立的配置类 ==========
    @Autowired
    private SystemProperties system;
    
    @Autowired
    private AIProperties ai;
    
    @Autowired
    private LLMProperties llm;
    
    @Autowired
    private PythonApiProperties python;
    
    @Autowired
    private ResourceProperties resource;
    
    @Autowired
    private WebSearchProperties webSearch;
    
    /**
     * 配置加载后的验证
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        logger.info("🔍 AppConfig配置验证开始...");
        logger.info("Resource对象: {}", resource);
        logger.info("Resource basePath: {}", resource != null ? resource.getBasePath() : "null");
        logger.info("Resource data对象: {}", resource != null ? resource.getData() : "null");
        logger.info("Resource data sessions: {}", resource != null && resource.getData() != null ? resource.getData().getSessions() : "null");
        logger.info("System config: {}", system != null ? "已加载" : "null");
        logger.info("AI config: {}", ai != null ? "已加载" : "null");
        logger.info("LLM config: {}", llm != null ? "已加载" : "null");
        
        if (resource == null) {
            logger.error("❌ ResourceProperties未加载！");
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
    
    // ========== 配置访问器 ==========
    
    public SystemProperties getSystem() { return system; }
    
    public AIProperties getAi() { return ai; }
    
    public LLMProperties getLlm() { return llm; }
    
    public PythonApiProperties getPython() { return python; }
    
    public ResourceProperties getResource() { return resource; }
    
    public WebSearchProperties getWebSearch() { return webSearch; }
}

package com.chatbot.config;

import com.chatbot.config.properties.ResourceProperties;
import com.chatbot.service.UserPreferencesService;
import com.chatbot.model.config.ASRConfig;
import com.chatbot.model.config.OutputChannelConfig;
import com.chatbot.model.config.UserPreferences;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 应用启动初始化配置
 * 确保系统在启动时正确初始化各项设置
 */
@Configuration
public class StartupInitializer {
    
    private static final Logger logger = LoggerFactory.getLogger(StartupInitializer.class);
    
    /**
     * 应用启动时的初始化任务
     */
    @Bean
    public ApplicationRunner initializeApplication(UserPreferencesService userPreferencesService, AppConfig appConfig) {
        return args -> {
            logger.info("🚀 开始执行应用启动初始化...");
            
            try {
                // 验证路径配置
                validatePathConfiguration(appConfig);
                
                // 确保默认用户的TTS状态为禁用
                initializeTTSSettings(userPreferencesService);
                
                logger.info("✅ 应用启动初始化完成");
            } catch (Exception e) {
                logger.error("❌ 应用启动初始化失败", e);
            }
        };
    }
    
    /**
     * 验证路径配置，确保不会创建null目录
     */
    private void validatePathConfiguration(AppConfig appConfig) {
        try {
            ResourceProperties resourceConfig = appConfig.getResource();
            
            logger.info("🔍 验证路径配置...");
            logger.info("BasePath: {}", resourceConfig.getBasePath());
            logger.info("SessionsPath: {}", resourceConfig.getSessionsPath());
            logger.info("MemoriesPath: {}", resourceConfig.getMemoriesPath());
            logger.info("PersonasPath: {}", resourceConfig.getPersonasPath());
            
            // 检查是否有null路径
            String[] paths = {
                resourceConfig.getSessionsPath(),
                resourceConfig.getMemoriesPath(),
                resourceConfig.getPersonasPath()
            };
            
            for (String path : paths) {
                if (path == null || path.contains("null")) {
                    logger.error("❌ 检测到异常路径: {}", path);
                    throw new IllegalStateException("路径配置异常，包含null值: " + path);
                }
            }
            
            logger.info("✅ 路径配置验证通过");
            
        } catch (Exception e) {
            logger.error("❌ 路径配置验证失败", e);
            throw e;
        }
    }
    
    /**
     * 初始化TTS设置，确保启动时为禁用状态
     */
    private void initializeTTSSettings(UserPreferencesService userPreferencesService) {
        try {
            String defaultUserId = "Taiming";
            UserPreferences preferences = userPreferencesService.getUserPreferences(defaultUserId);
            
            // 检查并强制重置TTS状态
            boolean needsSave = false;
            
            // 检查ChatOutput配置（使用新版API）
            OutputChannelConfig.ChatWindowOutput chatOutput = preferences.getOutputChannel().getChatWindow();
            if (chatOutput == null) {
                chatOutput = new OutputChannelConfig.ChatWindowOutput();
                preferences.getOutputChannel().setChatWindow(chatOutput);
                needsSave = true;
            }
            
            if (chatOutput.isEnabled() || chatOutput.isAutoTTS() || !"text_only".equals(chatOutput.getMode())) {
                chatOutput.setEnabled(false);
                chatOutput.setAutoTTS(false);
                chatOutput.setMode("text_only");
                needsSave = true;
                logger.info("🔧 重置聊天窗口TTS状态为禁用");
            }
            
            // 检查Live2D配置（使用新版API）
            OutputChannelConfig.Live2DOutput live2dOutput = preferences.getOutputChannel().getLive2d();
            if (live2dOutput == null) {
                live2dOutput = new OutputChannelConfig.Live2DOutput();
                preferences.getOutputChannel().setLive2d(live2dOutput);
                needsSave = true;
            }
            
            if (live2dOutput.isEnabled()) {
                live2dOutput.setEnabled(false);
                needsSave = true;
                logger.info("🔧 重置Live2D TTS状态为禁用");
            }
            
            // 检查全局语音设置（使用新版API）
            if (preferences.getTts().isEnabled()) {
                preferences.getTts().setEnabled(false);
                needsSave = true;
                logger.info("🔧 重置全局语音设置为禁用");
            }
            
            // 检查ASR配置（使用新版API）
            ASRConfig asrConfig = preferences.getAsr();
            if (asrConfig == null) {
                asrConfig = new ASRConfig();
                preferences.setAsr(asrConfig);
                needsSave = true;
            }
            
            if (asrConfig.isEnabled()) {
                asrConfig.setEnabled(false);
                needsSave = true;
                logger.info("🔧 重置ASR状态为禁用");
            }
            
            // 如果有变更则保存
            if (needsSave) {
                userPreferencesService.saveUserPreferences(defaultUserId, preferences);
                logger.info("💾 语音相关设置已保存到用户配置");
            } else {
                logger.info("✅ 语音相关设置已经是正确的禁用状态");
            }
            
        } catch (Exception e) {
            logger.error("初始化TTS设置失败", e);
        }
    }
}

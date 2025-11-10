package com.chatbot.service;

import com.chatbot.config.AppConfig;
import com.chatbot.config.properties.AIProperties;
import com.chatbot.config.properties.LLMProperties;
import com.chatbot.config.properties.WebSearchProperties;
import com.chatbot.model.config.UserPreferences;
import com.chatbot.util.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * 用户配置管理服务
 * 负责用户个人配置的存储、加载和管理
 */
@Service
public class UserPreferencesService {
    
    private static final Logger logger = LoggerFactory.getLogger(UserPreferencesService.class);
    
    private final AppConfig appConfig;
    private final ConcurrentHashMap<String, UserPreferences> userPreferencesCache;
    private static final String PREFERENCES_DIR = "data/user_preferences";
    private static final String DEFAULT_USER_ID = "Taiming";
    
    public UserPreferencesService(AppConfig appConfig) {
        this.appConfig = appConfig;
        this.userPreferencesCache = new ConcurrentHashMap<>();
        initializePreferencesDirectory();
    }
    
    /**
     * 初始化用户配置目录
     */
    private void initializePreferencesDirectory() {
        try {
            String preferencesPath = appConfig.getResource().getFullPath(PREFERENCES_DIR);
            Path dir = Paths.get(preferencesPath);
            if (!Files.exists(dir)) {
                Files.createDirectories(dir);
                logger.info("创建用户配置目录: {}", preferencesPath);
            }
        } catch (Exception e) {
            logger.error("初始化用户配置目录失败", e);
        }
    }
    
    /**
     * 获取用户配置
     * @param userId 用户ID，如果为null则使用默认用户
     * @return 用户配置
     */
    public UserPreferences getUserPreferences(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            userId = DEFAULT_USER_ID;
        }
        
        // 先从缓存获取
        UserPreferences preferences = userPreferencesCache.get(userId);
        if (preferences != null) {
            return preferences;
        }
        
        // 单用户模式：直接基于application.yml创建配置，不使用文件
        preferences = createDefaultPreferencesFromAppConfig(userId);
        logger.debug("单用户模式：直接从application.yml创建配置，userId: {}", userId);
        
        // 缓存配置
        userPreferencesCache.put(userId, preferences);
        return preferences;
    }
    
    /**
     * 保存用户配置
     * @param userId 用户ID
     * @param preferences 用户配置
     * @return 是否保存成功
     */
    public boolean saveUserPreferences(String userId, UserPreferences preferences) {
        if (userId == null || userId.trim().isEmpty()) {
            userId = DEFAULT_USER_ID;
        }
        
        try {
            // 设置用户ID和更新时间
            preferences.setUserId(userId);
            preferences.updateLastModified();
            
            // 单用户模式：只更新内存缓存，不保存到文件
            userPreferencesCache.put(userId, preferences);
            
            logger.debug("用户配置已更新到内存缓存 - userId: {}", userId);
            return true;
        } catch (Exception e) {
            logger.error("保存用户配置失败 - userId: {}", userId, e);
            return false;
        }
    }
    
    /**
     * 更新用户配置的特定字段
     * @param userId 用户ID
     * @param key 配置键
     * @param value 配置值
     * @return 是否更新成功
     */
    public boolean updateUserPreference(String userId, String key, Object value) {
        try {
            UserPreferences preferences = getUserPreferences(userId);
            
            // 使用反射或者switch语句来设置对应的字段
            boolean updated = updatePreferenceField(preferences, key, value);
            
            if (updated) {
                return saveUserPreferences(userId, preferences);
            }
            
            return false;
        } catch (Exception e) {
            logger.error("更新用户配置失败 - userId: {}, key: {}", userId, key, e);
            return false;
        }
    }
    
    /**
     * 获取用户配置的特定字段
     * @param userId 用户ID
     * @param key 配置键
     * @return 配置值
     */
    public Object getUserPreference(String userId, String key) {
        try {
            UserPreferences preferences = getUserPreferences(userId);
            return getPreferenceField(preferences, key);
        } catch (Exception e) {
            logger.error("获取用户配置失败 - userId: {}, key: {}", userId, key, e);
            return null;
        }
    }
    
    /**
     * 重置用户配置为默认值
     * @param userId 用户ID
     * @return 是否重置成功
     */
    public boolean resetUserPreferences(String userId) {
        try {
            UserPreferences defaultPreferences = createDefaultPreferencesFromAppConfig(userId);
            return saveUserPreferences(userId, defaultPreferences);
        } catch (Exception e) {
            logger.error("重置用户配置失败 - userId: {}", userId, e);
            return false;
        }
    }
    
    /**
     * 获取所有已缓存的用户配置
     * @return 用户配置映射
     */
    public Map<String, UserPreferences> getAllCachedPreferences() {
        return new ConcurrentHashMap<>(userPreferencesCache);
    }
    
    /**
     * 清除用户配置缓存
     * @param userId 用户ID，如果为null则清除所有缓存
     */
    public void clearPreferencesCache(String userId) {
        if (userId == null) {
            userPreferencesCache.clear();
            logger.info("清除所有用户配置缓存");
        } else {
            userPreferencesCache.remove(userId);
            logger.info("清除用户配置缓存 - userId: {}", userId);
        }
    }
    
    /**
     * 从文件加载用户配置
     */
    private UserPreferences loadUserPreferencesFromFile(String userId) {
        try {
            String fileName = userId + ".json";
            String filePath = appConfig.getResource().getFullPath(PREFERENCES_DIR + "/" + fileName);
            Path path = Paths.get(filePath);
            
            if (!Files.exists(path)) {
                return null;
            }
            
            String json = Files.readString(path);
            UserPreferences preferences = JsonUtil.fromJson(json, UserPreferences.class);
            
            logger.debug("从文件加载用户配置 - userId: {}, file: {}", userId, filePath);
            return preferences;
        } catch (Exception e) {
            logger.error("从文件加载用户配置失败 - userId: {}", userId, e);
            return null;
        }
    }
    
    /**
     * 保存用户配置到文件
     */
    private void saveUserPreferencesToFile(String userId, UserPreferences preferences) throws IOException {
        String fileName = userId + ".json";
        String filePath = appConfig.getResource().getFullPath(PREFERENCES_DIR + "/" + fileName);
        Path path = Paths.get(filePath);
        
        // 确保目录存在
        Files.createDirectories(path.getParent());
        
        String json = JsonUtil.toJson(preferences);
        Files.writeString(path, json);
        
        logger.debug("保存用户配置到文件 - userId: {}, file: {}", userId, filePath);
    }
    
    /**
     * 基于application.yml创建默认用户配置
     */
    private UserPreferences createDefaultPreferencesFromAppConfig(String userId) {
        UserPreferences preferences = new UserPreferences(userId);
        
        // LLM配置
        LLMProperties llmConfig = appConfig.getLlm();
        preferences.getLlm().setProvider("ollama");
        preferences.getLlm().setBaseUrl(llmConfig.getBaseUrl());
        preferences.getLlm().setModel(llmConfig.getModel());
        preferences.getLlm().setTimeout(llmConfig.getTimeout());
        preferences.getLlm().setMaxTokens(llmConfig.getMaxTokens());
        preferences.getLlm().setStream(llmConfig.isStream());
        
        // 流式输出配置
        AIProperties aiConfig = appConfig.getAi();
        preferences.getStreaming().setChunkSize(aiConfig.getStreamingChunkSize());
        preferences.getStreaming().setDelayMs(aiConfig.getStreamingDelayMs());
        
        // ASR配置
        preferences.getAsr().setModel(aiConfig.getVoiceAsrModel());
        preferences.getAsr().setEnabled(false);  // 默认禁用
        
        // TTS配置
        preferences.getTts().setPreferredSpeaker(aiConfig.getVoiceTtsVoice());
        preferences.getTts().setEnabled(false);  // 默认禁用
        
        // 联网搜索配置
        WebSearchProperties webSearchConfig = appConfig.getWebSearch();
        preferences.getWebSearch().setEnabled(webSearchConfig.isEnabled());
        preferences.getWebSearch().setMaxResults(webSearchConfig.getMaxResults());
        preferences.getWebSearch().setTimeout(webSearchConfig.getTimeoutSeconds());
        preferences.getWebSearch().setEngine(webSearchConfig.getDefaultEngine());
        
        // 输出通道配置 - 聊天窗口
        preferences.getOutputChannel().getChatWindow().setEnabled(false);
        preferences.getOutputChannel().getChatWindow().setAutoTTS(false);
        preferences.getOutputChannel().getChatWindow().setMode("text_only");
        
        // 输出通道配置 - Live2D
        preferences.getOutputChannel().getLive2d().setEnabled(false);
        
        logger.info("创建默认用户配置 - userId: {}, TTS状态: 禁用, ASR状态: 禁用", userId);
        return preferences;
    }
    
    /**
     * 更新配置字段（使用新的模块化API）
     */
    private boolean updatePreferenceField(UserPreferences preferences, String key, Object value) {
        try {
            switch (key) {
                // 基础设置
                case "language":
                    preferences.getBasic().setLanguage((String) value);
                    return true;
                    
                // 语音设置 - TTS
                case "enableVoice":
                    preferences.getTts().setEnabled((Boolean) value);
                    return true;
                case "preferredSpeakerId":
                    preferences.getTts().setPreferredSpeaker((String) value);
                    return true;
                case "responseSpeed":
                    preferences.getTts().setSpeed(((Number) value).doubleValue());
                    return true;
                    
                // 语音设置 - ASR
                case "asrModel":
                    preferences.getAsr().setModel((String) value);
                    return true;
                    
                // 界面设置
                case "darkMode":
                    preferences.getUi().setDarkMode((Boolean) value);
                    return true;
                case "enableAnimations":
                    preferences.getUi().setEnableAnimations((Boolean) value);
                    return true;
                case "autoScroll":
                    preferences.getUi().setAutoScroll((Boolean) value);
                    return true;
                case "soundNotification":
                    preferences.getUi().setSoundNotification((Boolean) value);
                    return true;
                    
                // LLM设置
                case "ollamaBaseUrl":
                    preferences.getLlm().setBaseUrl((String) value);
                    return true;
                case "ollamaModel":
                    preferences.getLlm().setModel((String) value);
                    return true;
                case "ollamaTimeout":
                    preferences.getLlm().setTimeout(((Number) value).intValue());
                    return true;
                case "ollamaMaxTokens":
                    preferences.getLlm().setMaxTokens(((Number) value).intValue());
                    return true;
                case "ollamaStream":
                    preferences.getLlm().setStream((Boolean) value);
                    return true;
                    
                // 联网搜索设置
                case "webSearchEnabled":
                    preferences.getWebSearch().setEnabled((Boolean) value);
                    return true;
                case "webSearchMaxResults":
                    preferences.getWebSearch().setMaxResults(((Number) value).intValue());
                    return true;
                case "webSearchTimeout":
                    preferences.getWebSearch().setTimeout(((Number) value).intValue());
                    return true;
                case "webSearchEngine":
                    preferences.getWebSearch().setEngine((String) value);
                    return true;
                    
                // 流式输出设置
                case "streamingChunkSize":
                    preferences.getStreaming().setChunkSize(((Number) value).intValue());
                    return true;
                case "streamingDelayMs":
                    preferences.getStreaming().setDelayMs(((Number) value).intValue());
                    return true;
                    
                // 自定义设置
                default:
                    preferences.setCustomSetting(key, value);
                    return true;
            }
        } catch (Exception e) {
            logger.error("更新配置字段失败 - key: {}, value: {}", key, value, e);
            return false;
        }
    }
    
    /**
     * 获取配置字段值（使用新的模块化API）
     */
    private Object getPreferenceField(UserPreferences preferences, String key) {
        switch (key) {
            // 基础设置
            case "userId": return preferences.getUserId();
            case "language": return preferences.getBasic().getLanguage();
            case "lastUpdated": return preferences.getLastUpdated();
            
            // 语音设置 - TTS
            case "enableVoice": return preferences.getTts().isEnabled();
            case "preferredSpeakerId": return preferences.getTts().getPreferredSpeaker();
            case "responseSpeed": return preferences.getTts().getSpeed();
            
            // 语音设置 - ASR
            case "asrModel": return preferences.getAsr().getModel();
            
            // 界面设置
            case "darkMode": return preferences.getUi().isDarkMode();
            case "enableAnimations": return preferences.getUi().isEnableAnimations();
            case "autoScroll": return preferences.getUi().isAutoScroll();
            case "soundNotification": return preferences.getUi().isSoundNotification();
            
            // LLM设置
            case "ollamaBaseUrl": return preferences.getLlm().getBaseUrl();
            case "ollamaModel": return preferences.getLlm().getModel();
            case "ollamaTimeout": return preferences.getLlm().getTimeout();
            case "ollamaMaxTokens": return preferences.getLlm().getMaxTokens();
            case "ollamaStream": return preferences.getLlm().isStream();
            
            // 联网搜索设置
            case "webSearchEnabled": return preferences.getWebSearch().isEnabled();
            case "webSearchMaxResults": return preferences.getWebSearch().getMaxResults();
            case "webSearchTimeout": return preferences.getWebSearch().getTimeout();
            case "webSearchEngine": return preferences.getWebSearch().getEngine();
            
            // 流式输出设置
            case "streamingChunkSize": return preferences.getStreaming().getChunkSize();
            case "streamingDelayMs": return preferences.getStreaming().getDelayMs();
            
            // 自定义设置
            default: return preferences.getCustomSetting(key);
        }
    }
}

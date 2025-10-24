package com.chatbot.controller;

import com.chatbot.model.UserPreferences;
import com.chatbot.service.UserPreferencesService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 用户配置控制器
 * 提供用户配置的REST API接口
 */
@RestController
@RequestMapping("/api/preferences")
public class UserPreferencesController {
    
    private static final Logger logger = LoggerFactory.getLogger(UserPreferencesController.class);
    
    private final UserPreferencesService userPreferencesService;
    
    public UserPreferencesController(UserPreferencesService userPreferencesService) {
        this.userPreferencesService = userPreferencesService;
    }
    
    /**
     * 获取用户配置
     * @param userId 用户ID（可选，默认为default）
     * @return 用户配置
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getUserPreferences(
            @RequestParam(value = "userId", required = false) String userId) {
        try {
            UserPreferences preferences = userPreferencesService.getUserPreferences(userId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", preferences);
            response.put("message", "获取用户配置成功");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("获取用户配置失败", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "获取用户配置失败: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 保存用户配置
     * @param userId 用户ID（可选，默认为default）
     * @param preferences 用户配置
     * @return 保存结果
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> saveUserPreferences(
            @RequestParam(value = "userId", required = false) String userId,
            @RequestBody UserPreferences preferences) {
        try {
            boolean success = userPreferencesService.saveUserPreferences(userId, preferences);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", success);
            response.put("message", success ? "保存用户配置成功" : "保存用户配置失败");
            
            if (success) {
                response.put("data", preferences);
            }
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("保存用户配置失败", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "保存用户配置失败: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 更新用户配置的特定字段
     * @param userId 用户ID（可选，默认为default）
     * @param key 配置键
     * @param value 配置值
     * @return 更新结果
     */
    @PutMapping("/{key}")
    public ResponseEntity<Map<String, Object>> updateUserPreference(
            @RequestParam(value = "userId", required = false) String userId,
            @PathVariable String key,
            @RequestBody Object value) {
        try {
            boolean success = userPreferencesService.updateUserPreference(userId, key, value);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", success);
            response.put("message", success ? "更新用户配置成功" : "更新用户配置失败");
            
            if (success) {
                response.put("key", key);
                response.put("value", value);
            }
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("更新用户配置失败 - key: {}", key, e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "更新用户配置失败: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 获取用户配置的特定字段
     * @param userId 用户ID（可选，默认为default）
     * @param key 配置键
     * @return 配置值
     */
    @GetMapping("/{key}")
    public ResponseEntity<Map<String, Object>> getUserPreference(
            @RequestParam(value = "userId", required = false) String userId,
            @PathVariable String key) {
        try {
            Object value = userPreferencesService.getUserPreference(userId, key);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("key", key);
            response.put("value", value);
            response.put("message", "获取用户配置成功");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("获取用户配置失败 - key: {}", key, e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "获取用户配置失败: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 重置用户配置为默认值
     * @param userId 用户ID（可选，默认为default）
     * @return 重置结果
     */
    @PostMapping("/reset")
    public ResponseEntity<Map<String, Object>> resetUserPreferences(
            @RequestParam(value = "userId", required = false) String userId) {
        try {
            boolean success = userPreferencesService.resetUserPreferences(userId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", success);
            response.put("message", success ? "重置用户配置成功" : "重置用户配置失败");
            
            if (success) {
                UserPreferences preferences = userPreferencesService.getUserPreferences(userId);
                response.put("data", preferences);
            }
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("重置用户配置失败", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "重置用户配置失败: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 批量更新用户配置
     * @param userId 用户ID（可选，默认为default）
     * @param updates 配置更新映射
     * @return 更新结果
     */
    @PutMapping("/batch")
    public ResponseEntity<Map<String, Object>> batchUpdateUserPreferences(
            @RequestParam(value = "userId", required = false) String userId,
            @RequestBody Map<String, Object> updates) {
        try {
            Map<String, Object> results = new HashMap<>();
            Map<String, Object> errors = new HashMap<>();
            
            for (Map.Entry<String, Object> entry : updates.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                
                try {
                    boolean success = userPreferencesService.updateUserPreference(userId, key, value);
                    results.put(key, success);
                    
                    if (!success) {
                        errors.put(key, "更新失败");
                    }
                } catch (Exception e) {
                    results.put(key, false);
                    errors.put(key, e.getMessage());
                    logger.error("批量更新用户配置失败 - key: {}", key, e);
                }
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", errors.isEmpty());
            response.put("results", results);
            response.put("message", errors.isEmpty() ? "批量更新用户配置成功" : "部分配置更新失败");
            
            if (!errors.isEmpty()) {
                response.put("errors", errors);
            }
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("批量更新用户配置失败", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "批量更新用户配置失败: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 获取配置模板（包含所有可配置项及其默认值）
     * @return 配置模板
     */
    @GetMapping("/template")
    public ResponseEntity<Map<String, Object>> getPreferencesTemplate() {
        try {
            UserPreferences template = userPreferencesService.getUserPreferences("template");
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", template);
            response.put("message", "获取配置模板成功");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("获取配置模板失败", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "获取配置模板失败: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 更新TTS配置
     * @param userId 用户ID（可选，默认为default）
     * @param ttsConfig TTS配置
     * @return 更新结果
     */
    @PostMapping("/tts")
    public ResponseEntity<Map<String, Object>> updateTTSConfig(
            @RequestParam(value = "userId", required = false) String userId,
            @RequestBody Map<String, Object> ttsConfig) {
        try {
            logger.info("更新TTS配置: userId={}, config={}", userId, ttsConfig);
            
            UserPreferences preferences = userPreferencesService.getUserPreferences(userId);
            
            // 更新ChatOutputConfig
            if (preferences.getChatOutput() == null) {
                preferences.setChatOutput(new UserPreferences.ChatOutputConfig());
            }
            
            UserPreferences.ChatOutputConfig chatOutput = preferences.getChatOutput();
            
            // 更新配置字段
            if (ttsConfig.containsKey("chatEnabled")) {
                boolean enabled = (Boolean) ttsConfig.get("chatEnabled");
                chatOutput.setEnabled(enabled);
                chatOutput.setAutoTTS(enabled);
            }
            
            if (ttsConfig.containsKey("chatMode")) {
                String mode = (String) ttsConfig.get("chatMode");
                chatOutput.setMode(mode);
            }
            
            if (ttsConfig.containsKey("speakerId")) {
                String speakerId = (String) ttsConfig.get("speakerId");
                if (speakerId != null && !speakerId.isEmpty()) {
                    chatOutput.setSpeakerId(speakerId);
                    preferences.setPreferredSpeakerId(speakerId); // 同时更新全局配置
                }
            }
            
            if (ttsConfig.containsKey("speed")) {
                Double speed = ((Number) ttsConfig.get("speed")).doubleValue();
                preferences.setResponseSpeed(speed);
            }
            
            // 保存配置
            boolean success = userPreferencesService.saveUserPreferences(userId, preferences);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", success);
            response.put("message", success ? "TTS配置更新成功" : "TTS配置更新失败");
            
            if (success) {
                response.put("data", chatOutput);
            }
            
            logger.info("TTS配置更新结果: success={}", success);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("更新TTS配置失败", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "更新TTS配置失败: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 清除用户配置缓存
     * @param userId 用户ID（可选，如果不提供则清除所有缓存）
     * @return 清除结果
     */
    @DeleteMapping("/cache")
    public ResponseEntity<Map<String, Object>> clearPreferencesCache(
            @RequestParam(value = "userId", required = false) String userId) {
        try {
            userPreferencesService.clearPreferencesCache(userId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", userId == null ? "清除所有用户配置缓存成功" : "清除用户配置缓存成功");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("清除用户配置缓存失败", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "清除用户配置缓存失败: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }
}

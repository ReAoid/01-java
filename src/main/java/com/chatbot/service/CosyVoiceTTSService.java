package com.chatbot.service;

import com.chatbot.config.AppConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * CosyVoice TTS服务实现
 * 提供健康检查、自定义说话人管理和语音合成功能
 */
@Service
public class CosyVoiceTTSService {
    
    private static final Logger logger = LoggerFactory.getLogger(CosyVoiceTTSService.class);
    
    private final OkHttpClient okHttpClient;
    private final ObjectMapper objectMapper;
    private final String ttsBaseUrl;
    private final AppConfig.PythonApiConfig.ServicesConfig servicesConfig;
    
    // 默认配置
    private static final int TIMEOUT_SECONDS = 60;
    
    public CosyVoiceTTSService(AppConfig appConfig) {
        // 初始化OkHttp客户端
        this.okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(Duration.ofSeconds(10))
                .readTimeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                .writeTimeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                .build();
        
        this.objectMapper = new ObjectMapper();
        
        // 从配置中获取TTS服务地址
        AppConfig.PythonApiConfig pythonConfig = appConfig.getPython();
        if (pythonConfig != null && pythonConfig.getServices() != null && 
            pythonConfig.getServices().getTtsUrl() != null) {
            // 使用配置的TTS URL
            this.ttsBaseUrl = pythonConfig.getServices().getTtsUrl();
            this.servicesConfig = pythonConfig.getServices();
        } else {
            // 默认值
            this.ttsBaseUrl = "http://localhost:50000";
            this.servicesConfig = null;
        }
        
        logger.info("CosyVoice TTS服务初始化，服务地址: {}", this.ttsBaseUrl);
    }
    
    /**
     * 健康检查
     * @return 健康检查结果
     */
    public HealthCheckResult healthCheck() {
        logger.info("执行健康检查");
        String healthUrl = ttsBaseUrl + "/health";
        
        Request request = new Request.Builder()
                .url(healthUrl)
                .get()
                .build();
        
        try (Response response = okHttpClient.newCall(request).execute()) {
            logger.info("健康检查响应状态: {}", response.code());
            
            String responseBody = response.body().string();
            logger.info("健康检查响应内容: {}", responseBody);
            
            if (response.isSuccessful()) {
                JsonNode jsonResponse = objectMapper.readTree(responseBody);
                boolean isHealthy = "healthy".equals(jsonResponse.get("status").asText());
                
                logger.info("健康检查结果: {}", isHealthy ? "通过" : "失败");
                return new HealthCheckResult(true, isHealthy, responseBody);
            } else {
                logger.warn("健康检查失败，HTTP状态: {}", response.code());
                return new HealthCheckResult(false, false, "HTTP " + response.code());
            }
        } catch (Exception e) {
            logger.error("健康检查异常", e);
            return new HealthCheckResult(false, false, e.getMessage());
        }
    }
    
    /**
     * 注册自定义说话人
     * @param speakerName 说话人名称
     * @param referenceText 参考文本
     * @param referenceAudio 参考音频数据
     * @return 注册结果
     */
    public SpeakerRegistrationResult registerCustomSpeaker(String speakerName, String referenceText, byte[] referenceAudio) {
        logger.info("注册自定义说话人: {}", speakerName);
        logger.info("参考文本: {}", referenceText);
        logger.info("参考音频大小: {} 字节", referenceAudio.length);
        
        // 使用OkHttp构建multipart请求
        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("speaker_name", speakerName)
                .addFormDataPart("reference_text", referenceText)
                .addFormDataPart("reference_audio", speakerName + ".wav", 
                    RequestBody.create(referenceAudio, MediaType.parse("audio/wav")))
                .build();
        
        String registerUrl = ttsBaseUrl + "/register_speaker";
        
        Request request = new Request.Builder()
                .url(registerUrl)
                .post(requestBody)
                .build();
        
        try (Response response = okHttpClient.newCall(request).execute()) {
            logger.info("注册响应状态: {}", response.code());
            
            String responseBodyStr = response.body().string();
            logger.info("注册响应内容: {}", responseBodyStr);
            
            if (response.isSuccessful()) {
                JsonNode result = objectMapper.readTree(responseBodyStr);
                String registeredName = result.get("speaker_name").asText();
                logger.info("自定义说话人注册成功: {}", registeredName);
                return new SpeakerRegistrationResult(true, "注册成功", registeredName);
            } else {
                logger.warn("自定义说话人注册失败: HTTP {}", response.code());
                return new SpeakerRegistrationResult(false, "注册失败: HTTP " + response.code(), null);
            }
        } catch (Exception e) {
            logger.error("注册自定义说话人异常", e);
            return new SpeakerRegistrationResult(false, "注册异常: " + e.getMessage(), null);
        }
    }
    
    /**
     * 使用自定义说话人进行语音合成
     * @param text 要合成的文本
     * @param speakerName 自定义说话人名称
     * @param speed 语速（默认1.0）
     * @param format 音频格式（默认wav）
     * @return 合成结果
     */
    public SynthesisResult customSpeakerSynthesis(String text, String speakerName, Double speed, String format) {
        logger.info("使用自定义说话人合成语音");
        logger.info("文本: {}", text);
        logger.info("说话人: {}", speakerName);
        
        // 设置默认值
        double actualSpeed = speed != null ? speed : 1.0;
        String actualFormat = format != null ? format : "wav";
        
        // 使用OkHttp构建multipart/form-data请求
        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("tts_text", text)
                .addFormDataPart("spk_id", speakerName)
                .addFormDataPart("stream", "false")
                .addFormDataPart("speed", String.valueOf(actualSpeed))
                .addFormDataPart("format", actualFormat)
                .build();
        
        String synthesisUrl = ttsBaseUrl + "/inference_custom_speaker";
        
        Request request = new Request.Builder()
                .url(synthesisUrl)
                .post(requestBody)
                .build();
        
        try (Response response = okHttpClient.newCall(request).execute()) {
            logger.info("合成响应状态: {}", response.code());
            
            if (response.isSuccessful()) {
                byte[] audioData = response.body().bytes();
                logger.info("合成成功，音频大小: {} 字节", audioData.length);
                return new SynthesisResult(true, "合成成功", audioData);
            } else {
                logger.warn("自定义说话人合成失败: HTTP {}", response.code());
                return new SynthesisResult(false, "合成失败: HTTP " + response.code(), null);
            }
        } catch (Exception e) {
            logger.error("自定义说话人合成异常", e);
            return new SynthesisResult(false, "合成异常: " + e.getMessage(), null);
        }
    }
    
    /**
     * 获取说话人列表
     * @return 说话人查询结果
     */
    public SpeakerListResult getSpeakers() {
        logger.info("获取说话人列表");
        
        // 使用配置的URL构建方法或回退到传统方式
        String speakersUrl = ttsBaseUrl + "/model/speakers";
        
        Request request = new Request.Builder()
                .url(speakersUrl)
                .get()
                .build();
        
        try (Response response = okHttpClient.newCall(request).execute()) {
            logger.info("说话人列表响应状态: {}", response.code());
            
            String responseBody = response.body().string();
            logger.info("说话人列表响应内容: {}", responseBody);
            
            if (response.isSuccessful()) {
                JsonNode result = objectMapper.readTree(responseBody);
                JsonNode builtinSpeakers = result.get("builtin_speakers");
                JsonNode customSpeakers = result.get("custom_speakers");
                
                logger.info("说话人列表获取成功");
                return new SpeakerListResult(true, "获取成功", builtinSpeakers, customSpeakers, responseBody);
            } else {
                logger.warn("说话人列表获取失败: HTTP {}", response.code());
                return new SpeakerListResult(false, "获取失败: HTTP " + response.code(), null, null, responseBody);
            }
        } catch (Exception e) {
            logger.error("获取说话人列表异常", e);
            return new SpeakerListResult(false, "获取异常: " + e.getMessage(), null, null, null);
        }
    }
    
    /**
     * 删除自定义说话人
     * @param speakerName 要删除的说话人名称
     * @return 删除结果
     */
    public SpeakerDeletionResult deleteCustomSpeaker(String speakerName) {
        logger.info("删除自定义说话人: {}", speakerName);
        
        String deleteUrl = ttsBaseUrl + "/speaker/" + speakerName;
        
        Request request = new Request.Builder()
                .url(deleteUrl)
                .delete()
                .build();
        
        try (Response response = okHttpClient.newCall(request).execute()) {
            logger.info("删除响应状态: {}", response.code());
            
            String responseBody = response.body().string();
            logger.info("删除响应内容: {}", responseBody);
            
            if (response.isSuccessful()) {
                logger.info("自定义说话人删除成功: {}", speakerName);
                return new SpeakerDeletionResult(true, "删除成功", speakerName);
            } else {
                logger.warn("自定义说话人删除失败: HTTP {}", response.code());
                return new SpeakerDeletionResult(false, "删除失败: HTTP " + response.code(), speakerName);
            }
        } catch (Exception e) {
            logger.error("删除自定义说话人异常", e);
            return new SpeakerDeletionResult(false, "删除异常: " + e.getMessage(), speakerName);
        }
    }
    
    // ==================== 结果类定义 ====================
    
    /**
     * 健康检查结果
     */
    public static class HealthCheckResult {
        private final boolean success;
        private final boolean healthy;
        private final String message;
        
        public HealthCheckResult(boolean success, boolean healthy, String message) {
            this.success = success;
            this.healthy = healthy;
            this.message = message;
        }
        
        public boolean isSuccess() { return success; }
        public boolean isHealthy() { return healthy; }
        public String getMessage() { return message; }
        
        @Override
        public String toString() {
            return String.format("HealthCheckResult{success=%s, healthy=%s, message='%s'}", 
                               success, healthy, message);
        }
    }
    
    /**
     * 说话人注册结果
     */
    public static class SpeakerRegistrationResult {
        private final boolean success;
        private final String message;
        private final String speakerName;
        
        public SpeakerRegistrationResult(boolean success, String message, String speakerName) {
            this.success = success;
            this.message = message;
            this.speakerName = speakerName;
        }
        
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public String getSpeakerName() { return speakerName; }
        
        @Override
        public String toString() {
            return String.format("SpeakerRegistrationResult{success=%s, message='%s', speakerName='%s'}", 
                               success, message, speakerName);
        }
    }
    
    /**
     * 语音合成结果
     */
    public static class SynthesisResult {
        private final boolean success;
        private final String message;
        private final byte[] audioData;
        
        public SynthesisResult(boolean success, String message, byte[] audioData) {
            this.success = success;
            this.message = message;
            this.audioData = audioData;
        }
        
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public byte[] getAudioData() { return audioData; }
        
        @Override
        public String toString() {
            return String.format("SynthesisResult{success=%s, message='%s', audioSize=%d}", 
                               success, message, audioData != null ? audioData.length : 0);
        }
    }
    
    /**
     * 说话人列表查询结果
     */
    public static class SpeakerListResult {
        private final boolean success;
        private final String message;
        private final JsonNode builtinSpeakers;
        private final JsonNode customSpeakers;
        private final String rawResponse;
        
        public SpeakerListResult(boolean success, String message, JsonNode builtinSpeakers, 
                               JsonNode customSpeakers, String rawResponse) {
            this.success = success;
            this.message = message;
            this.builtinSpeakers = builtinSpeakers;
            this.customSpeakers = customSpeakers;
            this.rawResponse = rawResponse;
        }
        
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public JsonNode getBuiltinSpeakers() { return builtinSpeakers; }
        public JsonNode getCustomSpeakers() { return customSpeakers; }
        public String getRawResponse() { return rawResponse; }
        
        @Override
        public String toString() {
            int builtinCount = builtinSpeakers != null ? builtinSpeakers.size() : 0;
            int customCount = customSpeakers != null ? customSpeakers.size() : 0;
            return String.format("SpeakerListResult{success=%s, message='%s', builtinCount=%d, customCount=%d}", 
                               success, message, builtinCount, customCount);
        }
    }
    
    /**
     * 说话人删除结果
     */
    public static class SpeakerDeletionResult {
        private final boolean success;
        private final String message;
        private final String speakerName;
        
        public SpeakerDeletionResult(boolean success, String message, String speakerName) {
            this.success = success;
            this.message = message;
            this.speakerName = speakerName;
        }
        
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public String getSpeakerName() { return speakerName; }
        
        @Override
        public String toString() {
            return String.format("SpeakerDeletionResult{success=%s, message='%s', speakerName='%s'}", 
                               success, message, speakerName);
        }
    }
}

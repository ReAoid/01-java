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
 * CosyVoice TTS服务实现（已废弃）
 * 
 * @deprecated 请使用 {@link com.chatbot.service.tts.impl.CosyVoiceTTSServiceImpl} 
 *             和 {@link com.chatbot.service.tts.TTSService} 接口代替
 * 
 * 此类保留用于向后兼容，将在未来版本中删除。
 * 内部类（HealthCheckResult等）仍在Controller中临时使用，
 * 请尽快迁移到统一的 ApiResult 和 HealthCheckResult。
 */
@Deprecated
@Service("legacyCosyVoiceTTSService")
public class CosyVoiceTTSService {
    
    private static final Logger logger = LoggerFactory.getLogger(CosyVoiceTTSService.class);
    
    private final OkHttpClient okHttpClient;
    private final ObjectMapper objectMapper;
    private final String ttsBaseUrl;
    private final AppConfig.PythonApiConfig.ServicesConfig servicesConfig;
    
    public CosyVoiceTTSService(AppConfig appConfig) {
        // 从配置中获取超时设置
        AppConfig.TimeoutConfig timeoutConfig = appConfig.getPython().getTimeout();
        
        // 初始化OkHttp客户端
        this.okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(Duration.ofSeconds(timeoutConfig.getConnectTimeoutSeconds()))
                .readTimeout(Duration.ofSeconds(timeoutConfig.getReadTimeoutSeconds()))
                .writeTimeout(Duration.ofSeconds(timeoutConfig.getWriteTimeoutSeconds()))
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
        
        // 文本验证和预处理
        String validatedText = validateAndPreprocessText(text);
        if (validatedText == null) {
            return new SynthesisResult(false, "文本验证失败：文本为空或包含无效字符", null);
        }
        
        // 设置默认值
        double actualSpeed = speed != null ? speed : 1.0;
        String actualFormat = format != null ? format : "wav";
        
        // 使用重试机制进行合成
        return performSynthesisWithRetry(validatedText, speakerName, actualSpeed, actualFormat);
    }
    
    /**
     * 文本验证和预处理
     * @param text 原始文本
     * @return 处理后的文本，如果无效返回null
     */
    private String validateAndPreprocessText(String text) {
        if (text == null || text.trim().isEmpty()) {
            logger.warn("文本为空或只包含空格");
            return null;
        }
        
        // 移除控制字符和特殊字符
        String cleaned = text.replaceAll("[\\p{Cntrl}&&[^\r\n\t]]", "")
                             .replaceAll("[\u200B-\u200D\uFEFF]", "") // 零宽字符
                             .trim();
        
        if (cleaned.isEmpty()) {
            logger.warn("文本清理后为空");
            return null;
        }
        
        // 限制文本长度
        if (cleaned.length() > 500) {
            logger.warn("文本过长，截断到500字符: 原长度={}", cleaned.length());
            cleaned = cleaned.substring(0, 500);
        }
        
        logger.debug("文本预处理完成: 原长度={}, 处理后长度={}", text.length(), cleaned.length());
        return cleaned;
    }
    
    /**
     * 带重试机制的语音合成
     * @param text 验证后的文本
     * @param speakerName 说话人名称
     * @param speed 语速
     * @param format 格式
     * @return 合成结果
     */
    private SynthesisResult performSynthesisWithRetry(String text, String speakerName, double speed, String format) {
        final int maxRetries = 3;
        final long baseDelayMs = 1000; // 1秒基础延迟
        
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                logger.debug("TTS合成尝试 {}/{}", attempt, maxRetries);
                
                // 构建请求
                RequestBody requestBody = new MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart("tts_text", text)
                        .addFormDataPart("spk_id", speakerName)
                        .addFormDataPart("stream", "false")
                        .addFormDataPart("speed", String.valueOf(speed))
                        .addFormDataPart("format", format)
                        .build();
                
                String synthesisUrl = ttsBaseUrl + "/inference_custom_speaker";
                
                Request request = new Request.Builder()
                        .url(synthesisUrl)
                        .post(requestBody)
                        .build();
                
                try (Response response = okHttpClient.newCall(request).execute()) {
                    logger.info("合成响应状态: {} (尝试 {}/{})", response.code(), attempt, maxRetries);
                    
                    if (response.isSuccessful()) {
                        byte[] audioData = response.body().bytes();
                        logger.info("合成成功，音频大小: {} 字节 (尝试 {}/{})", audioData.length, attempt, maxRetries);
                        return new SynthesisResult(true, "合成成功", audioData);
                    } else if (response.code() >= 500 && attempt < maxRetries) {
                        // 5xx错误且还有重试机会
                        logger.warn("服务器错误 HTTP {}, 将在 {}ms 后重试 (尝试 {}/{})", 
                                   response.code(), baseDelayMs * (1L << (attempt - 1)), attempt, maxRetries);
                        Thread.sleep(baseDelayMs * (1L << (attempt - 1))); // 指数退避
                        continue;
                    } else {
                        // 4xx错误或最后一次尝试失败
                        logger.error("自定义说话人合成失败: HTTP {} (尝试 {}/{})", response.code(), attempt, maxRetries);
                        return new SynthesisResult(false, "合成失败: HTTP " + response.code(), null);
                    }
                }
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.error("TTS合成被中断 (尝试 {}/{})", attempt, maxRetries);
                return new SynthesisResult(false, "合成被中断", null);
            } catch (Exception e) {
                if (attempt < maxRetries) {
                    logger.warn("TTS合成异常，将重试 (尝试 {}/{}): {}", attempt, maxRetries, e.getMessage());
                    try {
                        Thread.sleep(baseDelayMs * (1L << (attempt - 1))); // 指数退避
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return new SynthesisResult(false, "合成被中断", null);
                    }
                } else {
                    logger.error("自定义说话人合成异常 (最终失败，尝试 {}/{})", attempt, maxRetries, e);
                    return new SynthesisResult(false, "合成异常: " + e.getMessage(), null);
                }
            }
        }
        
        return new SynthesisResult(false, "合成失败：超过最大重试次数", null);
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

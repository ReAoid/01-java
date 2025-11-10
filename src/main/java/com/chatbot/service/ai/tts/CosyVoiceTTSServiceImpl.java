package com.chatbot.service.ai.tts;

import com.chatbot.config.AppConfig;
import com.chatbot.config.properties.PythonApiProperties;
import com.chatbot.config.properties.TimeoutProperties;
import com.chatbot.model.dto.common.ApiResult;
import com.chatbot.model.dto.common.HealthCheckResult;
import com.chatbot.model.dto.tts.SpeakerInfo;
import com.chatbot.model.dto.tts.TTSRequest;
import com.chatbot.model.dto.tts.TTSResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * CosyVoice TTS服务实现
 * 将CosyVoice的特定API适配到统一的TTS接口
 */
@Service("cosyVoiceTTSService")
public class CosyVoiceTTSServiceImpl implements TTSService {
    
    private static final Logger logger = LoggerFactory.getLogger(CosyVoiceTTSServiceImpl.class);
    
    private final OkHttpClient okHttpClient;
    private final ObjectMapper objectMapper;
    private final String ttsBaseUrl;
    
    public CosyVoiceTTSServiceImpl(AppConfig appConfig) {
        // 从配置中获取超时设置
        TimeoutProperties timeoutConfig = appConfig.getPython().getTimeout();
        
        // 初始化OkHttp客户端
        this.okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(Duration.ofSeconds(timeoutConfig.getConnectTimeoutSeconds()))
                .readTimeout(Duration.ofSeconds(timeoutConfig.getReadTimeoutSeconds()))
                .writeTimeout(Duration.ofSeconds(timeoutConfig.getWriteTimeoutSeconds()))
                .build();
        
        this.objectMapper = new ObjectMapper();
        
        // 从配置中获取TTS服务地址
        PythonApiProperties pythonConfig = appConfig.getPython();
        if (pythonConfig != null && pythonConfig.getServices() != null && 
            pythonConfig.getServices().getTtsUrl() != null) {
            this.ttsBaseUrl = pythonConfig.getServices().getTtsUrl();
        } else {
            this.ttsBaseUrl = "http://localhost:50000";
        }
        
        logger.info("CosyVoice TTS服务初始化完成，引擎: {}, URL: {}", getEngineName(), ttsBaseUrl);
    }
    
    @Override
    public String getEngineName() {
        return "CosyVoice";
    }
    
    @Override
    public HealthCheckResult healthCheck() {
        long startTime = System.currentTimeMillis();
        
        try {
            logger.info("执行CosyVoice健康检查");
            String healthUrl = ttsBaseUrl + "/health";
            
            Request request = new Request.Builder()
                    .url(healthUrl)
                    .get()
                    .build();
            
            try (Response response = okHttpClient.newCall(request).execute()) {
                long responseTime = System.currentTimeMillis() - startTime;
                String responseBody = response.body().string();
                
                logger.info("健康检查响应状态: {}, 响应时间: {}ms", response.code(), responseTime);
                
                if (response.isSuccessful()) {
                    JsonNode json = objectMapper.readTree(responseBody);
                    boolean isHealthy = "healthy".equals(json.get("status").asText());
                    String version = json.has("version") ? json.get("version").asText() : "unknown";
                    
                    logger.info("健康检查结果: {}", isHealthy ? "通过" : "失败");
                    
                    return new HealthCheckResult.Builder()
                            .serviceName(getEngineName())
                            .healthy(isHealthy)
                            .responseTime(responseTime)
                            .version(version)
                            .detail("url", ttsBaseUrl)
                            .detail("rawResponse", responseBody)
                            .build();
                } else {
                    logger.warn("健康检查失败，HTTP状态: {}", response.code());
                    return new HealthCheckResult.Builder()
                            .serviceName(getEngineName())
                            .healthy(false)
                            .status("DOWN")
                            .responseTime(responseTime)
                            .detail("httpStatus", response.code())
                            .detail("error", "HTTP " + response.code())
                            .build();
                }
            }
        } catch (Exception e) {
            long responseTime = System.currentTimeMillis() - startTime;
            logger.error("健康检查异常", e);
            
            return new HealthCheckResult.Builder()
                    .serviceName(getEngineName())
                    .healthy(false)
                    .status("DOWN")
                    .responseTime(responseTime)
                    .detail("error", e.getMessage())
                    .build();
        }
    }
    
    @Override
    public ApiResult<TTSResult> synthesize(TTSRequest request) {
        try {
            String text = request.getText();
            logger.info("CosyVoice合成请求: text='{}...', speaker={}, speed={}", 
                       text.length() > 20 ? text.substring(0, 20) : text,
                       request.getSpeakerId(), request.getSpeed());
            
            // 文本验证和预处理
            String validatedText = validateAndPreprocessText(text);
            if (validatedText == null) {
                return ApiResult.failure("文本验证失败：文本为空或包含无效字符", "TTS_INVALID_TEXT");
            }
            
            // 使用重试机制进行合成
            return performSynthesisWithRetry(validatedText, request);
            
        } catch (Exception e) {
            logger.error("CosyVoice合成异常", e);
            return ApiResult.failure("合成异常: " + e.getMessage(), "TTS_SYNTHESIS_ERROR");
        }
    }
    
    @Override
    public CompletableFuture<ApiResult<TTSResult>> synthesizeAsync(TTSRequest request) {
        return CompletableFuture.supplyAsync(() -> synthesize(request));
    }
    
    @Override
    public ApiResult<SpeakerInfo> registerSpeaker(String speakerName, String referenceText, byte[] referenceAudio) {
        try {
            logger.info("注册自定义说话人: {}", speakerName);
            logger.info("参考文本: {}", referenceText);
            logger.info("参考音频大小: {} 字节", referenceAudio.length);
            
            // 构建CosyVoice注册请求
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
                
                if (response.isSuccessful()) {
                    String responseBodyStr = response.body().string();
                    JsonNode json = objectMapper.readTree(responseBodyStr);
                    String registeredName = json.get("speaker_name").asText();
                    
                    // 转换为统一的SpeakerInfo
                    SpeakerInfo speakerInfo = new SpeakerInfo.Builder()
                            .id(registeredName)
                            .name(registeredName)
                            .type("custom")
                            .metadata("engine", getEngineName())
                            .metadata("referenceText", referenceText)
                            .build();
                    
                    logger.info("自定义说话人注册成功: {}", registeredName);
                    return ApiResult.success(speakerInfo, "注册成功");
                } else {
                    logger.warn("自定义说话人注册失败: HTTP {}", response.code());
                    return ApiResult.failure("注册失败: HTTP " + response.code(), "SPEAKER_REGISTRATION_FAILED");
                }
            }
        } catch (Exception e) {
            logger.error("注册自定义说话人异常", e);
            return ApiResult.failure("注册异常: " + e.getMessage(), "SPEAKER_REGISTRATION_ERROR");
        }
    }
    
    @Override
    public ApiResult<Void> deleteSpeaker(String speakerId) {
        try {
            logger.info("删除自定义说话人: {}", speakerId);
            
            String deleteUrl = ttsBaseUrl + "/speaker/" + speakerId;
            Request request = new Request.Builder()
                    .url(deleteUrl)
                    .delete()
                    .build();
            
            try (Response response = okHttpClient.newCall(request).execute()) {
                logger.info("删除响应状态: {}", response.code());
                
                if (response.isSuccessful()) {
                    logger.info("自定义说话人删除成功: {}", speakerId);
                    return ApiResult.success(null, "删除成功");
                } else {
                    logger.warn("自定义说话人删除失败: HTTP {}", response.code());
                    return ApiResult.failure("删除失败: HTTP " + response.code(), "SPEAKER_DELETION_FAILED");
                }
            }
        } catch (Exception e) {
            logger.error("删除自定义说话人异常", e);
            return ApiResult.failure("删除异常: " + e.getMessage(), "SPEAKER_DELETION_ERROR");
        }
    }
    
    @Override
    public ApiResult<List<SpeakerInfo>> listSpeakers() {
        try {
            logger.info("获取说话人列表");
            
            String speakersUrl = ttsBaseUrl + "/model/speakers";
            Request request = new Request.Builder()
                    .url(speakersUrl)
                    .get()
                    .build();
            
            try (Response response = okHttpClient.newCall(request).execute()) {
                logger.info("说话人列表响应状态: {}", response.code());
                
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    JsonNode json = objectMapper.readTree(responseBody);
                    JsonNode builtinSpeakers = json.get("builtin_speakers");
                    JsonNode customSpeakers = json.get("custom_speakers");
                    
                    List<SpeakerInfo> speakers = new ArrayList<>();
                    
                    // 转换内置说话人
                    if (builtinSpeakers != null && builtinSpeakers.isArray()) {
                        for (JsonNode node : builtinSpeakers) {
                            speakers.add(convertToSpeakerInfo(node, "builtin"));
                        }
                    }
                    
                    // 转换自定义说话人
                    if (customSpeakers != null && customSpeakers.isArray()) {
                        for (JsonNode node : customSpeakers) {
                            speakers.add(convertToSpeakerInfo(node, "custom"));
                        }
                    }
                    
                    logger.info("说话人列表获取成功，共{}个", speakers.size());
                    return ApiResult.success(speakers, "获取成功");
                } else {
                    logger.warn("说话人列表获取失败: HTTP {}", response.code());
                    return ApiResult.failure("获取失败: HTTP " + response.code(), "SPEAKER_LIST_FAILED");
                }
            }
        } catch (Exception e) {
            logger.error("获取说话人列表异常", e);
            return ApiResult.failure("获取异常: " + e.getMessage(), "SPEAKER_LIST_ERROR");
        }
    }
    
    @Override
    public ApiResult<SpeakerInfo> getSpeaker(String speakerId) {
        // 通过列表查找特定说话人
        ApiResult<List<SpeakerInfo>> listResult = listSpeakers();
        if (!listResult.isSuccess()) {
            return ApiResult.failure(listResult.getMessage(), listResult.getErrorCode());
        }
        
        for (SpeakerInfo speaker : listResult.getData()) {
            if (speaker.getId().equals(speakerId)) {
                return ApiResult.success(speaker);
            }
        }
        
        return ApiResult.failure("说话人不存在: " + speakerId, "SPEAKER_NOT_FOUND");
    }
    
    // ==================== 私有辅助方法 ====================
    
    /**
     * 文本验证和预处理
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
     */
    private ApiResult<TTSResult> performSynthesisWithRetry(String text, TTSRequest request) {
        final int maxRetries = 3;
        final long baseDelayMs = 1000;
        
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                logger.debug("TTS合成尝试 {}/{}", attempt, maxRetries);
                
                // 构建CosyVoice特定的请求
                RequestBody requestBody = new MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart("tts_text", text)
                        .addFormDataPart("spk_id", request.getSpeakerId())
                        .addFormDataPart("speed", String.valueOf(request.getSpeed()))
                        .addFormDataPart("format", request.getFormat())
                        .addFormDataPart("stream", "false")
                        .build();
                
                String synthesisUrl = ttsBaseUrl + "/inference_custom_speaker";
                Request httpRequest = new Request.Builder()
                        .url(synthesisUrl)
                        .post(requestBody)
                        .build();
                
                try (Response response = okHttpClient.newCall(httpRequest).execute()) {
                    logger.info("合成响应状态: {} (尝试 {}/{})", response.code(), attempt, maxRetries);
                    
                    if (response.isSuccessful()) {
                        byte[] audioData = response.body().bytes();
                        
                        // 转换为统一的TTSResult
                        TTSResult ttsResult = new TTSResult.Builder()
                                .audioData(audioData)
                                .format(request.getFormat())
                                .sampleRate(22050) // CosyVoice默认采样率
                                .metadata("engine", getEngineName())
                                .metadata("speakerId", request.getSpeakerId())
                                .metadata("speed", request.getSpeed())
                                .build();
                        
                        logger.info("合成成功，音频大小: {} bytes (尝试 {}/{})", 
                                   audioData.length, attempt, maxRetries);
                        return ApiResult.success(ttsResult, "合成成功");
                        
                    } else if (response.code() >= 500 && attempt < maxRetries) {
                        // 5xx错误且还有重试机会
                        long delay = baseDelayMs * (1L << (attempt - 1));
                        logger.warn("服务器错误 HTTP {}, 将在 {}ms 后重试 (尝试 {}/{})", 
                                   response.code(), delay, attempt, maxRetries);
                        Thread.sleep(delay);
                        continue;
                    } else {
                        // 4xx错误或最后一次尝试失败
                        logger.error("合成失败: HTTP {} (尝试 {}/{})", 
                                    response.code(), attempt, maxRetries);
                        return ApiResult.failure("合成失败: HTTP " + response.code(), 
                                               "TTS_SYNTHESIS_FAILED");
                    }
                }
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.error("TTS合成被中断 (尝试 {}/{})", attempt, maxRetries);
                return ApiResult.failure("合成被中断", "TTS_INTERRUPTED");
            } catch (Exception e) {
                if (attempt < maxRetries) {
                    logger.warn("TTS合成异常，将重试 (尝试 {}/{}): {}", 
                               attempt, maxRetries, e.getMessage());
                    try {
                        Thread.sleep(baseDelayMs * (1L << (attempt - 1)));
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return ApiResult.failure("合成被中断", "TTS_INTERRUPTED");
                    }
                } else {
                    logger.error("合成异常 (最终失败，尝试 {}/{})", attempt, maxRetries, e);
                    return ApiResult.failure("合成异常: " + e.getMessage(), "TTS_SYNTHESIS_ERROR");
                }
            }
        }
        
        return ApiResult.failure("合成失败：超过最大重试次数", "TTS_MAX_RETRIES_EXCEEDED");
    }
    
    /**
     * 转换JsonNode为SpeakerInfo
     */
    private SpeakerInfo convertToSpeakerInfo(JsonNode node, String type) {
        String id = node.isTextual() ? node.asText() : node.get("id").asText();
        String name = node.has("name") ? node.get("name").asText() : id;
        
        SpeakerInfo.Builder builder = new SpeakerInfo.Builder()
                .id(id)
                .name(name)
                .type(type)
                .metadata("engine", getEngineName());
        
        // 可选字段
        if (node.has("language")) {
            builder.language(node.get("language").asText());
        }
        if (node.has("gender")) {
            builder.gender(node.get("gender").asText());
        }
        
        return builder.build();
    }
}


package com.chatbot.service.asr.impl;

import com.chatbot.config.AppConfig;
import com.chatbot.config.properties.PythonApiProperties;
import com.chatbot.model.dto.asr.ASRConnectionInfo;
import com.chatbot.model.dto.asr.ASRRequest;
import com.chatbot.model.dto.asr.ASRResponse;
import com.chatbot.model.dto.common.ApiResult;
import com.chatbot.model.dto.common.HealthCheckResult;
import com.chatbot.service.asr.ASRService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * FunASR服务实现
 * 实现ASR服务接口，对接FunASR Python服务
 * 
 * 功能特性：
 * - 健康检查
 * - 同步/异步识别
 * - 流式识别支持
 * - WebSocket连接管理
 * 
 * @version 1.0
 */
@Service("funASRService")
public class FunASRServiceImpl implements ASRService {
    
    private static final Logger logger = LoggerFactory.getLogger(FunASRServiceImpl.class);
    
    private final String asrServerUrl;
    private final String asrHealthUrl;
    private final boolean asrEnabled;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    
    // 连接管理
    private final Map<String, ASRConnectionInfo> connections = new ConcurrentHashMap<>();
    private volatile boolean serviceAvailable = false;
    private volatile long lastHealthCheck = 0;
    private static final long HEALTH_CHECK_INTERVAL = 30000; // 30秒
    
    public FunASRServiceImpl(AppConfig appConfig, ObjectMapper objectMapper) {
        PythonApiProperties pythonConfig = appConfig.getPython();
        
        this.asrServerUrl = pythonConfig.getServices() != null && pythonConfig.getServices().getAsrUrl() != null
                ? pythonConfig.getServices().getAsrUrl()
                : "ws://localhost:8767/asr";
        
        this.asrHealthUrl = "http://localhost:8768/health"; // 可以从配置读取
        this.asrEnabled = true; // 可以从配置读取
        
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        
        logger.info("FunASR服务实现初始化，服务地址: {}", this.asrServerUrl);
    }
    
    @Override
    public String getEngineName() {
        return "FunASR";
    }
    
    @Override
    public HealthCheckResult healthCheck() {
        long startTime = System.currentTimeMillis();
        
        if (!asrEnabled) {
            return new HealthCheckResult.Builder()
                    .serviceName(getEngineName())
                    .healthy(false)
                    .status("DISABLED")
                    .responseTime(0)
                    .detail("message", "ASR服务未启用")
                    .build();
        }
        
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(asrHealthUrl))
                    .timeout(Duration.ofSeconds(3))
                    .GET()
                    .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            long responseTime = System.currentTimeMillis() - startTime;
            
            boolean isHealthy = response.statusCode() == 200;
            this.serviceAvailable = isHealthy;
            this.lastHealthCheck = System.currentTimeMillis();
            
            String message = "健康";
            if (isHealthy) {
                try {
                    JsonNode jsonResponse = objectMapper.readTree(response.body());
                    message = jsonResponse.has("message") ? jsonResponse.get("message").asText() : message;
                } catch (Exception e) {
                    logger.debug("解析健康检查响应失败", e);
                }
            }
            
            return new HealthCheckResult.Builder()
                    .serviceName(getEngineName())
                    .healthy(isHealthy)
                    .status(isHealthy ? "UP" : "DOWN")
                    .responseTime(responseTime)
                    .detail("statusCode", response.statusCode())
                    .detail("message", message)
                    .detail("serviceUrl", asrServerUrl)
                    .build();
                    
        } catch (Exception e) {
            long responseTime = System.currentTimeMillis() - startTime;
            this.serviceAvailable = false;
            
            logger.error("ASR健康检查失败", e);
            
            return new HealthCheckResult.Builder()
                    .serviceName(getEngineName())
                    .healthy(false)
                    .status("DOWN")
                    .responseTime(responseTime)
                    .detail("error", e.getMessage())
                    .detail("serviceUrl", asrServerUrl)
                    .build();
        }
    }
    
    @Override
    public ASRConnectionInfo getConnectionInfo() {
        // 如果有活跃连接，返回第一个
        if (!connections.isEmpty()) {
            return connections.values().iterator().next();
        }
        
        return new ASRConnectionInfo(
            asrServerUrl,
            "disconnected",
            null,
            0,
            null
        );
    }
    
    @Override
    public ApiResult<ASRResponse> recognize(ASRRequest request) {
        // 同步识别实现
        if (!asrEnabled || !serviceAvailable) {
            return ApiResult.failure("ASR_SERVICE_UNAVAILABLE", "ASR服务不可用");
        }
        
        if (request.getAudioData() == null || request.getAudioData().isEmpty()) {
            return ApiResult.failure("INVALID_REQUEST", "音频数据为空");
        }
        
        try {
            // 这里可以实现HTTP方式的ASR识别
            // 目前主要使用WebSocket流式识别，所以返回不支持
            return ApiResult.failure("NOT_SUPPORTED", "请使用流式识别接口（recognizeStreaming）");
            
        } catch (Exception e) {
            logger.error("ASR识别失败", e);
            return ApiResult.failure("RECOGNITION_FAILED", "识别失败: " + e.getMessage());
        }
    }
    
    @Override
    public CompletableFuture<ApiResult<ASRResponse>> recognizeAsync(ASRRequest request) {
        return CompletableFuture.supplyAsync(() -> recognize(request));
    }
    
    @Override
    public ApiResult<ASRResponse> recognizeStreaming(ASRRequest request) {
        // 流式识别通过WebSocket实现
        // 实际的音频发送由ASRWebSocketHandler处理
        // 这里只做参数验证
        
        if (request.getAudioData() == null || request.getAudioData().isEmpty()) {
            return ApiResult.failure("INVALID_REQUEST", "音频数据为空");
        }
        
        // 返回空响应表示已接收
        ASRResponse response = new ASRResponse("", 0.0, false, 0, null);
        return ApiResult.success(response);
    }
    
    @Override
    public ApiResult<Void> connect(String sessionId) {
        if (!asrEnabled) {
            return ApiResult.failure("ASR_DISABLED", "ASR服务未启用");
        }
        
        // 检查健康状态
        if (System.currentTimeMillis() - lastHealthCheck > HEALTH_CHECK_INTERVAL) {
            healthCheck();
        }
        
        if (!serviceAvailable) {
            return ApiResult.failure("ASR_SERVICE_UNAVAILABLE", "ASR服务不可用，请检查服务状态");
        }
        
        try {
            // WebSocket连接由ASRWebSocketHandler管理
            // 这里记录连接信息
            ASRConnectionInfo info = new ASRConnectionInfo(
                asrServerUrl,
                "connecting",
                sessionId,
                0,
                null
            );
            connections.put(sessionId, info);
            
            logger.info("ASR连接请求: sessionId={}", sessionId);
            return ApiResult.success((Void) null);
            
        } catch (Exception e) {
            logger.error("ASR连接失败: sessionId={}", sessionId, e);
            return ApiResult.failure("CONNECTION_FAILED", "连接失败: " + e.getMessage());
        }
    }
    
    @Override
    public ApiResult<Void> disconnect(String sessionId) {
        try {
            connections.remove(sessionId);
            logger.info("ASR连接已断开: sessionId={}", sessionId);
            return ApiResult.success((Void) null);
        } catch (Exception e) {
            logger.error("断开ASR连接失败: sessionId={}", sessionId, e);
            return ApiResult.failure("DISCONNECT_FAILED", "断开连接失败: " + e.getMessage());
        }
    }
    
    @Override
    public boolean isConnected(String sessionId) {
        ASRConnectionInfo info = connections.get(sessionId);
        return info != null && info.isConnected();
    }
    
    @Override
    public ApiResult<Void> startSession(String sessionId) {
        logger.info("启动ASR会话: sessionId={}", sessionId);
        
        // 更新连接状态
        ASRConnectionInfo oldInfo = connections.get(sessionId);
        if (oldInfo != null) {
            ASRConnectionInfo newInfo = new ASRConnectionInfo(
                oldInfo.getServiceUrl(),
                "connected",
                sessionId,
                oldInfo.getReconnectCount(),
                null
            );
            connections.put(sessionId, newInfo);
        }
        
        return ApiResult.success((Void) null);
    }
    
    @Override
    public ApiResult<Void> endSession(String sessionId) {
        logger.info("结束ASR会话: sessionId={}", sessionId);
        connections.remove(sessionId);
        return ApiResult.success(null);
    }
    
    @Override
    public ApiResult<List<String>> getSupportedLanguages() {
        // FunASR支持的语言
        List<String> languages = Arrays.asList(
            "zh-CN",  // 中文
            "en-US",  // 英文
            "ja-JP",  // 日文
            "ko-KR"   // 韩文
        );
        return ApiResult.success(languages);
    }
    
    @Override
    public ApiResult<List<String>> getSupportedFormats() {
        // FunASR支持的音频格式
        List<String> formats = Arrays.asList(
            "wav",
            "pcm",
            "opus",
            "flac",
            "mp3"
        );
        return ApiResult.success(formats);
    }
}


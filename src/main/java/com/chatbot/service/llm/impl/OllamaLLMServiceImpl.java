package com.chatbot.service.llm.impl;

import com.chatbot.config.AppConfig;
import com.chatbot.model.dto.llm.OllamaChatRequest;
import com.chatbot.model.dto.common.ApiResult;
import com.chatbot.model.dto.common.HealthCheckResult;
import com.chatbot.model.dto.llm.*;
import com.chatbot.service.llm.LLMService;
import com.chatbot.util.JsonUtil;
import com.fasterxml.jackson.databind.JsonNode;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Ollama LLM服务实现
 * 实现LLMService接口，对接Ollama本地LLM服务
 * 
 * 设计模式：
 * - 适配器模式：将Ollama API适配为统一的LLMService接口
 * - 门面模式：简化Ollama API的复杂调用
 */
@Service("ollamaLLMService")
public class OllamaLLMServiceImpl implements LLMService {

    private static final Logger logger = LoggerFactory.getLogger(OllamaLLMServiceImpl.class);

    private final AppConfig.OllamaConfig ollamaConfig;
    private final OkHttpClient httpClient;

    // 健康检查缓存
    private volatile boolean serviceAvailable = false;
    private volatile long lastHealthCheck = 0;
    private static final long HEALTH_CHECK_CACHE_MS = 30 * 1000; // 30秒

    public OllamaLLMServiceImpl(AppConfig appConfig) {
        this.ollamaConfig = appConfig.getOllama();

        // 配置HTTP客户端
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .connectionPool(new ConnectionPool(10, 5, TimeUnit.MINUTES))
                .retryOnConnectionFailure(true)
                .build();

        logger.info("Ollama LLM服务实现初始化完成，引擎: Ollama, URL: {}", ollamaConfig.getChatUrl());

        // 启动时进行一次健康检查
        healthCheck();
    }

    @Override
    public String getEngineName() {
        return "Ollama";
    }

    @Override
    public HealthCheckResult healthCheck() {
        long startTime = System.currentTimeMillis();

        // 使用缓存避免频繁检查
        if (System.currentTimeMillis() - lastHealthCheck < HEALTH_CHECK_CACHE_MS) {
            return new HealthCheckResult.Builder()
                    .serviceName(getEngineName())
                    .healthy(serviceAvailable)
                    .status(serviceAvailable ? "AVAILABLE" : "UNAVAILABLE")
                    .responseTime(0)
                    .detail("cached", "使用缓存结果")
                    .build();
        }

        try {
            Request request = new Request.Builder()
                    .url(ollamaConfig.getModelsUrl())
                    .get()
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                long responseTime = System.currentTimeMillis() - startTime;
                serviceAvailable = response.isSuccessful();
                lastHealthCheck = System.currentTimeMillis();

                if (serviceAvailable) {
                    logger.debug("Ollama LLM服务健康检查成功，响应时间: {}ms", responseTime);

                    return new HealthCheckResult.Builder()
                            .serviceName(getEngineName())
                            .healthy(true)
                            .status("AVAILABLE")
                            .responseTime(responseTime)
                            .detail("url", ollamaConfig.getChatUrl())
                            .detail("model", ollamaConfig.getModel())
                            .build();
                } else {
                    logger.warn("Ollama LLM服务健康检查失败，HTTP状态码: {}", response.code());

                    return new HealthCheckResult.Builder()
                            .serviceName(getEngineName())
                            .healthy(false)
                            .status("UNAVAILABLE")
                            .responseTime(responseTime)
                            .detail("error", "HTTP " + response.code())
                            .build();
                }
            }
        } catch (Exception e) {
            long responseTime = System.currentTimeMillis() - startTime;
            serviceAvailable = false;
            lastHealthCheck = System.currentTimeMillis();

            logger.error("Ollama LLM服务健康检查异常", e);

            return new HealthCheckResult.Builder()
                    .serviceName(getEngineName())
                    .healthy(false)
                    .status("ERROR")
                    .responseTime(responseTime)
                    .detail("error", e.getMessage())
                    .build();
        }
    }

    @Override
    public ApiResult<LLMResponse> generate(LLMRequest request) {
        try {
            // 验证请求
            ApiResult<Void> validation = validateRequest(request);
            if (!validation.isSuccess()) {
                return ApiResult.failure(validation.getErrorCode(), validation.getMessage());
            }

            // 构建Ollama请求
            String requestBody = buildRequestJson(request);
            String url = ollamaConfig.getChatUrl();

            Request httpRequest = new Request.Builder()
                    .url(url)
                    .post(RequestBody.create(requestBody, MediaType.parse("application/json")))
                    .build();

            logger.info("🤖 发送LLM非流式请求 - 模型: {}, 消息数: {}", request.getModel(), request.getMessages().size());

            long startTime = System.currentTimeMillis();

            try (Response response = httpClient.newCall(httpRequest).execute()) {
                long durationMs = System.currentTimeMillis() - startTime;

                if (!response.isSuccessful()) {
                    String errorMsg = "Ollama API返回错误: " + response.code();
                    logger.error("❌ {}", errorMsg);
                    return ApiResult.failure("LLM_ERROR", errorMsg);
                }

                ResponseBody responseBody = response.body();
                if (responseBody == null) {
                    return ApiResult.failure("EMPTY_RESPONSE", "响应体为空");
                }

                String responseText = responseBody.string();
                LLMResponse llmResponse = parseResponse(responseText, request.getModel(), durationMs);

                logger.info("✅ LLM非流式响应完成，内容长度: {}, 耗时: {}ms", llmResponse.getContent().length(), durationMs);

                return ApiResult.success(llmResponse);
            }
        } catch (IOException e) {
            logger.error("❌ LLM请求异常", e);
            return ApiResult.failure("IO_ERROR", "网络请求失败: " + e.getMessage());
        } catch (Exception e) {
            logger.error("❌ LLM处理异常", e);
            return ApiResult.failure("PROCESSING_ERROR", "处理失败: " + e.getMessage());
        }
    }

    @Override
    public CompletableFuture<ApiResult<LLMResponse>> generateAsync(LLMRequest request) {
        return CompletableFuture.supplyAsync(() -> generate(request));
    }

    @Override
    public Object generateStream(
            LLMRequest request,
            Consumer<LLMStreamChunk> onChunk,
            Consumer<Throwable> onError,
            Runnable onComplete) {
        return generateStreamWithInterruptCheck(request, onChunk, onError, onComplete, null);
    }

    @Override
    public Object generateStreamWithInterruptCheck(
            LLMRequest request,
            Consumer<LLMStreamChunk> onChunk,
            Consumer<Throwable> onError,
            Runnable onComplete,
            java.util.function.Supplier<Boolean> interruptChecker) {

        // 验证请求
        ApiResult<Void> validation = validateRequest(request);
        if (!validation.isSuccess()) {
            onError.accept(new IllegalArgumentException(validation.getMessage()));
            return null;
        }

        try {
            // 构建Ollama请求
            String requestBody = buildRequestJson(request);
            String url = ollamaConfig.getChatUrl();

            Request httpRequest = new Request.Builder()
                    .url(url)
                    .post(RequestBody.create(requestBody, MediaType.parse("application/json")))
                    .build();

            logger.info("🤖 发送LLM流式请求 - 模型: {}, 消息数: {}", request.getModel(), request.getMessages().size());

            // 异步执行请求
            okhttp3.Call call = httpClient.newCall(httpRequest);
            call.enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    logger.error("❌ LLM流式请求失败: {}", e.getMessage());
                    onError.accept(e);
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (!response.isSuccessful()) {
                        String errorMsg = "LLM API返回错误: " + response.code();
                        logger.error("❌ {}", errorMsg);
                        onError.accept(new RuntimeException(errorMsg));
                        return;
                    }

                    logger.info("✅ LLM流式响应开始 - 状态码: {}", response.code());

                    try (ResponseBody responseBody = response.body()) {
                        if (responseBody == null) {
                            onError.accept(new RuntimeException("响应体为空"));
                            return;
                        }

                        // 处理流式响应
                        processStreamingResponse(responseBody, request.getModel(), onChunk, onError, onComplete, interruptChecker);
                    }
                }
            });

            return call;

        } catch (Exception e) {
            logger.error("❌ 构建LLM请求时发生错误", e);
            onError.accept(e);
            return null;
        }
    }

    @Override
    public ApiResult<List<ModelInfo>> getAvailableModels() {
        try {
            Request request = new Request.Builder()
                    .url(ollamaConfig.getModelsUrl())
                    .get()
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    return ApiResult.failure("API_ERROR", "无法获取模型列表: HTTP " + response.code());
                }

                ResponseBody responseBody = response.body();
                if (responseBody == null) {
                    return ApiResult.failure("EMPTY_RESPONSE", "响应体为空");
                }

                String responseText = responseBody.string();
                List<ModelInfo> models = parseModelsResponse(responseText);

                return ApiResult.success(models);
            }
        } catch (Exception e) {
            logger.error("获取可用模型列表失败", e);
            return ApiResult.failure("ERROR", "获取失败: " + e.getMessage());
        }
    }

    @Override
    public ApiResult<ModelInfo> getModelInfo(String modelName) {
        ApiResult<List<ModelInfo>> modelsResult = getAvailableModels();
        if (!modelsResult.isSuccess()) {
            return ApiResult.failure(modelsResult.getErrorCode(), modelsResult.getMessage());
        }

        List<ModelInfo> models = modelsResult.getData();
        for (ModelInfo model : models) {
            if (model.getName().equals(modelName)) {
                return ApiResult.success(model);
            }
        }

        return ApiResult.failure("NOT_FOUND", "未找到模型: " + modelName);
    }

    @Override
    public boolean isServiceAvailable() {
        // 使用缓存的健康检查结果
        if (System.currentTimeMillis() - lastHealthCheck < HEALTH_CHECK_CACHE_MS) {
            return serviceAvailable;
        }

        // 执行新的健康检查
        HealthCheckResult result = healthCheck();
        return result.isHealthy();
    }

    @Override
    public ApiResult<Void> validateRequest(LLMRequest request) {
        if (request == null) {
            return ApiResult.failure("INVALID_REQUEST", "请求不能为空");
        }

        if (request.getMessages() == null || request.getMessages().isEmpty()) {
            return ApiResult.failure("INVALID_REQUEST", "消息列表不能为空");
        }

        if (request.getModel() == null || request.getModel().trim().isEmpty()) {
            return ApiResult.failure("INVALID_REQUEST", "模型名称不能为空");
        }

        if (request.getTemperature() != null && (request.getTemperature() < 0 || request.getTemperature() > 2)) {
            return ApiResult.failure("INVALID_REQUEST", "温度参数必须在0-2之间");
        }

        return ApiResult.success(null);
    }

    @Override
    public ApiResult<Integer> estimateTokens(LLMRequest request) {
        if (request == null || request.getMessages() == null) {
            return ApiResult.failure("INVALID_REQUEST", "请求不能为空");
        }

        // 简单估算：每个字符约0.4个token（中文），每个单词约1个token（英文）
        int totalChars = 0;
        for (var message : request.getMessages()) {
            if (message.getContent() != null) {
                totalChars += message.getContent().length();
            }
        }

        // 粗略估算
        int estimatedTokens = (int) (totalChars * 0.5);

        return ApiResult.success(estimatedTokens);
    }

    // ========== 私有辅助方法 ==========

    /**
     * 转换通用 Message 为 OllamaMessage
     * 实现统一接口层到 Ollama 实现层的适配
     */
    private List<OllamaMessage> convertToOllamaMessages(List<Message> messages) {
        if (messages == null) {
            return new ArrayList<>();
        }
        
        List<OllamaMessage> ollamaMessages = new ArrayList<>();
        for (Message message : messages) {
            ollamaMessages.add(new OllamaMessage(message.getRole(), message.getContent()));
        }
        
        return ollamaMessages;
    }
    
    /**
     * 转换 OllamaMessage 为通用 Message
     * 用于向上层返回数据时的转换
     */
    private List<Message> convertFromOllamaMessages(List<OllamaMessage> ollamaMessages) {
        if (ollamaMessages == null) {
            return new ArrayList<>();
        }
        
        List<Message> messages = new ArrayList<>();
        for (OllamaMessage ollamaMessage : ollamaMessages) {
            messages.add(new Message(ollamaMessage.getRole(), ollamaMessage.getContent()));
        }
        
        return messages;
    }

    /**
     * 构建Ollama请求JSON
     */
    private String buildRequestJson(LLMRequest request) {
        try {
            // 将统一接口层的 Message 转换为 Ollama 特定的 OllamaMessage
            List<OllamaMessage> ollamaMessages = convertToOllamaMessages(request.getMessages());
            
            OllamaChatRequest ollamaRequest = OllamaChatRequest.fromMessages(
                    request.getModel(),
                    ollamaMessages,
                    request.isStream(),
                    request.getTemperature() != null ? request.getTemperature() : ollamaConfig.getTemperature()
            );

            return JsonUtil.toJson(ollamaRequest);
        } catch (Exception e) {
            logger.error("构建请求JSON失败", e);
            throw new RuntimeException("构建请求失败", e);
        }
    }

    /**
     * 解析非流式响应
     */
    private LLMResponse parseResponse(String responseText, String model, long durationMs) {
        try {
            JsonNode jsonNode = JsonUtil.parseJson(responseText);
            if (jsonNode == null) {
                return new LLMResponse.Builder()
                        .content("解析响应失败")
                        .model(model)
                        .durationMs(durationMs)
                        .build();
            }

            // 提取内容
            String content = "";
            JsonNode messageNode = jsonNode.get("message");
            if (messageNode != null) {
                content = JsonUtil.getStringValue(messageNode, "content");
            }

            // 提取token信息
            Integer promptTokensObj = JsonUtil.getIntValue(jsonNode, "prompt_eval_count");
            Integer completionTokensObj = JsonUtil.getIntValue(jsonNode, "eval_count");
            int promptTokens = promptTokensObj != null ? promptTokensObj : 0;
            int completionTokens = completionTokensObj != null ? completionTokensObj : 0;
            int totalTokens = promptTokens + completionTokens;

            Boolean done = JsonUtil.getBooleanValue(jsonNode, "done");

            return new LLMResponse.Builder()
                    .content(content != null ? content : "")
                    .model(model)
                    .done(done != null ? done : true)
                    .promptTokens(promptTokens)
                    .completionTokens(completionTokens)
                    .totalTokens(totalTokens)
                    .durationMs(durationMs)
                    .build();

        } catch (Exception e) {
            logger.error("解析LLM响应失败", e);
            return new LLMResponse.Builder()
                    .content("解析响应失败: " + e.getMessage())
                    .model(model)
                    .durationMs(durationMs)
                    .build();
        }
    }

    /**
     * 处理流式响应
     */
    private void processStreamingResponse(
            ResponseBody responseBody,
            String model,
            Consumer<LLMStreamChunk> onChunk,
            Consumer<Throwable> onError,
            Runnable onComplete,
            java.util.function.Supplier<Boolean> interruptChecker) {

        int chunkIndex = 0;
        boolean hasContent = false;

        try {
            try (var source = responseBody.source()) {
                String line;
                while ((line = source.readUtf8Line()) != null) {
                    // 检查中断
                    if (interruptChecker != null && interruptChecker.get()) {
                        logger.info("检测到中断信号，停止处理LLM流式响应");
                        return;
                    }

                    if (line.trim().isEmpty()) {
                        continue;
                    }

                    try {
                        JsonNode jsonNode = JsonUtil.parseJson(line);
                        if (jsonNode == null) {
                            continue;
                        }

                        // 检查错误
                        String error = JsonUtil.getStringValue(jsonNode, "error");
                        if (error != null && !error.isEmpty()) {
                            logger.error("Ollama返回错误: {}", error);
                            onError.accept(new RuntimeException("LLM API错误: " + error));
                            return;
                        }

                        // 检查完成
                        Boolean done = JsonUtil.getBooleanValue(jsonNode, "done");
                        if (done != null && done) {
                            logger.debug("LLM流式响应完成信号接收");
                            break;
                        }

                        // 提取内容
                        JsonNode messageNode = jsonNode.get("message");
                        if (messageNode != null) {
                            String content = JsonUtil.getStringValue(messageNode, "content");
                            if (content != null && !content.isEmpty()) {
                                hasContent = true;

                                LLMStreamChunk chunk = new LLMStreamChunk.Builder()
                                        .content(content)
                                        .model(model)
                                        .done(false)
                                        .chunkIndex(chunkIndex++)
                                        .build();

                                onChunk.accept(chunk);
                            }
                        }

                    } catch (Exception e) {
                        logger.warn("解析流式响应行失败: {}", line, e);
                    }
                }

                // 发送完成块
                if (hasContent) {
                    LLMStreamChunk finalChunk = new LLMStreamChunk.Builder()
                            .content("")
                            .model(model)
                            .done(true)
                            .chunkIndex(chunkIndex)
                            .build();

                    onChunk.accept(finalChunk);
                }

                logger.info("📊 LLM流式响应完成 - 数据块: {}", chunkIndex);

                // 调用完成回调
                if (onComplete != null) {
                    onComplete.run();
                }
            }

        } catch (Exception e) {
            logger.error("处理LLM流式响应时发生错误", e);
            onError.accept(e);
        } finally {
            try {
                responseBody.close();
            } catch (Exception e) {
                logger.debug("关闭响应体时出现异常", e);
            }
        }
    }

    /**
     * 解析模型列表响应
     */
    private List<ModelInfo> parseModelsResponse(String responseText) {
        List<ModelInfo> models = new ArrayList<>();

        try {
            JsonNode jsonNode = JsonUtil.parseJson(responseText);
            if (jsonNode == null || !jsonNode.has("models")) {
                return models;
            }

            JsonNode modelsNode = jsonNode.get("models");
            if (modelsNode.isArray()) {
                for (JsonNode modelNode : modelsNode) {
                    String name = JsonUtil.getStringValue(modelNode, "name");
                    if (name != null) {
                        // 解析模型名称，如 "yi:6b" -> family="yi", size="6b"
                        String[] parts = name.split(":");
                        String family = parts.length > 0 ? parts[0] : name;
                        String size = parts.length > 1 ? parts[1] : "unknown";

                        ModelInfo model = new ModelInfo.Builder()
                                .name(name)
                                .displayName(name)
                                .family(family)
                                .size(size)
                                .available(true)
                                .build();

                        models.add(model);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("解析模型列表失败", e);
        }

        return models;
    }
}


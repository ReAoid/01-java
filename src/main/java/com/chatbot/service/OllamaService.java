package com.chatbot.service;

import com.chatbot.config.AppConfig;
import com.chatbot.model.dto.OllamaMessage;
import com.chatbot.model.dto.OllamaChatRequest;
import com.chatbot.model.config.UserPreferences;
import com.chatbot.util.JsonUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Ollama服务
 * 负责与本地Ollama API进行通信，支持流式和非流式对话
 */
@Service
public class OllamaService {
    
    private static final Logger logger = LoggerFactory.getLogger(OllamaService.class);
    
    private final AppConfig.OllamaConfig ollamaConfig;
    private final OkHttpClient httpClient;
    @SuppressWarnings("unused")
    private final ObjectMapper objectMapper;
    
    public OllamaService(AppConfig appConfig) {
        this.ollamaConfig = appConfig.getOllama();
        this.objectMapper = new ObjectMapper();
        
        // 配置HTTP客户端 - 改进连接池和超时配置
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)  // 增加读取超时到120秒
                .writeTimeout(15, TimeUnit.SECONDS)
                .connectionPool(new ConnectionPool(10, 5, TimeUnit.MINUTES))  // 增加连接池大小和保持时间
                .retryOnConnectionFailure(true)  // 启用连接失败重试
                .build();
    }
    
    /**
     * 流式生成响应
     */
    public void generateStreamingResponse(String prompt, Consumer<String> onChunk, Consumer<Throwable> onError) {
        generateStreamingResponse(null, prompt, onChunk, onError);
    }
    
    /**
     * 流式生成响应（支持系统提示词分离）
     */
    public void generateStreamingResponse(String systemPrompt, String userPrompt, Consumer<String> onChunk, Consumer<Throwable> onError) {
        logger.debug("系统提示内容 (长度: {}):\n{}", 
                   systemPrompt != null ? systemPrompt.length() : 0, 
                   systemPrompt != null ? systemPrompt : "无");
        logger.debug("用户提示内容 (长度: {}):\n{}", userPrompt.length(), userPrompt);
        
        try {
            // 构建请求体
            String requestBody = buildChatRequest(systemPrompt, userPrompt, true);
            logger.debug("完整请求体:\n{}", requestBody);
            
            String url = ollamaConfig.getChatUrl();
            
            Request request = new Request.Builder()
                    .url(url)
                    .post(RequestBody.create(requestBody, MediaType.parse("application/json")))
                    .build();
            
            logger.debug("HTTP请求详情 - URL: {}, Method: {}, Content-Type: application/json", 
                        request.url(), request.method());
            
            // 异步执行请求
            httpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    logger.error("Ollama API调用失败", e);
                    onError.accept(e);
                }
                
                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (!response.isSuccessful()) {
                        String errorMsg = "Ollama API返回错误: " + response.code() + " " + response.message();
                        logger.error(errorMsg);
                        onError.accept(new RuntimeException(errorMsg));
                        return;
                    }
                    
                    try (ResponseBody responseBody = response.body()) {
                        if (responseBody == null) {
                            logger.error("Ollama流式响应体为空");
                            onError.accept(new RuntimeException("响应体为空"));
                            return;
                        }
                        
                        // 处理流式响应
                        processStreamingResponse(responseBody, onChunk, onError, null);
                    }
                }
            });
            
        } catch (Exception e) {
            logger.error("构建Ollama请求时发生错误", e);
            onError.accept(e);
        }
    }
    
    /**
     * 流式生成响应（支持完整消息列表和中断检查）
     */
    public void generateStreamingResponse(List<OllamaMessage> messages, Consumer<String> onChunk, Consumer<Throwable> onError, Runnable onComplete) {
        generateStreamingResponseWithInterruptCheck(messages, onChunk, onError, onComplete, null);
    }
    
    /**
     * 流式生成响应（支持完整消息列表和用户配置）
     */
    public void generateStreamingResponse(List<OllamaMessage> messages, Consumer<String> onChunk, Consumer<Throwable> onError, Runnable onComplete, UserPreferences userPrefs) {
        generateStreamingResponseWithInterruptCheck(messages, onChunk, onError, onComplete, null, userPrefs);
    }
    
    /**
     * 流式生成响应（支持完整消息列表和中断检查）
     * @return Call对象，可用于取消请求
     */
    public okhttp3.Call generateStreamingResponseWithInterruptCheck(List<OllamaMessage> messages, Consumer<String> onChunk, Consumer<Throwable> onError, Runnable onComplete, java.util.function.Supplier<Boolean> interruptChecker) {
        return generateStreamingResponseWithInterruptCheck(messages, onChunk, onError, onComplete, interruptChecker, null);
    }
    
    /**
     * 流式生成响应（支持完整消息列表、中断检查和用户配置）
     * @return Call对象，可用于取消请求
     */
    public okhttp3.Call generateStreamingResponseWithInterruptCheck(List<OllamaMessage> messages, Consumer<String> onChunk, Consumer<Throwable> onError, Runnable onComplete, java.util.function.Supplier<Boolean> interruptChecker, UserPreferences userPrefs) {
        // 记录LLM请求基本信息
        String modelName = (userPrefs != null && userPrefs.getOllamaModel() != null) 
            ? userPrefs.getOllamaModel() 
            : extractModelFromRequest(messages);
        logger.info("🤖 发送LLM流式请求 - 消息数: {}, 模型: {}", messages.size(), modelName);
        logger.debug("消息数量: {}", messages.size());
        
        try {
            String url = ollamaConfig.getChatUrl();
            
            // 构建请求体（使用用户配置）
            String requestBody = buildChatRequestFromMessages(messages, true, userPrefs);
            logger.info("📤 LLM请求详情 - URL: {}, 请求体长度: {}字符", url, requestBody.length());
            logger.info("📋 LLM请求内容:\n{}", requestBody);
            
            Request request = new Request.Builder()
                    .url(url)
                    .post(RequestBody.create(requestBody, MediaType.parse("application/json")))
                    .build();
            
            logger.debug("HTTP请求详情 - URL: {}, Method: {}, Content-Type: application/json", 
                        request.url(), request.method());
            
            // 异步执行请求
            okhttp3.Call call = httpClient.newCall(request);
            call.enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    logger.error("❌ LLM API调用失败: {}", e.getMessage());
                    onError.accept(e);
                }
                
                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (!response.isSuccessful()) {
                        String errorMsg = "LLM API返回错误: " + response.code() + " " + response.message();
                        logger.error("❌ {}", errorMsg);
                        onError.accept(new RuntimeException(errorMsg));
                        return;
                    }
                    
                    logger.info("✅ LLM响应开始 - 状态码: {}", response.code());
                    
                    try (ResponseBody responseBody = response.body()) {
                        if (responseBody == null) {
                            logger.error("Ollama流式响应体为空");
                            onError.accept(new RuntimeException("响应体为空"));
                            return;
                        }
                        
                        // 处理流式响应
                        processStreamingResponseWithInterruptCheck(responseBody, onChunk, onError, onComplete, interruptChecker);
                    }
                }
            });
            
            return call;
            
        } catch (Exception e) {
            logger.error("构建Ollama请求时发生错误", e);
            onError.accept(e);
            return null;
        }
    }
    
    /**
     * 非流式生成响应
     */
    public String generateResponse(String prompt) throws IOException {
        return generateResponse(null, prompt);
    }
    
    /**
     * 非流式生成响应（支持系统提示词分离）
     */
    public String generateResponse(String systemPrompt, String userPrompt) throws IOException {
        logger.info("开始构建Ollama非流式请求");
        logger.debug("系统提示内容 (长度: {}):\n{}", 
                   systemPrompt != null ? systemPrompt.length() : 0, 
                   systemPrompt != null ? systemPrompt : "无");
        logger.debug("用户提示内容 (长度: {}):\n{}", userPrompt.length(), userPrompt);
        
        // 构建请求体
        String requestBody = buildChatRequest(systemPrompt, userPrompt, false);
        logger.info("🤖 非流式请求体构建完成，长度: {}", requestBody.length());
        logger.info("📋 非流式请求内容:\n{}", requestBody);
        
            String url = ollamaConfig.getChatUrl();
        logger.info("发送Ollama非流式请求到: {}", url);
        
        Request request = new Request.Builder()
                .url(url)
                .post(RequestBody.create(requestBody, MediaType.parse("application/json")))
                .build();
        
        logger.debug("HTTP请求详情 - URL: {}, Method: {}, Content-Type: application/json", 
                    request.url(), request.method());
        
        try (Response response = httpClient.newCall(request).execute()) {
            logger.info("Ollama非流式响应接收完成，状态码: {}", response.code());
            
            if (!response.isSuccessful()) {
                String errorMsg = "Ollama API返回错误: " + response.code() + " " + response.message();
                logger.error(errorMsg);
                throw new IOException(errorMsg);
            }
            
            ResponseBody responseBody = response.body();
            if (responseBody == null) {
                logger.error("Ollama响应体为空");
                throw new IOException("响应体为空");
            }
            
            String responseText = responseBody.string();
            logger.info("📥 LLM原始响应 (长度: {}):\n{}", responseText.length(), responseText);
            
            String extractedText = extractResponseText(responseText);
            logger.info("✅ LLM响应文本提取完成，最终长度: {}", extractedText.length());
            logger.info("📄 提取的响应文本: '{}'", extractedText.replace("\n", "\\n"));
            
            return extractedText;
        }
    }
    
    /**
     * 检查Ollama服务是否可用
     */
    public boolean isServiceAvailable() {
        try {
            Request request = new Request.Builder()
                    .url(ollamaConfig.getModelsUrl())
                    .get()
                    .build();
            
            try (Response response = httpClient.newCall(request).execute()) {
                return response.isSuccessful();
            }
        } catch (Exception e) {
            logger.warn("检查Ollama服务可用性时发生错误", e);
            return false;
        }
    }
    
    /**
     * 构建聊天请求的JSON
     */
    private String buildChatRequest(String systemPrompt, String userPrompt, boolean stream) {
        return buildChatRequest(systemPrompt, userPrompt, stream, null);
    }
    
    /**
     * 构建聊天请求的JSON（支持用户配置）
     */
    private String buildChatRequest(String systemPrompt, String userPrompt, boolean stream, UserPreferences userPrefs) {
        logger.debug("开始构建Ollama聊天请求参数");
        
        try {
            // 优先使用用户配置，如果没有则使用默认配置
            String model = (userPrefs != null && userPrefs.getOllamaModel() != null) 
                ? userPrefs.getOllamaModel() 
                : ollamaConfig.getModel();
            double temperature = ollamaConfig.getTemperature();
            
            logger.debug("使用模型: {}, 温度: {}", model, temperature);
            
            OllamaChatRequest request = new OllamaChatRequest(
                    model,
                    systemPrompt,
                    userPrompt,
                    stream,
                    temperature
            );
            
            String jsonRequest = JsonUtil.toJson(request);
            
            return jsonRequest;
        } catch (Exception e) {
            logger.error("构建请求JSON失败", e);
            throw new RuntimeException("构建请求失败", e);
        }
    }
    
    /**
     * 从消息列表构建聊天请求的JSON
     */
    private String buildChatRequestFromMessages(List<OllamaMessage> messages, boolean stream) {
        return buildChatRequestFromMessages(messages, stream, null);
    }
    
    /**
     * 从消息列表构建聊天请求的JSON（支持用户配置）
     */
    private String buildChatRequestFromMessages(List<OllamaMessage> messages, boolean stream, UserPreferences userPrefs) {
        try {
            // 优先使用用户配置，如果没有则使用默认配置
            String model = (userPrefs != null && userPrefs.getOllamaModel() != null) 
                ? userPrefs.getOllamaModel() 
                : ollamaConfig.getModel();
            double temperature = ollamaConfig.getTemperature();
            
            logger.debug("使用模型: {}, 温度: {}", model, temperature);
            
            OllamaChatRequest request = OllamaChatRequest.fromMessages(
                    model,
                    messages,
                    stream,
                    temperature
            );
            
            String jsonRequest = JsonUtil.toJson(request);
            
            return jsonRequest;
        } catch (Exception e) {
            logger.error("构建请求JSON失败", e);
            throw new RuntimeException("构建请求失败", e);
        }
    }
    
    /**
     * 处理流式响应 - 优化版，支持真正的实时流式处理
     */
    private void processStreamingResponse(ResponseBody responseBody, Consumer<String> onChunk, Consumer<Throwable> onError, Runnable onComplete) {
        processStreamingResponseWithInterruptCheck(responseBody, onChunk, onError, onComplete, null);
    }
    
    /**
     * 处理流式响应 - 支持中断检查的版本
     */
    private void processStreamingResponseWithInterruptCheck(ResponseBody responseBody, Consumer<String> onChunk, Consumer<Throwable> onError, Runnable onComplete, java.util.function.Supplier<Boolean> interruptChecker) {
        StringBuilder totalResponse = new StringBuilder();
        int chunkCount = 0;
        boolean hasError = false;
        boolean hasContent = false;
        
        try {
            // 使用流式读取，而不是一次性读取所有内容
            try (var source = responseBody.source()) {
                String line;
                while ((line = source.readUtf8Line()) != null) {
                    // 检查是否需要中断
                    if (interruptChecker != null && interruptChecker.get()) {
                        logger.info("检测到中断信号，停止处理Ollama流式响应");
                        return;
                    }
                    
                    if (line.trim().isEmpty()) {
                        continue;
                    }
                    
                    try {
                        JsonNode jsonNode = JsonUtil.parseJson(line);
                        if (jsonNode == null) {
                            logger.debug("跳过无效的JSON行: {}", line);
                            continue;
                        }
                        
                        // 检查是否有错误
                        String error = JsonUtil.getStringValue(jsonNode, "error");
                        if (error != null && !error.isEmpty()) {
                            logger.error("Ollama返回错误: {}", error);
                            onError.accept(new RuntimeException("Ollama API错误: " + error));
                            return;
                        }
                        
                        // 检查是否完成
                        Boolean done = JsonUtil.getBooleanValue(jsonNode, "done");
                        if (done != null && done) {
                            logger.debug("流式响应完成信号接收");
                            break;
                        }
                        
                        // 提取响应文本 - /api/chat 接口返回的是 message 对象
                        JsonNode messageNode = jsonNode.get("message");
                        if (messageNode != null) {
                            String chunk = JsonUtil.getStringValue(messageNode, "content");
                            if (chunk != null && !chunk.isEmpty()) {
                                chunkCount++;
                                hasContent = true;
                                totalResponse.append(chunk);
                                
//                                logger.debug("实时流式数据块#{}: '{}' (长度: {})",
//                                           chunkCount, chunk.replace("\n", "\\n"), chunk.length());
                                
                                // 立即发送给消费者
                                onChunk.accept(chunk);
                            }
                        }
                        
                    } catch (Exception e) {
                        logger.warn("解析流式响应行失败: {}", line, e);
                        hasError = true;
                    }
                }
            }
            
            logger.info("📊 LLM流式响应完成 - 数据块: {}, 响应长度: {}字符, 有错误: {}", 
                       chunkCount, totalResponse.length(), hasError);
            
            // 如果有内容，也记录完整的响应内容
            if (hasContent && totalResponse.length() > 0) {
                logger.info("📄 LLM流式响应完整内容:\n{}", totalResponse.toString());
            }
            
            // 如果没有收到任何内容，触发错误回调
            if (!hasContent && !hasError) {
                logger.warn("Ollama流式响应没有返回任何内容");
                onError.accept(new RuntimeException("AI服务返回空响应"));
            } else if (hasContent) {
                // 只有在成功收到内容时才调用完成回调
                logger.debug("调用流式响应完成回调");
                if (onComplete != null) {
                    onComplete.run();
                }
            }
            
        } catch (Exception e) {
            logger.error("处理流式响应时发生错误", e);
            onError.accept(e);
        } finally {
            // 确保响应体资源被释放
            try {
                responseBody.close();
            } catch (Exception e) {
                logger.debug("关闭响应体时出现异常", e);
            }
        }
    }
    
    /**
     * 从响应中提取文本内容
     */
    private String extractResponseText(String responseJson) {
        try {
            JsonNode jsonNode = JsonUtil.parseJson(responseJson);
            if (jsonNode == null) {
                logger.warn("无法解析响应JSON: {}", responseJson);
                return "抱歉，无法解析AI的响应。";
            }
            
            // /api/chat 接口返回的是 message 对象
            JsonNode messageNode = jsonNode.get("message");
            if (messageNode != null) {
                String response = JsonUtil.getStringValue(messageNode, "content");
                if (response != null) {
                    return response;
                } else {
                    logger.warn("响应message中未找到content字段: {}", responseJson);
                    return "抱歉，无法解析AI的响应。";
                }
            } else {
                logger.warn("响应中未找到message字段: {}", responseJson);
                return "抱歉，无法解析AI的响应。";
            }
        } catch (Exception e) {
            logger.error("解析响应JSON失败", e);
            return "抱歉，处理AI响应时出现错误。";
        }
    }
    
    /**
     * 从请求消息中提取模型名称
     */
    private String extractModelFromRequest(List<OllamaMessage> messages) {
        try {
            // 尝试从配置中获取模型名称
            return ollamaConfig.getModel();
        } catch (Exception e) {
            return "未知模型";
        }
    }
    
}

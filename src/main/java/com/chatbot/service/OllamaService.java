package com.chatbot.service;

import com.chatbot.config.AppConfig;
import com.chatbot.model.*;
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
                        processStreamingResponse(responseBody, onChunk, onError);
                    }
                }
            });
            
        } catch (Exception e) {
            logger.error("构建Ollama请求时发生错误", e);
            onError.accept(e);
        }
    }
    
    /**
     * 流式生成响应（支持完整消息列表）
     */
    public void generateStreamingResponse(List<OllamaMessage> messages, Consumer<String> onChunk, Consumer<Throwable> onError) {
        logger.debug("消息数量: {}", messages.size());
        
        try {
            // 构建请求体
            String requestBody = buildChatRequestFromMessages(messages, true);
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
                        processStreamingResponse(responseBody, onChunk, onError);
                    }
                }
            });
            
        } catch (Exception e) {
            logger.error("构建Ollama请求时发生错误", e);
            onError.accept(e);
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
        logger.info("Ollama非流式请求体构建完成，长度: {}", requestBody.length());
        logger.debug("Ollama完整请求体:\n{}", requestBody);
        
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
            logger.debug("Ollama原始响应 (长度: {}):\n{}", responseText.length(), responseText);
            
            String extractedText = extractResponseText(responseText);
            logger.info("Ollama响应文本提取完成，最终长度: {}", extractedText.length());
            logger.debug("提取的响应文本: '{}'", extractedText.replace("\n", "\\n"));
            
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
        logger.debug("开始构建Ollama聊天请求参数");
        
        try {
            String model = ollamaConfig.getModel();
            double temperature = ollamaConfig.getTemperature();
            
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
        try {
            String model = ollamaConfig.getModel();
            double temperature = ollamaConfig.getTemperature();
            
            OllamaChatRequestFromMessages request = new OllamaChatRequestFromMessages(
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
    private void processStreamingResponse(ResponseBody responseBody, Consumer<String> onChunk, Consumer<Throwable> onError) {
        StringBuilder totalResponse = new StringBuilder();
        int chunkCount = 0;
        boolean hasError = false;
        boolean hasContent = false;
        
        try {
            // 使用流式读取，而不是一次性读取所有内容
            try (var source = responseBody.source()) {
                String line;
                while ((line = source.readUtf8Line()) != null) {
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
            
            logger.info("Ollama流式响应处理完成，共处理{}个数据块，总响应长度: {}, 有错误: {}, 有内容: {}, 完整内容为: {}",
                       chunkCount, totalResponse.length(), hasError, hasContent, totalResponse.toString());
            
            // 如果没有收到任何内容，触发错误回调
            if (!hasContent && !hasError) {
                logger.warn("Ollama流式响应没有返回任何内容");
                onError.accept(new RuntimeException("AI服务返回空响应"));
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
    
}

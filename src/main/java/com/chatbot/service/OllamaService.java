package com.chatbot.service;

import com.chatbot.config.OllamaConfig;
import com.chatbot.util.JsonUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Ollama服务
 * 负责与本地Ollama API进行通信，支持流式和非流式对话
 */
@Service
public class OllamaService {
    
    private static final Logger logger = LoggerFactory.getLogger(OllamaService.class);
    
    private final OllamaConfig ollamaConfig;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    
    public OllamaService(OllamaConfig ollamaConfig) {
        this.ollamaConfig = ollamaConfig;
        this.objectMapper = new ObjectMapper();
        
        // 配置HTTP客户端
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(ollamaConfig.getTimeout(), TimeUnit.MILLISECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }
    
    /**
     * 流式生成响应
     */
    public void generateStreamingResponse(String prompt, Consumer<String> onChunk, Consumer<Throwable> onError) {
        logger.info("开始构建Ollama流式请求");
        logger.debug("原始提示内容 (长度: {}):\n{}", prompt.length(), prompt);
        
        try {
            // 构建请求体
            String requestBody = buildGenerateRequest(prompt, true);
            logger.info("Ollama请求体构建完成，长度: {}", requestBody.length());
            logger.debug("Ollama完整请求体:\n{}", requestBody);
            
            String url = ollamaConfig.getGenerateUrl();
            logger.info("发送Ollama流式请求到: {}", url);
            
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
                    logger.info("Ollama流式响应接收完成，状态码: {}", response.code());
                    logger.debug("响应头信息: {}", response.headers().toString());
                    
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
                        
                        logger.debug("开始处理Ollama流式响应体，Content-Type: {}", 
                                   response.header("Content-Type"));
                        
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
        logger.info("开始构建Ollama非流式请求");
        logger.debug("原始提示内容 (长度: {}):\n{}", prompt.length(), prompt);
        
        // 构建请求体
        String requestBody = buildGenerateRequest(prompt, false);
        logger.info("Ollama非流式请求体构建完成，长度: {}", requestBody.length());
        logger.debug("Ollama完整请求体:\n{}", requestBody);
        
        String url = ollamaConfig.getGenerateUrl();
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
     * 构建生成请求的JSON
     */
    private String buildGenerateRequest(String prompt, boolean stream) {
        logger.debug("开始构建Ollama请求参数");
        
        try {
            String model = ollamaConfig.getModel();
            int maxTokens = ollamaConfig.getMaxTokens();
            double temperature = ollamaConfig.getTemperature();
            
            logger.debug("Ollama请求参数 - 模型: {}, 流式: {}, 最大令牌: {}, 温度: {}", 
                        model, stream, maxTokens, temperature);
            logger.debug("提示文本长度: {}", prompt.length());
            
            GenerateRequest request = new GenerateRequest(
                    model,
                    prompt,
                    stream,
                    maxTokens,
                    temperature
            );
            
            String jsonRequest = JsonUtil.toJson(request);
            logger.debug("请求JSON构建成功，长度: {}", jsonRequest.length());
            
            return jsonRequest;
        } catch (Exception e) {
            logger.error("构建请求JSON失败", e);
            throw new RuntimeException("构建请求失败", e);
        }
    }
    
    /**
     * 处理流式响应
     */
    private void processStreamingResponse(ResponseBody responseBody, Consumer<String> onChunk, Consumer<Throwable> onError) {
        logger.debug("开始处理Ollama流式响应");
        StringBuilder totalResponse = new StringBuilder();
        int chunkCount = 0;
        
        try {
            // 按行读取响应
            byte[] bytes = responseBody.bytes();
            String responseText = new String(bytes);
            String[] lines = responseText.split("\n");
            
            logger.debug("Ollama响应包含{}行数据", lines.length);
            
            for (String responseLine : lines) {
                if (responseLine.trim().isEmpty()) {
                    continue;
                }
                
                try {
                    JsonNode jsonNode = JsonUtil.parseJson(responseLine);
                    if (jsonNode == null) {
                        logger.debug("跳过无效的JSON行: {}", responseLine);
                        continue;
                    }
                    
                    // 检查是否完成
                    Boolean done = JsonUtil.getBooleanValue(jsonNode, "done");
                    if (done != null && done) {
                        logger.debug("Ollama流式响应完成标志，done=true");
                        break;
                    }
                    
                    // 提取响应文本
                    String chunk = JsonUtil.getStringValue(jsonNode, "response");
                    if (chunk != null && !chunk.isEmpty()) {
                        chunkCount++;
                        totalResponse.append(chunk);
                        
                        logger.debug("Ollama流式数据块#{}: '{}' (长度: {})", 
                                   chunkCount, chunk.replace("\n", "\\n"), chunk.length());
                        logger.debug("累积响应文本: '{}' (总长度: {})", 
                                   totalResponse.toString().replace("\n", "\\n"), totalResponse.length());
                        
                        onChunk.accept(chunk);
                    }
                    
                } catch (Exception e) {
                    logger.warn("解析流式响应行失败: {}", responseLine, e);
                }
            }
            
            logger.info("Ollama流式响应处理完成，共处理{}个数据块，总响应长度: {}", 
                       chunkCount, totalResponse.length());
            
        } catch (Exception e) {
            logger.error("处理流式响应时发生错误", e);
            onError.accept(e);
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
            
            String response = JsonUtil.getStringValue(jsonNode, "response");
            if (response != null) {
                return response;
            } else {
                logger.warn("响应中未找到response字段: {}", responseJson);
                return "抱歉，无法解析AI的响应。";
            }
        } catch (Exception e) {
            logger.error("解析响应JSON失败", e);
            return "抱歉，处理AI响应时出现错误。";
        }
    }
    
    /**
     * 生成请求数据类
     */
    private static class GenerateRequest {
        public final String model;
        public final String prompt;
        public final boolean stream;
        public final int max_tokens;
        public final double temperature;
        
        public GenerateRequest(String model, String prompt, boolean stream, int maxTokens, double temperature) {
            this.model = model;
            this.prompt = prompt;
            this.stream = stream;
            this.max_tokens = maxTokens;
            this.temperature = temperature;
        }
        
        // Getters for Jackson serialization
        public String getModel() {
            return model;
        }
        
        public String getPrompt() {
            return prompt;
        }
        
        public boolean isStream() {
            return stream;
        }
        
        public int getMax_tokens() {
            return max_tokens;
        }
        
        public double getTemperature() {
            return temperature;
        }
    }
}

package com.chatbot.service;

import com.chatbot.config.OllamaConfig;
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
        try {
            // 构建请求体
            String requestBody = buildGenerateRequest(prompt, true);
            
            Request request = new Request.Builder()
                    .url(ollamaConfig.getGenerateUrl())
                    .post(RequestBody.create(requestBody, MediaType.parse("application/json")))
                    .build();
            
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
        // 构建请求体
        String requestBody = buildGenerateRequest(prompt, false);
        
        Request request = new Request.Builder()
                .url(ollamaConfig.getGenerateUrl())
                .post(RequestBody.create(requestBody, MediaType.parse("application/json")))
                .build();
        
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Ollama API返回错误: " + response.code() + " " + response.message());
            }
            
            ResponseBody responseBody = response.body();
            if (responseBody == null) {
                throw new IOException("响应体为空");
            }
            
            String responseText = responseBody.string();
            return extractResponseText(responseText);
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
        try {
            return objectMapper.writeValueAsString(new GenerateRequest(
                    ollamaConfig.getModel(),
                    prompt,
                    stream,
                    ollamaConfig.getMaxTokens(),
                    ollamaConfig.getTemperature()
            ));
        } catch (Exception e) {
            logger.error("构建请求JSON失败", e);
            throw new RuntimeException("构建请求失败", e);
        }
    }
    
    /**
     * 处理流式响应
     */
    private void processStreamingResponse(ResponseBody responseBody, Consumer<String> onChunk, Consumer<Throwable> onError) {
        try {
            // 按行读取响应
            byte[] bytes = responseBody.bytes();
            String responseText = new String(bytes);
            String[] lines = responseText.split("\n");
            
            for (String responseLine : lines) {
                if (responseLine.trim().isEmpty()) {
                    continue;
                }
                
                try {
                    JsonNode jsonNode = objectMapper.readTree(responseLine);
                    
                    // 检查是否完成
                    boolean done = jsonNode.has("done") && jsonNode.get("done").asBoolean();
                    
                    // 提取响应文本
                    if (jsonNode.has("response")) {
                        String chunk = jsonNode.get("response").asText();
                        if (!chunk.isEmpty()) {
                            onChunk.accept(chunk);
                        }
                    }
                    
                    // 如果完成，结束处理
                    if (done) {
                        break;
                    }
                    
                } catch (Exception e) {
                    logger.warn("解析流式响应行失败: {}", responseLine, e);
                }
            }
            
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
            JsonNode jsonNode = objectMapper.readTree(responseJson);
            if (jsonNode.has("response")) {
                return jsonNode.get("response").asText();
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

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
        generateStreamingResponse(null, prompt, onChunk, onError);
    }
    
    /**
     * 流式生成响应（支持系统提示词分离）
     */
    public void generateStreamingResponse(String systemPrompt, String userPrompt, Consumer<String> onChunk, Consumer<Throwable> onError) {
        logger.info("开始构建Ollama流式请求");
        logger.debug("系统提示内容 (长度: {}):\n{}", 
                   systemPrompt != null ? systemPrompt.length() : 0, 
                   systemPrompt != null ? systemPrompt : "无");
        logger.debug("用户提示内容 (长度: {}):\n{}", userPrompt.length(), userPrompt);
        
        try {
            // 构建请求体
            String requestBody = buildChatRequest(systemPrompt, userPrompt, true);
            logger.info("Ollama请求体构建完成，长度: {}", requestBody.length());
            logger.debug("Ollama完整请求体:\n{}", requestBody);
            
            String url = ollamaConfig.getChatUrl();
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
     * 流式生成响应（支持完整消息列表）
     */
    public void generateStreamingResponse(List<ChatService.OllamaMessage> messages, Consumer<String> onChunk, Consumer<Throwable> onError) {
        logger.info("开始构建Ollama流式请求（消息列表模式）");
        logger.debug("消息数量: {}", messages.size());
        
        try {
            // 构建请求体
            String requestBody = buildChatRequestFromMessages(messages, true);
            logger.info("Ollama请求体构建完成，长度: {}", requestBody.length());
            logger.debug("Ollama完整请求体:\n{}", requestBody);
            
            String url = ollamaConfig.getChatUrl();
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
            
            logger.debug("Ollama请求参数 - 模型: {}, 流式: {}, 温度: {}", 
                        model, stream, temperature);
            logger.debug("系统提示长度: {}, 用户提示长度: {}", 
                        systemPrompt != null ? systemPrompt.length() : 0, userPrompt.length());
            
            ChatRequest request = new ChatRequest(
                    model,
                    systemPrompt,
                    userPrompt,
                    stream,
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
     * 从消息列表构建聊天请求的JSON
     */
    private String buildChatRequestFromMessages(List<ChatService.OllamaMessage> messages, boolean stream) {
        logger.debug("开始构建Ollama聊天请求参数（消息列表模式）");
        
        try {
            String model = ollamaConfig.getModel();
            double temperature = ollamaConfig.getTemperature();
            
            logger.debug("Ollama请求参数 - 模型: {}, 流式: {}, 温度: {}, 消息数量: {}", 
                        model, stream, temperature, messages.size());
            
            ChatRequestFromMessages request = new ChatRequestFromMessages(
                    model,
                    messages,
                    stream,
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
                    
                    // 提取响应文本 - /api/chat 接口返回的是 message 对象
                    JsonNode messageNode = jsonNode.get("message");
                    if (messageNode != null) {
                        String chunk = JsonUtil.getStringValue(messageNode, "content");
                        if (chunk != null && !chunk.isEmpty()) {
                            chunkCount++;
                            totalResponse.append(chunk);
                            
                            logger.debug("Ollama流式数据块#{}: '{}' (长度: {})", 
                                       chunkCount, chunk.replace("\n", "\\n"), chunk.length());
                            logger.debug("累积响应文本: '{}' (总长度: {})", 
                                       totalResponse.toString().replace("\n", "\\n"), totalResponse.length());
                            
                            onChunk.accept(chunk);
                        }
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
     * 聊天请求数据类
     */
    private static class ChatRequest {
        public final String model;
        public final Message[] messages;
        public final boolean stream;
        public final Options options;
        
        public ChatRequest(String model, String systemPrompt, String userPrompt, boolean stream, double temperature) {
            this.model = model;
            
            // 构建消息数组
            if (systemPrompt != null && !systemPrompt.trim().isEmpty()) {
                this.messages = new Message[] { 
                    new Message("system", systemPrompt.trim()),
                    new Message("user", userPrompt) 
                };
            } else {
                this.messages = new Message[] { 
                    new Message("user", userPrompt) 
                };
            }
            
            this.stream = stream;
            this.options = new Options(temperature);
        }
        
        // Getters for Jackson serialization
        public String getModel() {
            return model;
        }
        
        public Message[] getMessages() {
            return messages;
        }
        
        public boolean isStream() {
            return stream;
        }
        
        public Options getOptions() {
            return options;
        }
    }
    
    /**
     * 从消息列表构建的聊天请求数据类
     */
    private static class ChatRequestFromMessages {
        public final String model;
        public final Message[] messages;
        public final boolean stream;
        public final Options options;
        
        public ChatRequestFromMessages(String model, List<ChatService.OllamaMessage> messageList, boolean stream, double temperature) {
            this.model = model;
            
            // 将 OllamaMessage 列表转换为 Message 数组
            this.messages = messageList.stream()
                    .map(msg -> new Message(msg.getRole(), msg.getContent()))
                    .toArray(Message[]::new);
            
            this.stream = stream;
            this.options = new Options(temperature);
        }
        
        // Getters for Jackson serialization
        public String getModel() {
            return model;
        }
        
        public Message[] getMessages() {
            return messages;
        }
        
        public boolean isStream() {
            return stream;
        }
        
        public Options getOptions() {
            return options;
        }
    }
    
    /**
     * 消息数据类
     */
    private static class Message {
        public final String role;
        public final String content;
        
        public Message(String role, String content) {
            this.role = role;
            this.content = content;
        }
        
        public String getRole() {
            return role;
        }
        
        public String getContent() {
            return content;
        }
    }
    
    /**
     * 选项数据类
     */
    private static class Options {
        public final double temperature;
        
        public Options(double temperature) {
            this.temperature = temperature;
        }
        
        public double getTemperature() {
            return temperature;
        }
    }
}

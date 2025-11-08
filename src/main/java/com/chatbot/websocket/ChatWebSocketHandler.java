package com.chatbot.websocket;

import com.chatbot.mapper.ChatMessageMapper;
import com.chatbot.model.domain.ChatMessage;
import com.chatbot.model.dto.websocket.ChatMessageDTO;
import com.chatbot.service.ChatService;
import com.chatbot.service.MultiChannelDispatcher;
import com.chatbot.service.llm.impl.OllamaLLMServiceImpl;
import com.chatbot.service.channel.Live2DChannel;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 聊天WebSocket处理器
 * 处理WebSocket连接、消息收发和会话管理
 */
@Component
public class ChatWebSocketHandler implements WebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(ChatWebSocketHandler.class);

    private final ChatService chatService;
    private final MultiChannelDispatcher multiChannelDispatcher;
    private final Live2DChannel live2dChannel;
    private final OllamaLLMServiceImpl llmService;  // 使用新的 LLM 服务
    private final ObjectMapper objectMapper;
    private final ASRWebSocketHandler asrWebSocketHandler;
    private final ChatMessageMapper chatMessageMapper;  // ✅ DTO转换器

    // 存储活跃的WebSocket会话
    private final ConcurrentHashMap<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    
    // 存储会话的当前任务ID
    private final ConcurrentHashMap<String, String> sessionTasks = new ConcurrentHashMap<>();

    public ChatWebSocketHandler(ChatService chatService, 
                               MultiChannelDispatcher multiChannelDispatcher,
                               Live2DChannel live2dChannel,
                               @Qualifier("ollamaLLMService") OllamaLLMServiceImpl llmService,
                               ObjectMapper objectMapper,
                               ASRWebSocketHandler asrWebSocketHandler,
                               ChatMessageMapper chatMessageMapper) {  // ✅ 注入Mapper
        this.chatService = chatService;
        this.multiChannelDispatcher = multiChannelDispatcher;
        this.live2dChannel = live2dChannel;
        this.llmService = llmService;  // 使用新的 LLM 服务
        this.objectMapper = objectMapper;
        this.asrWebSocketHandler = asrWebSocketHandler;
        this.chatMessageMapper = chatMessageMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        long startTime = System.currentTimeMillis();
        String sessionId = generateSessionId();
        session.getAttributes().put("sessionId", sessionId);
        sessions.put(sessionId, session);

        logger.debug("WebSocket连接建立，sessionId: {}, 连接数: {}", sessionId, sessions.size());
        logger.debug("WebSocket连接详情 - RemoteAddress: {}, Uri: {}",
                session.getRemoteAddress(), session.getUri());

        // 发送连接成功消息
        ChatMessage welcomeMessage = new ChatMessage();
        welcomeMessage.setType("system");
        welcomeMessage.setContent("连接成功，欢迎使用AI聊天机器人！");
        welcomeMessage.setSessionId(sessionId);

        sendMessage(session, welcomeMessage);

        long connectionTime = System.currentTimeMillis() - startTime;
        logger.debug("WebSocket连接建立完成，sessionId: {}, 耗时: {}ms", sessionId, connectionTime);
    }

    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
        String sessionId = (String) session.getAttributes().get("sessionId");
        long startTime = System.currentTimeMillis();

        logger.debug("接收到WebSocket消息，sessionId: {}, messageType: {}",
                sessionId, message.getClass().getSimpleName());

        if (message instanceof TextMessage textMessage) {
            try {
                // 解析收到的消息
                String payload = textMessage.getPayload();

                // ✅ 先反序列化为DTO，再转换为领域模型
                ChatMessageDTO dto = objectMapper.readValue(payload, ChatMessageDTO.class);
                ChatMessage chatMessage = chatMessageMapper.fromDTO(dto);
                chatMessage.setSessionId(sessionId);

                // 记录用户消息接收时间戳
                long userMessageTimestamp = System.currentTimeMillis();

                // 检查是否是前端反馈消息
                if ("audio_playback_completed".equals(chatMessage.getType())) {
                    handleAudioPlaybackCompleted(chatMessage, sessionId);
                    return;
                }
                
                // 检查是否是ASR音频数据
                if ("asr_audio_chunk".equals(chatMessage.getType())) {
                    handleASRAudioChunk(chatMessage, sessionId, session);
                    return;
                }
                
                // 检查是否是ASR相关消息
                if (asrWebSocketHandler.handleASRMessage(session, chatMessage)) {
                    return; // ASR消息已处理
                }
                
                // 检查是否是系统命令
                if ("system".equals(chatMessage.getType()) && chatMessage.getMetadata() != null) {
                    String action = (String) chatMessage.getMetadata().get("action");
                    
                    if ("check_service".equals(action)) {
                        // 处理Ollama服务状态检查
                        handleOllamaStatusCheck(session, sessionId);
                        return;
                    } else if ("toggle_thinking".equals(action)) {
                        // 处理思考显示切换
                        handleThinkingToggle(session, sessionId, chatMessage);
                        return;
                    } else if ("toggle_web_search".equals(action)) {
                        // 处理联网搜索切换
                        handleWebSearchToggle(session, sessionId, chatMessage);
                        return;
                    } else if ("interrupt".equals(action)) {
                        // 处理打断信号
                        handleInterruptSignal(session, sessionId, chatMessage);
                        return;
                    }
                }
                
                // 用于跟踪是否是第一次响应，isFirstResponse 设计成 boolean[] 是为了绕过 Java Lambda 表达式的变量捕获限制。
                final boolean[] isFirstResponse = {true};
                
                // 使用多通道分发器处理消息
                String taskId = multiChannelDispatcher.processMessage(chatMessage, response -> {
                    try {
                        // 记录第一次响应时间
                        if (isFirstResponse[0] && response.getContent() != null && !response.getContent().isEmpty()) {
                            long firstResponseTime = System.currentTimeMillis();
                            long timeToFirstResponse = firstResponseTime - userMessageTimestamp;
                            
                            logger.info("🚀 首次响应时间统计 - sessionId: {}, 从用户消息到首次响应: {}ms",
                                       sessionId, timeToFirstResponse);
                            
                            isFirstResponse[0] = false;
                        }
                        
                        // 优化流式消息发送
                        sendStreamingMessage(session, response, sessionId);
                        
                    } catch (IOException e) {
                        // 检查是否是连接关闭相关的错误
                        if (e.getMessage() != null && 
                            (e.getMessage().contains("ClosedChannelException") ||
                             e.getMessage().contains("Connection reset") ||
                             e.getMessage().contains("Broken pipe"))) {
                            logger.debug("WebSocket连接已关闭，停止发送消息，sessionId: {}", sessionId);
                        } else {
                            logger.error("发送消息失败，sessionId: {}", sessionId, e);
                        }
                    }
                });
                
                // 存储当前任务ID
                sessionTasks.put(sessionId, taskId);

            } catch (Exception e) {
                long processingTime = System.currentTimeMillis() - startTime;
                logger.error("处理WebSocket消息时发生错误，sessionId: {}, 处理时间: {}ms",
                        sessionId, processingTime, e);

                // 发送错误消息
                ChatMessage errorMessage = new ChatMessage();
                errorMessage.setType("error");
                errorMessage.setContent("处理消息时发生错误，请稍后重试");
                errorMessage.setSessionId(sessionId);

                sendMessage(session, errorMessage);
            }
        } else {
            logger.warn("收到不支持的消息类型，sessionId: {}, messageType: {}",
                    sessionId, message.getClass().getSimpleName());
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        String sessionId = (String) session.getAttributes().get("sessionId");
        
        // 检查是否是应用关闭时的正常错误
        if (exception instanceof IOException && 
            (exception.getMessage() != null && 
             (exception.getMessage().contains("ClosedChannelException") ||
              exception.getMessage().contains("Connection reset") ||
              exception.getMessage().contains("Broken pipe")))) {
            // 这些是应用关闭时的正常错误，使用DEBUG级别记录
            logger.debug("WebSocket连接在应用关闭时断开，会话ID: {}, 错误: {}", 
                        sessionId, exception.getMessage());
        } else {
            // 其他错误使用ERROR级别记录
            logger.error("WebSocket传输错误，会话ID: {}", sessionId, exception);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
        String sessionId = (String) session.getAttributes().get("sessionId");
        sessions.remove(sessionId);

        logger.info("WebSocket连接关闭，会话ID: {}, 关闭状态: {}", sessionId, closeStatus);

        // 清理会话相关资源
        chatService.cleanupSession(sessionId);
        multiChannelDispatcher.cleanupSession(sessionId);
        
        // 清理ASR会话资源
        if (asrWebSocketHandler != null) {
            asrWebSocketHandler.cleanupSession(sessionId);
        }
        
        // 清理任务ID映射
        sessionTasks.remove(sessionId);
    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }

    /**
     * 发送消息到客户端（使用DTO层）
     */
    private void sendMessage(WebSocketSession session, ChatMessage message) throws IOException {
        if (session != null && session.isOpen()) {
            try {
                // ✅ 转换为DTO再序列化
                ChatMessageDTO dto = chatMessageMapper.toDTO(message);
                String messageJson = objectMapper.writeValueAsString(dto);
                session.sendMessage(new TextMessage(messageJson));
                
                logger.trace("消息发送成功: type={}, sessionId={}", dto.getType(), dto.getSessionId());
            } catch (IOException e) {
                // 检查是否是连接关闭相关的错误
                if (e.getMessage() != null && 
                    (e.getMessage().contains("ClosedChannelException") ||
                     e.getMessage().contains("Connection reset") ||
                     e.getMessage().contains("Broken pipe"))) {
                    logger.debug("WebSocket连接已关闭，无法发送消息，sessionId: {}", message.getSessionId());
                } else {
                    logger.error("发送WebSocket消息失败，sessionId: {}", message.getSessionId(), e);
                    throw e;
                }
            }
        } else {
            logger.debug("WebSocket会话已关闭或为空，无法发送消息，sessionId: {}", message.getSessionId());
        }
    }
    
    /**
     * 优化的流式消息发送（使用DTO层）
     */
    private void sendStreamingMessage(WebSocketSession session, ChatMessage message, String sessionId) throws IOException {
        if (!session.isOpen()) {
            logger.warn("WebSocket会话已关闭，无法发送消息，sessionId: {}", sessionId);
            return;
        }
        
        try {
            // ✅ 统一使用DTO层（流式消息也通过DTO）
            ChatMessageDTO dto = chatMessageMapper.toDTO(message);
            String messageJson = objectMapper.writeValueAsString(dto);
            
            session.sendMessage(new TextMessage(messageJson));
            
        } catch (Exception e) {
            logger.error("发送流式消息失败，sessionId: {}", sessionId, e);
            throw new IOException("发送流式消息失败", e);
        }
    }
    
    // JSON转义功能已由ObjectMapper自动处理

    /**
     * 生成基于日期的会话ID (格式: YYYYMMDD)
     * 同一天内所有连接都使用相同的会话ID，实现单用户单日会话模式
     */
    private String generateSessionId() {
        return java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
    }

    /**
     * 处理Ollama服务状态检查
     */
    private void handleOllamaStatusCheck(WebSocketSession session, String sessionId) {
        try {
            boolean isAvailable = llmService.isServiceAvailable();

            ChatMessage statusMessage = new ChatMessage();
            statusMessage.setType("system");
            statusMessage.setContent("Ollama服务状态已更新");
            statusMessage.setSessionId(sessionId);
            statusMessage.setMetadata(Map.of("ollama_status", isAvailable ? "available" : "unavailable"));

            sendMessage(session, statusMessage);

        } catch (Exception e) {
            logger.error("检查Ollama服务状态时发生错误，会话ID: {}", sessionId, e);

            try {
                ChatMessage errorMessage = new ChatMessage();
                errorMessage.setType("system");
                errorMessage.setContent("检查服务状态时发生错误");
                errorMessage.setSessionId(sessionId);
                errorMessage.setMetadata(Map.of("ollama_status", "unavailable"));

                sendMessage(session, errorMessage);
            } catch (IOException ex) {
                logger.error("发送错误消息失败", ex);
            }
        }
    }

    /**
     * 处理思考显示切换
     */
    private void handleThinkingToggle(WebSocketSession session, String sessionId, ChatMessage message) {
        try {
            Boolean showThinking = (Boolean) message.getMetadata().get("showThinking");
            if (showThinking == null) {
                showThinking = false;
            }
            
            // 设置用户偏好
            chatService.setUserThinkingPreference(sessionId, showThinking);
            
            // 发送确认消息
            ChatMessage confirmMessage = new ChatMessage();
            confirmMessage.setType("system");
            confirmMessage.setContent(showThinking ? "已开启思考过程显示" : "已关闭思考过程显示");
            confirmMessage.setSessionId(sessionId);
            confirmMessage.setMetadata(Map.of(
                "thinking_toggle", "confirmed",
                "showThinking", showThinking
            ));
            
            sendMessage(session, confirmMessage);
            
            logger.info("用户切换思考显示状态 - sessionId: {}, showThinking: {}", sessionId, showThinking);
            
        } catch (Exception e) {
            logger.error("处理思考显示切换时发生错误，会话 ID: {}", sessionId, e);
            
            try {
                ChatMessage errorMessage = new ChatMessage();
                errorMessage.setType("system");
                errorMessage.setContent("切换思考显示状态失败");
                errorMessage.setSessionId(sessionId);
                
                sendMessage(session, errorMessage);
            } catch (IOException ex) {
                logger.error("发送错误消息失败", ex);
            }
        }
    }
    
    /**
     * 处理联网搜索切换
     */
    private void handleWebSearchToggle(WebSocketSession session, String sessionId, ChatMessage message) {
        try {
            Boolean useWebSearch = (Boolean) message.getMetadata().get("useWebSearch");
            if (useWebSearch == null) {
                useWebSearch = false;
            }
            
            // 设置用户偏好
            chatService.setUserWebSearchPreference(sessionId, useWebSearch);
            
            // 发送确认消息
            ChatMessage confirmMessage = new ChatMessage();
            confirmMessage.setType("system");
            confirmMessage.setContent(useWebSearch ? "已开启联网搜索功能" : "已关闭联网搜索功能");
            confirmMessage.setSessionId(sessionId);
            confirmMessage.setMetadata(Map.of(
                "web_search_toggle", "confirmed",
                "useWebSearch", useWebSearch
            ));
            
            sendMessage(session, confirmMessage);
            
            logger.info("用户切换联网搜索状态 - sessionId: {}, useWebSearch: {}", sessionId, useWebSearch);
            
        } catch (Exception e) {
            logger.error("处理联网搜索切换时发生错误，会话 ID: {}", sessionId, e);
            
            try {
                ChatMessage errorMessage = new ChatMessage();
                errorMessage.setType("system");
                errorMessage.setContent("切换联网搜索状态失败");
                errorMessage.setSessionId(sessionId);
                
                sendMessage(session, errorMessage);
            } catch (IOException ex) {
                logger.error("发送错误消息失败", ex);
            }
        }
    }
    
    
    /**
     * 处理打断信号
     */
    private void handleInterruptSignal(WebSocketSession session, String sessionId, ChatMessage message) {
        try {
            String interruptType = (String) message.getMetadata().get("interruptType");
            String reason = (String) message.getMetadata().get("reason");
            
            logger.info("收到打断信号 - sessionId: {}, type: {}, reason: {}", sessionId, interruptType, reason);
            
            // 中断当前会话的所有任务
            int interruptedTasks = chatService.interruptSessionTasks(sessionId);
            
            // 发送打断确认消息
            ChatMessage confirmMessage = new ChatMessage();
            confirmMessage.setType("system");
            confirmMessage.setContent("AI回复已被中断");
            confirmMessage.setSessionId(sessionId);
            confirmMessage.setMetadata(Map.of(
                "interrupt_confirmed", true,
                "interrupted_tasks", interruptedTasks,
                "interrupt_type", interruptType != null ? interruptType : "user_stop"
            ));
            
            sendMessage(session, confirmMessage);
            
            logger.info("打断处理完成 - sessionId: {}, 中断了 {} 个任务", sessionId, interruptedTasks);
            
        } catch (Exception e) {
            logger.error("处理打断信号时发生错误，会话ID: {}", sessionId, e);
            
            try {
                ChatMessage errorMessage = new ChatMessage();
                errorMessage.setType("system");
                errorMessage.setContent("处理打断信号时发生错误");
                errorMessage.setSessionId(sessionId);
                
                sendMessage(session, errorMessage);
            } catch (IOException ex) {
                logger.error("发送错误消息失败", ex);
            }
        }
    }
    
    /**
     * 处理音频播放完成通知
     */
    private void handleAudioPlaybackCompleted(ChatMessage message, String sessionId) {
        try {
            String sentenceId = message.getSentenceId();
            
            if (sentenceId != null) {
                logger.debug("收到音频播放完成通知: sentenceId={}, sessionId={}", sentenceId, sessionId);
                
                // 通知Live2D通道处理下一句
                live2dChannel.onAudioCompleted(sessionId, sentenceId);
            } else {
                logger.warn("音频播放完成通知缺少sentenceId: sessionId={}", sessionId);
            }
            
        } catch (Exception e) {
            logger.error("处理音频播放完成通知失败: sessionId={}", sessionId, e);
        }
    }
    
    /**
     * 处理ASR音频数据块
     */
    private void handleASRAudioChunk(ChatMessage message, String sessionId, WebSocketSession session) {
        try {
            logger.debug("收到ASR音频数据: sessionId={}", sessionId);
            
            // 委托给ASRWebSocketHandler处理
            asrWebSocketHandler.handleAudioChunk(session, message);
            
        } catch (Exception e) {
            logger.error("处理ASR音频数据失败: sessionId={}", sessionId, e);
            
            try {
                ChatMessage errorMessage = new ChatMessage();
                errorMessage.setType("system");
                errorMessage.setContent("ASR音频处理失败: " + e.getMessage());
                errorMessage.setSessionId(sessionId);
                
                sendMessage(session, errorMessage);
            } catch (IOException ex) {
                logger.error("发送ASR错误消息失败", ex);
            }
        }
    }
    
    /**
     * 获取活跃会话数量
     */
    public int getActiveSessionCount() {
        return sessions.size();
    }
}

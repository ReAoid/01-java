package com.chatbot.model;

import com.chatbot.util.IdUtil;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 聊天消息模型
 * 支持多种消息类型和元数据
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatMessage {
    
    /**
     * 消息类型：text, voice, image, system, error
     */
    private String type;
    
    /**
     * 消息内容
     */
    private String content;
    
    /**
     * 会话ID
     */
    private String sessionId;
    
    /**
     * 消息ID
     */
    private String messageId;
    
    /**
     * 角色：user, assistant, system
     */
    private String role;
    
    /**
     * 时间戳
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;
    
    /**
     * 是否为流式消息的一部分
     */
    private boolean streaming;
    
    /**
     * 流式消息是否完成
     */
    private boolean streamComplete;
    
    /**
     * 是否为思考内容
     */
    private boolean thinking;
    
    /**
     * 思考内容（如果是思考消息）
     */
    private String thinkingContent;
    
    /**
     * 是否显示思考过程（用户设置）
     */
    private Boolean showThinking;
    
    /**
     * TTS模式：none, char_stream, sentence_sync
     */
    private String ttsMode;
    
    /**
     * 输出通道类型：chat_window, live2d
     */
    private String channelType;
    
    /**
     * 句子唯一标识
     */
    private String sentenceId;
    
    /**
     * 句子在会话中的顺序
     */
    private Integer sentenceOrder;
    
    /**
     * 是否为句子结束
     */
    private Boolean sentenceComplete;
    
    /**
     * 音频数据（Base64编码或二进制）
     */
    private byte[] audioData;
    
    /**
     * 音频URL（可选，用于大文件）
     */
    private String audioUrl;
    
    /**
     * 文本是否准备就绪
     */
    private Boolean textReady;
    
    /**
     * 音频是否准备就绪
     */
    private Boolean audioReady;
    
    /**
     * 文本和音频是否都准备就绪
     */
    private Boolean bothReady;
    
    /**
     * 额外元数据
     */
    private Map<String, Object> metadata;
    
    public ChatMessage() {
        this.timestamp = LocalDateTime.now();
        this.messageId = generateMessageId();
    }
    
    // Getters and Setters
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public String getSessionId() {
        return sessionId;
    }
    
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
    
    public String getMessageId() {
        return messageId;
    }
    
    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }
    
    public String getRole() {
        return role;
    }
    
    public void setRole(String role) {
        this.role = role;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
    
    public boolean isStreaming() {
        return streaming;
    }
    
    public void setStreaming(boolean streaming) {
        this.streaming = streaming;
    }
    
    public boolean isStreamComplete() {
        return streamComplete;
    }
    
    public void setStreamComplete(boolean streamComplete) {
        this.streamComplete = streamComplete;
    }
    
    public Map<String, Object> getMetadata() {
        return metadata;
    }
    
    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
    
    public boolean isThinking() {
        return thinking;
    }
    
    public void setThinking(boolean thinking) {
        this.thinking = thinking;
    }
    
    public String getThinkingContent() {
        return thinkingContent;
    }
    
    public void setThinkingContent(String thinkingContent) {
        this.thinkingContent = thinkingContent;
    }
    
    public Boolean getShowThinking() {
        return showThinking;
    }
    
    public void setShowThinking(Boolean showThinking) {
        this.showThinking = showThinking;
    }
    
    public String getTtsMode() {
        return ttsMode;
    }
    
    public void setTtsMode(String ttsMode) {
        this.ttsMode = ttsMode;
    }
    
    public String getChannelType() {
        return channelType;
    }
    
    public void setChannelType(String channelType) {
        this.channelType = channelType;
    }
    
    public String getSentenceId() {
        return sentenceId;
    }
    
    public void setSentenceId(String sentenceId) {
        this.sentenceId = sentenceId;
    }
    
    public Integer getSentenceOrder() {
        return sentenceOrder;
    }
    
    public void setSentenceOrder(Integer sentenceOrder) {
        this.sentenceOrder = sentenceOrder;
    }
    
    public Boolean getSentenceComplete() {
        return sentenceComplete;
    }
    
    public void setSentenceComplete(Boolean sentenceComplete) {
        this.sentenceComplete = sentenceComplete;
    }
    
    /**
     * 隐藏原始byte[]字段，避免JSON序列化
     */
    @com.fasterxml.jackson.annotation.JsonIgnore
    public byte[] getAudioData() {
        return audioData;
    }
    
    @com.fasterxml.jackson.annotation.JsonIgnore
    public void setAudioData(byte[] audioData) {
        this.audioData = audioData;
    }
    
    /**
     * 获取Base64编码的音频数据（用于JSON序列化）
     */
    @com.fasterxml.jackson.annotation.JsonProperty("audioData")
    public String getAudioDataBase64() {
        if (audioData == null) {
            return null;
        }
        return java.util.Base64.getEncoder().encodeToString(audioData);
    }
    
    /**
     * 设置Base64编码的音频数据（用于JSON反序列化）
     */
    @com.fasterxml.jackson.annotation.JsonProperty("audioData")
    public void setAudioDataBase64(String base64AudioData) {
        if (base64AudioData == null) {
            this.audioData = null;
        } else {
            this.audioData = java.util.Base64.getDecoder().decode(base64AudioData);
        }
    }
    
    public String getAudioUrl() {
        return audioUrl;
    }
    
    public void setAudioUrl(String audioUrl) {
        this.audioUrl = audioUrl;
    }
    
    public Boolean getTextReady() {
        return textReady;
    }
    
    public void setTextReady(Boolean textReady) {
        this.textReady = textReady;
    }
    
    public Boolean getAudioReady() {
        return audioReady;
    }
    
    public void setAudioReady(Boolean audioReady) {
        this.audioReady = audioReady;
    }
    
    public Boolean getBothReady() {
        return bothReady;
    }
    
    public void setBothReady(Boolean bothReady) {
        this.bothReady = bothReady;
    }
    
    /**
     * 生成唯一消息ID (使用IdUtil工具类)
     */
    private String generateMessageId() {
        return IdUtil.messageId();
    }
    
    @Override
    public String toString() {
        return "ChatMessage{type='" + type + "', content='" + content + "', sessionId='" + sessionId + "', role='" + role + "', timestamp=" + timestamp + "}";
    }
}

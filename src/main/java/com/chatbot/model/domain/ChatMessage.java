package com.chatbot.model.domain;

import com.chatbot.util.IdUtil;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 聊天消息模型（重构版 - 组合模式 + 便捷API）
 * 
 * 设计理念：
 * - 内部使用组件模式实现职责分离
 * - 对外提供便捷方法保持API简洁
 * - 支持两种使用方式：
 *   1. 便捷API：msg.setContentText("hello")
 *   2. 组件API：msg.getContent().setText("hello")
 * 
 * @version 2.0 - 组合模式重构
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatMessage {
    
    // ========== 核心字段 ==========
    
    private String messageId;
    private String sessionId;
    private String role;
    private String type;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;
    
    // ========== 功能组件（内部实现） ==========
    
    private ChatMessageContent content;
    private ChatMessageStreaming streaming;
    private ChatMessageThinking thinking;
    private ChatMessageAudio audio;
    private ChatMessageOutput output;
    private ChatMessageSync sync;
    
    // ========== 扩展元数据 ==========
    
    private Map<String, Object> metadata;
    
    // ========== 构造函数 ==========
    
    public ChatMessage() {
        this.timestamp = LocalDateTime.now();
        this.messageId = generateMessageId();
    }
    
    private String generateMessageId() {
        return IdUtil.messageId();
    }
    
    // ========== 便捷API - 内容操作 ==========
    
    /**
     * 获取消息文本内容
     */
    public String getContent() {
        return content != null ? content.getText() : null;
    }
    
    /**
     * 设置消息文本内容
     */
    public void setContent(String text) {
        if (content == null) {
            content = new ChatMessageContent();
        }
        content.setText(text);
    }
    
    // ========== 便捷API - 流式控制 ==========
    
    /**
     * 是否为流式消息
     */
    public boolean isStreaming() {
        return streaming != null && streaming.isStreaming();
    }
    
    /**
     * 设置流式状态
     */
    public void setStreaming(boolean value) {
        if (streaming == null) {
            streaming = new ChatMessageStreaming();
        }
        streaming.setStreaming(value);
    }
    
    /**
     * 流式是否完成
     */
    public boolean isStreamComplete() {
        return streaming != null && streaming.isStreamComplete();
    }
    
    /**
     * 设置流式完成状态
     */
    public void setStreamComplete(boolean complete) {
        if (streaming == null) {
            streaming = new ChatMessageStreaming();
        }
        streaming.setStreamComplete(complete);
    }
    
    /**
     * 获取句子ID
     */
    public String getSentenceId() {
        return streaming != null ? streaming.getSentenceId() : null;
    }
    
    /**
     * 设置句子ID
     */
    public void setSentenceId(String sentenceId) {
        if (streaming == null) {
            streaming = new ChatMessageStreaming();
        }
        streaming.setSentenceId(sentenceId);
    }
    
    /**
     * 获取句子顺序
     */
    public Integer getSentenceOrder() {
        return streaming != null ? streaming.getSentenceOrder() : null;
    }
    
    /**
     * 设置句子顺序
     */
    public void setSentenceOrder(Integer order) {
        if (streaming == null) {
            streaming = new ChatMessageStreaming();
        }
        streaming.setSentenceOrder(order);
    }
    
    /**
     * 获取句子完成状态
     */
    public Boolean getSentenceComplete() {
        return streaming != null ? streaming.getSentenceComplete() : null;
    }
    
    /**
     * 设置句子完成状态
     */
    public void setSentenceComplete(Boolean complete) {
        if (streaming == null) {
            streaming = new ChatMessageStreaming();
        }
        streaming.setSentenceComplete(complete);
    }
    
    // ========== 便捷API - 思考数据 ==========
    
    /**
     * 是否有思考内容
     */
    public boolean isThinking() {
        return thinking != null;
    }
    
    /**
     * 设置思考状态
     */
    public void setThinking(boolean value) {
        if (value && thinking == null) {
            thinking = new ChatMessageThinking();
        } else if (!value) {
            thinking = null;
        }
    }
    
    /**
     * 获取思考内容
     */
    public String getThinkingContent() {
        return thinking != null ? thinking.getThinkingContent() : null;
    }
    
    /**
     * 设置思考内容
     */
    public void setThinkingContent(String content) {
        if (thinking == null) {
            thinking = new ChatMessageThinking();
        }
        thinking.setThinkingContent(content);
    }
    
    /**
     * 获取是否显示思考
     */
    public Boolean getShowThinking() {
        return thinking != null ? thinking.getShowThinking() : null;
    }
    
    /**
     * 设置是否显示思考
     */
    public void setShowThinking(Boolean show) {
        if (thinking == null) {
            thinking = new ChatMessageThinking();
        }
        thinking.setShowThinking(show);
    }
    
    // ========== 便捷API - 输出配置 ==========
    
    /**
     * 获取TTS模式
     */
    public String getTtsMode() {
        return output != null ? output.getTtsMode() : null;
    }
    
    /**
     * 设置TTS模式
     */
    public void setTtsMode(String mode) {
        if (output == null) {
            output = new ChatMessageOutput();
        }
        output.setTtsMode(mode);
    }
    
    /**
     * 获取通道类型
     */
    public String getChannelType() {
        return output != null ? output.getChannelType() : null;
    }
    
    /**
     * 设置通道类型
     */
    public void setChannelType(String channelType) {
        if (output == null) {
            output = new ChatMessageOutput();
        }
        output.setChannelType(channelType);
    }
    
    // ========== 便捷API - 音频数据 ==========
    
    /**
     * 获取音频数据
     */
    public byte[] getAudioData() {
        return audio != null ? audio.getAudioBytes() : null;
    }
    
    /**
     * 设置音频数据
     */
    public void setAudioData(byte[] audioData) {
        if (audio == null) {
            audio = new ChatMessageAudio();
        }
        audio.setAudioBytes(audioData);
    }
    
    /**
     * 获取音频URL
     */
    public String getAudioUrl() {
        return audio != null ? audio.getAudioUrl() : null;
    }
    
    /**
     * 设置音频URL
     */
    public void setAudioUrl(String url) {
        if (audio == null) {
            audio = new ChatMessageAudio();
        }
        audio.setAudioUrl(url);
    }
    
    // ========== 便捷API - 同步状态 ==========
    
    /**
     * 获取文本就绪状态
     */
    public Boolean getTextReady() {
        return sync != null ? sync.getTextReady() : null;
    }
    
    /**
     * 设置文本就绪状态
     */
    public void setTextReady(Boolean ready) {
        if (sync == null) {
            sync = new ChatMessageSync();
        }
        sync.setTextReady(ready);
    }
    
    /**
     * 获取音频就绪状态
     */
    public Boolean getAudioReady() {
        return sync != null ? sync.getAudioReady() : null;
    }
    
    /**
     * 设置音频就绪状态
     */
    public void setAudioReady(Boolean ready) {
        if (sync == null) {
            sync = new ChatMessageSync();
        }
        sync.setAudioReady(ready);
    }
    
    /**
     * 检查文本和音频是否都就绪
     */
    public Boolean getBothReady() {
        return sync != null ? sync.isBothReady() : null;
    }
    
    /**
     * 设置双重就绪状态
     */
    public void setBothReady(Boolean ready) {
        if (Boolean.TRUE.equals(ready)) {
            if (sync == null) {
                sync = new ChatMessageSync();
            }
            sync.setTextReady(true);
            sync.setAudioReady(true);
        }
    }
    
    // ========== 组件API - 高级访问 ==========
    
    /**
     * 获取内容组件（高级API）
     */
    public ChatMessageContent getContentObject() {
        return content;
    }
    
    /**
     * 设置内容组件（高级API）
     */
    public void setContentObject(ChatMessageContent content) {
        this.content = content;
    }
    
    /**
     * 获取流式控制组件（高级API）
     */
    public ChatMessageStreaming getStreamingObject() {
        return streaming;
    }
    
    /**
     * 设置流式控制组件（高级API）
     */
    public void setStreamingObject(ChatMessageStreaming streaming) {
        this.streaming = streaming;
    }
    
    /**
     * 获取思考数据组件（高级API）
     */
    public ChatMessageThinking getThinkingObject() {
        return thinking;
    }
    
    /**
     * 设置思考数据组件（高级API）
     */
    public void setThinkingObject(ChatMessageThinking thinking) {
        this.thinking = thinking;
    }
    
    /**
     * 获取音频数据组件（高级API）
     */
    public ChatMessageAudio getAudioObject() {
        return audio;
    }
    
    /**
     * 设置音频数据组件（高级API）
     */
    public void setAudioObject(ChatMessageAudio audio) {
        this.audio = audio;
    }
    
    /**
     * 获取输出配置组件（高级API）
     */
    public ChatMessageOutput getOutputObject() {
        return output;
    }
    
    /**
     * 设置输出配置组件（高级API）
     */
    public void setOutputObject(ChatMessageOutput output) {
        this.output = output;
    }
    
    /**
     * 获取同步状态组件（高级API）
     */
    public ChatMessageSync getSyncObject() {
        return sync;
    }
    
    /**
     * 设置同步状态组件（高级API）
     */
    public void setSyncObject(ChatMessageSync sync) {
        this.sync = sync;
    }
    
    // ========== 标准 Getters & Setters ==========
    
    public String getMessageId() {
        return messageId;
    }
    
    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }
    
    public String getSessionId() {
        return sessionId;
    }
    
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
    
    public String getRole() {
        return role;
    }
    
    public void setRole(String role) {
        this.role = role;
    }
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
    
    public Map<String, Object> getMetadata() {
        return metadata;
    }
    
    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
    
    @Override
    public String toString() {
        return "ChatMessage{" +
               "type='" + type + '\'' +
               ", content='" + getContent() + '\'' +
               ", sessionId='" + sessionId + '\'' +
               ", role='" + role + '\'' +
               ", streaming=" + isStreaming() +
               ", timestamp=" + timestamp +
               '}';
    }
}

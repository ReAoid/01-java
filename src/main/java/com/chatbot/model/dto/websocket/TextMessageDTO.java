package com.chatbot.model.dto.websocket;

/**
 * 文本消息DTO
 * 用于文本内容的传输（包括字符流模式）
 */
public class TextMessageDTO extends ChatMessageDTO {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 消息角色：user, assistant, system
     */
    private String role;
    
    /**
     * 文本内容
     */
    private String content;
    
    /**
     * 是否为流式消息
     */
    private Boolean streaming;
    
    /**
     * TTS模式：null, char_stream, sentence
     */
    private String ttsMode;
    
    /**
     * 句子顺序（char_stream模式）
     */
    private Integer sentenceOrder;
    
    /**
     * 句子ID（char_stream模式）
     */
    private String sentenceId;
    
    /**
     * 是否完成（流式消息）
     */
    private Boolean done;
    
    public TextMessageDTO() {
        super("text");
    }
    
    // ========== Getters & Setters ==========
    
    public String getRole() {
        return role;
    }
    
    public void setRole(String role) {
        this.role = role;
    }
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public Boolean getStreaming() {
        return streaming;
    }
    
    public void setStreaming(Boolean streaming) {
        this.streaming = streaming;
    }
    
    public String getTtsMode() {
        return ttsMode;
    }
    
    public void setTtsMode(String ttsMode) {
        this.ttsMode = ttsMode;
    }
    
    public Integer getSentenceOrder() {
        return sentenceOrder;
    }
    
    public void setSentenceOrder(Integer sentenceOrder) {
        this.sentenceOrder = sentenceOrder;
    }
    
    public String getSentenceId() {
        return sentenceId;
    }
    
    public void setSentenceId(String sentenceId) {
        this.sentenceId = sentenceId;
    }
    
    public Boolean getDone() {
        return done;
    }
    
    public void setDone(Boolean done) {
        this.done = done;
    }
    
    @Override
    public String toString() {
        return "TextMessageDTO{" +
                "role='" + role + '\'' +
                ", content='" + (content != null ? content.substring(0, Math.min(content.length(), 50)) + "..." : "null") + '\'' +
                ", streaming=" + streaming +
                ", ttsMode='" + ttsMode + '\'' +
                ", sentenceOrder=" + sentenceOrder +
                ", sentenceId='" + sentenceId + '\'' +
                ", done=" + done +
                ", " + super.toString() +
                '}';
    }
}


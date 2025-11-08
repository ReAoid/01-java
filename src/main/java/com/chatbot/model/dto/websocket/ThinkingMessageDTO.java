package com.chatbot.model.dto.websocket;

/**
 * 思考过程消息DTO
 * 用于传输AI的思考过程（可选显示）
 */
public class ThinkingMessageDTO extends ChatMessageDTO {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 思考内容
     */
    private String content;
    
    /**
     * 思考阶段：analyzing, searching, reasoning, generating
     */
    private String stage;
    
    /**
     * 是否流式
     */
    private Boolean streaming;
    
    /**
     * 是否完成
     */
    private Boolean done;
    
    public ThinkingMessageDTO() {
        super("thinking");
    }
    
    // ========== Getters & Setters ==========
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public String getStage() {
        return stage;
    }
    
    public void setStage(String stage) {
        this.stage = stage;
    }
    
    public Boolean getStreaming() {
        return streaming;
    }
    
    public void setStreaming(Boolean streaming) {
        this.streaming = streaming;
    }
    
    public Boolean getDone() {
        return done;
    }
    
    public void setDone(Boolean done) {
        this.done = done;
    }
    
    @Override
    public String toString() {
        return "ThinkingMessageDTO{" +
                "content='" + (content != null ? content.substring(0, Math.min(content.length(), 50)) + "..." : "null") + '\'' +
                ", stage='" + stage + '\'' +
                ", streaming=" + streaming +
                ", done=" + done +
                ", " + super.toString() +
                '}';
    }
}


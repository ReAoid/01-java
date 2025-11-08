package com.chatbot.model.dto.llm;

import java.io.Serializable;
import java.util.Map;

/**
 * LLM响应DTO
 * 封装LLM服务返回的完整结果（非流式）
 */
public class LLMResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String content;          // 生成的文本内容
    private final String model;            // 使用的模型名称
    private final boolean done;            // 是否完成
    private final int totalTokens;         // 总token数
    private final int promptTokens;        // 提示词token数
    private final int completionTokens;    // 生成内容token数
    private final long durationMs;         // 生成耗时（毫秒）
    private final Map<String, Object> metadata; // 附加元数据

    private LLMResponse(Builder builder) {
        this.content = builder.content;
        this.model = builder.model;
        this.done = builder.done;
        this.totalTokens = builder.totalTokens;
        this.promptTokens = builder.promptTokens;
        this.completionTokens = builder.completionTokens;
        this.durationMs = builder.durationMs;
        this.metadata = builder.metadata;
    }

    // ========== Getters ==========

    public String getContent() {
        return content;
    }

    public String getModel() {
        return model;
    }

    public boolean isDone() {
        return done;
    }

    public int getTotalTokens() {
        return totalTokens;
    }

    public int getPromptTokens() {
        return promptTokens;
    }

    public int getCompletionTokens() {
        return completionTokens;
    }

    public long getDurationMs() {
        return durationMs;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public boolean isEmpty() {
        return content == null || content.trim().isEmpty();
    }

    @Override
    public String toString() {
        return "LLMResponse{" +
                "contentLength=" + (content != null ? content.length() : 0) +
                ", model='" + model + '\'' +
                ", done=" + done +
                ", totalTokens=" + totalTokens +
                ", promptTokens=" + promptTokens +
                ", completionTokens=" + completionTokens +
                ", durationMs=" + durationMs +
                '}';
    }

    /**
     * Builder模式构建器
     */
    public static class Builder {
        private String content = "";
        private String model;
        private boolean done = true;
        private int totalTokens = 0;
        private int promptTokens = 0;
        private int completionTokens = 0;
        private long durationMs = 0;
        private Map<String, Object> metadata = new java.util.HashMap<>();

        public Builder content(String content) {
            this.content = content;
            return this;
        }

        public Builder model(String model) {
            this.model = model;
            return this;
        }

        public Builder done(boolean done) {
            this.done = done;
            return this;
        }

        public Builder totalTokens(int totalTokens) {
            this.totalTokens = totalTokens;
            return this;
        }

        public Builder promptTokens(int promptTokens) {
            this.promptTokens = promptTokens;
            return this;
        }

        public Builder completionTokens(int completionTokens) {
            this.completionTokens = completionTokens;
            return this;
        }

        public Builder durationMs(long durationMs) {
            this.durationMs = durationMs;
            return this;
        }

        public Builder metadata(String key, Object value) {
            this.metadata.put(key, value);
            return this;
        }

        public Builder metadata(Map<String, Object> metadata) {
            if (metadata != null) {
                this.metadata.putAll(metadata);
            }
            return this;
        }

        public LLMResponse build() {
            return new LLMResponse(this);
        }
    }
}


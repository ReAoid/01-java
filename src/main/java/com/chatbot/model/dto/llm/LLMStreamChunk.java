package com.chatbot.model.dto.llm;

import java.io.Serializable;
import java.util.Map;

/**
 * LLM流式数据块DTO
 * 封装流式响应中的每一个数据块
 */
public class LLMStreamChunk implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String content;          // 当前数据块的文本内容
    private final String model;            // 模型名称
    private final boolean done;            // 是否为最后一个数据块
    private final int chunkIndex;          // 数据块序号
    private final Map<String, Object> metadata; // 附加元数据

    private LLMStreamChunk(Builder builder) {
        this.content = builder.content;
        this.model = builder.model;
        this.done = builder.done;
        this.chunkIndex = builder.chunkIndex;
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

    public int getChunkIndex() {
        return chunkIndex;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public boolean isEmpty() {
        return content == null || content.isEmpty();
    }

    @Override
    public String toString() {
        return "LLMStreamChunk{" +
                "contentLength=" + (content != null ? content.length() : 0) +
                ", model='" + model + '\'' +
                ", done=" + done +
                ", chunkIndex=" + chunkIndex +
                '}';
    }

    /**
     * Builder模式构建器
     */
    public static class Builder {
        private String content = "";
        private String model;
        private boolean done = false;
        private int chunkIndex = 0;
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

        public Builder chunkIndex(int chunkIndex) {
            this.chunkIndex = chunkIndex;
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

        public LLMStreamChunk build() {
            return new LLMStreamChunk(this);
        }
    }
}


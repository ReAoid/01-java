package com.chatbot.model.dto.llm;

import java.io.Serializable;
import java.util.Map;

/**
 * LLM模型信息DTO
 * 用于返回可用模型的详细信息
 */
public class ModelInfo implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String name;             // 模型名称，如 "yi:6b"
    private final String displayName;      // 显示名称，如 "Yi 6B"
    private final String family;           // 模型家族，如 "yi", "qwen"
    private final String size;             // 模型大小，如 "6B", "14B"
    private final String format;           // 模型格式，如 "gguf"
    private final long parameterSize;      // 参数量（字节）
    private final boolean available;       // 是否可用
    private final String description;      // 模型描述
    private final Map<String, Object> metadata; // 附加元数据

    private ModelInfo(Builder builder) {
        this.name = builder.name;
        this.displayName = builder.displayName;
        this.family = builder.family;
        this.size = builder.size;
        this.format = builder.format;
        this.parameterSize = builder.parameterSize;
        this.available = builder.available;
        this.description = builder.description;
        this.metadata = builder.metadata;
    }

    // ========== Getters ==========

    public String getName() {
        return name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getFamily() {
        return family;
    }

    public String getSize() {
        return size;
    }

    public String getFormat() {
        return format;
    }

    public long getParameterSize() {
        return parameterSize;
    }

    public boolean isAvailable() {
        return available;
    }

    public String getDescription() {
        return description;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    @Override
    public String toString() {
        return "ModelInfo{" +
                "name='" + name + '\'' +
                ", displayName='" + displayName + '\'' +
                ", family='" + family + '\'' +
                ", size='" + size + '\'' +
                ", available=" + available +
                '}';
    }

    /**
     * Builder模式构建器
     */
    public static class Builder {
        private String name;
        private String displayName;
        private String family;
        private String size;
        private String format;
        private long parameterSize = 0;
        private boolean available = true;
        private String description;
        private Map<String, Object> metadata = new java.util.HashMap<>();

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder displayName(String displayName) {
            this.displayName = displayName;
            return this;
        }

        public Builder family(String family) {
            this.family = family;
            return this;
        }

        public Builder size(String size) {
            this.size = size;
            return this;
        }

        public Builder format(String format) {
            this.format = format;
            return this;
        }

        public Builder parameterSize(long parameterSize) {
            this.parameterSize = parameterSize;
            return this;
        }

        public Builder available(boolean available) {
            this.available = available;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
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

        public ModelInfo build() {
            if (name == null || name.trim().isEmpty()) {
                throw new IllegalArgumentException("模型名称不能为空");
            }
            return new ModelInfo(this);
        }
    }
}


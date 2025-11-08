package com.chatbot.model.dto.llm;

import com.chatbot.model.dto.OllamaMessage;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * LLM请求DTO
 * 封装LLM服务所需的输入参数（统一接口层）
 */
public class LLMRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private final List<OllamaMessage> messages;
    private final String model;           // 模型名称，如 "yi:6b", "qwen:14b"
    private final Double temperature;     // 温度参数，控制输出随机性 (0.0-2.0)
    private final Integer maxTokens;      // 最大生成token数
    private final boolean stream;         // 是否使用流式响应
    private final Map<String, Object> options; // 其他模型参数（如top_p, top_k等）
    private final Map<String, Object> metadata; // 附加元数据

    private LLMRequest(Builder builder) {
        this.messages = builder.messages;
        this.model = builder.model;
        this.temperature = builder.temperature;
        this.maxTokens = builder.maxTokens;
        this.stream = builder.stream;
        this.options = builder.options;
        this.metadata = builder.metadata;
    }

    // ========== Getters ==========

    public List<OllamaMessage> getMessages() {
        return messages;
    }

    public String getModel() {
        return model;
    }

    public Double getTemperature() {
        return temperature;
    }

    public Integer getMaxTokens() {
        return maxTokens;
    }

    public boolean isStream() {
        return stream;
    }

    public Map<String, Object> getOptions() {
        return options;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    @Override
    public String toString() {
        return "LLMRequest{" +
                "messagesCount=" + (messages != null ? messages.size() : 0) +
                ", model='" + model + '\'' +
                ", temperature=" + temperature +
                ", maxTokens=" + maxTokens +
                ", stream=" + stream +
                ", hasOptions=" + (options != null && !options.isEmpty()) +
                ", hasMetadata=" + (metadata != null && !metadata.isEmpty()) +
                '}';
    }

    /**
     * Builder模式构建器
     */
    public static class Builder {
        private List<OllamaMessage> messages;
        private String model;
        private Double temperature = 0.7;
        private Integer maxTokens = 4000;
        private boolean stream = true;
        private Map<String, Object> options = new java.util.HashMap<>();
        private Map<String, Object> metadata = new java.util.HashMap<>();

        public Builder messages(List<OllamaMessage> messages) {
            this.messages = messages;
            return this;
        }

        public Builder model(String model) {
            this.model = model;
            return this;
        }

        public Builder temperature(Double temperature) {
            this.temperature = temperature;
            return this;
        }

        public Builder maxTokens(Integer maxTokens) {
            this.maxTokens = maxTokens;
            return this;
        }

        public Builder stream(boolean stream) {
            this.stream = stream;
            return this;
        }

        public Builder option(String key, Object value) {
            this.options.put(key, value);
            return this;
        }

        public Builder options(Map<String, Object> options) {
            if (options != null) {
                this.options.putAll(options);
            }
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

        public LLMRequest build() {
            if (messages == null || messages.isEmpty()) {
                throw new IllegalArgumentException("messages不能为空");
            }
            if (model == null || model.trim().isEmpty()) {
                throw new IllegalArgumentException("model不能为空");
            }
            return new LLMRequest(this);
        }
    }
}


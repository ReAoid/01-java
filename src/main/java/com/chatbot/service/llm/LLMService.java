package com.chatbot.service.llm;

import com.chatbot.model.dto.common.ApiResult;
import com.chatbot.model.dto.common.HealthCheckResult;
import com.chatbot.model.dto.llm.LLMRequest;
import com.chatbot.model.dto.llm.LLMResponse;
import com.chatbot.model.dto.llm.LLMStreamChunk;
import com.chatbot.model.dto.llm.ModelInfo;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * LLM服务抽象接口
 * 定义了大语言模型的核心功能，支持多种LLM引擎的切换
 * 
 * 设计原则：
 * - 依赖倒置：依赖抽象而非具体实现
 * - 开闭原则：对扩展开放，对修改封闭
 * - 统一接口：提供统一的LLM服务接口
 */
public interface LLMService {

    /**
     * 获取当前LLM引擎的名称
     * @return 引擎名称，如 "Ollama", "OpenAI", "Claude"
     */
    String getEngineName();

    /**
     * 执行LLM服务的健康检查
     * @return 健康检查结果
     */
    HealthCheckResult healthCheck();

    /**
     * 同步生成响应（非流式）
     * @param request LLM请求对象
     * @return 完整的LLM响应
     */
    ApiResult<LLMResponse> generate(LLMRequest request);

    /**
     * 异步生成响应（非流式）
     * @param request LLM请求对象
     * @return CompletableFuture，包含LLM响应
     */
    CompletableFuture<ApiResult<LLMResponse>> generateAsync(LLMRequest request);

    /**
     * 流式生成响应
     * @param request LLM请求对象
     * @param onChunk 接收每个数据块的回调
     * @param onError 错误处理回调
     * @param onComplete 完成回调
     * @return 可取消的Call对象（如果支持）
     */
    Object generateStream(
        LLMRequest request,
        Consumer<LLMStreamChunk> onChunk,
        Consumer<Throwable> onError,
        Runnable onComplete
    );

    /**
     * 流式生成响应（支持中断检查）
     * @param request LLM请求对象
     * @param onChunk 接收每个数据块的回调
     * @param onError 错误处理回调
     * @param onComplete 完成回调
     * @param interruptChecker 中断检查器
     * @return 可取消的Call对象（如果支持）
     */
    Object generateStreamWithInterruptCheck(
        LLMRequest request,
        Consumer<LLMStreamChunk> onChunk,
        Consumer<Throwable> onError,
        Runnable onComplete,
        java.util.function.Supplier<Boolean> interruptChecker
    );

    /**
     * 获取当前LLM服务支持的模型列表
     * @return 模型信息列表
     */
    ApiResult<List<ModelInfo>> getAvailableModels();

    /**
     * 获取指定模型的详细信息
     * @param modelName 模型名称
     * @return 模型信息
     */
    ApiResult<ModelInfo> getModelInfo(String modelName);

    /**
     * 检查LLM服务是否可用
     * @return true如果服务可用，否则false
     */
    boolean isServiceAvailable();

    /**
     * 验证请求参数是否有效
     * @param request LLM请求对象
     * @return 验证结果，如果验证通过返回success，否则返回包含错误信息的failure
     */
    ApiResult<Void> validateRequest(LLMRequest request);

    /**
     * 估算请求的token数量
     * @param request LLM请求对象
     * @return 估算的token数量
     */
    ApiResult<Integer> estimateTokens(LLMRequest request);
}


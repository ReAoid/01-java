package com.chatbot.service.asr;

import com.chatbot.model.dto.asr.ASRConnectionInfo;
import com.chatbot.model.dto.asr.ASRRequest;
import com.chatbot.model.dto.asr.ASRResponse;
import com.chatbot.model.dto.common.ApiResult;
import com.chatbot.model.dto.common.HealthCheckResult;

import java.util.concurrent.CompletableFuture;

/**
 * ASR服务抽象接口
 * 定义了语音识别的核心功能，支持多种ASR引擎的切换
 * 
 * 设计原则：
 * - 支持同步和异步识别
 * - 支持流式和非流式识别
 * - 统一的错误处理
 * - 健康检查机制
 * 
 * @version 1.0
 */
public interface ASRService {
    
    /**
     * 获取当前ASR引擎的名称
     * @return 引擎名称，如 "FunASR", "Whisper"
     */
    String getEngineName();
    
    /**
     * 执行ASR服务的健康检查
     * @return 健康检查结果
     */
    HealthCheckResult healthCheck();
    
    /**
     * 获取ASR连接信息
     * @return 连接状态和配置信息
     */
    ASRConnectionInfo getConnectionInfo();
    
    /**
     * 同步语音识别
     * @param request ASR请求，包含音频数据和配置
     * @return 识别结果
     */
    ApiResult<ASRResponse> recognize(ASRRequest request);
    
    /**
     * 异步语音识别
     * @param request ASR请求，包含音频数据和配置
     * @return CompletableFuture，包含识别结果
     */
    CompletableFuture<ApiResult<ASRResponse>> recognizeAsync(ASRRequest request);
    
    /**
     * 流式语音识别 - 发送音频块
     * @param request ASR请求，包含音频块数据
     * @return 部分识别结果（可能非最终结果）
     */
    ApiResult<ASRResponse> recognizeStreaming(ASRRequest request);
    
    /**
     * 建立ASR WebSocket连接
     * @param sessionId 会话ID
     * @return 连接结果
     */
    ApiResult<Void> connect(String sessionId);
    
    /**
     * 断开ASR WebSocket连接
     * @param sessionId 会话ID
     * @return 断开结果
     */
    ApiResult<Void> disconnect(String sessionId);
    
    /**
     * 检查连接是否活跃
     * @param sessionId 会话ID
     * @return true表示连接活跃
     */
    boolean isConnected(String sessionId);
    
    /**
     * 启动ASR会话（流式识别前）
     * @param sessionId 会话ID
     * @return 启动结果
     */
    ApiResult<Void> startSession(String sessionId);
    
    /**
     * 结束ASR会话（流式识别后）
     * @param sessionId 会话ID
     * @return 结束结果
     */
    ApiResult<Void> endSession(String sessionId);
    
    /**
     * 获取支持的语言列表
     * @return 语言代码列表
     */
    ApiResult<java.util.List<String>> getSupportedLanguages();
    
    /**
     * 获取支持的音频格式列表
     * @return 音频格式列表
     */
    ApiResult<java.util.List<String>> getSupportedFormats();
}


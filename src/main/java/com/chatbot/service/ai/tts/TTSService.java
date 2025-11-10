package com.chatbot.service.ai.tts;

import com.chatbot.model.dto.common.ApiResult;
import com.chatbot.model.dto.common.HealthCheckResult;
import com.chatbot.model.dto.tts.SpeakerInfo;
import com.chatbot.model.dto.tts.TTSRequest;
import com.chatbot.model.dto.tts.TTSResult;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * TTS服务抽象接口
 * 定义所有TTS引擎必须实现的标准方法
 * 
 * 实现类：
 * - CosyVoiceTTSServiceImpl (CosyVoice引擎)
 * - AzureTTSServiceImpl (Azure TTS引擎 - 未来扩展)
 * - GoogleTTSServiceImpl (Google TTS引擎 - 未来扩展)
 */
public interface TTSService {
    
    /**
     * 获取TTS引擎名称
     * @return 引擎名称，如 "CosyVoice"、"Azure"、"Google"
     */
    String getEngineName();
    
    /**
     * 健康检查
     * @return 健康检查结果
     */
    HealthCheckResult healthCheck();
    
    /**
     * 文本转语音（同步）
     * @param request TTS请求
     * @return 包含音频数据的结果
     */
    ApiResult<TTSResult> synthesize(TTSRequest request);
    
    /**
     * 文本转语音（异步）
     * @param request TTS请求
     * @return CompletableFuture包含的结果
     */
    CompletableFuture<ApiResult<TTSResult>> synthesizeAsync(TTSRequest request);
    
    /**
     * 注册自定义说话人
     * @param speakerName 说话人名称
     * @param referenceText 参考文本
     * @param referenceAudio 参考音频数据
     * @return 注册结果
     */
    ApiResult<SpeakerInfo> registerSpeaker(String speakerName, String referenceText, byte[] referenceAudio);
    
    /**
     * 删除自定义说话人
     * @param speakerId 说话人ID
     * @return 删除结果
     */
    ApiResult<Void> deleteSpeaker(String speakerId);
    
    /**
     * 获取所有说话人列表
     * @return 说话人列表
     */
    ApiResult<List<SpeakerInfo>> listSpeakers();
    
    /**
     * 获取说话人详情
     * @param speakerId 说话人ID
     * @return 说话人信息
     */
    ApiResult<SpeakerInfo> getSpeaker(String speakerId);
}


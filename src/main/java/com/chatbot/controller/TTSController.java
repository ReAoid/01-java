package com.chatbot.controller;

import com.chatbot.model.dto.common.ApiResult;
import com.chatbot.model.dto.common.HealthCheckResult;
import com.chatbot.model.dto.tts.SpeakerInfo;
import com.chatbot.model.dto.tts.TTSRequest;
import com.chatbot.model.dto.tts.TTSResult;
import com.chatbot.service.tts.TTSService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * TTS 控制器
 * 提供HTTP接口来调用TTS服务
 */
@RestController
@RequestMapping("/api/tts")
public class TTSController {
    
    private static final Logger logger = LoggerFactory.getLogger(TTSController.class);
    
    private final TTSService ttsService;
    
    public TTSController(@Qualifier("cosyVoiceTTSService") TTSService ttsService) {
        this.ttsService = ttsService;
    }
    
    /**
     * 健康检查接口
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        logger.info("收到健康检查请求");
        
        HealthCheckResult result = ttsService.healthCheck();
        
        // 转换为Map格式以添加前端需要的success字段
        Map<String, Object> response = new HashMap<>();
        response.put("success", result.isHealthy());
        response.put("healthy", result.isHealthy());
        response.put("serviceName", result.getServiceName());
        response.put("status", result.getStatus());
        response.put("responseTime", result.getResponseTime());
        response.put("timestamp", result.getTimestamp());
        
        if (result.getVersion() != null) {
            response.put("version", result.getVersion());
        }
        if (result.getDetails() != null && !result.getDetails().isEmpty()) {
            response.put("details", result.getDetails());
        }
        
        if (result.isHealthy()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(503).body(response); // 503 Service Unavailable
        }
    }
    
    /**
     * 注册自定义说话人接口
     */
    @PostMapping("/speaker/register")
    public ResponseEntity<?> registerSpeaker(
            @RequestParam("speakerName") String speakerName,
            @RequestParam("referenceText") String referenceText,
            @RequestParam("referenceAudio") MultipartFile referenceAudio) {
        
        logger.info("收到注册说话人请求: {}", speakerName);
        
        try {
            byte[] audioData = referenceAudio.getBytes();
            
            ApiResult<SpeakerInfo> result = 
                ttsService.registerSpeaker(speakerName, referenceText, audioData);
            
            if (result.isSuccess()) {
                return ResponseEntity.ok(result);
            } else {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", result.getMessage());
                errorResponse.put("errorCode", result.getErrorCode());
                return ResponseEntity.status(400).body(errorResponse);
            }
            
        } catch (IOException e) {
            logger.error("读取音频文件失败", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "音频文件读取失败: " + e.getMessage());
            return ResponseEntity.status(400).body(errorResponse);
        }
    }
    
    /**
     * 使用自定义说话人合成语音接口
     */
    @PostMapping("/synthesis/custom")
    public ResponseEntity<?> customSpeakerSynthesis(
            @RequestParam("text") String text,
            @RequestParam("speakerName") String speakerName,
            @RequestParam(value = "speed", required = false) Double speed,
            @RequestParam(value = "format", required = false, defaultValue = "wav") String format) {
        
        logger.info("收到自定义说话人合成请求: 说话人={}, 文本长度={}", speakerName, text.length());
        
        TTSRequest request = new TTSRequest.Builder()
                .text(text)
                .speakerId(speakerName)
                .speed(speed != null ? speed : 1.0)
                .format(format)
                .build();
        
        ApiResult<TTSResult> result = ttsService.synthesize(request);
        
        if (result.isSuccess()) {
            // 返回音频数据
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("audio/" + format));
            headers.setContentDispositionFormData("attachment", "synthesis." + format);
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(result.getData().getAudioData());
        } else {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", result.getMessage());
            errorResponse.put("errorCode", result.getErrorCode());
            return ResponseEntity.status(400).body(errorResponse);
        }
    }
    
    /**
     * 获取说话人列表接口
     */
    @GetMapping("/speakers")
    public ResponseEntity<?> getSpeakers() {
        logger.info("收到获取说话人列表请求");
        
        ApiResult<List<SpeakerInfo>> result = ttsService.listSpeakers();
        
        if (result.isSuccess()) {
            // 转换为兼容格式
            List<SpeakerInfo> speakers = result.getData();
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", result.getMessage());
            response.put("builtin_speakers", speakers.stream()
                    .filter(SpeakerInfo::isBuiltin)
                    .map(SpeakerInfo::getName)
                    .collect(Collectors.toList()));
            response.put("custom_speakers", speakers.stream()
                    .filter(SpeakerInfo::isCustom)
                    .map(SpeakerInfo::getName)
                    .collect(Collectors.toList()));
            
            return ResponseEntity.ok(response);
        } else {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", result.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
    
    /**
     * 删除自定义说话人接口（包含本地文件删除）
     */
    @DeleteMapping("/speaker/delete/{speakerName}")
    public ResponseEntity<?> deleteSpeaker(@PathVariable String speakerName) {
        logger.info("收到删除说话人请求: {}", speakerName);
        
        try {
            // 验证输入参数
            if (speakerName == null || speakerName.trim().isEmpty()) {
                return ResponseEntity.status(400).body("语音人设名称不能为空");
            }
            
            String cleanSpeakerName = speakerName.trim();
            
            // 检查是否为默认语音人设（在reference_audio目录中的不能删除）
            Path referenceAudioDir = Paths.get("src/main/resources/data/tts_data/reference_audio");
            Path referenceAudioFile = referenceAudioDir.resolve(cleanSpeakerName + ".wav");
            Path referenceTextFile = referenceAudioDir.resolve(cleanSpeakerName + ".txt");
            
            if (Files.exists(referenceAudioFile) || Files.exists(referenceTextFile)) {
                return ResponseEntity.status(400).body("默认语音人设不能删除");
            }
            
            // 调用TTS服务删除语音人设
            ApiResult<Void> result = ttsService.deleteSpeaker(cleanSpeakerName);
            
            if (result.isSuccess()) {
                // 删除本地文件（user_audio目录中的文件）
                deleteLocalSpeakerFiles(cleanSpeakerName);
                
                logger.info("语音人设删除成功: {}", cleanSpeakerName);
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", result.getMessage());
                response.put("speakerName", cleanSpeakerName);
                return ResponseEntity.ok(response);
            } else {
                logger.warn("TTS服务删除失败: {}", result.getMessage());
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", result.getMessage());
                return ResponseEntity.status(400).body(errorResponse);
            }
            
        } catch (Exception e) {
            logger.error("删除语音人设时发生错误", e);
            return ResponseEntity.status(500).body("删除失败: " + e.getMessage());
        }
    }

    /**
     * 删除本地语音人设文件
     */
    private void deleteLocalSpeakerFiles(String speakerName) {
        try {
            Path userAudioDir = Paths.get("src/main/resources/data/tts_data/user_audio");
            Path audioFilePath = userAudioDir.resolve(speakerName + ".wav");
            Path textFilePath = userAudioDir.resolve(speakerName + ".txt");
            
            boolean audioDeleted = false;
            boolean textDeleted = false;
            
            // 删除音频文件
            if (Files.exists(audioFilePath)) {
                Files.delete(audioFilePath);
                audioDeleted = true;
                logger.info("已删除音频文件: {}", audioFilePath.toAbsolutePath());
            }
            
            // 删除文本文件
            if (Files.exists(textFilePath)) {
                Files.delete(textFilePath);
                textDeleted = true;
                logger.info("已删除文本文件: {}", textFilePath.toAbsolutePath());
            }
            
            if (!audioDeleted && !textDeleted) {
                logger.warn("未找到要删除的本地文件: {}", speakerName);
            }
            
        } catch (IOException e) {
            logger.error("删除本地文件失败: {}", speakerName, e);
            // 不抛出异常，因为CosyVoice服务中的删除已经成功
        }
    }

    /**
     * 创建自定义语音人设接口（包含文件存储）
     */
    @PostMapping("/speaker/create")
    public ResponseEntity<?> createCustomSpeaker(
            @RequestParam("speakerName") String speakerName,
            @RequestParam("referenceText") String referenceText,
            @RequestParam("referenceAudio") MultipartFile referenceAudio) {
        
        logger.info("收到创建语音人设请求: {}", speakerName);
        
        try {
            // 验证输入参数
            if (speakerName == null || speakerName.trim().isEmpty()) {
                return ResponseEntity.status(400).body("语音人物名称不能为空");
            }
            
            if (referenceText == null || referenceText.trim().isEmpty()) {
                return ResponseEntity.status(400).body("参考文本不能为空");
            }
            
            if (referenceAudio == null || referenceAudio.isEmpty()) {
                return ResponseEntity.status(400).body("参考音频文件不能为空");
            }
            
            // 验证文件格式
            String originalFilename = referenceAudio.getOriginalFilename();
            if (originalFilename == null || !originalFilename.toLowerCase().endsWith(".wav")) {
                return ResponseEntity.status(400).body("音频文件必须是WAV格式");
            }
            
            // 清理文件名，确保安全
            String cleanSpeakerName = speakerName.trim().replaceAll("[^\\w\\u4e00-\\u9fa5.-]", "_");
            
            // 确保user_audio目录存在
            Path userAudioDir = Paths.get("src/main/resources/data/tts_data/user_audio");
            if (!Files.exists(userAudioDir)) {
                Files.createDirectories(userAudioDir);
                logger.info("创建user_audio目录: {}", userAudioDir.toAbsolutePath());
            }
            
            // 保存文件到user_audio目录
            Path audioFilePath = userAudioDir.resolve(cleanSpeakerName + ".wav");
            Path textFilePath = userAudioDir.resolve(cleanSpeakerName + ".txt");
            
            // 检查文件是否已存在
            if (Files.exists(audioFilePath) || Files.exists(textFilePath)) {
                return ResponseEntity.status(400).body("语音人设 '" + cleanSpeakerName + "' 已存在，请使用不同的名称");
            }
            
            // 保存音频文件
            Files.copy(referenceAudio.getInputStream(), audioFilePath, StandardCopyOption.REPLACE_EXISTING);
            logger.info("音频文件已保存: {}", audioFilePath.toAbsolutePath());
            
            // 保存文本文件
            Files.write(textFilePath, referenceText.trim().getBytes("UTF-8"));
            logger.info("文本文件已保存: {}", textFilePath.toAbsolutePath());
            
            // 调用TTS服务注册语音人设
            byte[] audioData = referenceAudio.getBytes();
            ApiResult<SpeakerInfo> result = 
                ttsService.registerSpeaker(cleanSpeakerName, referenceText.trim(), audioData);
            
            if (result.isSuccess()) {
                logger.info("语音人设创建成功: {}", cleanSpeakerName);
                SpeakerInfo speaker = result.getData();
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", result.getMessage());
                response.put("speakerName", speaker.getName()); // 前端兼容字段
                response.put("speaker", speaker); // 完整的说话人信息
                return ResponseEntity.ok(response);
            } else {
                // 如果注册失败，删除已保存的文件
                try {
                    Files.deleteIfExists(audioFilePath);
                    Files.deleteIfExists(textFilePath);
                    logger.warn("注册失败，已清理本地文件: {}", cleanSpeakerName);
                } catch (IOException cleanupError) {
                    logger.error("清理文件失败", cleanupError);
                }
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", result.getMessage());
                return ResponseEntity.status(400).body(errorResponse);
            }
            
        } catch (IOException e) {
            logger.error("处理文件时发生错误", e);
            return ResponseEntity.status(500).body("文件处理失败: " + e.getMessage());
        } catch (Exception e) {
            logger.error("创建语音人设时发生未知错误", e);
            return ResponseEntity.status(500).body("创建失败: " + e.getMessage());
        }
    }

    /**
     * 测试语音合成接口（包含文件清理）
     */
    @PostMapping("/synthesis/test")
    public ResponseEntity<?> testSpeakerSynthesis(
            @RequestParam("text") String text,
            @RequestParam("speakerName") String speakerName,
            @RequestParam(value = "speed", required = false) Double speed,
            @RequestParam(value = "format", required = false, defaultValue = "wav") String format) {
        
        logger.info("收到测试语音合成请求: 说话人={}, 文本长度={}", speakerName, text.length());
        
        try {
            // 验证输入参数
            if (text == null || text.trim().isEmpty()) {
                return ResponseEntity.status(400).body("文本内容不能为空");
            }
            
            if (speakerName == null || speakerName.trim().isEmpty()) {
                return ResponseEntity.status(400).body("语音人设不能为空");
            }
            
            // 清理output_audio目录
            clearOutputAudioDirectory();
            
            // 调用TTS服务进行语音合成
            TTSRequest ttsRequest = new TTSRequest.Builder()
                    .text(text.trim())
                    .speakerId(speakerName.trim())
                    .speed(speed != null ? speed : 1.0)
                    .format(format)
                    .build();
            ApiResult<TTSResult> result = ttsService.synthesize(ttsRequest);
            
            if (result.isSuccess()) {
                TTSResult ttsResult = result.getData();
                byte[] audioData = ttsResult.getAudioData();
                
                // 保存音频文件到output_audio目录
                String fileName = "test_" + System.currentTimeMillis() + "." + format;
                Path outputDir = Paths.get("src/main/resources/data/tts_data/output_audio");
                
                // 确保output_audio目录存在
                if (!Files.exists(outputDir)) {
                    Files.createDirectories(outputDir);
                    logger.info("创建output_audio目录: {}", outputDir.toAbsolutePath());
                }
                
                Path audioFilePath = outputDir.resolve(fileName);
                Files.write(audioFilePath, audioData);
                logger.info("测试音频已保存: {}", audioFilePath.toAbsolutePath());
                
                // 返回音频数据
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.parseMediaType("audio/" + format));
                headers.setContentDispositionFormData("attachment", fileName);
                
                return ResponseEntity.ok()
                        .headers(headers)
                        .body(audioData);
            } else {
                logger.warn("测试语音合成失败: {}", result.getMessage());
                return ResponseEntity.status(400).body(result.getMessage());
            }
            
        } catch (IOException e) {
            logger.error("处理测试语音合成时发生IO错误", e);
            return ResponseEntity.status(500).body("文件处理失败: " + e.getMessage());
        } catch (Exception e) {
            logger.error("测试语音合成时发生未知错误", e);
            return ResponseEntity.status(500).body("合成失败: " + e.getMessage());
        }
    }

    /**
     * 清理output_audio目录
     */
    private void clearOutputAudioDirectory() {
        try {
            Path outputDir = Paths.get("src/main/resources/data/tts_data/output_audio");
            
            if (Files.exists(outputDir)) {
                // 删除目录中的所有文件
                Files.walk(outputDir)
                    .filter(Files::isRegularFile)
                    .forEach(file -> {
                        try {
                            Files.delete(file);
                            logger.debug("已删除文件: {}", file.getFileName());
                        } catch (IOException e) {
                            logger.warn("删除文件失败: {}", file.getFileName(), e);
                        }
                    });
                logger.info("已清理output_audio目录");
            }
        } catch (IOException e) {
            logger.error("清理output_audio目录失败", e);
        }
    }
}

package com.chatbot.controller;

import com.chatbot.service.CosyVoiceTTSService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

/**
 * CosyVoice TTS 控制器
 * 提供HTTP接口来调用CosyVoice TTS服务
 */
@RestController
@RequestMapping("/api/cosyvoice")
public class CosyVoiceController {
    
    private static final Logger logger = LoggerFactory.getLogger(CosyVoiceController.class);
    
    private final CosyVoiceTTSService cosyVoiceService;
    
    public CosyVoiceController(CosyVoiceTTSService cosyVoiceService) {
        this.cosyVoiceService = cosyVoiceService;
    }
    
    /**
     * 健康检查接口
     */
    @GetMapping("/health")
    public ResponseEntity<?> healthCheck() {
        logger.info("收到健康检查请求");
        
        CosyVoiceTTSService.HealthCheckResult result = cosyVoiceService.healthCheck();
        
        if (result.isSuccess()) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.status(500).body(result);
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
            
            CosyVoiceTTSService.SpeakerRegistrationResult result = 
                cosyVoiceService.registerCustomSpeaker(speakerName, referenceText, audioData);
            
            if (result.isSuccess()) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.status(400).body(result);
            }
            
        } catch (IOException e) {
            logger.error("读取音频文件失败", e);
            return ResponseEntity.status(400).body("音频文件读取失败: " + e.getMessage());
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
        
        CosyVoiceTTSService.SynthesisResult result = 
            cosyVoiceService.customSpeakerSynthesis(text, speakerName, speed, format);
        
        if (result.isSuccess()) {
            // 返回音频数据
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("audio/" + format));
            headers.setContentDispositionFormData("attachment", "synthesis." + format);
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(result.getAudioData());
        } else {
            return ResponseEntity.status(400).body(result);
        }
    }
    
    /**
     * 获取说话人列表接口
     */
    @GetMapping("/speakers")
    public ResponseEntity<?> getSpeakers() {
        logger.info("收到获取说话人列表请求");
        
        CosyVoiceTTSService.SpeakerListResult result = cosyVoiceService.getSpeakers();
        
        if (result.isSuccess()) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.status(500).body(result);
        }
    }
    
    /**
     * 删除自定义说话人接口
     */
    @DeleteMapping("/speaker/{speakerName}")
    public ResponseEntity<?> deleteSpeaker(@PathVariable String speakerName) {
        logger.info("收到删除说话人请求: {}", speakerName);
        
        CosyVoiceTTSService.SpeakerDeletionResult result = 
            cosyVoiceService.deleteCustomSpeaker(speakerName);
        
        if (result.isSuccess()) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.status(400).body(result);
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
            
            // 调用CosyVoice服务注册语音人设
            byte[] audioData = referenceAudio.getBytes();
            CosyVoiceTTSService.SpeakerRegistrationResult result = 
                cosyVoiceService.registerCustomSpeaker(cleanSpeakerName, referenceText.trim(), audioData);
            
            if (result.isSuccess()) {
                logger.info("语音人设创建成功: {}", cleanSpeakerName);
                return ResponseEntity.ok(result);
            } else {
                // 如果注册失败，删除已保存的文件
                try {
                    Files.deleteIfExists(audioFilePath);
                    Files.deleteIfExists(textFilePath);
                    logger.warn("注册失败，已清理本地文件: {}", cleanSpeakerName);
                } catch (IOException cleanupError) {
                    logger.error("清理文件失败", cleanupError);
                }
                return ResponseEntity.status(400).body(result);
            }
            
        } catch (IOException e) {
            logger.error("处理文件时发生错误", e);
            return ResponseEntity.status(500).body("文件处理失败: " + e.getMessage());
        } catch (Exception e) {
            logger.error("创建语音人设时发生未知错误", e);
            return ResponseEntity.status(500).body("创建失败: " + e.getMessage());
        }
    }
}

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
}

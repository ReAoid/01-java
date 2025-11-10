package com.chatbot.controller;

import com.chatbot.model.dto.common.HealthCheckResult;
import com.chatbot.service.MultiModalService;
import com.chatbot.service.ai.asr.ASRService;
import com.chatbot.service.ai.llm.LLMService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 统一健康检查控制器
 * 提供所有服务的健康检查端点（LLM、TTS、ASR、System）
 */
@RestController
@RequestMapping("/api/health")
public class HealthCheckController {
    
    private static final Logger logger = LoggerFactory.getLogger(HealthCheckController.class);
    
    private final MultiModalService multiModalService;
    private final ASRService asrService;
    private final LLMService llmService;
    
    public HealthCheckController(MultiModalService multiModalService,
                                @Qualifier("funASRService") ASRService asrService,
                                @Qualifier("ollamaLLMService") LLMService llmService) {
        this.multiModalService = multiModalService;
        this.asrService = asrService;
        this.llmService = llmService;
    }
    
    /**
     * 系统整体健康检查
     * @return 系统健康状态
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> health() {
        logger.debug("接收到系统健康检查请求");
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("timestamp", LocalDateTime.now());
        response.put("service", "AI Chatbot System");
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 所有服务的健康检查
     * @return 所有服务的健康状态
     */
    @GetMapping("/all")
    public ResponseEntity<Map<String, Object>> checkAllServices() {
        logger.info("接收到所有服务健康检查请求");
        long startTime = System.currentTimeMillis();
        
        Map<String, Object> results = new HashMap<>();
        
        // LLM服务健康检查
        try {
            HealthCheckResult llmHealth = llmService.healthCheck();
            results.put("llm", convertToMap(llmHealth));
        } catch (Exception e) {
            logger.error("LLM健康检查异常", e);
            results.put("llm", createErrorResult("LLM", e.getMessage()));
        }
        
        // TTS服务健康检查
        try {
            HealthCheckResult ttsHealth = multiModalService.checkTTSHealth();
            results.put("tts", convertToMap(ttsHealth));
        } catch (Exception e) {
            logger.error("TTS健康检查异常", e);
            results.put("tts", createErrorResult("TTS", e.getMessage()));
        }
        
        // ASR服务健康检查
        try {
            HealthCheckResult asrHealth = asrService.healthCheck();
            results.put("asr", convertToMap(asrHealth));
        } catch (Exception e) {
            logger.error("ASR健康检查异常", e);
            results.put("asr", createErrorResult("ASR", e.getMessage()));
        }
        
        // 系统服务健康检查
        HealthCheckResult systemHealth = checkSystemHealth();
        results.put("system", convertToMap(systemHealth));
        
        // 计算总体状态
        boolean allHealthy = results.values().stream()
                .filter(v -> v instanceof Map)
                .map(v -> (Map<?, ?>) v)
                .allMatch(m -> Boolean.TRUE.equals(m.get("healthy")));
        
        long responseTime = System.currentTimeMillis() - startTime;
        
        // 构建总体响应
        Map<String, Object> response = new HashMap<>();
        response.put("status", allHealthy ? "UP" : "DEGRADED");
        response.put("healthy", allHealthy);
        response.put("timestamp", LocalDateTime.now());
        response.put("responseTime", responseTime);
        response.put("services", results);
        
        logger.info("所有服务健康检查完成，总体状态: {}, 响应时间: {}ms", 
                   allHealthy ? "健康" : "降级", responseTime);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * LLM服务专用健康检查
     * @return LLM服务健康状态
     */
    @GetMapping("/llm")
    public ResponseEntity<Map<String, Object>> checkLLM() {
        logger.debug("接收到LLM健康检查请求");
        
        try {
            HealthCheckResult result = llmService.healthCheck();
            Map<String, Object> response = convertToMap(result);
            
            if (result.isHealthy()) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(503).body(response); // 503 Service Unavailable
            }
        } catch (Exception e) {
            logger.error("LLM健康检查异常", e);
            Map<String, Object> errorResponse = createErrorResult("LLM", e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
    
    /**
     * TTS服务专用健康检查
     * @return TTS服务健康状态
     */
    @GetMapping("/tts")
    public ResponseEntity<Map<String, Object>> checkTTS() {
        logger.debug("接收到TTS健康检查请求");
        
        try {
            HealthCheckResult result = multiModalService.checkTTSHealth();
            Map<String, Object> response = convertToMap(result);
            
            if (result.isHealthy()) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(503).body(response); // 503 Service Unavailable
            }
        } catch (Exception e) {
            logger.error("TTS健康检查异常", e);
            Map<String, Object> errorResponse = createErrorResult("TTS", e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
    
    /**
     * ASR服务专用健康检查
     * @return ASR服务健康状态
     */
    @GetMapping("/asr")
    public ResponseEntity<Map<String, Object>> checkASR() {
        logger.debug("接收到ASR健康检查请求");
        
        try {
            HealthCheckResult result = asrService.healthCheck();
            Map<String, Object> response = convertToMap(result);
            
            if (result.isHealthy()) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(503).body(response); // 503 Service Unavailable
            }
        } catch (Exception e) {
            logger.error("ASR健康检查异常", e);
            Map<String, Object> errorResponse = createErrorResult("ASR", e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
    
    /**
     * 系统服务健康检查
     * @return 系统健康状态
     */
    @GetMapping("/system")
    public ResponseEntity<Map<String, Object>> checkSystem() {
        logger.debug("接收到系统健康检查请求");
        
        HealthCheckResult result = checkSystemHealth();
        Map<String, Object> response = convertToMap(result);
        
        return ResponseEntity.ok(response);
    }
    
    // ==================== 私有辅助方法 ====================
    
    /**
     * 执行系统健康检查
     */
    private HealthCheckResult checkSystemHealth() {
        long startTime = System.currentTimeMillis();
        
        try {
            // 检查系统资源
            Runtime runtime = Runtime.getRuntime();
            long maxMemory = runtime.maxMemory();
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            long usedMemory = totalMemory - freeMemory;
            
            double memoryUsagePercent = (double) usedMemory / maxMemory * 100;
            boolean healthy = memoryUsagePercent < 90.0; // 内存使用率低于90%为健康
            
            long responseTime = System.currentTimeMillis() - startTime;
            
            return new HealthCheckResult.Builder()
                    .serviceName("System")
                    .healthy(healthy)
                    .responseTime(responseTime)
                    .version("1.0.0")
                    .detail("maxMemory", formatBytes(maxMemory))
                    .detail("totalMemory", formatBytes(totalMemory))
                    .detail("usedMemory", formatBytes(usedMemory))
                    .detail("freeMemory", formatBytes(freeMemory))
                    .detail("memoryUsagePercent", String.format("%.2f%%", memoryUsagePercent))
                    .detail("availableProcessors", runtime.availableProcessors())
                    .build();
                    
        } catch (Exception e) {
            logger.error("系统健康检查异常", e);
            long responseTime = System.currentTimeMillis() - startTime;
            
            return new HealthCheckResult.Builder()
                    .serviceName("System")
                    .healthy(false)
                    .status("ERROR")
                    .responseTime(responseTime)
                    .detail("error", e.getMessage())
                    .build();
        }
    }
    
    /**
     * 将HealthCheckResult转换为Map
     */
    private Map<String, Object> convertToMap(HealthCheckResult result) {
        Map<String, Object> map = new HashMap<>();
        map.put("serviceName", result.getServiceName());
        map.put("healthy", result.isHealthy());
        map.put("status", result.getStatus());
        map.put("responseTime", result.getResponseTime());
        map.put("timestamp", result.getTimestamp());
        
        if (result.getVersion() != null) {
            map.put("version", result.getVersion());
        }
        
        if (result.getDetails() != null && !result.getDetails().isEmpty()) {
            map.put("details", result.getDetails());
        }
        
        return map;
    }
    
    /**
     * 创建错误结果
     */
    private Map<String, Object> createErrorResult(String serviceName, String error) {
        Map<String, Object> map = new HashMap<>();
        map.put("serviceName", serviceName);
        map.put("healthy", false);
        map.put("status", "ERROR");
        map.put("timestamp", System.currentTimeMillis());
        map.put("error", error);
        return map;
    }
    
    /**
     * 格式化字节数
     */
    private String formatBytes(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.2f KB", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
        } else {
            return String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0));
        }
    }
}


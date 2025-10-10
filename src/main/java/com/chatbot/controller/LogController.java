package com.chatbot.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 日志管理控制器
 * 提供系统日志查询和监控接口
 */
@RestController
@RequestMapping("/api")
public class LogController {
    
    private static final Logger logger = LoggerFactory.getLogger(LogController.class);
    
    // 日志文件路径
    private static final String LOG_FILE_PATH = "src/main/resources/logs/app.log";
    
    // 日志级别正则表达式 - 更宽松的匹配
    private static final Pattern LOG_PATTERN = Pattern.compile(
        "(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{3}).*?(ERROR|WARN|INFO|DEBUG|TRACE).*?-\\s+(.+)"
    );
    
    /**
     * 获取系统日志
     */
    @GetMapping("/logs")
    public Map<String, Object> getLogs(@RequestParam(name = "limit", defaultValue = "100") int limit,
                                      @RequestParam(name = "level", defaultValue = "INFO") String level,
                                      @RequestParam(name = "timeRange", defaultValue = "30m") String timeRange,
                                      @RequestParam(name = "startTime", required = false) String startTime,
                                      @RequestParam(name = "endTime", required = false) String endTime) {
        
        // 移除频繁的API调用日志，避免日志污染
        logger.debug("获取日志请求: limit={}, level={}", limit, level);
        
        Map<String, Object> response = new HashMap<>();
        List<Map<String, Object>> logs = new ArrayList<>();
        
        try {
            // 尝试读取日志文件
            File logFile = new File(LOG_FILE_PATH);
            if (!logFile.exists()) {
                // 如果日志文件不存在，返回空日志
                response.put("success", true);
                response.put("message", "日志文件不存在: " + LOG_FILE_PATH);
                response.put("logs", logs);
                response.put("total", 0);
            } else {
                logs = readLogFile(logFile, limit, level, timeRange, startTime, endTime);
                response.put("success", true);
                response.put("message", "日志获取成功，来源: " + logFile.getPath() + "，级别: " + level + "，时间范围: " + timeRange);
                response.put("logs", logs);
                response.put("total", logs.size());
            }
            
        } catch (Exception e) {
            logger.error("获取日志失败", e);
            response.put("success", false);
            response.put("message", "获取日志失败: " + e.getMessage());
            response.put("logs", new ArrayList<>());
            response.put("total", 0);
        }
        
        return response;
    }
    
    /**
     * 获取日志统计信息
     */
    @GetMapping("/logs/stats")
    public Map<String, Object> getLogStats() {
        logger.debug("接收到获取日志统计请求");
        long startTime = System.currentTimeMillis();
        
        Map<String, Object> stats = new HashMap<>();
        
        try {
            File logFile = new File(LOG_FILE_PATH);
            if (logFile.exists()) {
                stats = calculateLogStats(logFile);
            } else {
                // 返回空统计
                stats.put("total", 0);
                stats.put("error", 0);
                stats.put("warn", 0);
                stats.put("info", 0);
                stats.put("debug", 0);
            }
            
            stats.put("success", true);
            stats.put("lastUpdate", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            
        } catch (Exception e) {
            logger.error("获取日志统计失败", e);
            stats.put("success", false);
            stats.put("message", "获取日志统计失败: " + e.getMessage());
            stats.put("total", 0);
            stats.put("error", 0);
            stats.put("warn", 0);
            stats.put("info", 0);
            stats.put("debug", 0);
        }
        
        long responseTime = System.currentTimeMillis() - startTime;
        logger.debug("日志统计响应时间: {}ms", responseTime);
        
        return stats;
    }
    
    /**
     * 读取日志文件
     */
    private List<Map<String, Object>> readLogFile(File logFile, int limit, String levelFilter, 
                                                 String timeRange, String startTime, String endTime) throws IOException {
        List<Map<String, Object>> logs = new ArrayList<>();
        List<String> lines = new ArrayList<>();
        
        // 读取文件的最后N行
        try (BufferedReader reader = new BufferedReader(new FileReader(logFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
        }
        
        // 仅在DEBUG级别记录详细读取信息
        logger.debug("读取到 {} 行日志，文件: {}", lines.size(), logFile.getName());
        
        // 计算时间筛选范围
        LocalDateTime filterStartTime = calculateFilterStartTime(timeRange, startTime);
        LocalDateTime filterEndTime = calculateFilterEndTime(endTime);
        
        // 从后往前处理，获取最新的日志
        Collections.reverse(lines);
        
        int count = 0;
        for (String line : lines) {
            if (count >= limit) break;
            
            // 尝试匹配日志格式
            Matcher matcher = LOG_PATTERN.matcher(line);
            
            if (matcher.find()) {
                String timestamp = matcher.group(1);
                String level = matcher.group(2);
                String message = matcher.group(3);
                
                // 解析日志时间
                LocalDateTime logTime = parseLogTimestamp(timestamp);
                
                // 过滤日志级别和时间
                if (shouldIncludeLevel(level, levelFilter) && 
                    isWithinTimeRange(logTime, filterStartTime, filterEndTime)) {
                    Map<String, Object> logEntry = new HashMap<>();
                    logEntry.put("timestamp", timestamp);
                    logEntry.put("level", level);
                    logEntry.put("message", message);
                    logs.add(logEntry);
                    count++;
                }
            }
        }
        
        // 重新按时间顺序排列
        Collections.reverse(logs);
        return logs;
    }
    
    /**
     * 计算日志统计信息
     */
    private Map<String, Object> calculateLogStats(File logFile) throws IOException {
        Map<String, Object> stats = new HashMap<>();
        Map<String, Integer> levelCounts = new HashMap<>();
        
        levelCounts.put("ERROR", 0);
        levelCounts.put("WARN", 0);
        levelCounts.put("INFO", 0);
        levelCounts.put("DEBUG", 0);
        levelCounts.put("TRACE", 0);
        
        int total = 0;
        
        try (BufferedReader reader = new BufferedReader(new FileReader(logFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                Matcher matcher = LOG_PATTERN.matcher(line);
                if (matcher.find()) {
                    String level = matcher.group(2);
                    levelCounts.put(level, levelCounts.getOrDefault(level, 0) + 1);
                    total++;
                }
            }
        }
        
        stats.put("total", total);
        stats.put("error", levelCounts.get("ERROR"));
        stats.put("warn", levelCounts.get("WARN"));
        stats.put("info", levelCounts.get("INFO"));
        stats.put("debug", levelCounts.get("DEBUG"));
        stats.put("trace", levelCounts.get("TRACE"));
        
        return stats;
    }
    
    /**
     * 判断是否应该包含某个日志级别
     */
    private boolean shouldIncludeLevel(String logLevel, String filterLevel) {
        // 如果过滤级别是ALL，显示所有级别
        if ("ALL".equalsIgnoreCase(filterLevel)) {
            return true;
        }
        
        // 如果过滤级别与日志级别完全匹配，或者过滤级别是更低的级别，则包含
        List<String> levels = Arrays.asList("ERROR", "WARN", "INFO", "DEBUG", "TRACE");
        int logLevelIndex = levels.indexOf(logLevel.toUpperCase());
        int filterLevelIndex = levels.indexOf(filterLevel.toUpperCase());
        
        // 如果找不到级别，默认包含
        if (logLevelIndex == -1 || filterLevelIndex == -1) {
            return true;
        }
        
        return logLevelIndex <= filterLevelIndex;
    }
    
    /**
     * 计算筛选开始时间
     */
    private LocalDateTime calculateFilterStartTime(String timeRange, String startTime) {
        if (startTime != null && !startTime.isEmpty()) {
            // 如果提供了自定义开始时间，解析它
            return parseCustomTime(startTime);
        }
        
        LocalDateTime now = LocalDateTime.now();
        
        // 根据时间范围计算开始时间
        switch (timeRange.toLowerCase()) {
            case "30m":
                return now.minusMinutes(30);
            case "1h":
                return now.minusHours(1);
            case "3h":
                return now.minusHours(3);
            case "12h":
                return now.minusHours(12);
            case "1d":
                return now.minusDays(1);
            case "all":
                return LocalDateTime.of(2000, 1, 1, 0, 0); // 很早的时间
            default:
                return now.minusMinutes(30); // 默认30分钟
        }
    }
    
    /**
     * 计算筛选结束时间
     */
    private LocalDateTime calculateFilterEndTime(String endTime) {
        if (endTime != null && !endTime.isEmpty()) {
            return parseCustomTime(endTime);
        }
        return LocalDateTime.now();
    }
    
    /**
     * 解析日志时间戳
     */
    private LocalDateTime parseLogTimestamp(String timestamp) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
            return LocalDateTime.parse(timestamp, formatter);
        } catch (Exception e) {
            logger.warn("解析日志时间戳失败: {}", timestamp);
            return LocalDateTime.now();
        }
    }
    
    /**
     * 解析自定义时间
     */
    private LocalDateTime parseCustomTime(String timeStr) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            return LocalDateTime.parse(timeStr, formatter);
        } catch (Exception e) {
            logger.warn("解析自定义时间失败: {}", timeStr);
            return LocalDateTime.now();
        }
    }
    
    /**
     * 检查日志时间是否在筛选范围内
     */
    private boolean isWithinTimeRange(LocalDateTime logTime, LocalDateTime startTime, LocalDateTime endTime) {
        if (logTime == null) {
            return true; // 如果无法解析时间，默认包含
        }
        return !logTime.isBefore(startTime) && !logTime.isAfter(endTime);
    }
    
}

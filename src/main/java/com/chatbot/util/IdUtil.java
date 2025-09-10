package com.chatbot.util;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/**
 * ID生成工具类
 * 提供各种类型的唯一ID生成方法，使用Java 21特性
 */
public class IdUtil {
    
    private static final AtomicLong SEQUENCE = new AtomicLong(0);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    
    // 字符集定义
    private static final String NUMBERS = "0123456789";
    private static final String LETTERS_LOWER = "abcdefghijklmnopqrstuvwxyz";
    private static final String LETTERS_UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String LETTERS = LETTERS_LOWER + LETTERS_UPPER;
    private static final String ALPHANUMERIC = NUMBERS + LETTERS;
    private static final String BASE62 = NUMBERS + LETTERS;
    
    /**
     * 生成标准UUID
     */
    public static String uuid() {
        return UUID.randomUUID().toString();
    }
    
    /**
     * 生成简化UUID（去掉横线）
     */
    public static String simpleUuid() {
        return UUID.randomUUID().toString().replace("-", "");
    }
    
    /**
     * 生成短UUID（Base62编码）
     */
    public static String shortUuid() {
        UUID uuid = UUID.randomUUID();
        return toBase62(uuid.getMostSignificantBits()) + toBase62(uuid.getLeastSignificantBits());
    }
    
    /**
     * 生成雪花ID（简化版）
     */
    public static long snowflakeId() {
        long timestamp = System.currentTimeMillis();
        long sequence = SEQUENCE.incrementAndGet() % 4096; // 12位序列号
        
        // 简化的雪花算法：41位时间戳 + 10位机器ID + 12位序列号 + 1位符号位
        return (timestamp << 22) | (1L << 12) | sequence;
    }
    
    /**
     * 生成时间戳ID
     */
    public static String timestampId() {
        return System.currentTimeMillis() + String.valueOf(SEQUENCE.incrementAndGet() % 1000);
    }
    
    /**
     * 生成时间戳ID（包含纳秒）
     */
    public static String nanoTimestampId() {
        return System.currentTimeMillis() + "_" + (System.nanoTime() % 1000000);
    }
    
    /**
     * 生成有序ID（基于时间）
     */
    public static String orderedId() {
        LocalDateTime now = LocalDateTime.now();
        String timeStr = now.format(TimeUtil.COMPACT_DATE_TIME_FORMATTER);
        return timeStr + String.format("%04d", SEQUENCE.incrementAndGet() % 10000);
    }
    
    /**
     * 生成随机数字ID
     */
    public static String randomNumericId(int length) {
        if (length <= 0) {
            return "";
        }
        
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(NUMBERS.charAt(SECURE_RANDOM.nextInt(NUMBERS.length())));
        }
        return sb.toString();
    }
    
    /**
     * 生成随机字母ID
     */
    public static String randomAlphaId(int length) {
        if (length <= 0) {
            return "";
        }
        
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(LETTERS.charAt(SECURE_RANDOM.nextInt(LETTERS.length())));
        }
        return sb.toString();
    }
    
    /**
     * 生成随机字母数字ID
     */
    public static String randomAlphanumericId(int length) {
        if (length <= 0) {
            return "";
        }
        
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(ALPHANUMERIC.charAt(SECURE_RANDOM.nextInt(ALPHANUMERIC.length())));
        }
        return sb.toString();
    }
    
    /**
     * 生成随机ID（自定义字符集）
     */
    public static String randomId(int length, String charset) {
        if (length <= 0 || StringUtil.isEmpty(charset)) {
            return "";
        }
        
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(charset.charAt(SECURE_RANDOM.nextInt(charset.length())));
        }
        return sb.toString();
    }
    
    /**
     * 生成带前缀的ID
     */
    public static String prefixedId(String prefix) {
        if (StringUtil.isEmpty(prefix)) {
            return uuid();
        }
        return prefix + "_" + simpleUuid();
    }
    
    /**
     * 生成带前缀的短ID
     */
    public static String prefixedShortId(String prefix, int length) {
        if (StringUtil.isEmpty(prefix)) {
            return randomAlphanumericId(length);
        }
        return prefix + "_" + randomAlphanumericId(length);
    }
    
    /**
     * 生成会话ID
     */
    public static String sessionId() {
        return "session_" + SEQUENCE.incrementAndGet() + "_" + System.currentTimeMillis();
    }
    
    /**
     * 生成消息ID
     */
    public static String messageId() {
        return "msg_" + System.currentTimeMillis() + "_" + System.nanoTime();
    }
    
    /**
     * 生成用户ID
     */
    public static String userId() {
        return "user_" + shortUuid();
    }
    
    /**
     * 生成订单ID
     */
    public static String orderId() {
        LocalDateTime now = LocalDateTime.now();
        String dateStr = now.format(TimeUtil.DEFAULT_DATE_FORMATTER).replace("-", "");
        return dateStr + randomNumericId(8);
    }
    
    /**
     * 生成交易ID
     */
    public static String transactionId() {
        return "tx_" + System.currentTimeMillis() + randomNumericId(6);
    }
    
    /**
     * 生成批次ID
     */
    public static String batchId() {
        return "batch_" + orderedId();
    }
    
    /**
     * 生成追踪ID
     */
    public static String traceId() {
        return "trace_" + simpleUuid().substring(0, 16);
    }
    
    /**
     * 生成请求ID
     */
    public static String requestId() {
        return "req_" + System.currentTimeMillis() + randomAlphanumericId(6);
    }
    
    /**
     * 生成文件ID
     */
    public static String fileId() {
        return "file_" + shortUuid();
    }
    
    /**
     * 生成临时ID
     */
    public static String tempId() {
        return "temp_" + System.currentTimeMillis() + randomNumericId(4);
    }
    
    /**
     * 验证ID格式（UUID）
     */
    public static boolean isValidUuid(String id) {
        if (StringUtil.isEmpty(id)) {
            return false;
        }
        
        try {
            UUID.fromString(id);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
    
    /**
     * 验证ID格式（数字）
     */
    public static boolean isValidNumericId(String id) {
        return StringUtil.isNotEmpty(id) && StringUtil.isNumeric(id);
    }
    
    /**
     * 验证ID格式（字母数字）
     */
    public static boolean isValidAlphanumericId(String id) {
        return StringUtil.isNotEmpty(id) && StringUtil.isAlphanumeric(id);
    }
    
    /**
     * 从ID中提取时间戳（如果包含）
     */
    public static Long extractTimestamp(String id) {
        if (StringUtil.isEmpty(id)) {
            return null;
        }
        
        // 尝试从不同格式的ID中提取时间戳
        String[] parts = id.split("_");
        for (String part : parts) {
            if (StringUtil.isNumeric(part) && part.length() >= 10) {
                try {
                    long timestamp = Long.parseLong(part);
                    // 验证时间戳是否合理（2000年到2100年之间）
                    if (timestamp >= 946684800000L && timestamp <= 4102444800000L) {
                        return timestamp;
                    }
                } catch (NumberFormatException e) {
                    // 继续尝试下一个部分
                }
            }
        }
        
        return null;
    }
    
    /**
     * 检查ID是否过期
     */
    public static boolean isExpired(String id, long maxAgeMillis) {
        Long timestamp = extractTimestamp(id);
        if (timestamp == null) {
            return false;
        }
        
        return System.currentTimeMillis() - timestamp > maxAgeMillis;
    }
    
    /**
     * Base62编码
     */
    private static String toBase62(long value) {
        if (value == 0) {
            return "0";
        }
        
        StringBuilder sb = new StringBuilder();
        long absValue = Math.abs(value);
        
        while (absValue > 0) {
            sb.append(BASE62.charAt((int) (absValue % 62)));
            absValue /= 62;
        }
        
        return sb.reverse().toString();
    }
    
    /**
     * Base62解码
     */
    private static long fromBase62(String base62) {
        if (StringUtil.isEmpty(base62)) {
            return 0;
        }
        
        long result = 0;
        long power = 1;
        
        for (int i = base62.length() - 1; i >= 0; i--) {
            char c = base62.charAt(i);
            int index = BASE62.indexOf(c);
            if (index == -1) {
                throw new IllegalArgumentException("Invalid Base62 character: " + c);
            }
            result += index * power;
            power *= 62;
        }
        
        return result;
    }
    
    /**
     * 生成校验码（简单的校验算法）
     */
    public static String generateChecksum(String data) {
        if (StringUtil.isEmpty(data)) {
            return "00";
        }
        
        int sum = 0;
        for (char c : data.toCharArray()) {
            sum += c;
        }
        
        return String.format("%02d", sum % 100);
    }
    
    /**
     * 验证校验码
     */
    public static boolean validateChecksum(String data, String checksum) {
        return generateChecksum(data).equals(checksum);
    }
    
    /**
     * 生成带校验码的ID
     */
    public static String idWithChecksum(String id) {
        if (StringUtil.isEmpty(id)) {
            return id;
        }
        
        String checksum = generateChecksum(id);
        return id + checksum;
    }
    
    /**
     * 验证带校验码的ID
     */
    public static boolean validateIdWithChecksum(String idWithChecksum) {
        if (StringUtil.isEmpty(idWithChecksum) || idWithChecksum.length() < 3) {
            return false;
        }
        
        String id = idWithChecksum.substring(0, idWithChecksum.length() - 2);
        String checksum = idWithChecksum.substring(idWithChecksum.length() - 2);
        
        return validateChecksum(id, checksum);
    }
}

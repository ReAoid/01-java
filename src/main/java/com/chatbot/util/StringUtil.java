package com.chatbot.util;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 字符串工具类
 * 提供常用的字符串操作方法，使用Java 21特性
 */
public class StringUtil {
    
    // 常用正则表达式模式
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );
    
    private static final Pattern PHONE_PATTERN = Pattern.compile(
        "^1[3-9]\\d{9}$"
    );
    
    private static final Pattern URL_PATTERN = Pattern.compile(
        "^(https?|ftp)://[^\\s/$.?#].[^\\s]*$"
    );
    
    /**
     * 检查字符串是否为空或null
     */
    public static boolean isEmpty(String str) {
        return str == null || str.isEmpty();
    }
    
    /**
     * 检查字符串是否不为空且不为null
     */
    public static boolean isNotEmpty(String str) {
        return !isEmpty(str);
    }
    
    /**
     * 检查字符串是否为空白（null、空字符串或只包含空白字符）
     */
    public static boolean isBlank(String str) {
        return str == null || str.trim().isEmpty();
    }
    
    /**
     * 检查字符串是否不为空白
     */
    public static boolean isNotBlank(String str) {
        return !isBlank(str);
    }
    
    /**
     * 安全的trim操作，处理null值
     */
    public static String safeTrim(String str) {
        return str == null ? null : str.trim();
    }
    
    /**
     * 如果字符串为null则返回默认值
     */
    public static String defaultIfNull(String str, String defaultValue) {
        return str == null ? defaultValue : str;
    }
    
    /**
     * 如果字符串为空则返回默认值
     */
    public static String defaultIfEmpty(String str, String defaultValue) {
        return isEmpty(str) ? defaultValue : str;
    }
    
    /**
     * 如果字符串为空白则返回默认值
     */
    public static String defaultIfBlank(String str, String defaultValue) {
        return isBlank(str) ? defaultValue : str;
    }
    
    /**
     * 截断字符串到指定长度
     */
    public static String truncate(String str, int maxLength) {
        if (isEmpty(str) || maxLength <= 0) {
            return str;
        }
        
        return str.length() <= maxLength ? str : str.substring(0, maxLength);
    }
    
    /**
     * 截断字符串并添加省略号
     */
    public static String truncateWithEllipsis(String str, int maxLength) {
        if (isEmpty(str) || maxLength <= 3) {
            return str;
        }
        
        return str.length() <= maxLength ? str : str.substring(0, maxLength - 3) + "...";
    }
    
    /**
     * 重复字符串指定次数
     */
    public static String repeat(String str, int count) {
        if (isEmpty(str) || count <= 0) {
            return "";
        }
        
        return str.repeat(count);
    }
    
    /**
     * 反转字符串
     */
    public static String reverse(String str) {
        if (isEmpty(str)) {
            return str;
        }
        
        return new StringBuilder(str).reverse().toString();
    }
    
    /**
     * 首字母大写
     */
    public static String capitalize(String str) {
        if (isEmpty(str)) {
            return str;
        }
        
        return Character.toUpperCase(str.charAt(0)) + str.substring(1);
    }
    
    /**
     * 首字母小写
     */
    public static String uncapitalize(String str) {
        if (isEmpty(str)) {
            return str;
        }
        
        return Character.toLowerCase(str.charAt(0)) + str.substring(1);
    }
    
    /**
     * 转换为驼峰命名
     */
    public static String toCamelCase(String str, String delimiter) {
        if (isEmpty(str)) {
            return str;
        }
        
        String[] words = str.split(delimiter);
        if (words.length == 0) {
            return str;
        }
        
        StringBuilder result = new StringBuilder(words[0].toLowerCase());
        for (int i = 1; i < words.length; i++) {
            result.append(capitalize(words[i].toLowerCase()));
        }
        
        return result.toString();
    }
    
    /**
     * 转换为蛇形命名
     */
    public static String toSnakeCase(String str) {
        if (isEmpty(str)) {
            return str;
        }
        
        return str.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
    }
    
    /**
     * 转换为短横线命名
     */
    public static String toKebabCase(String str) {
        if (isEmpty(str)) {
            return str;
        }
        
        return str.replaceAll("([a-z])([A-Z])", "$1-$2").toLowerCase();
    }
    
    /**
     * 移除字符串中的所有空白字符
     */
    public static String removeWhitespace(String str) {
        if (isEmpty(str)) {
            return str;
        }
        
        return str.replaceAll("\\s+", "");
    }
    
    /**
     * 标准化空白字符（将多个空白字符替换为单个空格）
     */
    public static String normalizeWhitespace(String str) {
        if (isEmpty(str)) {
            return str;
        }
        
        return str.replaceAll("\\s+", " ").trim();
    }
    
    /**
     * 检查字符串是否只包含数字
     */
    public static boolean isNumeric(String str) {
        if (isEmpty(str)) {
            return false;
        }
        
        return str.matches("\\d+");
    }
    
    /**
     * 检查字符串是否只包含字母
     */
    public static boolean isAlpha(String str) {
        if (isEmpty(str)) {
            return false;
        }
        
        return str.matches("[a-zA-Z]+");
    }
    
    /**
     * 检查字符串是否只包含字母和数字
     */
    public static boolean isAlphanumeric(String str) {
        if (isEmpty(str)) {
            return false;
        }
        
        return str.matches("[a-zA-Z0-9]+");
    }
    
    /**
     * 验证邮箱格式
     */
    public static boolean isValidEmail(String email) {
        if (isEmpty(email)) {
            return false;
        }
        
        return EMAIL_PATTERN.matcher(email).matches();
    }
    
    /**
     * 验证手机号格式（中国大陆）
     */
    public static boolean isValidPhone(String phone) {
        if (isEmpty(phone)) {
            return false;
        }
        
        return PHONE_PATTERN.matcher(phone).matches();
    }
    
    /**
     * 验证URL格式
     */
    public static boolean isValidUrl(String url) {
        if (isEmpty(url)) {
            return false;
        }
        
        return URL_PATTERN.matcher(url).matches();
    }
    
    /**
     * 掩码字符串（用于隐藏敏感信息）
     */
    public static String mask(String str, int visibleStart, int visibleEnd, char maskChar) {
        if (isEmpty(str)) {
            return str;
        }
        
        int length = str.length();
        if (visibleStart + visibleEnd >= length) {
            return str;
        }
        
        StringBuilder masked = new StringBuilder();
        masked.append(str.substring(0, visibleStart));
        masked.append(String.valueOf(maskChar).repeat(length - visibleStart - visibleEnd));
        masked.append(str.substring(length - visibleEnd));
        
        return masked.toString();
    }
    
    /**
     * 掩码邮箱地址
     */
    public static String maskEmail(String email) {
        if (!isValidEmail(email)) {
            return email;
        }
        
        int atIndex = email.indexOf('@');
        String username = email.substring(0, atIndex);
        String domain = email.substring(atIndex);
        
        if (username.length() <= 2) {
            return "*" + domain;
        }
        
        return username.charAt(0) + "***" + username.charAt(username.length() - 1) + domain;
    }
    
    /**
     * 掩码手机号
     */
    public static String maskPhone(String phone) {
        if (!isValidPhone(phone)) {
            return phone;
        }
        
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }
    
    /**
     * 连接字符串数组
     */
    public static String join(String[] array, String separator) {
        if (array == null || array.length == 0) {
            return "";
        }
        
        return String.join(separator, array);
    }
    
    /**
     * 连接字符串集合
     */
    public static String join(Collection<String> collection, String separator) {
        if (collection == null || collection.isEmpty()) {
            return "";
        }
        
        return String.join(separator, collection);
    }
    
    /**
     * 分割字符串并过滤空值
     */
    public static List<String> splitAndTrim(String str, String delimiter) {
        if (isEmpty(str)) {
            return List.of();
        }
        
        return Arrays.stream(str.split(delimiter))
                .map(String::trim)
                .filter(StringUtil::isNotEmpty)
                .collect(Collectors.toList());
    }
    
    /**
     * 计算字符串的字节长度（UTF-8编码）
     */
    public static int getByteLength(String str) {
        if (isEmpty(str)) {
            return 0;
        }
        
        return str.getBytes().length;
    }
    
    /**
     * 生成随机字符串
     */
    public static String randomString(int length) {
        if (length <= 0) {
            return "";
        }
        
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder result = new StringBuilder();
        
        for (int i = 0; i < length; i++) {
            result.append(chars.charAt((int) (Math.random() * chars.length())));
        }
        
        return result.toString();
    }
    
    /**
     * 计算两个字符串的相似度（简单的编辑距离算法）
     */
    public static double similarity(String str1, String str2) {
        if (str1 == null && str2 == null) {
            return 1.0;
        }
        
        if (str1 == null || str2 == null) {
            return 0.0;
        }
        
        if (str1.equals(str2)) {
            return 1.0;
        }
        
        int maxLength = Math.max(str1.length(), str2.length());
        if (maxLength == 0) {
            return 1.0;
        }
        
        int distance = levenshteinDistance(str1, str2);
        return 1.0 - (double) distance / maxLength;
    }
    
    /**
     * 计算编辑距离
     */
    private static int levenshteinDistance(String str1, String str2) {
        int[][] dp = new int[str1.length() + 1][str2.length() + 1];
        
        for (int i = 0; i <= str1.length(); i++) {
            dp[i][0] = i;
        }
        
        for (int j = 0; j <= str2.length(); j++) {
            dp[0][j] = j;
        }
        
        for (int i = 1; i <= str1.length(); i++) {
            for (int j = 1; j <= str2.length(); j++) {
                if (str1.charAt(i - 1) == str2.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1];
                } else {
                    dp[i][j] = Math.min(Math.min(dp[i - 1][j], dp[i][j - 1]), dp[i - 1][j - 1]) + 1;
                }
            }
        }
        
        return dp[str1.length()][str2.length()];
    }
}

package com.chatbot.util;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;

/**
 * 时间工具类
 * 基于Java 8+的时间API，使用Java 21特性
 */
public class TimeUtil {
    
    // 常用的时间格式
    public static final String DEFAULT_DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
    public static final String DEFAULT_DATE_FORMAT = "yyyy-MM-dd";
    public static final String DEFAULT_TIME_FORMAT = "HH:mm:ss";
    public static final String ISO_DATE_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss";
    public static final String COMPACT_DATE_TIME_FORMAT = "yyyyMMddHHmmss";
    public static final String CHINESE_DATE_TIME_FORMAT = "yyyy年MM月dd日 HH:mm:ss";
    
    // 常用的DateTimeFormatter
    public static final DateTimeFormatter DEFAULT_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern(DEFAULT_DATE_TIME_FORMAT);
    public static final DateTimeFormatter DEFAULT_DATE_FORMATTER = DateTimeFormatter.ofPattern(DEFAULT_DATE_FORMAT);
    public static final DateTimeFormatter DEFAULT_TIME_FORMATTER = DateTimeFormatter.ofPattern(DEFAULT_TIME_FORMAT);
    public static final DateTimeFormatter ISO_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern(ISO_DATE_TIME_FORMAT);
    public static final DateTimeFormatter COMPACT_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern(COMPACT_DATE_TIME_FORMAT);
    public static final DateTimeFormatter CHINESE_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern(CHINESE_DATE_TIME_FORMAT);
    
    /**
     * 获取当前时间
     */
    public static LocalDateTime now() {
        return LocalDateTime.now();
    }
    
    /**
     * 获取当前日期
     */
    public static LocalDate today() {
        return LocalDate.now();
    }
    
    /**
     * 获取当前时间（指定时区）
     */
    public static LocalDateTime now(ZoneId zoneId) {
        return LocalDateTime.now(zoneId);
    }
    
    /**
     * 获取当前时间戳（毫秒）
     */
    public static long currentTimeMillis() {
        return System.currentTimeMillis();
    }
    
    /**
     * 获取当前时间戳（秒）
     */
    public static long currentTimeSeconds() {
        return System.currentTimeMillis() / 1000;
    }
    
    /**
     * 格式化时间为默认格式
     */
    public static String format(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.format(DEFAULT_DATE_TIME_FORMATTER);
    }
    
    /**
     * 格式化时间为指定格式
     */
    public static String format(LocalDateTime dateTime, String pattern) {
        if (dateTime == null || StringUtil.isEmpty(pattern)) {
            return null;
        }
        
        try {
            return dateTime.format(DateTimeFormatter.ofPattern(pattern));
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * 格式化时间为指定格式
     */
    public static String format(LocalDateTime dateTime, DateTimeFormatter formatter) {
        if (dateTime == null || formatter == null) {
            return null;
        }
        
        try {
            return dateTime.format(formatter);
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * 格式化日期为默认格式
     */
    public static String formatDate(LocalDate date) {
        if (date == null) {
            return null;
        }
        return date.format(DEFAULT_DATE_FORMATTER);
    }
    
    /**
     * 格式化时间为默认格式
     */
    public static String formatTime(LocalTime time) {
        if (time == null) {
            return null;
        }
        return time.format(DEFAULT_TIME_FORMATTER);
    }
    
    /**
     * 解析时间字符串（默认格式）
     */
    public static LocalDateTime parse(String dateTimeStr) {
        if (StringUtil.isEmpty(dateTimeStr)) {
            return null;
        }
        
        try {
            return LocalDateTime.parse(dateTimeStr, DEFAULT_DATE_TIME_FORMATTER);
        } catch (DateTimeParseException e) {
            return null;
        }
    }
    
    /**
     * 解析时间字符串（指定格式）
     */
    public static LocalDateTime parse(String dateTimeStr, String pattern) {
        if (StringUtil.isEmpty(dateTimeStr) || StringUtil.isEmpty(pattern)) {
            return null;
        }
        
        try {
            return LocalDateTime.parse(dateTimeStr, DateTimeFormatter.ofPattern(pattern));
        } catch (DateTimeParseException e) {
            return null;
        }
    }
    
    /**
     * 解析时间字符串（指定格式）
     */
    public static LocalDateTime parse(String dateTimeStr, DateTimeFormatter formatter) {
        if (StringUtil.isEmpty(dateTimeStr) || formatter == null) {
            return null;
        }
        
        try {
            return LocalDateTime.parse(dateTimeStr, formatter);
        } catch (DateTimeParseException e) {
            return null;
        }
    }
    
    /**
     * 解析日期字符串
     */
    public static LocalDate parseDate(String dateStr) {
        if (StringUtil.isEmpty(dateStr)) {
            return null;
        }
        
        try {
            return LocalDate.parse(dateStr, DEFAULT_DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            return null;
        }
    }
    
    /**
     * 时间戳转LocalDateTime
     */
    public static LocalDateTime fromTimestamp(long timestamp) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault());
    }
    
    /**
     * 时间戳转LocalDateTime（秒）
     */
    public static LocalDateTime fromTimestampSeconds(long timestampSeconds) {
        return LocalDateTime.ofInstant(Instant.ofEpochSecond(timestampSeconds), ZoneId.systemDefault());
    }
    
    /**
     * LocalDateTime转时间戳
     */
    public static long toTimestamp(LocalDateTime dateTime) {
        if (dateTime == null) {
            return 0;
        }
        return dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }
    
    /**
     * LocalDateTime转时间戳（秒）
     */
    public static long toTimestampSeconds(LocalDateTime dateTime) {
        if (dateTime == null) {
            return 0;
        }
        return dateTime.atZone(ZoneId.systemDefault()).toInstant().getEpochSecond();
    }
    
    /**
     * 计算两个时间之间的天数差
     */
    public static long daysBetween(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            return 0;
        }
        return ChronoUnit.DAYS.between(start.toLocalDate(), end.toLocalDate());
    }
    
    /**
     * 计算两个时间之间的小时差
     */
    public static long hoursBetween(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            return 0;
        }
        return ChronoUnit.HOURS.between(start, end);
    }
    
    /**
     * 计算两个时间之间的分钟差
     */
    public static long minutesBetween(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            return 0;
        }
        return ChronoUnit.MINUTES.between(start, end);
    }
    
    /**
     * 计算两个时间之间的秒数差
     */
    public static long secondsBetween(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            return 0;
        }
        return ChronoUnit.SECONDS.between(start, end);
    }
    
    /**
     * 添加天数
     */
    public static LocalDateTime addDays(LocalDateTime dateTime, long days) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.plusDays(days);
    }
    
    /**
     * 添加小时
     */
    public static LocalDateTime addHours(LocalDateTime dateTime, long hours) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.plusHours(hours);
    }
    
    /**
     * 添加分钟
     */
    public static LocalDateTime addMinutes(LocalDateTime dateTime, long minutes) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.plusMinutes(minutes);
    }
    
    /**
     * 添加秒数
     */
    public static LocalDateTime addSeconds(LocalDateTime dateTime, long seconds) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.plusSeconds(seconds);
    }
    
    /**
     * 减去天数
     */
    public static LocalDateTime subtractDays(LocalDateTime dateTime, long days) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.minusDays(days);
    }
    
    /**
     * 减去小时
     */
    public static LocalDateTime subtractHours(LocalDateTime dateTime, long hours) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.minusHours(hours);
    }
    
    /**
     * 减去分钟
     */
    public static LocalDateTime subtractMinutes(LocalDateTime dateTime, long minutes) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.minusMinutes(minutes);
    }
    
    /**
     * 减去秒数
     */
    public static LocalDateTime subtractSeconds(LocalDateTime dateTime, long seconds) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.minusSeconds(seconds);
    }
    
    /**
     * 获取一天的开始时间
     */
    public static LocalDateTime startOfDay(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.toLocalDate().atStartOfDay();
    }
    
    /**
     * 获取一天的结束时间
     */
    public static LocalDateTime endOfDay(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.toLocalDate().atTime(23, 59, 59, 999_999_999);
    }
    
    /**
     * 获取一周的开始时间（周一）
     */
    public static LocalDateTime startOfWeek(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        LocalDate date = dateTime.toLocalDate();
        return date.minusDays(date.getDayOfWeek().getValue() - 1).atStartOfDay();
    }
    
    /**
     * 获取一月的开始时间
     */
    public static LocalDateTime startOfMonth(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.toLocalDate().withDayOfMonth(1).atStartOfDay();
    }
    
    /**
     * 获取一年的开始时间
     */
    public static LocalDateTime startOfYear(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.toLocalDate().withDayOfYear(1).atStartOfDay();
    }
    
    /**
     * 判断是否为同一天
     */
    public static boolean isSameDay(LocalDateTime dateTime1, LocalDateTime dateTime2) {
        if (dateTime1 == null || dateTime2 == null) {
            return false;
        }
        return dateTime1.toLocalDate().equals(dateTime2.toLocalDate());
    }
    
    /**
     * 判断是否为今天
     */
    public static boolean isToday(LocalDateTime dateTime) {
        if (dateTime == null) {
            return false;
        }
        return dateTime.toLocalDate().equals(LocalDate.now());
    }
    
    /**
     * 判断是否为昨天
     */
    public static boolean isYesterday(LocalDateTime dateTime) {
        if (dateTime == null) {
            return false;
        }
        return dateTime.toLocalDate().equals(LocalDate.now().minusDays(1));
    }
    
    /**
     * 判断是否为明天
     */
    public static boolean isTomorrow(LocalDateTime dateTime) {
        if (dateTime == null) {
            return false;
        }
        return dateTime.toLocalDate().equals(LocalDate.now().plusDays(1));
    }
    
    /**
     * 判断是否为工作日
     */
    public static boolean isWeekday(LocalDateTime dateTime) {
        if (dateTime == null) {
            return false;
        }
        DayOfWeek dayOfWeek = dateTime.getDayOfWeek();
        return dayOfWeek != DayOfWeek.SATURDAY && dayOfWeek != DayOfWeek.SUNDAY;
    }
    
    /**
     * 判断是否为周末
     */
    public static boolean isWeekend(LocalDateTime dateTime) {
        return !isWeekday(dateTime);
    }
    
    /**
     * 获取友好的时间描述
     */
    public static String getTimeAgo(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "未知时间";
        }
        
        LocalDateTime now = LocalDateTime.now();
        long seconds = secondsBetween(dateTime, now);
        
        if (seconds < 0) {
            return "未来时间";
        }
        
        if (seconds < 60) {
            return seconds + "秒前";
        }
        
        long minutes = seconds / 60;
        if (minutes < 60) {
            return minutes + "分钟前";
        }
        
        long hours = minutes / 60;
        if (hours < 24) {
            return hours + "小时前";
        }
        
        long days = hours / 24;
        if (days < 30) {
            return days + "天前";
        }
        
        long months = days / 30;
        if (months < 12) {
            return months + "个月前";
        }
        
        long years = months / 12;
        return years + "年前";
    }
    
    /**
     * 睡眠指定毫秒数
     */
    public static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * 睡眠指定时间
     */
    public static void sleep(long time, TimeUnit timeUnit) {
        try {
            timeUnit.sleep(time);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * 获取当前时间的中文描述
     */
    public static String getChineseDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.format(CHINESE_DATE_TIME_FORMATTER);
    }
    
    /**
     * 获取当前时间的紧凑格式
     */
    public static String getCompactDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.format(COMPACT_DATE_TIME_FORMATTER);
    }
}

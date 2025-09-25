package com.chatbot.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

/**
 * JSON工具类
 * 使用Jackson进行JSON序列化和反序列化操作
 * 支持Java 21特性和现代化编程风格
 */
public class JsonUtil {
    
    private static final Logger logger = LoggerFactory.getLogger(JsonUtil.class);
    
    private static final ObjectMapper objectMapper;
    
    static {
        objectMapper = new ObjectMapper();
        // 注册Java时间模块
        objectMapper.registerModule(new JavaTimeModule());
        // 禁用将日期写为时间戳
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        // 配置其他序列化特性
        objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        objectMapper.configure(JsonParser.Feature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER, true);
        objectMapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_CONTROL_CHARS, true);
    }
    
    /**
     * 将对象转换为JSON字符串
     */
    public static String toJson(Object obj) {
        if (obj == null) {
            return null;
        }
        
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            logger.error("对象转JSON失败: " + obj.getClass().getSimpleName(), e);
            return null;
        }
    }
    
    /**
     * 将对象转换为格式化的JSON字符串
     */
    public static String toPrettyJson(Object obj) {
        if (obj == null) {
            return null;
        }
        
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            logger.error("对象转格式化JSON失败: " + obj.getClass().getSimpleName(), e);
            return null;
        }
    }
    
    /**
     * 将JSON字符串转换为指定类型的对象
     */
    public static <T> T fromJson(String json, Class<T> clazz) {
        if (StringUtil.isEmpty(json) || clazz == null) {
            return null;
        }
        
        try {
            return objectMapper.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            logger.error("JSON转对象失败: " + clazz.getSimpleName() + ", JSON: " + json, e);
            return null;
        }
    }
    
    /**
     * 将JSON字符串转换为指定类型的对象（使用TypeReference）
     */
    public static <T> T fromJson(String json, TypeReference<T> typeReference) {
        if (StringUtil.isEmpty(json) || typeReference == null) {
            return null;
        }
        
        try {
            return objectMapper.readValue(json, typeReference);
        } catch (JsonProcessingException e) {
            logger.error("JSON转对象失败: " + typeReference.getType() + ", JSON: " + json, e);
            return null;
        }
    }
    
    /**
     * 将JSON字符串转换为List
     */
    public static <T> List<T> fromJsonToList(String json, Class<T> clazz) {
        if (StringUtil.isEmpty(json) || clazz == null) {
            return null;
        }
        
        try {
            return objectMapper.readValue(json, 
                objectMapper.getTypeFactory().constructCollectionType(List.class, clazz));
        } catch (JsonProcessingException e) {
            logger.error("JSON转List失败: " + clazz.getSimpleName() + ", JSON: " + json, e);
            return null;
        }
    }
    
    /**
     * 将JSON字符串转换为Map
     */
    public static Map<String, Object> fromJsonToMap(String json) {
        if (StringUtil.isEmpty(json)) {
            return null;
        }
        
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (JsonProcessingException e) {
            logger.error("JSON转Map失败, JSON: " + json, e);
            return null;
        }
    }
    
    /**
     * 将JSON字符串解析为JsonNode
     */
    public static JsonNode parseJson(String json) {
        if (StringUtil.isEmpty(json)) {
            return null;
        }
        
        try {
            return objectMapper.readTree(json);
        } catch (JsonProcessingException e) {
            logger.error("JSON解析失败, JSON: " + json, e);
            return null;
        }
    }
    
    /**
     * 检查字符串是否为有效的JSON
     */
    public static boolean isValidJson(String json) {
        if (StringUtil.isEmpty(json)) {
            return false;
        }
        
        try {
            objectMapper.readTree(json);
            return true;
        } catch (JsonProcessingException e) {
            return false;
        }
    }
    
    /**
     * 从JsonNode中安全获取字符串值
     */
    public static String getStringValue(JsonNode node, String fieldName) {
        if (node == null || !node.has(fieldName)) {
            return null;
        }
        
        JsonNode fieldNode = node.get(fieldName);
        return fieldNode.isNull() ? null : fieldNode.asText();
    }
    
    /**
     * 从JsonNode中安全获取整数值
     */
    public static Integer getIntValue(JsonNode node, String fieldName) {
        if (node == null || !node.has(fieldName)) {
            return null;
        }
        
        JsonNode fieldNode = node.get(fieldName);
        return fieldNode.isNull() ? null : fieldNode.asInt();
    }
    
    /**
     * 从JsonNode中安全获取布尔值
     */
    public static Boolean getBooleanValue(JsonNode node, String fieldName) {
        if (node == null || !node.has(fieldName)) {
            return null;
        }
        
        JsonNode fieldNode = node.get(fieldName);
        return fieldNode.isNull() ? null : fieldNode.asBoolean();
    }
    
    /**
     * 对象深拷贝（通过JSON序列化/反序列化）
     */
    public static <T> T deepCopy(T obj, Class<T> clazz) {
        if (obj == null) {
            return null;
        }
        
        try {
            String json = objectMapper.writeValueAsString(obj);
            return objectMapper.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            logger.error("对象深拷贝失败: " + clazz.getSimpleName(), e);
            return null;
        }
    }
    
    /**
     * 合并两个JSON对象
     */
    public static JsonNode mergeJson(JsonNode mainNode, JsonNode updateNode) {
        if (mainNode == null) {
            return updateNode;
        }
        if (updateNode == null) {
            return mainNode;
        }
        
        try {
            return objectMapper.readerForUpdating(mainNode).readValue(updateNode);
        } catch (IOException e) {
            logger.error("JSON合并失败", e);
            return mainNode;
        }
    }
    
    /**
     * 将对象保存为格式化的JSON文件
     */
    public static boolean saveToPrettyJsonFile(Object obj, String filePath) {
        if (obj == null || StringUtil.isEmpty(filePath)) {
            return false;
        }
        
        try {
            String jsonContent = toPrettyJson(obj);
            if (jsonContent == null) {
                return false;
            }
            
            Path path = Paths.get(filePath);
            Files.createDirectories(path.getParent());
            Files.writeString(path, jsonContent, StandardCharsets.UTF_8);
            
            logger.debug("对象已保存为格式化JSON文件: {}", filePath);
            return true;
            
        } catch (IOException e) {
            logger.error("保存对象到JSON文件失败: " + filePath, e);
            return false;
        }
    }
    
    /**
     * 将对象保存为压缩的JSON文件
     */
    public static boolean saveToJsonFile(Object obj, String filePath) {
        if (obj == null || StringUtil.isEmpty(filePath)) {
            return false;
        }
        
        try {
            String jsonContent = toJson(obj);
            if (jsonContent == null) {
                return false;
            }
            
            Path path = Paths.get(filePath);
            Files.createDirectories(path.getParent());
            Files.writeString(path, jsonContent, StandardCharsets.UTF_8);
            
            logger.debug("对象已保存为压缩JSON文件: {}", filePath);
            return true;
            
        } catch (IOException e) {
            logger.error("保存对象到JSON文件失败: " + filePath, e);
            return false;
        }
    }
    
    /**
     * 获取ObjectMapper实例（用于特殊情况）
     */
    public static ObjectMapper getObjectMapper() {
        return objectMapper;
    }
}

package com.chatbot.model.dto.common;

/**
 * 通用API调用结果封装
 * 适用于所有外部服务调用（TTS、ASR、OCR等）
 * 
 * @param <T> 具体的数据类型
 */
public class ApiResult<T> {
    private final boolean success;
    private final String message;
    private final T data;
    private final String errorCode;
    private final long timestamp;
    
    private ApiResult(boolean success, String message, T data, String errorCode) {
        this.success = success;
        this.message = message;
        this.data = data;
        this.errorCode = errorCode;
        this.timestamp = System.currentTimeMillis();
    }
    
    /**
     * 创建成功结果
     * @param data 数据
     * @return ApiResult实例
     */
    public static <T> ApiResult<T> success(T data) {
        return new ApiResult<>(true, "Success", data, null);
    }
    
    /**
     * 创建成功结果（带消息）
     * @param data 数据
     * @param message 消息
     * @return ApiResult实例
     */
    public static <T> ApiResult<T> success(T data, String message) {
        return new ApiResult<>(true, message, data, null);
    }
    
    /**
     * 创建失败结果
     * @param message 错误消息
     * @return ApiResult实例
     */
    public static <T> ApiResult<T> failure(String message) {
        return new ApiResult<>(false, message, null, null);
    }
    
    /**
     * 创建失败结果（带错误码）
     * @param message 错误消息
     * @param errorCode 错误码
     * @return ApiResult实例
     */
    public static <T> ApiResult<T> failure(String message, String errorCode) {
        return new ApiResult<>(false, message, null, errorCode);
    }
    
    // Getters
    public boolean isSuccess() { 
        return success; 
    }
    
    public String getMessage() { 
        return message; 
    }
    
    public T getData() { 
        return data; 
    }
    
    public String getErrorCode() { 
        return errorCode; 
    }
    
    public long getTimestamp() { 
        return timestamp; 
    }
    
    /**
     * 如果失败则抛出异常
     * @throws RuntimeException 如果结果失败
     */
    public void throwIfFailure() {
        if (!success) {
            throw new RuntimeException(message);
        }
    }
    
    /**
     * 如果失败则抛出自定义异常
     * @param exceptionSupplier 异常供应商
     * @param <X> 异常类型
     * @throws X 自定义异常
     */
    public <X extends Throwable> void throwIfFailure(java.util.function.Supplier<X> exceptionSupplier) throws X {
        if (!success) {
            throw exceptionSupplier.get();
        }
    }
    
    @Override
    public String toString() {
        return String.format("ApiResult{success=%s, message='%s', errorCode='%s', hasData=%s, timestamp=%d}",
                           success, message, errorCode, data != null, timestamp);
    }
}


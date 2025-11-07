package com.chatbot.model.dto;

import okhttp3.Headers;

/**
 * HTTP响应结果封装类
 */
public class HttpResponse {
    private final int code;
    private final String body;
    private final boolean success;
    private final String message;
    private final Headers headers;
    
    public HttpResponse(int code, String body, Headers headers) {
        this.code = code;
        this.body = body;
        this.headers = headers;
        this.success = code >= 200 && code < 300;
        this.message = success ? "Success" : "HTTP " + code;
    }
    
    public int getCode() { 
        return code; 
    }
    
    public String getBody() { 
        return body; 
    }
    
    public boolean isSuccess() { 
        return success; 
    }
    
    public String getMessage() { 
        return message; 
    }
    
    public Headers getHeaders() { 
        return headers; 
    }
    
    public String getHeader(String name) {
        return headers.get(name);
    }
    
    @Override
    public String toString() {
        return "HttpResponse{code=" + code + ", success=" + success + ", bodyLength=" + (body != null ? body.length() : 0) + "}";
    }
}

package com.chatbot.util;

import com.chatbot.model.dto.common.HttpResponse;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * HTTP工具类
 * 基于OkHttp实现，支持同步和异步请求，使用Java 21特性
 */
public class HttpUtil {
    
    private static final Logger logger = LoggerFactory.getLogger(HttpUtil.class);
    
    private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");
    private static final MediaType FORM_MEDIA_TYPE = MediaType.parse("application/x-www-form-urlencoded");
    
    // 默认的HTTP客户端
    private static final OkHttpClient DEFAULT_CLIENT = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();
    
    
    /**
     * GET请求
     */
    public static HttpResponse get(String url) {
        return get(url, null, null);
    }
    
    /**
     * GET请求（带请求头）
     */
    public static HttpResponse get(String url, Map<String, String> headers) {
        return get(url, headers, null);
    }
    
    /**
     * GET请求（带请求头和自定义客户端）
     */
    public static HttpResponse get(String url, Map<String, String> headers, OkHttpClient client) {
        long startTime = System.currentTimeMillis();
        logger.debug("开始GET请求，URL: {}", url);
        
        if (StringUtil.isEmpty(url)) {
            logger.warn("GET请求失败：URL为空");
            return new HttpResponse(400, "URL不能为空", new Headers.Builder().build());
        }
        
        Request.Builder requestBuilder = new Request.Builder().url(url);
        
        // 添加请求头
        if (headers != null) {
            headers.forEach(requestBuilder::addHeader);
            logger.debug("添加请求头，数量: {}", headers.size());
        }
        
        Request request = requestBuilder.build();
        OkHttpClient httpClient = client != null ? client : DEFAULT_CLIENT;
        
        try (Response response = httpClient.newCall(request).execute()) {
            ResponseBody responseBody = response.body();
            String body = responseBody != null ? responseBody.string() : "";
            long responseTime = System.currentTimeMillis() - startTime;
            
            logger.info("GET请求完成，URL: {}, 状态码: {}, 响应时间: {}ms, 响应长度: {}", 
                       url, response.code(), responseTime, body.length());
            
            return new HttpResponse(response.code(), body, response.headers());
        } catch (IOException e) {
            long responseTime = System.currentTimeMillis() - startTime;
            logger.error("GET请求异常，URL: {}, 响应时间: {}ms", url, responseTime, e);
            return new HttpResponse(500, "请求异常: " + e.getMessage(), new Headers.Builder().build());
        }
    }
    
    /**
     * POST请求（JSON数据）
     */
    public static HttpResponse postJson(String url, String jsonBody) {
        return postJson(url, jsonBody, null, null);
    }
    
    /**
     * POST请求（JSON数据，带请求头）
     */
    public static HttpResponse postJson(String url, String jsonBody, Map<String, String> headers) {
        return postJson(url, jsonBody, headers, null);
    }
    
    /**
     * POST请求（JSON数据，带请求头和自定义客户端）
     */
    public static HttpResponse postJson(String url, String jsonBody, Map<String, String> headers, OkHttpClient client) {
        if (StringUtil.isEmpty(url)) {
            return new HttpResponse(400, "URL不能为空", new Headers.Builder().build());
        }
        
        RequestBody body = RequestBody.create(jsonBody != null ? jsonBody : "", JSON_MEDIA_TYPE);
        Request.Builder requestBuilder = new Request.Builder()
                .url(url)
                .post(body);
        
        // 添加请求头
        if (headers != null) {
            headers.forEach(requestBuilder::addHeader);
        }
        
        Request request = requestBuilder.build();
        OkHttpClient httpClient = client != null ? client : DEFAULT_CLIENT;
        
        try (Response response = httpClient.newCall(request).execute()) {
            ResponseBody responseBody = response.body();
            String responseStr = responseBody != null ? responseBody.string() : "";
            return new HttpResponse(response.code(), responseStr, response.headers());
        } catch (IOException e) {
            logger.error("POST JSON请求失败: " + url, e);
            return new HttpResponse(500, "请求异常: " + e.getMessage(), new Headers.Builder().build());
        }
    }
    
    /**
     * POST请求（对象转JSON）
     */
    public static HttpResponse postJson(String url, Object object) {
        String jsonBody = JsonUtil.toJson(object);
        return postJson(url, jsonBody);
    }
    
    /**
     * POST请求（对象转JSON，带请求头）
     */
    public static HttpResponse postJson(String url, Object object, Map<String, String> headers) {
        String jsonBody = JsonUtil.toJson(object);
        return postJson(url, jsonBody, headers);
    }
    
    /**
     * POST请求（表单数据）
     */
    public static HttpResponse postForm(String url, Map<String, String> formData) {
        return postForm(url, formData, null, null);
    }
    
    /**
     * POST请求（表单数据，带请求头）
     */
    public static HttpResponse postForm(String url, Map<String, String> formData, Map<String, String> headers) {
        return postForm(url, formData, headers, null);
    }
    
    /**
     * POST请求（表单数据，带请求头和自定义客户端）
     */
    public static HttpResponse postForm(String url, Map<String, String> formData, Map<String, String> headers, OkHttpClient client) {
        if (StringUtil.isEmpty(url)) {
            return new HttpResponse(400, "URL不能为空", new Headers.Builder().build());
        }
        
        FormBody.Builder formBuilder = new FormBody.Builder();
        if (formData != null) {
            formData.forEach(formBuilder::add);
        }
        
        Request.Builder requestBuilder = new Request.Builder()
                .url(url)
                .post(formBuilder.build());
        
        // 添加请求头
        if (headers != null) {
            headers.forEach(requestBuilder::addHeader);
        }
        
        Request request = requestBuilder.build();
        OkHttpClient httpClient = client != null ? client : DEFAULT_CLIENT;
        
        try (Response response = httpClient.newCall(request).execute()) {
            ResponseBody responseBody = response.body();
            String body = responseBody != null ? responseBody.string() : "";
            return new HttpResponse(response.code(), body, response.headers());
        } catch (IOException e) {
            logger.error("POST Form请求失败: " + url, e);
            return new HttpResponse(500, "请求异常: " + e.getMessage(), new Headers.Builder().build());
        }
    }
    
    /**
     * PUT请求（JSON数据）
     */
    public static HttpResponse putJson(String url, String jsonBody) {
        return putJson(url, jsonBody, null, null);
    }
    
    /**
     * PUT请求（JSON数据，带请求头和自定义客户端）
     */
    public static HttpResponse putJson(String url, String jsonBody, Map<String, String> headers, OkHttpClient client) {
        if (StringUtil.isEmpty(url)) {
            return new HttpResponse(400, "URL不能为空", new Headers.Builder().build());
        }
        
        RequestBody body = RequestBody.create(jsonBody != null ? jsonBody : "", JSON_MEDIA_TYPE);
        Request.Builder requestBuilder = new Request.Builder()
                .url(url)
                .put(body);
        
        // 添加请求头
        if (headers != null) {
            headers.forEach(requestBuilder::addHeader);
        }
        
        Request request = requestBuilder.build();
        OkHttpClient httpClient = client != null ? client : DEFAULT_CLIENT;
        
        try (Response response = httpClient.newCall(request).execute()) {
            ResponseBody responseBody = response.body();
            String responseStr = responseBody != null ? responseBody.string() : "";
            return new HttpResponse(response.code(), responseStr, response.headers());
        } catch (IOException e) {
            logger.error("PUT请求失败: " + url, e);
            return new HttpResponse(500, "请求异常: " + e.getMessage(), new Headers.Builder().build());
        }
    }
    
    /**
     * DELETE请求
     */
    public static HttpResponse delete(String url) {
        return delete(url, null, null);
    }
    
    /**
     * DELETE请求（带请求头和自定义客户端）
     */
    public static HttpResponse delete(String url, Map<String, String> headers, OkHttpClient client) {
        if (StringUtil.isEmpty(url)) {
            return new HttpResponse(400, "URL不能为空", new Headers.Builder().build());
        }
        
        Request.Builder requestBuilder = new Request.Builder()
                .url(url)
                .delete();
        
        // 添加请求头
        if (headers != null) {
            headers.forEach(requestBuilder::addHeader);
        }
        
        Request request = requestBuilder.build();
        OkHttpClient httpClient = client != null ? client : DEFAULT_CLIENT;
        
        try (Response response = httpClient.newCall(request).execute()) {
            ResponseBody responseBody = response.body();
            String body = responseBody != null ? responseBody.string() : "";
            return new HttpResponse(response.code(), body, response.headers());
        } catch (IOException e) {
            logger.error("DELETE请求失败: " + url, e);
            return new HttpResponse(500, "请求异常: " + e.getMessage(), new Headers.Builder().build());
        }
    }
    
    /**
     * 异步GET请求
     */
    public static CompletableFuture<HttpResponse> getAsync(String url) {
        return getAsync(url, null, null);
    }
    
    /**
     * 异步GET请求（带请求头和自定义客户端）
     */
    public static CompletableFuture<HttpResponse> getAsync(String url, Map<String, String> headers, OkHttpClient client) {
        CompletableFuture<HttpResponse> future = new CompletableFuture<>();
        
        if (StringUtil.isEmpty(url)) {
            future.complete(new HttpResponse(400, "URL不能为空", new Headers.Builder().build()));
            return future;
        }
        
        Request.Builder requestBuilder = new Request.Builder().url(url);
        
        // 添加请求头
        if (headers != null) {
            headers.forEach(requestBuilder::addHeader);
        }
        
        Request request = requestBuilder.build();
        OkHttpClient httpClient = client != null ? client : DEFAULT_CLIENT;
        
        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                logger.error("异步GET请求失败: " + url, e);
                future.complete(new HttpResponse(500, "请求异常: " + e.getMessage(), new Headers.Builder().build()));
            }
            
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (ResponseBody responseBody = response.body()) {
                    String body = responseBody != null ? responseBody.string() : "";
                    future.complete(new HttpResponse(response.code(), body, response.headers()));
                }
            }
        });
        
        return future;
    }
    
    /**
     * 异步POST请求（JSON数据）
     */
    public static CompletableFuture<HttpResponse> postJsonAsync(String url, String jsonBody) {
        return postJsonAsync(url, jsonBody, null, null);
    }
    
    /**
     * 异步POST请求（JSON数据，带请求头和自定义客户端）
     */
    public static CompletableFuture<HttpResponse> postJsonAsync(String url, String jsonBody, Map<String, String> headers, OkHttpClient client) {
        CompletableFuture<HttpResponse> future = new CompletableFuture<>();
        
        if (StringUtil.isEmpty(url)) {
            future.complete(new HttpResponse(400, "URL不能为空", new Headers.Builder().build()));
            return future;
        }
        
        RequestBody body = RequestBody.create(jsonBody != null ? jsonBody : "", JSON_MEDIA_TYPE);
        Request.Builder requestBuilder = new Request.Builder()
                .url(url)
                .post(body);
        
        // 添加请求头
        if (headers != null) {
            headers.forEach(requestBuilder::addHeader);
        }
        
        Request request = requestBuilder.build();
        OkHttpClient httpClient = client != null ? client : DEFAULT_CLIENT;
        
        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                logger.error("异步POST请求失败: " + url, e);
                future.complete(new HttpResponse(500, "请求异常: " + e.getMessage(), new Headers.Builder().build()));
            }
            
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (ResponseBody responseBody = response.body()) {
                    String responseStr = responseBody != null ? responseBody.string() : "";
                    future.complete(new HttpResponse(response.code(), responseStr, response.headers()));
                }
            }
        });
        
        return future;
    }
    
    /**
     * 创建自定义HTTP客户端
     */
    public static OkHttpClient createClient(Duration connectTimeout, Duration readTimeout, Duration writeTimeout) {
        return new OkHttpClient.Builder()
                .connectTimeout(connectTimeout)
                .readTimeout(readTimeout)
                .writeTimeout(writeTimeout)
                .build();
    }
    
    /**
     * 创建带代理的HTTP客户端
     */
    public static OkHttpClient createClientWithProxy(String proxyHost, int proxyPort) {
        return new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .proxy(new java.net.Proxy(java.net.Proxy.Type.HTTP, 
                    new java.net.InetSocketAddress(proxyHost, proxyPort)))
                .build();
    }
    
    /**
     * 下载文件
     */
    public static boolean downloadFile(String url, String filePath) {
        return downloadFile(url, filePath, null, null);
    }
    
    /**
     * 下载文件（带请求头和自定义客户端）
     */
    public static boolean downloadFile(String url, String filePath, Map<String, String> headers, OkHttpClient client) {
        if (StringUtil.isEmpty(url) || StringUtil.isEmpty(filePath)) {
            return false;
        }
        
        Request.Builder requestBuilder = new Request.Builder().url(url);
        
        // 添加请求头
        if (headers != null) {
            headers.forEach(requestBuilder::addHeader);
        }
        
        Request request = requestBuilder.build();
        OkHttpClient httpClient = client != null ? client : DEFAULT_CLIENT;
        
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                logger.error("下载文件失败，HTTP状态码: " + response.code());
                return false;
            }
            
            ResponseBody responseBody = response.body();
            if (responseBody == null) {
                logger.error("下载文件失败，响应体为空");
                return false;
            }
            
            // 写入文件
            return FileUtil.writeString(filePath, responseBody.string());
            
        } catch (IOException e) {
            logger.error("下载文件失败: " + url + " -> " + filePath, e);
            return false;
        }
    }
    
    /**
     * 检查URL是否可访问
     */
    public static boolean isUrlAccessible(String url) {
        return isUrlAccessible(url, Duration.ofSeconds(5));
    }
    
    /**
     * 检查URL是否可访问（指定超时时间）
     */
    public static boolean isUrlAccessible(String url, Duration timeout) {
        if (StringUtil.isEmpty(url)) {
            return false;
        }
        
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(timeout)
                .readTimeout(timeout)
                .writeTimeout(timeout)
                .build();
        
        Request request = new Request.Builder()
                .url(url)
                .head() // 使用HEAD请求减少流量
                .build();
        
        try (Response response = client.newCall(request).execute()) {
            return response.isSuccessful();
        } catch (IOException e) {
            logger.debug("URL不可访问: " + url, e);
            return false;
        }
    }
    
    /**
     * 构建URL（带查询参数）
     */
    public static String buildUrl(String baseUrl, Map<String, String> params) {
        if (StringUtil.isEmpty(baseUrl)) {
            return baseUrl;
        }
        
        if (params == null || params.isEmpty()) {
            return baseUrl;
        }
        
        HttpUrl.Builder urlBuilder = HttpUrl.parse(baseUrl).newBuilder();
        params.forEach(urlBuilder::addQueryParameter);
        
        return urlBuilder.build().toString();
    }
    
    /**
     * URL编码
     */
    public static String urlEncode(String value) {
        if (StringUtil.isEmpty(value)) {
            return value;
        }
        
        try {
            return java.net.URLEncoder.encode(value, "UTF-8");
        } catch (java.io.UnsupportedEncodingException e) {
            logger.error("URL编码失败", e);
            return value;
        }
    }
    
    /**
     * URL解码
     */
    public static String urlDecode(String value) {
        if (StringUtil.isEmpty(value)) {
            return value;
        }
        
        try {
            return java.net.URLDecoder.decode(value, "UTF-8");
        } catch (java.io.UnsupportedEncodingException e) {
            logger.error("URL解码失败", e);
            return value;
        }
    }
    
    /**
     * 获取默认的HTTP客户端
     */
    public static OkHttpClient getDefaultClient() {
        return DEFAULT_CLIENT;
    }
}

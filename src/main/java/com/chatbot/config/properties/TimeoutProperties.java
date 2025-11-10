package com.chatbot.config.properties;

/**
 * 超时配置
 */
public class TimeoutProperties {
    // HTTP连接超时（秒）
    private int connectTimeoutSeconds;
    
    // HTTP读取超时（秒）
    private int readTimeoutSeconds;
    
    // HTTP写入超时（秒）
    private int writeTimeoutSeconds;
    
    // TTS任务等待超时（秒）
    private int ttsTaskTimeoutSeconds;
    
    // Live2D TTS任务等待超时（秒）
    private int live2dTtsTaskTimeoutSeconds;
    
    public int getConnectTimeoutSeconds() { return connectTimeoutSeconds; }
    public void setConnectTimeoutSeconds(int connectTimeoutSeconds) { this.connectTimeoutSeconds = connectTimeoutSeconds; }
    
    public int getReadTimeoutSeconds() { return readTimeoutSeconds; }
    public void setReadTimeoutSeconds(int readTimeoutSeconds) { this.readTimeoutSeconds = readTimeoutSeconds; }
    
    public int getWriteTimeoutSeconds() { return writeTimeoutSeconds; }
    public void setWriteTimeoutSeconds(int writeTimeoutSeconds) { this.writeTimeoutSeconds = writeTimeoutSeconds; }
    
    public int getTtsTaskTimeoutSeconds() { return ttsTaskTimeoutSeconds; }
    public void setTtsTaskTimeoutSeconds(int ttsTaskTimeoutSeconds) { this.ttsTaskTimeoutSeconds = ttsTaskTimeoutSeconds; }
    
    public int getLive2dTtsTaskTimeoutSeconds() { return live2dTtsTaskTimeoutSeconds; }
    public void setLive2dTtsTaskTimeoutSeconds(int live2dTtsTaskTimeoutSeconds) { this.live2dTtsTaskTimeoutSeconds = live2dTtsTaskTimeoutSeconds; }
}


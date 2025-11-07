package com.chatbot.model.config;

/**
 * 输出通道配置
 * 整合聊天窗口和Live2D的输出设置
 */
public class OutputChannelConfig {
    
    private ChatWindowOutput chatWindow = new ChatWindowOutput();
    private Live2DOutput live2d = new Live2DOutput();
    
    public ChatWindowOutput getChatWindow() {
        return chatWindow;
    }
    
    public void setChatWindow(ChatWindowOutput chatWindow) {
        this.chatWindow = chatWindow;
    }
    
    public Live2DOutput getLive2d() {
        return live2d;
    }
    
    public void setLive2d(Live2DOutput live2d) {
        this.live2d = live2d;
    }
    
    /**
     * 聊天窗口输出配置
     */
    public static class ChatWindowOutput {
        private boolean enabled = false;
        private String mode = "text_only";  // text_only, char_stream_tts
        private boolean autoTTS = false;
        private String speakerId = "派蒙";
        
        public boolean isEnabled() {
            return enabled;
        }
        
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
        
        public String getMode() {
            return mode;
        }
        
        public void setMode(String mode) {
            this.mode = mode;
        }
        
        public boolean isAutoTTS() {
            return autoTTS;
        }
        
        public void setAutoTTS(boolean autoTTS) {
            this.autoTTS = autoTTS;
        }
        
        public String getSpeakerId() {
            return speakerId;
        }
        
        public void setSpeakerId(String speakerId) {
            this.speakerId = speakerId;
        }
    }
    
    /**
     * Live2D输出配置
     */
    public static class Live2DOutput {
        private boolean enabled = false;
        private String mode = "sentence_sync";
        private String speakerId = "派蒙";
        private double speed = 1.0;
        private boolean showBubble = true;
        private int bubbleTimeout = 5000;
        
        public boolean isEnabled() {
            return enabled;
        }
        
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
        
        public String getMode() {
            return mode;
        }
        
        public void setMode(String mode) {
            this.mode = mode;
        }
        
        public String getSpeakerId() {
            return speakerId;
        }
        
        public void setSpeakerId(String speakerId) {
            this.speakerId = speakerId;
        }
        
        public double getSpeed() {
            return speed;
        }
        
        public void setSpeed(double speed) {
            this.speed = speed;
        }
        
        public boolean isShowBubble() {
            return showBubble;
        }
        
        public void setShowBubble(boolean showBubble) {
            this.showBubble = showBubble;
        }
        
        public int getBubbleTimeout() {
            return bubbleTimeout;
        }
        
        public void setBubbleTimeout(int bubbleTimeout) {
            this.bubbleTimeout = bubbleTimeout;
        }
    }
}


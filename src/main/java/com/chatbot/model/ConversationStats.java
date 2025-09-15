package com.chatbot.model;

/**
 * 对话统计信息
 */
public class ConversationStats {
    private int totalConversations = 0;
    private int totalMessages = 0;
    private long totalFileSize = 0;
    
    // Getters
    public int getTotalConversations() { 
        return totalConversations; 
    }
    
    public int getTotalMessages() { 
        return totalMessages; 
    }
    
    public long getTotalFileSize() { 
        return totalFileSize; 
    }
    
    // Setters
    public void setTotalConversations(int totalConversations) {
        this.totalConversations = totalConversations;
    }
    
    public void setTotalMessages(int totalMessages) {
        this.totalMessages = totalMessages;
    }
    
    public void setTotalFileSize(long totalFileSize) {
        this.totalFileSize = totalFileSize;
    }
    
    public String getFormattedFileSize() {
        if (totalFileSize < 1024) return totalFileSize + " B";
        if (totalFileSize < 1024 * 1024) return String.format("%.1f KB", totalFileSize / 1024.0);
        return String.format("%.1f MB", totalFileSize / (1024.0 * 1024.0));
    }
}

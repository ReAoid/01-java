package com.chatbot.model.record;

/**
 * 记忆统计信息类
 */
public class MemoryStats {
    private final int totalMemories;
    private final int activeMemories;
    
    public MemoryStats(int totalMemories, int activeMemories) {
        this.totalMemories = totalMemories;
        this.activeMemories = activeMemories;
    }
    
    public int getTotalMemories() {
        return totalMemories;
    }
    
    public int getActiveMemories() {
        return activeMemories;
    }
}

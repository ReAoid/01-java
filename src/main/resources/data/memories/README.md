# 记忆数据存储

此目录用于存储聊天记忆数据。

## 文件结构

- `{sessionId}_memories.json` - 会话记忆数据
- `{sessionId}_summary.json` - 会话摘要数据

## 数据格式

### 记忆文件格式
```json
{
  "sessionId": "session_123",
  "memories": [
    {
      "memoryId": "memory_001",
      "content": "用户喜欢喝咖啡",
      "importance": 0.8,
      "timestamp": "2024-01-01T12:00:00Z",
      "type": "preference"
    }
  ],
  "lastUpdated": "2024-01-01T12:00:00Z"
}
```

### 摘要文件格式
```json
{
  "sessionId": "session_123",
  "summary": "用户是一名软件开发者，喜欢喝咖啡，对AI技术感兴趣。",
  "keyPoints": [
    "职业：软件开发者",
    "兴趣：AI技术",
    "偏好：咖啡"
  ],
  "lastUpdated": "2024-01-01T12:00:00Z"
}
```

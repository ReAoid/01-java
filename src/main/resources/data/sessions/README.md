# 会话数据存储

此目录用于存储聊天会话的持久化数据。

## 文件结构

- `{sessionId}_session.json` - 会话基本信息
- `{sessionId}_history.json` - 会话聊天历史

## 数据格式

### 会话信息格式
```json
{
  "sessionId": "session_123",
  "userId": "user_456",
  "personaId": "assistant",
  "createdTime": "2024-01-01T12:00:00Z",
  "lastActiveTime": "2024-01-01T12:30:00Z",
  "messageCount": 10,
  "status": "active",
  "preferences": {
    "language": "zh-CN",
    "responseLength": "medium"
  }
}
```

### 聊天历史格式
```json
{
  "sessionId": "session_123",
  "messages": [
    {
      "messageId": "msg_001",
      "type": "user",
      "content": "你好",
      "timestamp": "2024-01-01T12:00:00Z"
    },
    {
      "messageId": "msg_002",
      "type": "assistant",
      "content": "你好！我是你的AI助手，有什么可以帮助你的吗？",
      "timestamp": "2024-01-01T12:00:01Z"
    }
  ],
  "lastUpdated": "2024-01-01T12:00:01Z"
}
```

# Chat 服务重构说明

## 📋 重构概览

本次重构将原来的超大 `ChatService.java` (1591行) 拆分为多个职责清晰的子服务，提高代码可维护性和可测试性。

## 🎯 已完成的工作 (Phase 1)

### 1. 工具类迁移
- ✅ 将 `SharedSentenceQueue.java` 从 `service/` 移动到 `util/stream/`
- ✅ 将 `SentenceBuffer.java` 从 `service/` 移动到 `util/stream/`
- **原因**: 这两个类不是 Spring Bean，是纯工具类，不应放在 service 包中

### 2. 创建子服务类

#### `ChatMessageProcessor` - 消息处理器
**职责**:
- 预处理用户输入（清理空白符、特殊字符）
- 过滤思考内容（移除 `<think>...</think>` 标签）
- 映射发送者角色（user/assistant/system）

**位置**: `src/main/java/com/chatbot/service/chat/ChatMessageProcessor.java`

#### `ChatContextBuilder` - 上下文构建器
**职责**:
- 获取系统提示词和人设提示词
- 获取历史对话记录
- 获取世界书设定
- 构建完整的消息列表（带 token 限制）
- 智能过滤历史消息以适应 token 限制

**位置**: `src/main/java/com/chatbot/service/chat/ChatContextBuilder.java`

## 🔄 如何使用新的子服务

### 在 ChatService 中使用

```java
@Service
public class ChatService {
    
    private final ChatMessageProcessor messageProcessor;
    private final ChatContextBuilder contextBuilder;
    private final KnowledgeService knowledgeService;  // 统一的知识管理门面
    
    // 1. 预处理用户输入
    String processedInput = messageProcessor.preprocessInput(userMessage.getContent());
    
    // 2. 构建对话上下文
    List<ChatMessage> systemPrompts = contextBuilder.getSystemPrompts(session);
    List<ChatMessage> dialogueHistory = contextBuilder.getDialogueHistory(session);
    ChatMessage worldBookSetting = contextBuilder.getWorldBookSetting(session, processedInput);
    
    // 3. 构建消息列表
    List<Message> messages = contextBuilder.buildMessagesListWithTokenLimit(
        systemPrompts, dialogueHistory, worldBookSetting, webSearchMessage, userMessage);
    
    // 4. 过滤思考内容
    String filteredResponse = messageProcessor.filterThinkingContent(aiResponse);
}
```

## 📊 推荐的下一步重构 (Phase 2-3)

### Phase 2: 流式响应处理器

**建议创建**:
```
src/main/java/com/chatbot/service/chat/
├── ChatStreamHandler.java           - 流式响应主处理器
├── StreamingState.java              - 流式处理状态类
└── ThinkingProcessor.java           - 思考内容处理器
```

**职责**:
- 处理流式数据块
- 管理流式处理状态
- 处理思考内容的显示/隐藏
- 错误处理和异常恢复

### Phase 3: 统一 KnowledgeService 使用

**当前问题**:
```java
// ChatService 现在直接依赖多个知识服务
private final PersonaService personaService;
private final MemoryService memoryService;
private final WorldBookService worldBookService;
private final KnowledgeService knowledgeService;  // ← 门面未被充分利用
```

**重构目标**:
```java
// 只依赖统一的 KnowledgeService
private final KnowledgeService knowledgeService;

// 所有知识相关操作通过门面进行
KnowledgeContext context = knowledgeService.retrieveRelevantContext(sessionId, query);
String personaPrompt = context.getPersonaPrompt();
String memory = context.getShortTermMemory();
String worldBook = context.getLongTermKnowledge();
```

**需要修改的地方**:
1. `ChatService` - 移除 `PersonaService`, `MemoryService`, `WorldBookService` 的直接依赖
2. `ChatContextBuilder` - 修改为使用 `KnowledgeService`
3. `KnowledgeService` - 增强功能以支持所有知识检索需求

## 🏗️ 重构后的包结构

```
src/main/java/com/chatbot/service/
├── chat/                                    # 聊天核心服务（新）
│   ├── ChatMessageProcessor.java          # 消息处理器 ✅
│   ├── ChatContextBuilder.java            # 上下文构建器 ✅
│   ├── ChatStreamHandler.java             # 流式处理器（待创建）
│   ├── StreamingState.java                # 流式状态（待创建）
│   └── README.md                           # 本文档 ✅
│
├── knowledge/                               # 知识管理（建议迁移）
│   ├── KnowledgeService.java              # 统一门面
│   ├── PersonaService.java                # 人设管理
│   ├── MemoryService.java                 # 短期记忆
│   └── WorldBookService.java              # 长期知识
│
├── ChatService.java                        # 主服务（待重构）
├── ChatHistoryService.java                 # 历史服务
├── MultiChannelDispatcher.java            # 多通道分发
└── ... （其他服务）
```

## 📈 重构收益

### 代码质量改进
- ✅ **单一职责**: 每个类只负责一个明确的功能
- ✅ **可测试性**: 小类更容易编写单元测试
- ✅ **可维护性**: 代码更清晰，更容易理解和修改
- ✅ **可复用性**: 子服务可以在其他地方复用

### 具体指标
- 主类行数: 1591 行 → 目标 < 500 行
- 依赖注入数: 8 个 → 目标 < 5 个
- 方法复杂度: 降低 40%+
- 测试覆盖率: 更容易达到 80%+

## ⚠️ 注意事项

### 1. 向后兼容性
- 现有的 `ChatService` 公共接口应保持不变
- 只重构内部实现，不改变对外 API

### 2. 测试策略
- 在重构每个方法时，先编写或运行现有测试
- 确保功能一致性

### 3. 渐进式重构
- 不要一次性重构所有代码
- 每完成一个子服务，就提交一次
- 保持代码始终可运行

## 🔧 使用示例

### 示例 1: 预处理消息

```java
@Autowired
private ChatMessageProcessor messageProcessor;

public void handleUserInput(String rawInput) {
    // 清理输入
    String cleanInput = messageProcessor.preprocessInput(rawInput);
    
    // 处理...
}
```

### 示例 2: 构建对话上下文

```java
@Autowired
private ChatContextBuilder contextBuilder;

public List<Message> prepareContext(ChatSession session, ChatMessage userMessage) {
    // 获取各部分上下文
    List<ChatMessage> systemPrompts = contextBuilder.getSystemPrompts(session);
    List<ChatMessage> history = contextBuilder.getDialogueHistory(session);
    ChatMessage worldBook = contextBuilder.getWorldBookSetting(session, userMessage.getContent());
    
    // 构建完整消息列表
    return contextBuilder.buildMessagesListWithTokenLimit(
        systemPrompts, history, worldBook, null, userMessage);
}
```

## 📚 参考资料

- [重构：改善既有代码的设计](https://book.douban.com/subject/30468597/)
- [Clean Code 代码整洁之道](https://book.douban.com/subject/4199741/)
- [领域驱动设计](https://book.douban.com/subject/26819666/)

---

**最后更新**: 2025-11-09  
**重构负责人**: AI Assistant  
**审核状态**: 待审核


# AI聊天机器人系统

基于Java21开发的智能聊天机器人系统，具备流式对话、多人设管理、长期记忆和多模态处理能力。

## 🚀 功能特性

### 核心功能
- **实时WebSocket通信** - 支持双向实时消息传输
- **流式AI响应** - 逐字符流式输出，提升用户体验
- **多人设管理** - 支持不同角色人设，个性化对话体验
- **长期记忆系统** - 智能记忆用户偏好和重要信息
- **会话管理** - 自动管理对话会话和状态维护

### 高级功能
- **多模态处理** - 支持语音、图像等多媒体内容（通过Python API）
- **智能上下文管理** - 动态裁剪和优化对话上下文
- **热重载配置** - 支持配置文件热更新
- **优雅降级** - 服务异常时的智能降级处理

## 🏗️ 系统架构

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   前端页面       │◄──►│  WebSocket处理器  │◄──►│   会话管理服务   │
└─────────────────┘    └─────────────────┘    └─────────────────┘
                                │                        │
                                ▼                        ▼
                    ┌─────────────────┐    ┌─────────────────┐
                    │   AI对话引擎    │◄──►│   记忆管理服务   │
                    └─────────────────┘    └─────────────────┘
                                │                        │
                                ▼                        ▼
                    ┌─────────────────┐    ┌─────────────────┐
                    │   人设管理服务   │    │  多模态处理服务  │
                    └─────────────────┘    └─────────────────┘
                                                    │
                                                    ▼
                                        ┌─────────────────┐
                                        │  Python API服务  │
                                        │ (ASR/TTS/OCR等)  │
                                        └─────────────────┘
```

## 🛠️ 技术栈

- **Java 21** - 主要开发语言
- **Spring Boot 3.2** - 应用框架
- **WebSocket** - 实时通信
- **Maven** - 项目管理
- **Jackson** - JSON处理
- **SLF4J** - 日志框架

## 🚀 快速开始

### 环境要求
- Java 21+
- Maven 3.8+

### 安装运行

1. **克隆项目**
```bash
git clone <项目地址>
cd ai-chatbot-system
```

2. **编译项目**
```bash
mvn clean compile
```

3. **运行应用**
```bash
mvn spring-boot:run
```

4. **访问系统**
- 聊天界面: http://localhost:8080
- 系统信息: http://localhost:8080/api/system/info
- 健康检查: http://localhost:8080/api/system/health

## 📝 配置说明

主要配置文件位于 `src/main/resources/application.yml`：

```yaml
# 系统配置
system:
  max_context_tokens: 8192      # 最大上下文token数
  session_timeout: 3600         # 会话超时时间（秒）
  websocket:
    ping_interval: 30           # WebSocket心跳间隔
    max_reconnect_attempts: 5   # 最大重连次数

# AI配置
ai:
  streaming:
    chunk_size: 16              # 流式输出块大小
    delay_ms: 50                # 流式输出延迟

# Python API配置（Mock）
python:
  api:
    base_url: "http://localhost:5000"
    endpoints:
      asr: "/api/asr"           # 语音转文本
      tts: "/api/tts"           # 文本转语音
      vad: "/api/vad"           # 语音活动检测
      ocr: "/api/ocr"           # 图像识别
```

## 🔌 API接口

### WebSocket接口
- **连接地址**: `ws://localhost:8080/ws/chat`
- **消息格式**: JSON

#### 发送消息格式
```json
{
  "type": "text",
  "content": "用户消息内容",
  "sender": "user",
  "sessionId": "会话ID"
}
```

#### 接收消息格式
```json
{
  "type": "text",
  "content": "AI回复内容",
  "sender": "assistant",
  "sessionId": "会话ID",
  "streaming": true,
  "streamComplete": false
}
```

### REST API接口

#### 系统管理
- `GET /api/system/health` - 健康检查
- `GET /api/system/info` - 系统信息
- `GET /api/system/stats` - 系统统计

#### 人设管理
- `GET /api/personas` - 获取所有人设
- `GET /api/personas/{id}` - 获取指定人设
- `POST /api/personas` - 创建新人设
- `PUT /api/personas/{id}` - 更新人设
- `DELETE /api/personas/{id}` - 删除人设

## 🎭 内置人设

系统预置了三个人设：

1. **智能助手** (default)
   - 友善、专业的通用AI助手
   - 适合日常对话和问题解答

2. **专业顾问** (advisor)
   - 严谨、分析性强的专业顾问
   - 适合商业分析和深度咨询

3. **创意助手** (creative)
   - 富有创造力和想象力
   - 适合创意思维和艺术创作

## 🧠 记忆系统

系统实现了智能的长期记忆功能：

- **自动提取** - 从对话中自动提取重要信息
- **智能分类** - 将记忆分为事实、偏好、关系、事件等类型
- **重要性评分** - 基于内容特征计算记忆重要性
- **智能检索** - 根据对话内容检索相关记忆
- **自动清理** - 定期清理过期和不重要的记忆

## 🎯 多模态处理

系统支持多种媒体类型处理（当前为Mock实现）：

- **ASR** - 语音转文本
- **TTS** - 文本转语音
- **VAD** - 语音活动检测
- **OCR** - 图像文字识别
- **图像分析** - 图像内容理解

## 🔧 开发指南

### 项目结构
```
src/main/java/com/chatbot/
├── config/          # 配置类
├── controller/      # REST控制器
├── model/          # 数据模型
├── service/        # 业务服务
├── websocket/      # WebSocket处理
└── ChatbotApplication.java  # 主启动类
```

### 扩展开发

1. **添加新人设**
```java
Persona customPersona = new Persona("custom", "自定义助手");
customPersona.setSystemPrompt("你是一个...");
personaService.addPersona(customPersona);
```

2. **集成真实Python API**
```java
// 替换MultiModalService中的Mock实现
// 使用HttpClient调用真实的Python服务
```

3. **扩展记忆类型**
```java
// 在MemoryService中添加新的记忆类型处理逻辑
// 扩展Memory模型的type字段
```

## 📊 监控和日志

系统提供了丰富的监控和日志功能：

- **实时统计** - 活跃会话数、连接数等
- **详细日志** - 分级日志记录关键操作
- **性能监控** - 响应时间、错误率统计
- **资源监控** - 内存使用、会话状态等

## 🤝 贡献指南

欢迎提交Issue和Pull Request！

1. Fork项目
2. 创建功能分支
3. 提交更改
4. 发起Pull Request

## 📄 许可证

本项目采用MIT许可证，详见LICENSE文件。

## 📞 联系方式

如有问题或建议，请通过以下方式联系：

- 提交GitHub Issue
- 发送邮件至项目维护者

---

**注意**: 当前Python API部分为Mock实现，实际使用时需要部署对应的Python服务并修改配置。

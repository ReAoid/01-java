<template>
  <div class="chat-view">
    <!-- 左侧会话列表 -->
    <aside class="sessions-sidebar" :class="{ collapsed: !showSessions }">
      <div class="sessions-header">
        <h2>会话历史</h2>
        <button @click="createNewSession" class="icon-btn" title="新建会话">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <line x1="12" y1="5" x2="12" y2="19"></line>
            <line x1="5" y1="12" x2="19" y2="12"></line>
          </svg>
        </button>
      </div>
      
      <div class="sessions-list">
        <div 
          v-for="session in sessions" 
          :key="session.id"
          :class="['session-item', { active: currentSessionId === session.id }]"
          @click="switchSession(session.id)"
        >
          <div class="session-info">
            <div class="session-title">{{ session.title || '新会话' }}</div>
            <div class="session-time">{{ formatSessionTime(session.timestamp) }}</div>
          </div>
          <button @click.stop="deleteSession(session.id)" class="delete-btn" title="删除">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <polyline points="3 6 5 6 21 6"></polyline>
              <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path>
            </svg>
          </button>
        </div>
      </div>
    </aside>

    <!-- 主聊天区域 -->
    <div class="chat-main">
      <!-- 顶部工具栏 -->
      <header class="chat-header">
        <button @click="showSessions = !showSessions" class="icon-btn toggle-sessions">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <line x1="3" y1="12" x2="21" y2="12"></line>
            <line x1="3" y1="6" x2="21" y2="6"></line>
            <line x1="3" y1="18" x2="21" y2="18"></line>
          </svg>
        </button>
        
        <div class="chat-title">
          <h1>AiChat</h1>
        </div>

        <div class="header-actions">
          <button class="icon-btn" title="More">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="12" cy="12" r="1"></circle>
              <circle cx="12" cy="5" r="1"></circle>
              <circle cx="12" cy="19" r="1"></circle>
            </svg>
          </button>
        </div>
      </header>

      <!-- 消息区域 -->
      <div class="messages-container" ref="messagesContainer">
        <div class="messages-wrapper">
          <!-- 欢迎消息 -->
          <div v-if="messages.length === 0" class="welcome-page">
            <div class="welcome-header">
              <div class="welcome-logo">
                <img src="@/assets/favicon.png" alt="AiChat Logo" class="welcome-logo-img" />
              </div>
              <h1>AiChat</h1>
              <p class="welcome-subtitle">Have fun!</p>
            </div>

          </div>

          <!-- 消息列表 -->
          <div 
            v-for="msg in messages" 
            :key="msg.id" 
            :class="['message-wrapper', msg.role]"
          >
            <div class="message-avatar">
              <img v-if="msg.role === 'user'" src="@/assets/user-avatar.jpg" alt="User" />
              <img v-else src="https://api.dicebear.com/7.x/bottts/svg?seed=CogniChat&backgroundColor=8b5cf6" alt="AI" />
            </div>
            <div class="message-bubble">
              <div class="message-content" v-html="msg.content"></div>
              <div class="message-footer">
                <span class="message-time">Just now</span>
                <div class="message-actions">
                  <button v-if="msg.role === 'user'" class="action-btn edit-btn" title="Edit">Edit</button>
                  <button v-if="msg.role === 'assistant'" class="action-btn copy-btn" title="Copy">Copy</button>
                  <button v-if="msg.role === 'assistant'" class="reaction-btn" title="React">😍</button>
                  <button v-if="msg.role === 'assistant'" class="reaction-btn" title="React">😊</button>
                </div>
              </div>
            </div>
          </div>
          
          <!-- 打字提示 -->
          <div v-if="isLoading" class="message-wrapper assistant">
            <div class="message-avatar">
              <span>🤖</span>
            </div>
            <div class="message-bubble typing">
              <div class="typing-indicator">
                <span class="dot"></span>
                <span class="dot"></span>
                <span class="dot"></span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 输入区域 -->
      <footer class="chat-footer">
        <div class="input-wrapper">
          <textarea
            v-model="inputMessage"
            @keydown.enter.exact.prevent="handleSend"
            @keydown.enter.shift.exact="inputMessage += '\n'"
            placeholder="Send a message"
            rows="1"
            class="message-input"
            ref="messageInput"
          ></textarea>
          
          <div class="input-actions">
            <button 
              class="send-btn" 
              @click="handleSend"
              :disabled="isLoading || !inputMessage.trim()"
              :class="{ active: inputMessage.trim() }"
            >
              <svg v-if="!isLoading" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <line x1="22" y1="2" x2="11" y2="13"></line>
                <polygon points="22 2 15 22 11 13 2 9 22 2"></polygon>
              </svg>
              <span v-else class="loading"></span>
            </button>
            
            <button class="voice-btn" title="Voice input">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M12 1a3 3 0 0 0-3 3v8a3 3 0 0 0 6 0V4a3 3 0 0 0-3-3z"></path>
                <path d="M19 10v2a7 7 0 0 1-14 0v-2"></path>
                <line x1="12" y1="19" x2="12" y2="23"></line>
                <line x1="8" y1="23" x2="16" y2="23"></line>
              </svg>
            </button>
          </div>
        </div>
      </footer>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick, watch } from 'vue'
import { chatApi, personaApi } from '@/api/chatApi'

const messages = ref([])
const inputMessage = ref('')
const isLoading = ref(false)
const currentPersona = ref('Assistant')
const personas = ref([])
const sessions = ref([])
const currentSessionId = ref(null)
const messagesContainer = ref(null)
const messageInput = ref(null)
const showSessions = ref(false)

// 加载角色列表
const loadPersonas = async () => {
  try {
    const data = await personaApi.getAllPersonas()
    personas.value = data.personas || []
    if (personas.value.length > 0) {
      currentPersona.value = personas.value[0]
    }
  } catch (error) {
    console.error('加载角色失败:', error)
  }
}

// 切换角色
const handlePersonaChange = async () => {
  try {
    await personaApi.switchPersona(currentPersona.value)
    console.log('角色切换成功:', currentPersona.value)
  } catch (error) {
    console.error('切换角色失败:', error)
  }
}

// 发送消息
const handleSend = async () => {
  if (!inputMessage.value.trim() || isLoading.value) return

  const userMessage = inputMessage.value.trim()
  inputMessage.value = ''

  // 添加用户消息
  messages.value.push({
    id: Date.now(),
    role: 'user',
    content: userMessage,
    timestamp: new Date()
  })

  scrollToBottom()
  isLoading.value = true

  try {
    const response = await chatApi.sendMessage({
      message: userMessage,
      sessionId: currentSessionId.value,
      personaName: currentPersona.value
    })

    // 添加AI回复
    messages.value.push({
      id: Date.now() + 1,
      role: 'assistant',
      content: response.reply || response.message,
      timestamp: new Date()
    })

    scrollToBottom()
  } catch (error) {
    console.error('发送消息失败:', error)
    messages.value.push({
      id: Date.now() + 1,
      role: 'assistant',
      content: '抱歉,发生了错误: ' + error,
      timestamp: new Date()
    })
  } finally {
    isLoading.value = false
  }
}

// 快速发送消息
const sendQuickMessage = (message) => {
  inputMessage.value = message
  handleSend()
}

// 创建新会话
const createNewSession = async () => {
  try {
    const response = await chatApi.createSession(currentPersona.value)
    currentSessionId.value = response.sessionId
    messages.value = []
    await loadSessions()
  } catch (error) {
    console.error('创建会话失败:', error)
  }
}

// 加载会话列表
const loadSessions = async () => {
  try {
    const data = await chatApi.getAllSessions()
    sessions.value = data.sessions || []
  } catch (error) {
    console.error('加载会话失败:', error)
  }
}

// 切换会话
const switchSession = async (sessionId) => {
  currentSessionId.value = sessionId
  try {
    const data = await chatApi.getChatHistory(sessionId)
    messages.value = data.messages || []
    scrollToBottom()
  } catch (error) {
    console.error('加载会话历史失败:', error)
  }
}

// 删除会话
const deleteSession = async (sessionId) => {
  if (!confirm('确定要删除这个会话吗?')) return
  
  try {
    await chatApi.deleteSession(sessionId)
    await loadSessions()
    if (currentSessionId.value === sessionId) {
      currentSessionId.value = null
      messages.value = []
    }
  } catch (error) {
    console.error('删除会话失败:', error)
  }
}

// 滚动到底部
const scrollToBottom = async () => {
  await nextTick()
  if (messagesContainer.value) {
    messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
  }
}

// 格式化时间
const formatTime = (date) => {
  if (!(date instanceof Date)) date = new Date(date)
  return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}

// 格式化会话时间
const formatSessionTime = (timestamp) => {
  if (!timestamp) return ''
  const date = new Date(timestamp)
  const now = new Date()
  const diff = now - date
  const days = Math.floor(diff / (1000 * 60 * 60 * 24))
  
  if (days === 0) return '今天'
  if (days === 1) return '昨天'
  if (days < 7) return `${days}天前`
  return date.toLocaleDateString('zh-CN', { month: '2-digit', day: '2-digit' })
}

// 监听输入框变化，自动调整高度
watch(inputMessage, () => {
  if (messageInput.value) {
    messageInput.value.style.height = 'auto'
    messageInput.value.style.height = messageInput.value.scrollHeight + 'px'
  }
})

onMounted(() => {
  loadPersonas()
  loadSessions()
  createNewSession()
})
</script>

<style scoped>
.chat-view {
  display: flex;
  height: 100%;
  width: 100%;
  overflow: hidden;
}

/* ============ 左侧会话列表 ============ */
.sessions-sidebar {
  width: 280px;
  background: var(--bg-secondary);
  border-right: 1px solid var(--border-light);
  display: none; /* 默认隐藏，匹配设计图 */
  flex-direction: column;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  overflow: hidden;
}

.sessions-sidebar:not(.collapsed) {
  display: flex;
}

.sessions-sidebar.collapsed {
  display: none;
}

.sessions-header {
  padding: 20px;
  border-bottom: 1px solid var(--border-light);
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.sessions-header h2 {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0;
}

.icon-btn {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  background: var(--bg-tertiary);
  border: none;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s ease;
  color: var(--text-secondary);
  flex-shrink: 0;
}

.icon-btn:hover {
  background: var(--primary-color);
  color: white;
  transform: scale(1.05);
}

.icon-img {
  width: 20px;
  height: 20px;
  object-fit: contain;
  display: block;
}

.sessions-list {
  flex: 1;
  overflow-y: auto;
  padding: 12px;
}

.session-item {
  padding: 12px 16px;
  border-radius: 12px;
  margin-bottom: 8px;
  cursor: pointer;
  transition: all 0.2s ease;
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: var(--bg-primary);
  border: 1px solid transparent;
}

.session-item:hover {
  background: var(--bg-tertiary);
  border-color: var(--border-color);
}

.session-item.active {
  background: var(--sidebar-active);
  border-color: var(--primary-color);
}

.session-info {
  flex: 1;
  min-width: 0;
}

.session-title {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  margin-bottom: 4px;
}

.session-time {
  font-size: 12px;
  color: var(--text-tertiary);
}

.delete-btn {
  width: 28px;
  height: 28px;
  border-radius: 6px;
  background: transparent;
  border: none;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: all 0.2s ease;
  color: var(--text-tertiary);
  flex-shrink: 0;
}

.session-item:hover .delete-btn {
  opacity: 1;
}

.delete-btn:hover {
  background: #fee;
  color: #f44;
}

/* ============ 主聊天区域 ============ */
.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: var(--bg-primary);
  overflow: hidden;
  width: 100%;
}

/* 顶部工具栏 */
.chat-header {
  height: 60px;
  width: 100%;
  background: transparent;
  border-bottom: 1px solid var(--border-light);
  display: flex;
  align-items: center;
  padding: 0 24px;
  gap: 16px;
  flex-shrink: 0;
}

.toggle-sessions {
  margin-right: 8px;
}

.chat-title {
  flex: 1;
}

.chat-title h1 {
  font-size: 16px;
  font-weight: 400;
  color: var(--text-primary);
  margin: 0;
  opacity: 0.9;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}


/* 消息容器 */
.messages-container {
  flex: 1;
  width: 100%;
  overflow-y: auto;
  scroll-behavior: smooth;
  padding: 24px;
}

.messages-wrapper {
  width: 100%;
  max-width: 1100px;
  margin: 0 auto;
}

/* 欢迎页面 */
.welcome-page {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 40px;
  animation: fadeIn 0.8s ease;
  width: 100%;
}

.welcome-header {
  text-align: center;
  margin-bottom: 48px;
}

.welcome-logo {
  margin-bottom: 24px;
  animation: float 3s ease-in-out infinite;
}

.welcome-logo-img {
  width: 64px;
  height: 64px;
  object-fit: contain;
  display: block;
}

.welcome-header h1 {
  font-size: 48px;
  font-weight: 700;
  color: var(--text-primary);
  margin-bottom: 16px;
  letter-spacing: -0.5px;
}

.welcome-subtitle {
  font-size: 16px;
  color: var(--text-secondary);
  line-height: 1.6;
  max-width: 600px;
  margin: 0 auto;
}

/* 功能卡片 */
.feature-cards {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
  width: 100%;
}

.feature-card {
  background: var(--card-bg);
  border: 1px solid var(--border-color);
  border-radius: 16px;
  padding: 24px;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  backdrop-filter: blur(10px);
}

.feature-card:hover {
  background: var(--card-hover);
  transform: translateY(-4px);
  box-shadow: var(--shadow-lg);
  border-color: var(--primary-color);
}

.feature-icon {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  background: var(--primary-gradient);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 16px;
  color: white;
}

.feature-card h3 {
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 16px;
}

.feature-items {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.feature-item {
  padding: 12px 16px;
  background: var(--bg-secondary);
  border: 1px solid var(--border-light);
  border-radius: 10px;
  font-size: 14px;
  color: var(--text-secondary);
  line-height: 1.5;
  transition: all 0.2s ease;
  cursor: pointer;
}

.feature-item:hover {
  background: var(--bg-tertiary);
  border-color: var(--primary-color);
  color: var(--text-primary);
}

/* 消息气泡 */
.message-wrapper {
  display: flex;
  gap: 12px;
  margin-bottom: 24px;
  animation: slideIn 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  width: 100%;
  max-width: 100%;
}

@keyframes slideIn {
  from {
    opacity: 0;
    transform: translateY(15px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.message-wrapper.user {
  flex-direction: row-reverse;
  justify-content: flex-start;
}

.message-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: var(--bg-tertiary);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  overflow: hidden;
}

.message-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.message-bubble {
  flex: 1;
  max-width: 85%;
  background: var(--bg-secondary);
  border-radius: 16px;
  padding: 20px 24px;
  box-shadow: var(--shadow-sm);
  position: relative;
  transition: all 0.3s ease;
}

.message-bubble:hover {
  box-shadow: var(--shadow-md);
}

.message-content {
  line-height: 1.7;
  color: var(--text-primary);
  font-size: 15px;
  word-wrap: break-word;
  margin-bottom: 16px;
}

.message-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 12px;
  border-top: 1px solid var(--border-light);
}

.message-time {
  font-size: 13px;
  color: var(--text-tertiary);
  font-weight: 400;
}

.message-actions {
  display: flex;
  gap: 8px;
  align-items: center;
}

.action-btn,
.reaction-btn {
  padding: 6px 14px;
  background: var(--bg-tertiary);
  border: 1px solid var(--border-color);
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s ease;
  font-size: 13px;
  color: var(--text-secondary);
  font-weight: 500;
}

.reaction-btn {
  padding: 4px 8px;
  font-size: 16px;
  background: transparent;
  border: none;
}

.action-btn:hover {
  background: var(--sidebar-hover);
  color: var(--text-primary);
  border-color: var(--primary-color);
}

.reaction-btn:hover {
  transform: scale(1.2);
}

/* 打字指示器 */
.message-bubble.typing {
  background: var(--assistant-message-bg);
  padding: 18px 24px;
}

.typing-indicator {
  display: flex;
  gap: 6px;
  align-items: center;
}

.dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--text-tertiary);
  animation: bounce 1.4s infinite ease-in-out;
}

.dot:nth-child(1) {
  animation-delay: -0.32s;
}

.dot:nth-child(2) {
  animation-delay: -0.16s;
}

@keyframes bounce {
  0%, 80%, 100% {
    transform: scale(0);
    opacity: 0.5;
  }
  40% {
    transform: scale(1);
    opacity: 1;
  }
}

/* 输入区域 */
.chat-footer {
  width: 100%;
  background: transparent;
  padding: 20px 24px 24px;
  flex-shrink: 0;
}

.input-wrapper {
  width: 100%;
  max-width: 1100px;
  margin: 0 auto;
  display: flex;
  gap: 12px;
  align-items: flex-end;
  background: var(--bg-secondary);
  border: 2px solid var(--border-color);
  border-radius: 16px;
  padding: 14px 16px;
  transition: all 0.3s ease;
  box-shadow: var(--shadow-md);
}

.input-wrapper:focus-within {
  border-color: var(--primary-color);
  box-shadow: 0 0 0 4px rgba(255, 153, 102, 0.2);
}

.message-input {
  flex: 1;
  border: none;
  background: transparent;
  resize: none;
  font-size: 15px;
  line-height: 1.6;
  color: var(--text-primary);
  max-height: 150px;
  min-height: 24px;
  overflow-y: auto;
  font-family: inherit;
  padding: 0;
}

.message-input:focus {
  outline: none;
}

.message-input::placeholder {
  color: var(--text-tertiary);
}

.input-actions {
  display: flex;
  gap: 8px;
  align-items: center;
}

.send-btn,
.voice-btn {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  background: var(--bg-tertiary);
  border: none;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  color: var(--text-secondary);
  flex-shrink: 0;
}

.send-btn.active {
  background: var(--primary-gradient);
  color: white;
  box-shadow: var(--shadow-sm);
}

.send-btn.active:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
}

.voice-btn:hover {
  background: var(--sidebar-hover);
  color: var(--primary-color);
  transform: scale(1.05);
}

.send-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
  transform: none !important;
}

/* 响应式设计 */
@media (max-width: 1024px) {
  .sessions-sidebar {
    width: 240px;
  }
  
  .feature-cards {
    grid-template-columns: 1fr;
    gap: 16px;
  }
}

@media (max-width: 768px) {
  .sessions-sidebar:not(.collapsed) {
    position: absolute;
    left: 0;
    top: 0;
    height: 100%;
    z-index: 100;
    box-shadow: var(--shadow-xl);
  }
  
  .chat-header {
    padding: 0 16px;
  }
  
  .messages-wrapper {
    padding: 16px;
  }
  
  .message-bubble {
    max-width: 95%;
    padding: 16px 20px;
  }
  
  .message-content {
    font-size: 14px;
  }
  
  .message-footer {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
  }
  
  .message-actions {
    width: 100%;
    justify-content: flex-end;
  }
  
  .chat-footer {
    padding: 16px;
  }
  
  .welcome-page {
    padding: 40px 20px;
  }
  
  .welcome-header h1 {
    font-size: 32px;
  }
  
  .welcome-subtitle {
    font-size: 14px;
  }
  
  .feature-cards {
    grid-template-columns: 1fr;
  }
  
  .feature-card {
    padding: 20px;
  }
}
</style>


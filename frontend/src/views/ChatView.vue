<template>
  <div class="chat-view">
    <!-- 主聊天区域 -->
    <div class="chat-main">
      <!-- 顶部工具栏 -->
      <header class="chat-header">
        <div class="chat-title">
          <h1>AiChat</h1>
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
          
          <button 
            class="send-btn" 
            @click="handleSend"
            :disabled="isLoading || !inputMessage.trim()"
            :class="{ active: inputMessage.trim() }"
          >
            <svg v-if="!isLoading" width="22" height="22" viewBox="0 0 24 24" fill="currentColor">
              <path d="M2.01 21L23 12 2.01 3 2 10l15 2-15 2z"/>
            </svg>
            <span v-else class="loading"></span>
          </button>
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
const currentSessionId = ref(null)
const messagesContainer = ref(null)
const messageInput = ref(null)

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


// 监听输入框变化，自动调整高度
watch(inputMessage, () => {
  if (messageInput.value) {
    messageInput.value.style.height = 'auto'
    messageInput.value.style.height = messageInput.value.scrollHeight + 'px'
  }
})

onMounted(() => {
  loadPersonas()
})
</script>

<style scoped>
.chat-view {
  display: flex;
  height: 100%;
  width: 100%;
  overflow: hidden;
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

.chat-title {
  flex: 1;
  text-align: center;
}

.chat-title h1 {
  font-size: 16px;
  font-weight: 400;
  color: var(--text-primary);
  margin: 0;
  opacity: 0.9;
}


/* 消息容器 */
.messages-container {
  flex: 1;
  width: 100%;
  overflow-y: auto;
  scroll-behavior: smooth;
}

.messages-wrapper {
  width: 100%;
  padding: 24px;
}

/* 欢迎页面 */
.welcome-page {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 100%;
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
  display: inline-block;
}

.welcome-logo-img {
  width: 80px;
  height: 80px;
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
  display: flex;
  gap: 12px;
  align-items: flex-end;
  background: var(--bg-secondary);
  border: 2px solid var(--border-color);
  border-radius: 16px;
  padding: 12px 16px;
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
  line-height: 1.5;
  color: var(--text-primary);
  max-height: 150px;
  min-height: 36px;
  overflow-y: auto;
  font-family: inherit;
  padding: 6px 0;
}

.message-input:focus {
  outline: none;
}

.message-input::placeholder {
  color: var(--text-tertiary);
}

.send-btn {
  width: 36px;
  height: 36px;
  min-height: 36px;
  border-radius: 8px;
  background: transparent;
  border: 1px solid var(--border-color);
  cursor: not-allowed;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  color: var(--text-tertiary);
  flex-shrink: 0;
  align-self: flex-end;
  opacity: 0.5;
}

.send-btn.active {
  background: var(--primary-gradient);
  color: white;
  box-shadow: var(--shadow-sm);
  border: none;
  cursor: pointer;
  opacity: 1;
}

.send-btn.active svg {
  color: white;
  fill: white;
}

.send-btn.active:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
}

.send-btn:disabled {
  opacity: 0.3;
  cursor: not-allowed;
  transform: none !important;
}

/* 响应式设计 */
@media (max-width: 1024px) {
  .feature-cards {
    grid-template-columns: 1fr;
    gap: 16px;
  }
}

@media (max-width: 768px) {
  
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


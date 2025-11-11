<template>
  <div class="page-container">
    <div class="content-card">
      <!-- 卡片头部 -->
      <div class="card-header">
        <h1>💬 AI聊天助手</h1>
        <div class="header-controls">
          <select v-model="currentPersona" @change="handlePersonaChange" class="persona-select">
            <option v-for="persona in personas" :key="persona" :value="persona">
              {{ persona }}
            </option>
          </select>
          <button @click="createNewSession" class="new-session-btn" title="新建会话">
            ➕ 新建
          </button>
        </div>
      </div>

      <!-- 消息列表 -->
      <div class="card-body chat-messages" ref="messagesContainer">
        <div 
          v-for="msg in messages" 
          :key="msg.id" 
          :class="['message', msg.role]"
          v-html="msg.content"
        >
        </div>
        
        <!-- 打字提示 -->
        <div v-if="isLoading" class="typing-indicator">
          <span class="dots">{{ currentPersona }} 正在思考...</span>
        </div>
      </div>

      <!-- 输入区域 -->
      <div class="card-footer input-area">
        <textarea
          v-model="inputMessage"
          @keydown.enter.exact.prevent="handleSend"
          @keydown.enter.shift.exact="inputMessage += '\n'"
          placeholder="输入消息... (Enter发送, Shift+Enter换行)"
          rows="3"
          class="message-input"
        ></textarea>
        <div class="input-actions">
          <button 
            class="primary send-btn" 
            @click="handleSend"
            :disabled="isLoading || !inputMessage.trim()"
          >
            <span v-if="!isLoading">📤 发送</span>
            <span v-else><span class="loading"></span> 发送中...</span>
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import { chatApi, personaApi } from '@/api/chatApi'

const messages = ref([])
const inputMessage = ref('')
const isLoading = ref(false)
const currentPersona = ref('Assistant')
const personas = ref([])
const sessions = ref([])
const currentSessionId = ref(null)
const messagesContainer = ref(null)

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

onMounted(() => {
  loadPersonas()
  loadSessions()
  createNewSession()
})
</script>

<style scoped>
/* 头部控制栏 */
.header-controls {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 10px;
}

.persona-select {
  background: rgba(255, 255, 255, 0.2);
  color: white;
  border: 1px solid rgba(255, 255, 255, 0.3);
  padding: 8px 15px;
  border-radius: 6px;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.persona-select:hover {
  background: rgba(255, 255, 255, 0.3);
}

.persona-select option {
  background: white;
  color: #333;
}

.new-session-btn {
  background: rgba(255, 255, 255, 0.2);
  color: white;
  border: 1px solid rgba(255, 255, 255, 0.3);
  padding: 8px 15px;
  border-radius: 6px;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.new-session-btn:hover {
  background: rgba(255, 255, 255, 0.3);
}

/* 消息区域 */
.chat-messages {
  padding: 20px;
  overflow-y: auto;
  background: white;
}

.message {
  margin-bottom: 15px;
  padding: 10px 15px;
  border-radius: 18px;
  max-width: 70%;
  word-wrap: break-word;
  line-height: 1.6;
  animation: fadeIn 0.3s ease;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.message.user {
  background: #007bff;
  color: white;
  margin-left: auto;
  text-align: right;
}

.message.assistant {
  background: #f1f3f4;
  color: #333;
  margin-right: auto;
}

.message.system {
  background: #fff3cd;
  color: #856404;
  text-align: center;
  max-width: 100%;
  margin: 0 auto;
}

.message.error {
  background: #f8d7da;
  color: #721c24;
  text-align: center;
  max-width: 100%;
  margin: 0 auto;
}

/* 打字提示动画 */
.typing-indicator {
  background: #f1f3f4;
  color: #666;
  padding: 10px 15px;
  border-radius: 18px;
  max-width: 200px;
  font-style: italic;
  animation: pulse 1.5s infinite;
}

.typing-indicator .dots::after {
  content: '...';
  animation: dots 1.5s steps(4, end) infinite;
}

@keyframes dots {
  0%, 20% { content: '.'; }
  40% { content: '..'; }
  60%, 100% { content: '...'; }
}

/* 输入区域 */
.input-area {
  background: white;
  border-top: 1px solid #eee;
  padding: 20px;
}

.message-input {
  width: 100%;
  padding: 12px 15px;
  border: 2px solid #e5e7eb;
  border-radius: 8px;
  font-size: 14px;
  font-family: inherit;
  resize: none;
  transition: border-color 0.3s;
  margin-bottom: 10px;
}

.message-input:focus {
  outline: none;
  border-color: #667eea;
}

.input-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.send-btn {
  padding: 10px 25px;
  font-size: 14px;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 5px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .content-card {
    width: 95%;
    height: 80vh;
  }
  
  .message {
    max-width: 85%;
  }
}
</style>


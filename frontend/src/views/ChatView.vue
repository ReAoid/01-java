<template>
  <div class="chat-view">
    <!-- Toast通知 -->
    <ToastNotification ref="toast" />
    
    <!-- 主聊天区域 -->
    <div class="chat-main">
      <!-- 顶部控制面板 -->
      <div class="control-panel">
        <!-- 控制选项 -->
        <div class="controls">
          <!-- 连接状态 -->
          <div class="control-item status-item">
            <span class="status-icon">{{ connectionStatus === 'connected' ? '🟢' : '🔴' }}</span>
            <span class="status-text">{{ connectionStatusText }}</span>
          </div>
          
          <!-- 选择人设 -->
          <div class="control-item">
            <label>选择人设:</label>
            <select v-model="currentPersona" @change="handlePersonaChange" class="persona-select">
              <option v-for="persona in personas" :key="persona" :value="persona">
                {{ persona }}
              </option>
            </select>
          </div>
          
          <!-- 显示思考 -->
          <div class="control-item">
            <label for="thinkingToggle">显示思考:</label>
            <div class="toggle-switch" :class="{ active: showThinking }" @click="toggleThinking"></div>
          </div>
          
          <!-- 开启ASR -->
          <div class="control-item">
            <label for="asrToggle">开启ASR:</label>
            <div class="toggle-switch" :class="{ active: asrEnabled }" @click="toggleASR"></div>
          </div>
          
          <!-- 开启TTS -->
          <div class="control-item">
            <label for="ttsToggle">开启TTS:</label>
            <div class="toggle-switch" :class="{ active: ttsEnabled }" @click="toggleTTS"></div>
          </div>
          
          <!-- 联网搜索 -->
          <div class="control-item">
            <label for="webSearchToggle">联网搜索:</label>
            <div class="toggle-switch" :class="{ active: webSearchEnabled }" @click="toggleWebSearch"></div>
          </div>
        </div>
      </div>

      <!-- 消息区域 -->
      <div class="messages-container" ref="messagesContainer">
        <div class="messages-wrapper">
          <!-- 消息列表 -->
          <div 
            v-for="msg in messages" 
            :key="msg.id" 
            :class="['message-wrapper', msg.role, { 'typing': msg.isTyping }]"
          >
            <div class="message-avatar">
              <img v-if="msg.role === 'user'" src="@/assets/user-avatar.jpg" alt="User" />
              <div v-else class="ai-avatar">🤖</div>
            </div>
            <div class="message-bubble">
              <div v-if="msg.isTyping" class="typing-indicator">
                <span class="dot"></span>
                <span class="dot"></span>
                <span class="dot"></span>
              </div>
              <div v-else class="message-content" v-html="msg.content"></div>
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
          
          <!-- 发送/停止按钮 -->
          <button 
            class="send-btn" 
            @click="isLoading ? handleInterrupt() : handleSend()"
            :disabled="!isLoading && !inputMessage.trim()"
            :class="{ active: inputMessage.trim() || isLoading, 'btn-stopping': isLoading }"
            :title="isLoading ? '停止AI回复' : '发送消息'"
          >
            <!-- 停止图标 -->
            <svg v-if="isLoading" width="18" height="18" viewBox="0 0 24 24" fill="white" stroke="none">
              <rect x="6" y="6" width="12" height="12" rx="1"/>
            </svg>
            <!-- 播放/发送图标 -->
            <svg v-else width="18" height="18" viewBox="0 0 24 24" fill="white" stroke="none">
              <path d="M8 5v14l11-7z"/>
            </svg>
          </button>
        </div>
      </footer>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { chatApi, personaApi } from '@/api/chatApi'
import wsManager from '@/api/websocket'
import ToastNotification from '@/components/ToastNotification.vue'

const toast = ref(null)
const messages = ref([])
const inputMessage = ref('')
const isLoading = ref(false)
const currentPersona = ref('智能助手')
const personas = ref(['智能助手', '专业顾问', '创意助手'])
const currentSessionId = ref(null)
const messagesContainer = ref(null)
const messageInput = ref(null)

// 连接状态
const connectionStatus = ref('disconnected')
const connectionStatusText = ref('连接断开')
const ollamaStatusText = ref('🤖 正在检查Ollama服务状态...')

// 控制开关
const showThinking = ref(false)
const asrEnabled = ref(false)
const ttsEnabled = ref(false)
const webSearchEnabled = ref(false)

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
  // 如果AI正在回复，不允许切换
  if (isLoading.value) {
    // 恢复到之前的选择
    return
  }
  
  if (!wsManager.isConnected || !currentSessionId.value) {
    addSystemMessage('请先连接到服务器')
    return
  }
  
  try {
    // 通过WebSocket发送角色切换请求
    const selectedPersona = personas.value.find(p => p.name === currentPersona.value)
    const personaText = selectedPersona ? selectedPersona.name : currentPersona.value
    
    const message = {
      type: 'system',
      content: `切换到人设: ${personaText}`,
      metadata: {
        action: 'change_persona',
        personaId: currentPersona.value
      }
    }
    
    wsManager.send(message)
    addSystemMessage(`已切换到 ${personaText} 人设`)
    console.log('📤 发送角色切换请求:', message)
  } catch (error) {
    console.error('切换角色失败:', error)
    addSystemMessage('❌ 切换角色失败')
  }
}

// 发送消息
const handleSend = () => {
  if (!inputMessage.value.trim() || isLoading.value) return
  
  if (!wsManager.isConnected) {
    addSystemMessage('❌ 未连接到服务器，请等待连接建立')
    return
  }

  const userMessage = inputMessage.value.trim()
  
  // 如果AI正在回复，先发送打断信号
  if (isLoading.value) {
    handleInterrupt()
  }

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
  
  // 添加"AI正在思考中..."的占位消息
  messages.value.push({
    id: Date.now() + 1,
    role: 'assistant',
    content: 'AI正在思考中...',
    timestamp: new Date(),
    isTyping: true  // 标记为思考中
  })
  
  scrollToBottom()

  // 通过WebSocket发送消息 (使用旧前端的格式)
  const success = wsManager.send({
    type: 'text',
    content: userMessage,
    role: 'user',
    sessionId: currentSessionId.value
  })

  if (!success) {
    addSystemMessage('❌ 发送消息失败，请检查连接状态')
    isLoading.value = false
    // 移除思考中的占位消息
    const lastMsg = messages.value[messages.value.length - 1]
    if (lastMsg && lastMsg.isTyping) {
      messages.value.pop()
    }
  }
}

// 快速发送消息
const sendQuickMessage = (message) => {
  inputMessage.value = message
  handleSend()
}

// 添加系统消息
const addSystemMessage = (content, type = 'info') => {
  // 使用Toast通知代替消息列表
  if (toast.value) {
    // 根据内容判断类型
    if (content.includes('✅') || content.includes('成功') || content.includes('正常')) {
      toast.value.success(content)
    } else if (content.includes('❌') || content.includes('失败') || content.includes('错误')) {
      toast.value.error(content)
    } else if (content.includes('⚠️') || content.includes('警告') || content.includes('无法')) {
      toast.value.warning(content)
    } else {
      toast.value.info(content)
    }
  }
}

// 打断AI回复
const handleInterrupt = () => {
  if (!wsManager.isConnected || !currentSessionId.value) {
    console.warn('无法发送打断信号：WebSocket未连接或无会话ID')
    return
  }
  
  console.log('🛑 用户点击停止按钮，开始中断处理')
  
  // 1. 停止所有音频播放 (TTS相关)
  // TODO: 如果有TTS音频播放，需要在这里停止
  
  // 2. 发送后端中断信号
  const interruptMessage = {
    type: 'system',
    content: 'interrupt',
    metadata: {
      action: 'interrupt',
      interruptType: 'user_stop',
      reason: '用户点击停止按钮'
    },
    sessionId: currentSessionId.value
  }
  
  wsManager.send(interruptMessage)
  console.log('📤 发送后端打断信号:', interruptMessage)
  
  // 3. 立即进行视觉反馈 - 在最后一条AI消息末尾添加中断提示
  const lastMsg = messages.value[messages.value.length - 1]
  if (lastMsg && lastMsg.role === 'assistant') {
    lastMsg.content += ' ...（已中断）'
  }
  
  // 4. 立即重置UI状态
  isLoading.value = false
  
  console.log('✅ 中断处理完成，UI状态已重置')
}

// 切换显示思考
const toggleThinking = () => {
  if (!wsManager.isConnected) {
    addSystemMessage('请先连接到服务器')
    return
  }
  
  // 如果AI正在回复，不做任何操作
  if (isLoading.value) {
    return
  }
  
  const newState = !showThinking.value
  
  const toggleMessage = {
    type: 'system',
    content: 'toggle_thinking',
    metadata: {
      action: 'toggle_thinking',
      showThinking: newState
    }
  }
  
  wsManager.send(toggleMessage)
  console.log('📤 发送思考切换请求:', toggleMessage)
}

// 检查ASR服务健康状态
const checkASRHealth = async () => {
  try {
    const response = await fetch('http://localhost:8768/health', {
      method: 'GET',
      timeout: 5000
    })
    
    if (response.ok) {
      const healthData = await response.json()
      return healthData.server_ready === true
    }
    return false
  } catch (error) {
    console.error('❌ ASR健康检查失败:', error)
    return false
  }
}

// 切换ASR
const toggleASR = async () => {
  // 如果AI正在回复，不允许切换
  if (isLoading.value) {
    addSystemMessage('⚠️ AI正在回复中，无法切换ASR状态')
    return
  }
  
  // 如果要启用ASR，先检查服务健康状态
  if (!asrEnabled.value) {
    console.log('🔍 正在检查ASR服务状态...')
    addSystemMessage('🔍 正在检查ASR服务状态...')
    
    // 执行健康检查
    const isHealthy = await checkASRHealth()
    
    if (!isHealthy) {
      console.error('⚠️ ASR服务不可用')
      addSystemMessage('⚠️ ASR服务不可用，无法启用')
      // 保持关闭状态
      return
    }
    
    // 服务可用，继续启用ASR
    asrEnabled.value = true
    console.log('✅ ASR服务可用，已启用')
    addSystemMessage('✅ 已启用ASR语音识别')
  } else {
    // 禁用ASR
    asrEnabled.value = false
    console.log('❌ 已禁用ASR')
    addSystemMessage('❌ 已禁用ASR语音识别')
  }
  
  // 发送切换消息到后端
  if (wsManager.isConnected) {
    wsManager.send({
      type: 'system',
      metadata: {
        action: 'toggle_asr',
        enabled: asrEnabled.value
      }
    })
  }
}

// 检查TTS服务健康状态
const checkTTSHealth = async () => {
  try {
    const response = await fetch('/api/cosyvoice/health', {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json'
      }
    })
    
    if (response.ok) {
      const result = await response.json()
      return result.success && result.healthy
    }
    return false
  } catch (error) {
    console.error('❌ TTS健康检查失败:', error)
    return false
  }
}

// 切换TTS
const toggleTTS = async () => {
  // 如果AI正在回复，不允许切换
  if (isLoading.value) {
    addSystemMessage('⚠️ AI正在回复中，无法切换TTS状态')
    return
  }
  
  // 如果要启用TTS，先检查服务健康状态
  if (!ttsEnabled.value) {
    console.log('🔍 正在检查TTS服务状态...')
    addSystemMessage('🔍 正在检查TTS服务状态...')
    
    // 执行健康检查
    const isHealthy = await checkTTSHealth()
    
    if (!isHealthy) {
      console.error('⚠️ TTS服务不可用')
      addSystemMessage('⚠️ TTS服务不可用，无法启用')
      // 保持关闭状态
      return
    }
    
    // 服务可用，继续启用TTS
    ttsEnabled.value = true
    console.log('✅ TTS服务可用，已启用')
    addSystemMessage('✅ 已启用TTS语音合成')
  } else {
    // 禁用TTS
    ttsEnabled.value = false
    console.log('❌ 已禁用TTS')
    addSystemMessage('❌ 已禁用TTS语音合成')
  }
  
  // 发送切换消息到后端
  if (wsManager.isConnected) {
    wsManager.send({
      type: 'system',
      metadata: {
        action: 'toggle_tts',
        enabled: ttsEnabled.value
      }
    })
  }
}

// 切换联网搜索
const toggleWebSearch = () => {
  if (!wsManager.isConnected) {
    addSystemMessage('请先连接到服务器')
    return
  }
  
  // 如果AI正在回复，不做任何操作
  if (isLoading.value) {
    return
  }
  
  const newState = !webSearchEnabled.value
  
  const toggleMessage = {
    type: 'system',
    content: 'toggle_web_search',
    metadata: {
      action: 'toggle_web_search',
      useWebSearch: newState
    }
  }
  
  wsManager.send(toggleMessage)
  console.log('📤 发送联网搜索切换请求:', toggleMessage)
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

// 初始化WebSocket连接
const initializeWebSocket = async () => {
  try {
    connectionStatus.value = 'connecting'
    connectionStatusText.value = '正在连接...'
    
    await wsManager.connect()
    
    connectionStatus.value = 'connected'
    connectionStatusText.value = '已连接'
    
    // 检查Ollama服务状态
    checkOllamaStatus()
  } catch (error) {
    console.error('WebSocket连接失败:', error)
    connectionStatus.value = 'disconnected'
    connectionStatusText.value = '连接断开'
  }
}

// 检查Ollama服务状态
const checkOllamaStatus = async () => {
  try {
    // 这里可以调用后端API检查Ollama状态
    ollamaStatusText.value = '🤖 Ollama服务正常'
  } catch (error) {
    ollamaStatusText.value = '🤖 Ollama服务检查失败'
  }
}

// 设置WebSocket消息处理器
const setupWebSocketHandlers = () => {
  // 监听连接状态变化
  wsManager.on('connection', (data) => {
    if (data.status === 'connected') {
      connectionStatus.value = 'connected'
      connectionStatusText.value = '已连接'
    } else if (data.status === 'disconnected') {
      connectionStatus.value = 'disconnected'
      connectionStatusText.value = '连接断开'
    } else if (data.status === 'failed') {
      connectionStatus.value = 'disconnected'
      connectionStatusText.value = '连接失败'
    }
  })

  // 监听文本消息 (AI回复) - 后端发送的是 'text' 类型
  wsManager.on('text', (message) => {
    console.log('📨 收到text消息:', message)
    
    // 处理流式消息
    if (message.streaming) {
      // 如果有内容
      if (message.content) {
        // 确保AI正在响应状态
        if (!isLoading.value) {
          isLoading.value = true
        }
        
        // 查找最后一条assistant消息
        const lastMsg = messages.value[messages.value.length - 1]
        
        // 如果没有assistant消息,创建新的
        if (!lastMsg || lastMsg.role !== 'assistant') {
          messages.value.push({
            id: Date.now(),
            role: 'assistant',
            content: message.content,
            timestamp: new Date()
          })
        } 
        // 如果最后一条是"AI正在思考中..."的占位符,替换它
        else if (lastMsg.isTyping || lastMsg.content === 'AI正在思考中...') {
          lastMsg.content = message.content
          delete lastMsg.isTyping  // 移除思考标记
        }
        // 否则追加到现有消息
        else {
          lastMsg.content += message.content
        }
        
        scrollToBottom()
      }
      
      // 检查流式是否完成
      if (message.streamComplete) {
        console.log('✅ 流式消息完成')
        isLoading.value = false
      }
    } else {
      // 非流式完整消息
      if (message.content) {
        // 检查是否需要替换思考占位符
        const lastMsg = messages.value[messages.value.length - 1]
        if (lastMsg && lastMsg.role === 'assistant' && (lastMsg.isTyping || lastMsg.content === 'AI正在思考中...')) {
          // 替换占位符
          lastMsg.content = message.content
          delete lastMsg.isTyping
        } else {
          // 创建新消息
          messages.value.push({
            id: Date.now(),
            role: 'assistant',
            content: message.content,
            timestamp: new Date()
          })
        }
        scrollToBottom()
      }
      isLoading.value = false
    }
  })

  // 监听系统消息
  wsManager.on('system', (message) => {
    console.log('📨 收到system消息:', message)
    
    // 处理会话ID
    if (message.sessionId && !currentSessionId.value) {
      currentSessionId.value = message.sessionId
    }
    
    // 处理Ollama状态更新
    if (message.metadata && message.metadata.ollama_status) {
      ollamaStatusText.value = message.metadata.ollama_status === 'available' 
        ? '🤖 Ollama服务正常' 
        : '🤖 Ollama服务异常'
    }
    
    // 处理思考切换确认
    if (message.metadata && message.metadata.thinking_toggle === 'confirmed') {
      showThinking.value = message.metadata.showThinking
      if (message.content) {
        addSystemMessage(message.content)
      }
    }
    // 处理联网搜索切换确认
    else if (message.metadata && message.metadata.web_search_toggle === 'confirmed') {
      webSearchEnabled.value = message.metadata.useWebSearch
      if (message.content) {
        addSystemMessage(message.content)
      }
    }
    // 处理打断确认
    else if (message.metadata && message.metadata.interrupt_confirmed) {
      if (message.content) {
        addSystemMessage(message.content)
      }
    }
    // 处理ASR识别结果
    else if (message.metadata && message.metadata.asr_result) {
      // TODO: 处理ASR识别结果
      if (message.content) {
        addSystemMessage(message.content)
      }
    }
    // 处理ASR连接失败
    else if (message.metadata && message.metadata.asr_connection_failed) {
      addSystemMessage('❌ ASR连接失败: ' + (message.content || '未知错误'))
    }
    // 处理ASR会话被接管
    else if (message.metadata && message.metadata.asr_session_taken_over) {
      addSystemMessage('⚠️ ASR会话被其他客户端接管')
    }
    // 处理其他系统消息
    else if (message.content) {
      addSystemMessage(message.content)
    }
  })

  // 监听错误消息
  wsManager.on('error', (message) => {
    console.error('📨 收到error消息:', message)
    
    // 显示错误消息
    addSystemMessage('❌ ' + (message.content || message.message || '发生错误'))
    
    // 重置加载状态
    isLoading.value = false
  })
}

onMounted(() => {
  loadPersonas()
  setupWebSocketHandlers()
  initializeWebSocket()
})

onUnmounted(() => {
  wsManager.disconnect()
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

/* 控制面板 */
.control-panel {
  background: linear-gradient(135deg, #ff9966 0%, #ff8c5a 100%);
  padding: 15px 24px;
  border-bottom: 1px solid var(--border-light);
  flex-shrink: 0;
}

.controls {
  display: flex;
  align-items: center;
  gap: 20px;
  flex-wrap: wrap;
}

/* 连接状态项 */
.status-item {
  background: rgba(255, 255, 255, 0.15);
  padding: 6px 12px;
  border-radius: 20px;
  display: flex;
  align-items: center;
  gap: 6px;
}

.status-item .status-icon {
  font-size: 10px;
}

.status-item .status-text {
  font-size: 13px;
  font-weight: 600;
  color: white;
}

.control-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.control-item label {
  font-size: 13px;
  color: white;
  font-weight: 500;
  white-space: nowrap;
}

.persona-select {
  padding: 6px 12px;
  border: none;
  border-radius: 6px;
  background: white;
  color: var(--text-primary);
  font-size: 13px;
  cursor: pointer;
  outline: none;
  transition: all 0.3s ease;
}

.persona-select:hover {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.toggle-switch {
  width: 44px;
  height: 24px;
  background: rgba(255, 255, 255, 0.3);
  border-radius: 12px;
  position: relative;
  cursor: pointer;
  transition: all 0.3s ease;
}

.toggle-switch::after {
  content: '';
  position: absolute;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: white;
  top: 2px;
  left: 2px;
  transition: all 0.3s ease;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
}

.toggle-switch.active {
  background: rgba(255, 255, 255, 0.9);
}

.toggle-switch.active::after {
  left: 22px;
  background: #667eea;
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

/* AI头像样式 */
.ai-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
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

.message-wrapper.system {
  justify-content: center;
}

.message-wrapper.system .message-bubble {
  background: rgba(102, 126, 234, 0.1);
  border: 1px solid rgba(102, 126, 234, 0.3);
  color: var(--text-secondary);
  font-size: 14px;
  text-align: center;
  max-width: 600px;
  padding: 12px 20px;
}

.message-wrapper.system .message-avatar {
  display: none;
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
  color: white !important;
  fill: white !important;
}

/* 停止状态(btn-stopping) */
.send-btn.btn-stopping {
  background: #f44336;  /* 红色背景 */
  color: white;
  box-shadow: var(--shadow-sm);
  border: none;
  cursor: pointer;
  opacity: 1;
  animation: btn-float 2s ease-in-out infinite !important;  /* 整个按钮浮动 */
}

.send-btn.btn-stopping svg {
  color: white !important;
  fill: white !important;
  animation: breathe 2s ease-in-out infinite;  /* SVG呼吸动画 */
}

/* 按钮浮动动画 */
@keyframes btn-float {
  0%, 100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-3px);
  }
}

/* SVG呼吸动画 */
@keyframes breathe {
  0%, 100% {
    opacity: 1;
  }
  50% {
    opacity: 0.7;
  }
}

/* 确保SVG显示 */
.send-btn svg {
  display: block;
  flex-shrink: 0;
}

.send-btn svg path,
.send-btn svg rect {
  fill: inherit;
}

/* 停止键无动画,保持静态 */

.send-btn.active:hover:not(:disabled),
.send-btn.btn-stopping:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
}

.send-btn:disabled {
  opacity: 0.3;
  cursor: not-allowed;
  transform: none !important;
}

/* 旧的停止按钮样式(已移除,合并到send-btn) */
.interrupt-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  background: #f44336;
  color: white;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 600;
  transition: all 0.3s ease;
  flex-shrink: 0;
}

.interrupt-btn:hover {
  background: #d32f2f;
  transform: translateY(-2px);
  box-shadow: 0 4px 8px rgba(244, 67, 54, 0.3);
}

.interrupt-icon {
  font-size: 16px;
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
  
  .feature-cards {
    grid-template-columns: 1fr;
  }
  
  .feature-card {
    padding: 20px;
  }
  
  .control-panel {
    padding: 12px 16px;
  }
  
  .controls {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }
}
</style>


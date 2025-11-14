import { ref } from 'vue'
import { chatApi } from '@/api/chatApi'

/**
 * 聊天历史管理 Composable
 */
export function useChatHistory() {
  const sessions = ref([])
  const currentSessionId = ref(null)
  const chatHistory = ref([])
  const isLoading = ref(false)

  /**
   * 获取所有会话列表
   */
  const loadSessions = async () => {
    try {
      isLoading.value = true
      const response = await chatApi.getAllSessions()
      if (response.data) {
        sessions.value = response.data
      }
      return sessions.value
    } catch (error) {
      console.error('加载会话列表失败:', error)
      return []
    } finally {
      isLoading.value = false
    }
  }

  /**
   * 创建新会话
   */
  const createSession = async (personaName = null) => {
    try {
      const response = await chatApi.createSession(personaName)
      if (response.data) {
        const newSession = response.data
        sessions.value.unshift(newSession)
        currentSessionId.value = newSession.sessionId
        chatHistory.value = []
        return newSession
      }
    } catch (error) {
      console.error('创建会话失败:', error)
      return null
    }
  }

  /**
   * 切换会话
   */
  const switchSession = async (sessionId) => {
    try {
      currentSessionId.value = sessionId
      await loadChatHistory(sessionId)
      return true
    } catch (error) {
      console.error('切换会话失败:', error)
      return false
    }
  }

  /**
   * 删除会话
   */
  const deleteSession = async (sessionId) => {
    try {
      await chatApi.deleteSession(sessionId)
      sessions.value = sessions.value.filter(s => s.sessionId !== sessionId)
      
      // 如果删除的是当前会话,切换到第一个会话
      if (currentSessionId.value === sessionId) {
        if (sessions.value.length > 0) {
          await switchSession(sessions.value[0].sessionId)
        } else {
          currentSessionId.value = null
          chatHistory.value = []
        }
      }
      return true
    } catch (error) {
      console.error('删除会话失败:', error)
      return false
    }
  }

  /**
   * 加载指定会话的聊天历史
   */
  const loadChatHistory = async (sessionId) => {
    try {
      isLoading.value = true
      const response = await chatApi.getChatHistory(sessionId)
      if (response.data) {
        chatHistory.value = response.data
      }
      return chatHistory.value
    } catch (error) {
      console.error('加载聊天历史失败:', error)
      return []
    } finally {
      isLoading.value = false
    }
  }

  /**
   * 添加消息到当前历史
   */
  const addMessage = (message) => {
    chatHistory.value.push(message)
  }

  /**
   * 清空当前历史
   */
  const clearHistory = () => {
    chatHistory.value = []
  }

  /**
   * 格式化会话时间
   */
  const formatSessionTime = (timestamp) => {
    if (!timestamp) return ''
    
    const date = new Date(timestamp)
    const now = new Date()
    const diff = now - date
    
    const minutes = Math.floor(diff / 60000)
    const hours = Math.floor(diff / 3600000)
    const days = Math.floor(diff / 86400000)
    
    if (minutes < 1) return '刚刚'
    if (minutes < 60) return `${minutes}分钟前`
    if (hours < 24) return `${hours}小时前`
    if (days < 7) return `${days}天前`
    
    return date.toLocaleDateString('zh-CN')
  }

  /**
   * 获取会话标题
   */
  const getSessionTitle = (session) => {
    if (session.title) return session.title
    if (session.personaName) return `与${session.personaName}的对话`
    return '新对话'
  }

  return {
    sessions,
    currentSessionId,
    chatHistory,
    isLoading,
    loadSessions,
    createSession,
    switchSession,
    deleteSession,
    loadChatHistory,
    addMessage,
    clearHistory,
    formatSessionTime,
    getSessionTitle
  }
}

export default useChatHistory


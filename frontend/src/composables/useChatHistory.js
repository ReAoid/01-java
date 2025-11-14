import { ref } from 'vue'
import { chatApi } from '@/api/chatApi'

/**
 * 聊天历史管理 Composable
 * 
 * 功能说明：
 * 这个composable负责管理多个聊天会话的历史记录
 * 
 * 核心概念：
 * - Session（会话）：一次完整的对话，包含多条消息
 * - 每个Session有独立的ID、创建时间、消息列表
 * - 可以在不同Session之间切换
 * - 支持创建新Session、删除Session
 * 
 * 使用场景：
 * - 左侧边栏显示所有会话列表
 * - 点击会话切换到对应的聊天历史
 * - 创建新对话开始新的会话
 * - 删除不需要的历史会话
 */
export function useChatHistory() {
  const sessions = ref([])               // 所有会话列表
  const currentSessionId = ref(null)     // 当前激活的会话ID
  const chatHistory = ref([])            // 当前会话的消息历史
  const isLoading = ref(false)           // 加载状态

  /**
   * 获取所有会话列表
   * 
   * 从后端API加载用户的所有会话
   * 会话按最后消息时间倒序排列
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
   * 
   * @param {string} personaName - 角色名称（可选）
   * 
   * 流程：
   * 1. 调用后端API创建新会话
   * 2. 将新会话添加到会话列表顶部
   * 3. 自动切换到新会话
   * 4. 清空当前消息历史
   */
  const createSession = async (personaName = null) => {
    try {
      const response = await chatApi.createSession(personaName)
      if (response.data) {
        const newSession = response.data
        // 添加到列表顶部（最新的在前）
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
   * 
   * @param {string} sessionId - 要切换到的会话ID
   * 
   * 流程：
   * 1. 设置当前会话ID
   * 2. 加载该会话的聊天历史
   * 3. 在UI中显示历史消息
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
   * 
   * @param {string} sessionId - 要删除的会话ID
   * 
   * 流程：
   * 1. 调用后端API删除会话
   * 2. 从本地列表中移除
   * 3. 如果删除的是当前会话，自动切换到第一个会话
   * 4. 如果没有会话了，清空所有数据
   */
  const deleteSession = async (sessionId) => {
    try {
      await chatApi.deleteSession(sessionId)
      sessions.value = sessions.value.filter(s => s.sessionId !== sessionId)
      
      // 如果删除的是当前会话，切换到第一个会话
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
   * 
   * @param {string} sessionId - 会话ID
   * 
   * 从后端获取该会话的所有消息记录
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
   * 
   * 用于实时添加新消息到聊天记录
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
   * 
   * 将时间戳转换为友好的相对时间
   * 例如："刚刚"、"5分钟前"、"2小时前"、"3天前"
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
   * 
   * 优先使用自定义标题，否则使用角色名称，否则显示"新对话"
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


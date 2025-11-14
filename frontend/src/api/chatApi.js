import apiClient from './axios'

/**
 * 聊天相关API
 */
export const chatApi = {
  /**
   * 发送聊天消息
   * @param {Object} data - { message, sessionId, personaName }
   */
  sendMessage(data) {
    return apiClient.post('/chat', data)
  },

  /**
   * 获取聊天历史
   * @param {string} sessionId - 会话ID
   */
  getChatHistory(sessionId) {
    return apiClient.get(`/chat/history/${sessionId}`)
  },

  /**
   * 获取所有会话
   */
  getAllSessions() {
    return apiClient.get('/chat/sessions')
  },

  /**
   * 创建新会话
   * @param {string} personaName - 角色名称
   */
  createSession(personaName) {
    return apiClient.post('/chat/session', { personaName })
  },

  /**
   * 删除会话
   * @param {string} sessionId - 会话ID
   */
  deleteSession(sessionId) {
    return apiClient.delete(`/chat/session/${sessionId}`)
  }
}

/**
 * 角色相关API
 */
export const personaApi = {
  /**
   * 获取所有角色
   */
  getAllPersonas() {
    return apiClient.get('/persona/all')
  },

  /**
   * 获取当前角色
   */
  getCurrentPersona() {
    return apiClient.get('/persona/current')
  },

  /**
   * 切换角色
   * @param {string} personaName - 角色名称
   */
  switchPersona(personaName) {
    return apiClient.post('/persona/switch', { personaName })
  }
}

/**
 * TTS相关API
 */
export const ttsApi = {
  /**
   * 健康检查
   */
  healthCheck() {
    return apiClient.get('/api/cosyvoice/health')
  },

  /**
   * 获取可用的说话人列表
   */
  getSpeakers() {
    return apiClient.get('/api/cosyvoice/speakers')
  },

  /**
   * 获取可用的参考音频列表
   */
  getReferenceAudios() {
    return apiClient.get('/api/cosyvoice/speakers')
  },

  /**
   * 合成语音(测试)
   * @param {Object} data - { text, spk_id, emotion, ... }
   */
  synthesize(data) {
    return apiClient.post('/api/cosyvoice/synthesis/test', data, {
      responseType: 'blob' // 返回音频blob
    })
  },

  /**
   * 创建自定义说话人
   * @param {FormData} formData - 包含音频文件和配置
   */
  createSpeaker(formData) {
    return apiClient.post('/api/cosyvoice/speaker/create', formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    })
  },

  /**
   * 删除说话人
   * @param {string} speakerName - 说话人名称
   */
  deleteSpeaker(speakerName) {
    return apiClient.delete(`/api/cosyvoice/speaker/delete/${encodeURIComponent(speakerName)}`)
  },

  /**
   * 使用自定义说话人合成
   * @param {FormData} formData - 包含音频文件和文本
   */
  synthesizeWithCustomSpeaker(formData) {
    return apiClient.post('/api/cosyvoice/synthesis/custom', formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      },
      responseType: 'blob'
    })
  }
}

/**
 * 系统相关API
 */
export const systemApi = {
  /**
   * 健康检查
   */
  healthCheck() {
    return apiClient.get('/health')
  },

  /**
   * 获取系统配置
   */
  getSystemConfig() {
    return apiClient.get('/system/config')
  }
}


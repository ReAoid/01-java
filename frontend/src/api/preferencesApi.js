import apiClient from './axios'

/**
 * 用户偏好设置API
 */
export const preferencesApi = {
  /**
   * 获取所有用户偏好设置
   */
  getPreferences() {
    return apiClient.get('/api/preferences')
  },

  /**
   * 保存用户偏好设置
   * @param {Object} preferences - 偏好设置对象
   */
  savePreferences(preferences) {
    return apiClient.post('/api/preferences', preferences)
  },

  /**
   * 更新单个偏好设置
   * @param {string} key - 设置键
   * @param {any} value - 设置值
   */
  updatePreference(key, value) {
    return apiClient.put(`/api/preferences/${key}`, { value })
  },

  /**
   * 获取单个偏好设置
   * @param {string} key - 设置键
   */
  getPreference(key) {
    return apiClient.get(`/api/preferences/${key}`)
  },

  /**
   * 重置所有偏好设置为默认值
   */
  resetPreferences() {
    return apiClient.post('/api/preferences/reset')
  },

  /**
   * 批量更新偏好设置
   * @param {Object} updates - 键值对对象
   */
  batchUpdate(updates) {
    return apiClient.put('/api/preferences/batch', updates)
  },

  /**
   * 保存TTS偏好设置
   * @param {Object} ttsSettings - TTS配置
   */
  saveTTSPreferences(ttsSettings) {
    return apiClient.post('/api/preferences/tts', ttsSettings)
  },

  /**
   * 清除偏好设置缓存
   */
  clearCache() {
    return apiClient.delete('/api/preferences/cache')
  }
}

/**
 * 日志查询API
 */
export const logsApi = {
  /**
   * 查询日志
   * @param {Object} params - 查询参数
   * @param {string} params.level - 日志级别 (ALL, ERROR, WARN, INFO, DEBUG)
   * @param {number} params.limit - 返回数量限制
   * @param {string} params.timeRange - 时间范围 (1h, 6h, 24h, 7d, custom)
   * @param {string} params.startTime - 开始时间 (ISO格式)
   * @param {string} params.endTime - 结束时间 (ISO格式)
   */
  getLogs(params) {
    return apiClient.get('/api/logs', { params })
  },

  /**
   * 获取日志统计信息
   */
  getLogStats() {
    return apiClient.get('/api/logs/stats')
  }
}

export default {
  preferencesApi,
  logsApi
}


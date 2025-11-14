import { ref, watch } from 'vue'
import { preferencesApi } from '@/api/preferencesApi'

// 默认偏好设置
const defaultPreferences = {
  // 界面设置
  darkMode: false,
  animations: true,
  autoScroll: true,
  soundNotification: false,
  
  // Ollama设置
  ollamaBaseUrl: 'http://localhost:11434',
  ollamaModel: 'qwen3:4b',
  ollamaTimeout: 30000,
  ollamaMaxTokens: 4096,
  ollamaStream: true,
  
  // 联网搜索设置
  webSearchEnabled: false,
  webSearchMaxResults: 5,
  webSearchTimeout: 10,
  
  // TTS设置
  ttsEnabled: false,
  ttsSpeed: 1.0,
  ttsSpkId: null,
  
  // ASR设置
  asrEnabled: false
}

// 全局偏好设置状态
const preferences = ref({ ...defaultPreferences })
const isLoaded = ref(false)

/**
 * 用户偏好设置 Composable
 */
export function usePreferences() {
  /**
   * 加载偏好设置
   */
  const loadPreferences = async () => {
    try {
      const response = await preferencesApi.getPreferences()
      if (response.data) {
        preferences.value = { ...defaultPreferences, ...response.data }
        isLoaded.value = true
      }
    } catch (error) {
      console.error('加载偏好设置失败:', error)
      // 使用默认设置
      preferences.value = { ...defaultPreferences }
      isLoaded.value = true
    }
  }

  /**
   * 保存偏好设置
   */
  const savePreferences = async () => {
    try {
      await preferencesApi.savePreferences(preferences.value)
      console.log('偏好设置已保存')
      return true
    } catch (error) {
      console.error('保存偏好设置失败:', error)
      return false
    }
  }

  /**
   * 更新单个偏好设置
   */
  const updatePreference = async (key, value) => {
    try {
      preferences.value[key] = value
      await preferencesApi.updatePreference(key, value)
      return true
    } catch (error) {
      console.error(`更新偏好设置 ${key} 失败:`, error)
      return false
    }
  }

  /**
   * 批量更新偏好设置
   */
  const batchUpdate = async (updates) => {
    try {
      Object.assign(preferences.value, updates)
      await preferencesApi.batchUpdate(updates)
      return true
    } catch (error) {
      console.error('批量更新偏好设置失败:', error)
      return false
    }
  }

  /**
   * 重置为默认值
   */
  const resetPreferences = async () => {
    try {
      await preferencesApi.resetPreferences()
      preferences.value = { ...defaultPreferences }
      return true
    } catch (error) {
      console.error('重置偏好设置失败:', error)
      return false
    }
  }

  /**
   * 获取单个偏好值
   */
  const getPreference = (key, defaultValue = null) => {
    return preferences.value[key] !== undefined 
      ? preferences.value[key] 
      : defaultValue
  }

  /**
   * 应用深色模式
   */
  const applyDarkMode = (enabled) => {
    if (enabled) {
      document.body.classList.add('dark-mode')
    } else {
      document.body.classList.remove('dark-mode')
    }
  }

  // 监听深色模式变化
  watch(() => preferences.value.darkMode, (newValue) => {
    applyDarkMode(newValue)
  })

  return {
    preferences,
    isLoaded,
    loadPreferences,
    savePreferences,
    updatePreference,
    batchUpdate,
    resetPreferences,
    getPreference,
    applyDarkMode
  }
}

export default usePreferences


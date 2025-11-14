<template>
  <div class="page-container">
    <div class="settings-view">
      <div class="settings-container">
      <div class="settings-header">
        <h1>⚙️ 系统设置</h1>
        <p>配置系统参数和用户偏好</p>
      </div>

      <div class="settings-content">
        <!-- 界面设置 -->
        <div class="settings-section">
          <h2>
            <span class="section-icon">🎨</span>
            界面设置
          </h2>
          <div class="setting-item">
            <label>
              <input type="checkbox" v-model="preferences.darkMode" @change="toggleDarkMode">
              深色模式
            </label>
            <span class="setting-desc">切换到深色主题</span>
          </div>
          <div class="setting-item">
            <label>
              <input type="checkbox" v-model="preferences.animations">
              启用动画效果
            </label>
            <span class="setting-desc">界面过渡动画</span>
          </div>
          <div class="setting-item">
            <label>
              <input type="checkbox" v-model="preferences.autoScroll">
              自动滚动到底部
            </label>
            <span class="setting-desc">收到新消息时自动滚动</span>
          </div>
          <div class="setting-item">
            <label>
              <input type="checkbox" v-model="preferences.soundNotification">
              消息提示音
            </label>
            <span class="setting-desc">收到消息时播放提示音</span>
          </div>
        </div>

        <!-- Ollama设置 -->
        <div class="settings-section">
          <h2>
            <span class="section-icon">🤖</span>
            Ollama设置
          </h2>
          <div class="form-group">
            <label>服务地址</label>
            <input 
              type="url" 
              v-model="preferences.ollamaBaseUrl" 
              placeholder="http://localhost:11434"
            >
          </div>
          <div class="form-group">
            <label>使用模型</label>
            <input 
              type="text" 
              v-model="preferences.ollamaModel" 
              placeholder="qwen3:4b"
            >
          </div>
          <div class="form-group">
            <label>连接超时 (毫秒)</label>
            <input 
              type="number" 
              v-model.number="preferences.ollamaTimeout" 
              min="5000" 
              max="120000" 
              step="1000"
            >
          </div>
          <div class="form-group">
            <label>最大输出长度 (tokens)</label>
            <input 
              type="number" 
              v-model.number="preferences.ollamaMaxTokens" 
              min="512" 
              max="8192" 
              step="256"
            >
          </div>
          <div class="setting-item">
            <label>
              <input type="checkbox" v-model="preferences.ollamaStream">
              启用流式输出
            </label>
            <span class="setting-desc">实时流式响应</span>
          </div>
        </div>

        <!-- 联网搜索设置 -->
        <div class="settings-section">
          <h2>
            <span class="section-icon">🌐</span>
            联网搜索设置
          </h2>
          <div class="setting-item">
            <label>
              <input type="checkbox" v-model="preferences.webSearchEnabled">
              启用联网搜索
            </label>
            <span class="setting-desc">允许AI搜索互联网信息</span>
          </div>
          <div class="form-group">
            <label>最大搜索结果数</label>
            <input 
              type="number" 
              v-model.number="preferences.webSearchMaxResults" 
              min="1" 
              max="20"
            >
          </div>
          <div class="form-group">
            <label>搜索超时 (秒)</label>
            <input 
              type="number" 
              v-model.number="preferences.webSearchTimeout" 
              min="5" 
              max="60"
            >
          </div>
        </div>

        <!-- TTS设置 -->
        <div class="settings-section">
          <h2>
            <span class="section-icon">🔊</span>
            TTS语音设置
          </h2>
          <div class="setting-item">
            <label>
              <input type="checkbox" v-model="preferences.ttsEnabled">
              启用TTS
            </label>
            <span class="setting-desc">自动朗读AI回复</span>
          </div>
          <div class="form-group">
            <label>语速: {{ preferences.ttsSpeed.toFixed(1) }}x</label>
            <input 
              type="range" 
              v-model.number="preferences.ttsSpeed" 
              min="0.5" 
              max="2.0" 
              step="0.1"
              class="slider-input"
            >
            <div class="slider-labels">
              <span>0.5x</span>
              <span>1.0x</span>
              <span>2.0x</span>
            </div>
          </div>
          <div class="form-group">
            <label>说话人ID</label>
            <input 
              type="text" 
              v-model="preferences.ttsSpkId" 
              placeholder="留空使用默认"
            >
          </div>
        </div>

        <!-- ASR设置 -->
        <div class="settings-section">
          <h2>
            <span class="section-icon">🎤</span>
            ASR语音识别设置
          </h2>
          <div class="setting-item">
            <label>
              <input type="checkbox" v-model="preferences.asrEnabled">
              启用ASR
            </label>
            <span class="setting-desc">语音转文字</span>
          </div>
        </div>

        <!-- 操作按钮 -->
        <div class="settings-actions">
          <button class="btn-primary" @click="savePreferences">
            💾 保存设置
          </button>
          <button class="btn-secondary" @click="resetPreferences">
            🔄 重置为默认
          </button>
          <button class="btn-danger" @click="clearCache">
            🗑️ 清除缓存
          </button>
        </div>

        <!-- 系统信息 -->
        <div class="settings-section system-info">
          <h2>
            <span class="section-icon">ℹ️</span>
            系统信息
          </h2>
          <div class="info-grid">
            <div class="info-item">
              <span class="info-label">前端版本</span>
              <span class="info-value">v2.0.0</span>
            </div>
            <div class="info-item">
              <span class="info-label">后端状态</span>
              <span class="info-value" :class="healthStatus">
                {{ healthStatusText }}
              </span>
            </div>
            <div class="info-item">
              <span class="info-label">最后同步</span>
              <span class="info-value">{{ lastSyncTime }}</span>
            </div>
          </div>
          <button class="btn-secondary" @click="checkHealth" style="margin-top: 16px;">
            🔍 检查系统状态
          </button>
        </div>
      </div>
    </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { usePreferences } from '@/composables/usePreferences'
import { systemApi } from '@/api/chatApi'
import Message from '@/utils/message'

const {
  preferences,
  loadPreferences,
  savePreferences: savePref,
  resetPreferences: resetPref,
  applyDarkMode
} = usePreferences()

const healthStatus = ref('unknown')
const healthStatusText = ref('未检查')
const lastSyncTime = ref('从未')

// 切换深色模式
const toggleDarkMode = () => {
  applyDarkMode(preferences.value.darkMode)
}

// 保存设置
const savePreferences = async () => {
  const success = await savePref()
  if (success) {
    Message.success('设置已保存')
    lastSyncTime.value = new Date().toLocaleTimeString('zh-CN')
  } else {
    Message.error('保存设置失败')
  }
}

// 重置设置
const resetPreferences = async () => {
  if (confirm('确定要重置所有设置为默认值吗?')) {
    const success = await resetPref()
    if (success) {
      Message.success('设置已重置')
      applyDarkMode(preferences.value.darkMode)
    } else {
      Message.error('重置设置失败')
    }
  }
}

// 清除缓存
const clearCache = () => {
  if (confirm('确定要清除所有本地缓存吗?')) {
    localStorage.clear()
    sessionStorage.clear()
    Message.success('缓存已清除')
  }
}

// 检查系统健康状态
const checkHealth = async () => {
  try {
    healthStatusText.value = '检查中...'
    healthStatus.value = 'checking'
    
    await systemApi.healthCheck()
    
    healthStatus.value = 'healthy'
    healthStatusText.value = '✅ 正常'
    Message.success('系统运行正常')
  } catch (error) {
    healthStatus.value = 'error'
    healthStatusText.value = '❌ 异常'
    Message.error('系统状态检查失败')
  }
}

onMounted(async () => {
  await loadPreferences()
  applyDarkMode(preferences.value.darkMode)
  await checkHealth()
})
</script>

<style scoped>
.page-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  padding: 30px;
  overflow: hidden;
}

.settings-view {
  width: 100%;
  max-width: 900px;
  height: 100%;
  margin: 0 auto;
  background: #ffffff;
  border-radius: 24px;
  box-shadow: var(--shadow-xl);
  display: flex;
  flex-direction: column;
  overflow-y: auto;
  border: 1px solid #e5e7eb;
}

.settings-container {
  padding: 32px 40px;
}

.settings-header {
  margin-bottom: 32px;
  text-align: center;
  padding-bottom: 24px;
  border-bottom: 2px solid #e5e7eb;
}

.settings-header h1 {
  font-size: 32px;
  font-weight: 700;
  color: #1a1a1a;
  margin: 0 0 8px 0;
}

.settings-header p {
  font-size: 16px;
  color: #6b7280;
  margin: 0;
}

.settings-content {
  background: transparent;
  border-radius: 0;
  padding: 0;
  box-shadow: none;
}

.settings-section {
  margin-bottom: 40px;
  padding: 24px;
  background: #f9fafb;
  border-radius: 12px;
  border: 1px solid #e5e7eb;
}

.settings-section:last-child {
  margin-bottom: 0;
}

.settings-section h2 {
  font-size: 22px;
  font-weight: 700;
  color: #1a1a1a;
  margin: 0 0 24px 0;
  display: flex;
  align-items: center;
  gap: 12px;
  padding-bottom: 16px;
  border-bottom: 2px solid #e5e7eb;
}

.section-icon {
  font-size: 28px;
}

.setting-item {
  padding: 16px 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.setting-item label {
  font-size: 16px;
  font-weight: 600;
  color: #1a1a1a;
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
}

.setting-item label input[type="checkbox"] {
  width: 20px;
  height: 20px;
  cursor: pointer;
}

.setting-desc {
  font-size: 14px;
  color: #6b7280;
  margin-left: 30px;
}

.form-group {
  margin-bottom: 20px;
}

.form-group label {
  display: block;
  font-size: 15px;
  font-weight: 600;
  color: #1a1a1a;
  margin-bottom: 8px;
}

.form-group input[type="text"],
.form-group input[type="url"],
.form-group input[type="number"] {
  width: 100%;
  padding: 12px 16px;
  font-size: 15px;
  border: 2px solid #d1d5db;
  border-radius: 8px;
  background: #ffffff;
  color: #1a1a1a;
  transition: border-color 0.3s;
}

.form-group input:focus {
  outline: none;
  border-color: var(--primary-color);
}

.slider-input {
  width: 100%;
  height: 6px;
  border-radius: 3px;
  background: #e5e7eb;
  outline: none;
  -webkit-appearance: none;
}

.slider-input::-webkit-slider-thumb {
  -webkit-appearance: none;
  appearance: none;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: var(--primary-color);
  cursor: pointer;
}

.slider-input::-moz-range-thumb {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: var(--primary-color);
  cursor: pointer;
  border: none;
}

.slider-labels {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: #9ca3af;
  margin-top: 4px;
}

.settings-actions {
  display: flex;
  gap: 16px;
  margin-top: 32px;
  flex-wrap: wrap;
}

.settings-actions button {
  padding: 12px 24px;
  font-size: 15px;
  font-weight: 600;
  border-radius: 8px;
  border: none;
  cursor: pointer;
  transition: all 0.3s;
}

.btn-primary {
  background: var(--primary-color);
  color: white;
}

.btn-primary:hover {
  background: #e65c00;
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
}

.btn-secondary {
  background: #ffffff;
  color: #1a1a1a;
  border: 2px solid #d1d5db;
}

.btn-secondary:hover {
  background: var(--hover-bg);
  border-color: var(--primary-color);
}

.btn-danger {
  background: #dc3545;
  color: white;
}

.btn-danger:hover {
  background: #c82333;
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
}

.system-info {
  background: #f0fdf4;
  padding: 24px;
  border-radius: 12px;
  border: 1px solid #86efac;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 16px;
  margin-bottom: 16px;
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.info-label {
  font-size: 12px;
  color: #6b7280;
  font-weight: 600;
  text-transform: uppercase;
}

.info-value {
  font-size: 16px;
  color: #1a1a1a;
  font-weight: 600;
}

.info-value.healthy {
  color: #28a745;
}

.info-value.error {
  color: #dc3545;
}

.info-value.checking {
  color: var(--primary-color);
}

@media (max-width: 768px) {
  .settings-container {
    padding: 16px;
  }

  .settings-content {
    padding: 20px;
  }

  .settings-actions {
    flex-direction: column;
  }

  .settings-actions button {
    width: 100%;
  }

  .info-grid {
    grid-template-columns: 1fr;
  }
}
</style>

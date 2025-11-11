<template>
  <div class="page-container">
    <div class="content-card">
      <!-- 卡片头部 -->
      <div class="card-header">
        <h1>⚙️ 系统设置</h1>
        <div class="subtitle">配置和管理系统参数</div>
      </div>

      <!-- 主体内容 -->
      <div class="card-body">
        <div class="settings-content">
          <!-- 系统信息 -->
          <div class="settings-section">
            <h2 class="section-title">
              <span class="section-icon">ℹ️</span>
              系统信息
            </h2>
            <div class="info-grid">
              <div class="info-item">
                <label>前端版本</label>
                <span>v1.0.0</span>
              </div>
              <div class="info-item">
                <label>后端地址</label>
                <span>{{ apiUrl }}</span>
              </div>
              <div class="info-item">
                <label>连接状态</label>
                <span :class="['status-badge', connectionStatus]">
                  {{ connectionStatus === 'connected' ? '✓ 已连接' : '✗ 未连接' }}
                </span>
              </div>
            </div>
          </div>

          <!-- 外观设置 -->
          <div class="settings-section">
            <h2 class="section-title">
              <span class="section-icon">🎨</span>
              外观设置
            </h2>
            <div class="setting-item">
              <div class="setting-info">
                <h3>深色模式</h3>
                <p>切换深色/浅色主题(开发中)</p>
              </div>
              <label class="switch">
                <input type="checkbox" v-model="darkMode" disabled>
                <span class="slider"></span>
              </label>
            </div>
          </div>

          <!-- API设置 -->
          <div class="settings-section">
            <h2 class="section-title">
              <span class="section-icon">🔌</span>
              API设置
            </h2>
            <div class="form-group">
              <label>API超时时间(毫秒)</label>
              <input 
                type="number" 
                v-model.number="apiTimeout" 
                class="form-input"
                placeholder="30000"
              >
            </div>
            <div class="form-group">
              <label>自动重试次数</label>
              <input 
                type="number" 
                v-model.number="retryCount" 
                class="form-input"
                placeholder="3"
              >
            </div>
          </div>

          <!-- 聊天设置 -->
          <div class="settings-section">
            <h2 class="section-title">
              <span class="section-icon">💬</span>
              聊天设置
            </h2>
            <div class="setting-item">
              <div class="setting-info">
                <h3>自动滚动</h3>
                <p>收到新消息时自动滚动到底部</p>
              </div>
              <label class="switch">
                <input type="checkbox" v-model="autoScroll">
                <span class="slider"></span>
              </label>
            </div>
            <div class="setting-item">
              <div class="setting-info">
                <h3>显示时间戳</h3>
                <p>在消息上显示发送时间</p>
              </div>
              <label class="switch">
                <input type="checkbox" v-model="showTimestamp">
                <span class="slider"></span>
              </label>
            </div>
          </div>

          <!-- 操作按钮 -->
          <div class="settings-actions">
            <button class="primary" @click="saveSettings">
              💾 保存设置
            </button>
            <button class="secondary" @click="resetSettings">
              🔄 重置为默认
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { systemApi } from '@/api/chatApi'

const apiUrl = ref(import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080')
const connectionStatus = ref('checking')
const darkMode = ref(false)
const apiTimeout = ref(30000)
const retryCount = ref(3)
const autoScroll = ref(true)
const showTimestamp = ref(true)

// 检查连接状态
const checkConnection = async () => {
  try {
    await systemApi.healthCheck()
    connectionStatus.value = 'connected'
  } catch (error) {
    connectionStatus.value = 'disconnected'
  }
}

// 保存设置
const saveSettings = () => {
  const settings = {
    darkMode: darkMode.value,
    apiTimeout: apiTimeout.value,
    retryCount: retryCount.value,
    autoScroll: autoScroll.value,
    showTimestamp: showTimestamp.value
  }
  localStorage.setItem('app-settings', JSON.stringify(settings))
  alert('✅ 设置已保存!')
}

// 重置设置
const resetSettings = () => {
  if (confirm('确定要重置所有设置为默认值吗?')) {
    darkMode.value = false
    apiTimeout.value = 30000
    retryCount.value = 3
    autoScroll.value = true
    showTimestamp.value = true
    localStorage.removeItem('app-settings')
    alert('✅ 设置已重置!')
  }
}

// 加载设置
const loadSettings = () => {
  try {
    const saved = localStorage.getItem('app-settings')
    if (saved) {
      const settings = JSON.parse(saved)
      darkMode.value = settings.darkMode || false
      apiTimeout.value = settings.apiTimeout || 30000
      retryCount.value = settings.retryCount || 3
      autoScroll.value = settings.autoScroll !== false
      showTimestamp.value = settings.showTimestamp !== false
    }
  } catch (error) {
    console.error('加载设置失败:', error)
  }
}

onMounted(() => {
  loadSettings()
  checkConnection()
})
</script>

<style scoped>
.settings-content {
  max-width: 800px;
  margin: 0 auto;
}

.settings-section {
  margin-bottom: 40px;
  padding-bottom: 30px;
  border-bottom: 2px solid #f0f0f0;
}

.settings-section:last-of-type {
  border-bottom: none;
}

.section-title {
  font-size: 20px;
  color: #374151;
  margin-bottom: 20px;
  display: flex;
  align-items: center;
  gap: 10px;
}

.section-icon {
  font-size: 24px;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 20px;
}

.info-item {
  padding: 15px;
  background: #f9fafb;
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.info-item label {
  font-size: 12px;
  color: #6b7280;
  font-weight: 600;
  text-transform: uppercase;
}

.info-item span {
  font-size: 14px;
  color: #1f2937;
  font-weight: 500;
}

.status-badge {
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 600;
  display: inline-block;
}

.status-badge.connected {
  background: #d1fae5;
  color: #065f46;
}

.status-badge.disconnected {
  background: #fee2e2;
  color: #991b1b;
}

.setting-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 0;
  border-bottom: 1px solid #f0f0f0;
}

.setting-item:last-child {
  border-bottom: none;
}

.setting-info h3 {
  margin: 0 0 5px 0;
  font-size: 16px;
  color: #1f2937;
}

.setting-info p {
  margin: 0;
  font-size: 14px;
  color: #6b7280;
}

/* 开关按钮 */
.switch {
  position: relative;
  display: inline-block;
  width: 50px;
  height: 28px;
}

.switch input {
  opacity: 0;
  width: 0;
  height: 0;
}

.slider {
  position: absolute;
  cursor: pointer;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: #cbd5e1;
  transition: 0.3s;
  border-radius: 28px;
}

.slider:before {
  position: absolute;
  content: "";
  height: 20px;
  width: 20px;
  left: 4px;
  bottom: 4px;
  background-color: white;
  transition: 0.3s;
  border-radius: 50%;
}

input:checked + .slider {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

input:checked + .slider:before {
  transform: translateX(22px);
}

input:disabled + .slider {
  opacity: 0.5;
  cursor: not-allowed;
}

.form-group {
  margin-bottom: 20px;
}

.form-group label {
  display: block;
  margin-bottom: 8px;
  font-weight: 600;
  color: #374151;
}

.form-input {
  width: 100%;
  padding: 10px 15px;
  border: 2px solid #e5e7eb;
  border-radius: 8px;
  font-size: 14px;
}

.settings-actions {
  display: flex;
  gap: 15px;
  margin-top: 40px;
  justify-content: center;
}

.settings-actions button {
  padding: 12px 30px;
  font-size: 15px;
  font-weight: 600;
}

@media (max-width: 768px) {
  .info-grid {
    grid-template-columns: 1fr;
  }
  
  .settings-actions {
    flex-direction: column;
  }
  
  .settings-actions button {
    width: 100%;
  }
}
</style>


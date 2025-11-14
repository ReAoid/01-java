<template>
  <div class="logs-view">
    <div class="logs-container">
      <div class="logs-header">
        <h1>📝 系统日志</h1>
        <div class="header-actions">
          <button class="btn-primary" @click="loadLogs">
            🔄 刷新
          </button>
          <button class="btn-secondary" @click="clearLogs">
            🗑️ 清空
          </button>
        </div>
      </div>

      <!-- 过滤器 -->
      <div class="logs-filters">
        <div class="filter-group">
          <label>日志级别:</label>
          <select v-model="filters.level" @change="loadLogs">
            <option value="ALL">全部</option>
            <option value="ERROR">错误</option>
            <option value="WARN">警告</option>
            <option value="INFO">信息</option>
            <option value="DEBUG">调试</option>
          </select>
        </div>

        <div class="filter-group">
          <label>时间范围:</label>
          <select v-model="filters.timeRange" @change="loadLogs">
            <option value="1h">最近1小时</option>
            <option value="6h">最近6小时</option>
            <option value="24h">最近24小时</option>
            <option value="7d">最近7天</option>
          </select>
        </div>

        <div class="filter-group">
          <label>数量限制:</label>
          <input 
            type="number" 
            v-model.number="filters.limit" 
            min="10" 
            max="1000" 
            step="10"
            @change="loadLogs"
          >
        </div>

        <div class="filter-group">
          <label class="checkbox-label">
            <input type="checkbox" v-model="autoRefresh" @change="toggleAutoRefresh">
            自动刷新
          </label>
        </div>
      </div>

      <!-- 统计信息 -->
      <div class="logs-stats">
        <div class="stat-item">
          <span class="stat-label">总数</span>
          <span class="stat-value">{{ stats.total }}</span>
        </div>
        <div class="stat-item error">
          <span class="stat-label">错误</span>
          <span class="stat-value">{{ stats.error }}</span>
        </div>
        <div class="stat-item warn">
          <span class="stat-label">警告</span>
          <span class="stat-value">{{ stats.warn }}</span>
        </div>
        <div class="stat-item info">
          <span class="stat-label">信息</span>
          <span class="stat-value">{{ stats.info }}</span>
        </div>
        <div class="stat-item">
          <span class="stat-label">最后更新</span>
          <span class="stat-value">{{ lastUpdate }}</span>
        </div>
      </div>

      <!-- 日志列表 -->
      <div class="logs-content">
        <div v-if="loading" class="loading-state">
          <div class="spinner"></div>
          <p>加载日志中...</p>
        </div>

        <div v-else-if="logs.length === 0" class="empty-state">
          <p>暂无日志记录</p>
        </div>

        <div v-else class="logs-list">
          <div 
            v-for="(log, index) in logs" 
            :key="index"
            class="log-entry"
            :class="log.level.toLowerCase()"
          >
            <div class="log-time">{{ formatTime(log.timestamp) }}</div>
            <div class="log-level">
              <span class="level-badge" :class="log.level.toLowerCase()">
                {{ log.level }}
              </span>
            </div>
            <div class="log-message">{{ log.message }}</div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { logsApi } from '@/api/preferencesApi'
import Message from '@/utils/message'

const logs = ref([])
const loading = ref(false)
const autoRefresh = ref(false)
const autoRefreshTimer = ref(null)
const lastUpdate = ref('从未')

const filters = ref({
  level: 'ALL',
  timeRange: '24h',
  limit: 100
})

const stats = ref({
  total: 0,
  error: 0,
  warn: 0,
  info: 0,
  debug: 0
})

// 加载日志
const loadLogs = async () => {
  loading.value = true
  try {
    const response = await logsApi.getLogs(filters.value)
    if (response.data) {
      logs.value = response.data.logs || []
      stats.value = response.data.stats || stats.value
      lastUpdate.value = new Date().toLocaleTimeString('zh-CN')
    }
  } catch (error) {
    console.error('加载日志失败:', error)
    Message.error('加载日志失败')
  } finally {
    loading.value = false
  }
}

// 清空日志
const clearLogs = () => {
  if (confirm('确定要清空所有日志吗?')) {
    logs.value = []
    stats.value = { total: 0, error: 0, warn: 0, info: 0, debug: 0 }
    Message.success('日志已清空')
  }
}

// 切换自动刷新
const toggleAutoRefresh = () => {
  if (autoRefresh.value) {
    startAutoRefresh()
    Message.info('已开启自动刷新 (10秒)')
  } else {
    stopAutoRefresh()
    Message.info('已关闭自动刷新')
  }
}

// 启动自动刷新
const startAutoRefresh = () => {
  stopAutoRefresh()
  autoRefreshTimer.value = setInterval(() => {
    loadLogs()
  }, 10000) // 10秒
}

// 停止自动刷新
const stopAutoRefresh = () => {
  if (autoRefreshTimer.value) {
    clearInterval(autoRefreshTimer.value)
    autoRefreshTimer.value = null
  }
}

// 格式化时间
const formatTime = (timestamp) => {
  if (!timestamp) return ''
  const date = new Date(timestamp)
  return date.toLocaleString('zh-CN')
}

onMounted(() => {
  loadLogs()
})

onUnmounted(() => {
  stopAutoRefresh()
})
</script>

<style scoped>
.logs-view {
  width: 100%;
  height: 100%;
  overflow-y: auto;
  background: var(--bg-primary);
}

.logs-container {
  max-width: 1400px;
  margin: 0 auto;
  padding: 32px 24px;
}

.logs-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.logs-header h1 {
  font-size: 32px;
  font-weight: 700;
  color: var(--text-primary);
  margin: 0;
}

.header-actions {
  display: flex;
  gap: 12px;
}

.header-actions button {
  padding: 10px 20px;
  font-size: 14px;
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
}

.btn-secondary {
  background: var(--bg-tertiary);
  color: var(--text-primary);
  border: 2px solid var(--border-color);
}

.btn-secondary:hover {
  border-color: var(--primary-color);
}

.logs-filters {
  background: white;
  padding: 20px;
  border-radius: 12px;
  box-shadow: var(--shadow-md);
  display: flex;
  gap: 20px;
  flex-wrap: wrap;
  margin-bottom: 20px;
}

.filter-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.filter-group label {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
}

.filter-group select,
.filter-group input[type="number"] {
  padding: 8px 12px;
  border: 2px solid var(--border-color);
  border-radius: 6px;
  font-size: 14px;
  background: var(--bg-secondary);
  color: var(--text-primary);
}

.filter-group select:focus,
.filter-group input:focus {
  outline: none;
  border-color: var(--primary-color);
}

.checkbox-label {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
}

.checkbox-label input[type="checkbox"] {
  width: 18px;
  height: 18px;
  cursor: pointer;
}

.logs-stats {
  background: white;
  padding: 20px;
  border-radius: 12px;
  box-shadow: var(--shadow-md);
  display: flex;
  gap: 20px;
  flex-wrap: wrap;
  margin-bottom: 20px;
}

.stat-item {
  flex: 1;
  min-width: 150px;
  padding: 12px;
  background: var(--bg-tertiary);
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  gap: 6px;
  border-left: 4px solid var(--primary-color);
}

.stat-item.error {
  border-left-color: #dc3545;
}

.stat-item.warn {
  border-left-color: #ffc107;
}

.stat-item.info {
  border-left-color: #17a2b8;
}

.stat-label {
  font-size: 12px;
  color: var(--text-tertiary);
  font-weight: 600;
  text-transform: uppercase;
}

.stat-value {
  font-size: 24px;
  font-weight: 700;
  color: var(--text-primary);
}

.logs-content {
  background: white;
  border-radius: 12px;
  box-shadow: var(--shadow-md);
  padding: 20px;
  min-height: 400px;
}

.loading-state,
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  color: var(--text-secondary);
}

.spinner {
  width: 40px;
  height: 40px;
  border: 4px solid var(--border-color);
  border-top-color: var(--primary-color);
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-bottom: 16px;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.logs-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.log-entry {
  display: grid;
  grid-template-columns: auto auto 1fr;
  gap: 16px;
  padding: 12px;
  background: var(--bg-secondary);
  border-radius: 8px;
  border-left: 4px solid var(--border-color);
  font-family: 'Courier New', monospace;
  transition: all 0.3s;
}

.log-entry:hover {
  transform: translateX(4px);
  box-shadow: var(--shadow-sm);
}

.log-entry.error {
  background: rgba(220, 53, 69, 0.05);
  border-left-color: #dc3545;
}

.log-entry.warn {
  background: rgba(255, 193, 7, 0.05);
  border-left-color: #ffc107;
}

.log-entry.info {
  background: rgba(23, 162, 184, 0.05);
  border-left-color: #17a2b8;
}

.log-time {
  font-size: 12px;
  color: var(--text-tertiary);
  white-space: nowrap;
}

.log-level {
  display: flex;
  align-items: center;
}

.level-badge {
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 11px;
  font-weight: 700;
  text-transform: uppercase;
}

.level-badge.error {
  background: #dc3545;
  color: white;
}

.level-badge.warn {
  background: #ffc107;
  color: #333;
}

.level-badge.info {
  background: #17a2b8;
  color: white;
}

.level-badge.debug {
  background: #6c757d;
  color: white;
}

.log-message {
  font-size: 13px;
  color: var(--text-primary);
  word-break: break-word;
}

@media (max-width: 768px) {
  .logs-header {
    flex-direction: column;
    gap: 16px;
    align-items: flex-start;
  }

  .header-actions {
    width: 100%;
  }

  .header-actions button {
    flex: 1;
  }

  .logs-filters,
  .logs-stats {
    flex-direction: column;
  }

  .log-entry {
    grid-template-columns: 1fr;
    gap: 8px;
  }
}
</style>


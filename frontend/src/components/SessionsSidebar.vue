<template>
  <aside class="sessions-sidebar" :class="{ collapsed: !isVisible }">
    <div class="sessions-header">
      <h3>会话历史</h3>
      <button class="new-session-btn" @click="handleNewSession" title="新建会话">
        ➕
      </button>
    </div>

    <div class="sessions-list">
      <div 
        v-for="session in sessions" 
        :key="session.sessionId"
        class="session-item"
        :class="{ active: session.sessionId === currentSessionId }"
        @click="handleSelectSession(session.sessionId)"
      >
        <div class="session-info">
          <div class="session-title">{{ getSessionTitle(session) }}</div>
          <div class="session-time">{{ formatSessionTime(session.lastMessageTime || session.createTime) }}</div>
          <div class="session-count">{{ session.messageCount || 0 }} 条消息</div>
        </div>
        <button 
          class="delete-session-btn" 
          @click.stop="handleDeleteSession(session.sessionId)"
          title="删除会话"
        >
          🗑️
        </button>
      </div>

      <div v-if="sessions.length === 0" class="empty-state">
        <p>暂无会话历史</p>
        <button class="btn-primary" @click="handleNewSession">
          创建新会话
        </button>
      </div>
    </div>

    <div class="sessions-footer">
      <button class="toggle-sidebar-btn" @click="toggleSidebar">
        {{ isVisible ? '◀' : '▶' }}
      </button>
    </div>
  </aside>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useChatHistory } from '@/composables/useChatHistory'
import Message from '@/utils/message'

const emit = defineEmits(['session-selected', 'new-session'])
const isVisible = ref(true)

const {
  sessions,
  currentSessionId,
  loadSessions,
  createSession,
  switchSession,
  deleteSession,
  formatSessionTime,
  getSessionTitle
} = useChatHistory()

// 选择会话
const handleSelectSession = async (sessionId) => {
  if (sessionId === currentSessionId.value) return
  
  const success = await switchSession(sessionId)
  if (success) {
    emit('session-selected', sessionId)
  } else {
    Message.error('切换会话失败')
  }
}

// 新建会话
const handleNewSession = async () => {
  const session = await createSession()
  if (session) {
    emit('new-session', session)
    Message.success('已创建新会话')
  } else {
    Message.error('创建会话失败')
  }
}

// 删除会话
const handleDeleteSession = async (sessionId) => {
  if (!confirm('确定要删除这个会话吗?')) return
  
  const success = await deleteSession(sessionId)
  if (success) {
    Message.success('会话已删除')
  } else {
    Message.error('删除会话失败')
  }
}

// 切换侧边栏显示
const toggleSidebar = () => {
  isVisible.value = !isVisible.value
}

onMounted(() => {
  loadSessions()
})

defineExpose({
  isVisible,
  toggleSidebar
})
</script>

<style scoped>
.sessions-sidebar {
  width: 280px;
  background: var(--bg-secondary);
  border-right: 1px solid var(--border-color);
  display: flex;
  flex-direction: column;
  transition: transform 0.3s ease, width 0.3s ease;
  position: relative;
  flex-shrink: 0;
}

.sessions-sidebar.collapsed {
  width: 0;
  transform: translateX(-280px);
}

.sessions-header {
  padding: 16px;
  border-bottom: 1px solid var(--border-color);
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.sessions-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
}

.new-session-btn {
  background: var(--primary-color);
  color: white;
  border: none;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  cursor: pointer;
  font-size: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s;
}

.new-session-btn:hover {
  background: #e65c00;
  transform: scale(1.1);
}

.sessions-list {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}

.session-item {
  padding: 12px;
  margin-bottom: 8px;
  background: var(--bg-tertiary);
  border: 1px solid var(--border-color);
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s;
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.session-item:hover {
  border-color: var(--primary-color);
  transform: translateX(4px);
}

.session-item.active {
  background: var(--primary-color);
  color: white;
  border-color: var(--primary-color);
}

.session-info {
  flex: 1;
  min-width: 0;
}

.session-title {
  font-size: 14px;
  font-weight: 600;
  margin-bottom: 4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.session-item.active .session-title {
  color: white;
}

.session-time {
  font-size: 12px;
  color: var(--text-tertiary);
  margin-bottom: 2px;
}

.session-item.active .session-time {
  color: rgba(255, 255, 255, 0.8);
}

.session-count {
  font-size: 11px;
  color: var(--text-tertiary);
}

.session-item.active .session-count {
  color: rgba(255, 255, 255, 0.7);
}

.delete-session-btn {
  background: transparent;
  border: none;
  cursor: pointer;
  font-size: 16px;
  padding: 4px;
  opacity: 0;
  transition: opacity 0.3s;
}

.session-item:hover .delete-session-btn {
  opacity: 0.6;
}

.delete-session-btn:hover {
  opacity: 1 !important;
}

.empty-state {
  text-align: center;
  padding: 40px 20px;
  color: var(--text-secondary);
}

.empty-state p {
  margin-bottom: 16px;
}

.btn-primary {
  background: var(--primary-color);
  color: white;
  border: none;
  padding: 10px 20px;
  border-radius: 8px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 600;
  transition: all 0.3s;
}

.btn-primary:hover {
  background: #e65c00;
  transform: translateY(-2px);
}

.sessions-footer {
  padding: 8px;
  border-top: 1px solid var(--border-color);
  text-align: center;
}

.toggle-sidebar-btn {
  background: var(--bg-tertiary);
  border: 1px solid var(--border-color);
  padding: 8px 16px;
  border-radius: 8px;
  cursor: pointer;
  font-size: 16px;
  transition: all 0.3s;
}

.toggle-sidebar-btn:hover {
  background: var(--hover-bg);
  border-color: var(--primary-color);
}

/* 滚动条样式 */
.sessions-list::-webkit-scrollbar {
  width: 6px;
}

.sessions-list::-webkit-scrollbar-track {
  background: var(--bg-secondary);
}

.sessions-list::-webkit-scrollbar-thumb {
  background: var(--border-color);
  border-radius: 3px;
}

.sessions-list::-webkit-scrollbar-thumb:hover {
  background: var(--text-tertiary);
}

@media (max-width: 768px) {
  .sessions-sidebar {
    position: absolute;
    left: 0;
    top: 0;
    bottom: 0;
    z-index: 100;
    box-shadow: 2px 0 8px rgba(0, 0, 0, 0.1);
  }
}
</style>


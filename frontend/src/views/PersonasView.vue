<template>
  <div class="page-container">
    <div class="content-card">
      <!-- 卡片头部 -->
      <div class="card-header">
        <h1>👥 角色管理</h1>
        <div class="subtitle">切换不同的 AI 角色人设</div>
      </div>

      <!-- 主体内容 -->
      <div class="card-body">
        <div v-if="isLoading" class="loading-container">
          <span class="loading large"></span>
          <p>加载角色列表中...</p>
        </div>

        <div v-else class="personas-grid">
          <div 
            v-for="persona in personas" 
            :key="persona.name"
            :class="['persona-card', { active: persona.name === currentPersona }]"
            @click="switchPersona(persona.name)"
          >
            <div class="persona-icon">{{ persona.icon || '🤖' }}</div>
            <h3 class="persona-name">{{ persona.name }}</h3>
            <p class="persona-description">{{ persona.description || '暂无描述' }}</p>
            <div v-if="persona.name === currentPersona" class="active-badge">
              ✓ 当前使用
            </div>
          </div>
        </div>

        <div v-if="!isLoading && personas.length === 0" class="empty-state">
          <div class="empty-icon">😔</div>
          <p>暂无可用角色</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { personaApi } from '@/api/chatApi'

const personas = ref([])
const currentPersona = ref('')
const isLoading = ref(false)

// 加载角色列表
const loadPersonas = async () => {
  isLoading.value = true
  try {
    const data = await personaApi.getAllPersonas()
    
    if (Array.isArray(data)) {
      personas.value = data.map(p => typeof p === 'string' ? { name: p } : p)
    } else if (data.personas) {
      personas.value = data.personas.map(p => typeof p === 'string' ? { name: p } : p)
    }

    try {
      const current = await personaApi.getCurrentPersona()
      currentPersona.value = current.personaName || current.name || ''
    } catch (error) {
      console.warn('获取当前角色失败:', error)
    }
  } catch (error) {
    console.error('加载角色失败:', error)
    personas.value = [
      { name: 'Assistant', description: '通用AI助手', icon: '🤖' },
      { name: 'Teacher', description: '耐心的教学助手', icon: '👨‍🏫' },
      { name: 'Friend', description: '友好的聊天伙伴', icon: '👋' }
    ]
  } finally {
    isLoading.value = false
  }
}

// 切换角色
const switchPersona = async (personaName) => {
  try {
    await personaApi.switchPersona(personaName)
    currentPersona.value = personaName
    console.log('切换到角色:', personaName)
  } catch (error) {
    console.error('切换角色失败:', error)
    alert('切换角色失败: ' + error)
  }
}

onMounted(() => {
  loadPersonas()
})
</script>

<style scoped>
.loading-container {
  text-align: center;
  padding: 80px 20px;
  color: var(--text-secondary);
}

.loading.large {
  width: 48px;
  height: 48px;
  border-width: 4px;
  margin-bottom: 24px;
}

.personas-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 24px;
  padding: 16px;
}

.persona-card {
  padding: 32px 24px;
  border: 2px solid var(--border-color);
  border-radius: 20px;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  text-align: center;
  position: relative;
  background: var(--bg-secondary);
  overflow: hidden;
}

.persona-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 4px;
  background: var(--primary-gradient);
  opacity: 0;
  transition: opacity 0.3s ease;
}

.persona-card:hover {
  border-color: var(--primary-color);
  transform: translateY(-6px);
  box-shadow: var(--shadow-xl);
}

.persona-card:hover::before {
  opacity: 1;
}

.persona-card.active {
  border-color: var(--primary-color);
  background: var(--sidebar-hover);
  box-shadow: var(--shadow-lg);
}

.persona-card.active::before {
  opacity: 1;
}

.persona-icon {
  font-size: 56px;
  margin-bottom: 20px;
  animation: float 3s ease-in-out infinite;
}

@keyframes float {
  0%, 100% { transform: translateY(0px); }
  50% { transform: translateY(-8px); }
}

.persona-name {
  margin: 12px 0;
  font-size: 22px;
  color: var(--text-primary);
  font-weight: 700;
}

.persona-description {
  color: var(--text-secondary);
  font-size: 14px;
  margin: 12px 0;
  line-height: 1.6;
}

.active-badge {
  position: absolute;
  top: 16px;
  right: 16px;
  background: var(--primary-gradient);
  color: white;
  padding: 6px 14px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 700;
  box-shadow: var(--shadow-md);
  animation: pulse 2s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.05); }
}

.empty-state {
  text-align: center;
  padding: 100px 20px;
  color: var(--text-tertiary);
}

.empty-icon {
  font-size: 72px;
  margin-bottom: 24px;
  opacity: 0.6;
}

.empty-state p {
  font-size: 18px;
  color: var(--text-secondary);
}

@media (max-width: 1024px) {
  .personas-grid {
    grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  }
}

@media (max-width: 768px) {
  .personas-grid {
    grid-template-columns: 1fr;
  }
  
  .persona-card {
    padding: 28px 20px;
  }
}
</style>

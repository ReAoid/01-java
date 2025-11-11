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
  padding: 60px 20px;
  color: #6b7280;
}

.loading.large {
  width: 40px;
  height: 40px;
  border-width: 4px;
  margin-bottom: 20px;
}

.personas-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(250px, 1fr));
  gap: 20px;
  padding: 10px;
}

.persona-card {
  padding: 30px 20px;
  border: 2px solid #e5e7eb;
  border-radius: 16px;
  cursor: pointer;
  transition: all 0.3s ease;
  text-align: center;
  position: relative;
  background: white;
}

.persona-card:hover {
  border-color: #667eea;
  transform: translateY(-5px);
  box-shadow: 0 8px 20px rgba(102, 126, 234, 0.2);
}

.persona-card.active {
  border-color: #667eea;
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.05) 0%, rgba(118, 75, 162, 0.05) 100%);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
}

.persona-icon {
  font-size: 48px;
  margin-bottom: 15px;
}

.persona-name {
  margin: 10px 0;
  font-size: 20px;
  color: #1f2937;
  font-weight: 600;
}

.persona-description {
  color: #6b7280;
  font-size: 14px;
  margin: 10px 0;
  line-height: 1.5;
}

.active-badge {
  position: absolute;
  top: 15px;
  right: 15px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  padding: 6px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
  box-shadow: 0 2px 8px rgba(102, 126, 234, 0.3);
}

.empty-state {
  text-align: center;
  padding: 80px 20px;
  color: #9ca3af;
}

.empty-icon {
  font-size: 64px;
  margin-bottom: 20px;
}

.empty-state p {
  font-size: 18px;
}

@media (max-width: 768px) {
  .personas-grid {
    grid-template-columns: 1fr;
  }
}
</style>

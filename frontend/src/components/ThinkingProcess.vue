<template>
  <div v-if="visible && steps.length > 0" class="thinking-process">
    <div class="thinking-header">
      <span class="thinking-icon">🤔</span>
      <span class="thinking-title">AI 思考过程</span>
      <button class="close-btn" @click="$emit('close')">✕</button>
    </div>
    <div class="thinking-content">
      <div 
        v-for="(step, index) in steps" 
        :key="index"
        class="thinking-step"
        :class="{ active: index === currentStep }"
      >
        <div class="step-number">{{ index + 1 }}</div>
        <div class="step-content">
          <div class="step-title">{{ step.title }}</div>
          <div class="step-description">{{ step.description }}</div>
          <div v-if="step.result" class="step-result">
            ✓ {{ step.result }}
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'

const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  },
  steps: {
    type: Array,
    default: () => []
  }
})

defineEmits(['close'])

const currentStep = ref(0)

// 监听步骤变化,自动更新当前步骤
watch(() => props.steps.length, (newLength) => {
  if (newLength > 0) {
    currentStep.value = newLength - 1
  }
})
</script>

<style scoped>
.thinking-process {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 12px;
  padding: 20px;
  margin-bottom: 16px;
  color: white;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  animation: slideIn 0.3s ease;
}

@keyframes slideIn {
  from {
    opacity: 0;
    transform: translateY(-10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.thinking-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.2);
}

.thinking-icon {
  font-size: 24px;
}

.thinking-title {
  flex: 1;
  font-size: 16px;
  font-weight: 700;
}

.close-btn {
  background: transparent;
  border: none;
  color: white;
  font-size: 20px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 4px;
  transition: background 0.3s;
}

.close-btn:hover {
  background: rgba(255, 255, 255, 0.2);
}

.thinking-content {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.thinking-step {
  display: flex;
  gap: 12px;
  padding: 12px;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 8px;
  border-left: 3px solid rgba(255, 255, 255, 0.3);
  transition: all 0.3s ease;
  opacity: 0.6;
}

.thinking-step.active {
  opacity: 1;
  border-left-color: #ffd700;
  background: rgba(255, 255, 255, 0.15);
  animation: pulse 1.5s ease infinite;
}

@keyframes pulse {
  0%, 100% {
    transform: scale(1);
  }
  50% {
    transform: scale(1.02);
  }
}

.step-number {
  flex-shrink: 0;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.2);
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 14px;
}

.thinking-step.active .step-number {
  background: #ffd700;
  color: #764ba2;
}

.step-content {
  flex: 1;
}

.step-title {
  font-size: 14px;
  font-weight: 600;
  margin-bottom: 4px;
}

.step-description {
  font-size: 13px;
  opacity: 0.9;
  line-height: 1.4;
}

.step-result {
  font-size: 12px;
  margin-top: 6px;
  padding: 6px 10px;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 6px;
  display: inline-block;
}
</style>


<template>
  <div v-if="visible && steps.length > 0" class="thinking-process">
    <!-- 头部 -->
    <div class="thinking-header">
      <span class="thinking-icon">🤔</span>
      <span class="thinking-title">AI 思考过程</span>
      <button class="close-btn" @click="$emit('close')">✕</button>
    </div>
    
    <!-- 思考步骤列表 -->
    <div class="thinking-content">
      <div 
        v-for="(step, index) in steps" 
        :key="index"
        class="thinking-step"
        :class="{ active: index === currentStep }"
      >
        <!-- 步骤编号 -->
        <div class="step-number">{{ index + 1 }}</div>
        
        <!-- 步骤内容 -->
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

/**
 * ThinkingProcess 组件说明
 * 
 * 功能：
 * 显示AI的思考过程，让用户了解AI是如何分析和处理问题的
 * 
 * 使用场景：
 * 1. 复杂问题解答时，展示AI的推理步骤
 * 2. 联网搜索时，展示搜索和信息整合过程
 * 3. 代码生成时，展示分析和设计步骤
 * 
 * 思考步骤示例：
 * [
 *   {
 *     title: "理解问题",
 *     description: "分析用户的提问内容和意图",
 *     result: "识别出用户想要了解Vue3的组合式API"
 *   },
 *   {
 *     title: "搜索相关信息",
 *     description: "从知识库中查找相关文档",
 *     result: "找到5篇相关文档"
 *   },
 *   {
 *     title: "整合答案",
 *     description: "综合信息生成回复",
 *     result: "生成详细的解释和代码示例"
 *   }
 * ]
 * 
 * 使用方式：
 * <ThinkingProcess 
 *   :visible="showThinking"
 *   :steps="thinkingSteps"
 *   @close="showThinking = false"
 * />
 * 
 * Props:
 * - visible: 是否显示思考过程
 * - steps: 思考步骤数组
 * 
 * Events:
 * - close: 关闭思考过程面板
 */

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

// 监听步骤变化，自动更新当前步骤（高亮最新的步骤）
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

/* 当前活跃的步骤 - 高亮显示 */
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


<template>
  <div class="page-container">
    <div class="content-card">
      <!-- 卡片头部 -->
      <div class="card-header">
        <h1>🎙️ 语音合成 (TTS)</h1>
        <div class="subtitle">使用 GPT-SoVITS 进行高质量语音合成</div>
      </div>

      <!-- 主体内容 -->
      <div class="card-body">
        <div class="tts-form">
          <!-- 文本输入 -->
          <div class="form-section">
            <label class="form-label">
              <span class="label-icon">📝</span>
              输入文本
            </label>
            <textarea
              v-model="text"
              placeholder="请输入要合成的文本..."
              rows="4"
              class="form-textarea"
            ></textarea>
          </div>

          <!-- 参考音频选择 -->
          <div class="form-row">
            <div class="form-section flex-1">
              <label class="form-label">
                <span class="label-icon">🎵</span>
                参考音频
              </label>
              <select v-model="selectedReference" class="form-select">
                <option value="">请选择参考音频</option>
                <option v-for="audio in referenceAudios" :key="audio" :value="audio">
                  {{ audio }}
                </option>
              </select>
            </div>

            <div class="form-section flex-1">
              <label class="form-label">
                <span class="label-icon">😊</span>
                情感控制
              </label>
              <input
                v-model="emotion"
                type="text"
                placeholder="例如: 开心、悲伤、激动..."
                class="form-input"
              />
            </div>
          </div>

          <!-- 语速控制 -->
          <div class="form-section">
            <label class="form-label">
              <span class="label-icon">⚡</span>
              语速控制: {{ speed }}x
            </label>
            <input
              v-model.number="speed"
              type="range"
              min="0.5"
              max="2.0"
              step="0.1"
              class="form-range"
            />
          </div>

          <!-- 合成按钮 -->
          <button 
            class="primary synthesize-btn" 
            @click="handleSynthesize"
            :disabled="isLoading || !text.trim()"
          >
            <span v-if="!isLoading">🎵 开始合成</span>
            <span v-else><span class="loading"></span> 合成中...</span>
          </button>

          <!-- 音频播放器 -->
          <div v-if="audioUrl" class="audio-result">
            <div class="result-header">✅ 合成成功!</div>
            <audio :src="audioUrl" controls class="audio-player"></audio>
            <button class="secondary download-btn" @click="downloadAudio">
              📥 下载音频
            </button>
          </div>
        </div>

        <!-- 分隔线 -->
        <div class="divider"></div>

        <!-- 自定义说话人 -->
        <div class="tts-form">
          <h2 class="section-title">📤 自定义说话人合成</h2>
          
          <div class="form-section">
            <label class="form-label">
              <span class="label-icon">🎤</span>
              参考音频文件
            </label>
            <input
              type="file"
              accept="audio/*"
              @change="handleFileChange"
              class="form-file"
            />
            <div v-if="audioFile" class="file-info">
              已选择: {{ audioFile.name }}
            </div>
          </div>

          <div class="form-section">
            <label class="form-label">
              <span class="label-icon">📄</span>
              参考音频对应文本
            </label>
            <textarea
              v-model="referenceText"
              placeholder="请输入参考音频对应的文本内容..."
              rows="2"
              class="form-textarea"
            ></textarea>
          </div>

          <div class="form-section">
            <label class="form-label">
              <span class="label-icon">✏️</span>
              要合成的文本
            </label>
            <textarea
              v-model="customText"
              placeholder="使用该说话人合成的文本..."
              rows="3"
              class="form-textarea"
            ></textarea>
          </div>

          <button 
            class="primary synthesize-btn" 
            @click="handleCustomSynthesize"
            :disabled="isCustomLoading || !audioFile || !customText.trim()"
          >
            <span v-if="!isCustomLoading">🎵 开始自定义合成</span>
            <span v-else><span class="loading"></span> 合成中...</span>
          </button>

          <div v-if="customAudioUrl" class="audio-result">
            <div class="result-header">✅ 自定义合成成功!</div>
            <audio :src="customAudioUrl" controls class="audio-player"></audio>
            <button class="secondary download-btn" @click="downloadCustomAudio">
              📥 下载音频
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ttsApi } from '@/api/chatApi'

const text = ref('')
const selectedReference = ref('')
const emotion = ref('')
const speed = ref(1.0)
const isLoading = ref(false)
const audioUrl = ref(null)
const referenceAudios = ref([])

// 自定义说话人
const audioFile = ref(null)
const referenceText = ref('')
const customText = ref('')
const isCustomLoading = ref(false)
const customAudioUrl = ref(null)

// 加载参考音频列表
const loadReferenceAudios = async () => {
  try {
    const data = await ttsApi.getReferenceAudios()
    referenceAudios.value = data.audios || []
  } catch (error) {
    console.error('加载参考音频失败:', error)
    referenceAudios.value = ['派蒙.wav', '三月七.wav']
  }
}

// 合成语音
const handleSynthesize = async () => {
  if (!text.value.trim()) return

  isLoading.value = true
  try {
    const blob = await ttsApi.synthesize({
      text: text.value,
      emotion: emotion.value || undefined,
      referenceAudio: selectedReference.value || undefined,
      speed: speed.value
    })

    if (audioUrl.value) {
      URL.revokeObjectURL(audioUrl.value)
    }
    audioUrl.value = URL.createObjectURL(blob)
  } catch (error) {
    console.error('语音合成失败:', error)
    alert('语音合成失败: ' + error)
  } finally {
    isLoading.value = false
  }
}

// 处理文件选择
const handleFileChange = (event) => {
  const file = event.target.files[0]
  if (file) {
    audioFile.value = file
  }
}

// 自定义说话人合成
const handleCustomSynthesize = async () => {
  if (!audioFile.value || !customText.value.trim()) return

  isCustomLoading.value = true
  try {
    const formData = new FormData()
    formData.append('audio', audioFile.value)
    formData.append('referenceText', referenceText.value)
    formData.append('text', customText.value)

    const blob = await ttsApi.synthesizeWithCustomSpeaker(formData)

    if (customAudioUrl.value) {
      URL.revokeObjectURL(customAudioUrl.value)
    }
    customAudioUrl.value = URL.createObjectURL(blob)
  } catch (error) {
    console.error('自定义合成失败:', error)
    alert('自定义合成失败: ' + error)
  } finally {
    isCustomLoading.value = false
  }
}

// 下载音频
const downloadAudio = () => {
  if (!audioUrl.value) return
  const a = document.createElement('a')
  a.href = audioUrl.value
  a.download = `tts_${Date.now()}.wav`
  a.click()
}

const downloadCustomAudio = () => {
  if (!customAudioUrl.value) return
  const a = document.createElement('a')
  a.href = customAudioUrl.value
  a.download = `custom_tts_${Date.now()}.wav`
  a.click()
}

onMounted(() => {
  loadReferenceAudios()
})
</script>

<style scoped>
.tts-form {
  max-width: 850px;
  margin: 0 auto;
}

.form-section {
  margin-bottom: 24px;
}

.form-label {
  display: block;
  margin-bottom: 10px;
  font-weight: 600;
  color: var(--text-primary);
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 15px;
}

.label-icon {
  font-size: 20px;
}

.form-textarea,
.form-input,
.form-select {
  width: 100%;
  padding: 14px 18px;
  border: 2px solid var(--border-color);
  border-radius: 12px;
  font-size: 15px;
  font-family: inherit;
  transition: all 0.3s ease;
  background: var(--bg-secondary);
  color: var(--text-primary);
}

.form-textarea {
  resize: vertical;
  min-height: 120px;
  line-height: 1.6;
}

.form-textarea:focus,
.form-input:focus,
.form-select:focus {
  outline: none;
  border-color: var(--primary-color);
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
}

.form-row {
  display: flex;
  gap: 24px;
}

.flex-1 {
  flex: 1;
}

.form-range {
  width: 100%;
  height: 8px;
  border-radius: 4px;
  background: var(--bg-tertiary);
  outline: none;
  -webkit-appearance: none;
  transition: all 0.3s ease;
}

.form-range::-webkit-slider-thumb {
  -webkit-appearance: none;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: var(--primary-gradient);
  cursor: pointer;
  box-shadow: var(--shadow-md);
  transition: transform 0.2s ease;
}

.form-range::-webkit-slider-thumb:hover {
  transform: scale(1.15);
}

.form-range::-moz-range-thumb {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: var(--primary-gradient);
  cursor: pointer;
  box-shadow: var(--shadow-md);
  border: none;
  transition: transform 0.2s ease;
}

.form-range::-moz-range-thumb:hover {
  transform: scale(1.15);
}

.form-file {
  width: 100%;
  padding: 12px;
  border: 2px dashed var(--border-color);
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.3s ease;
  background: var(--bg-secondary);
  color: var(--text-primary);
}

.form-file:hover {
  border-color: var(--primary-color);
  background: var(--sidebar-hover);
}

.file-info {
  margin-top: 10px;
  padding: 10px 16px;
  background: var(--sidebar-hover);
  color: var(--primary-color);
  border-radius: 10px;
  font-size: 14px;
  font-weight: 500;
  border: 1px solid var(--primary-color);
}

.synthesize-btn {
  width: 100%;
  padding: 16px;
  font-size: 17px;
  font-weight: 700;
  margin-top: 12px;
  box-shadow: var(--shadow-md);
}

.synthesize-btn:hover:not(:disabled) {
  box-shadow: var(--shadow-lg);
}

.audio-result {
  margin-top: 24px;
  padding: 24px;
  background: #f0fdf4;
  border: 2px solid #86efac;
  border-radius: 16px;
  text-align: center;
  animation: fadeIn 0.5s ease;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.result-header {
  font-size: 20px;
  font-weight: 700;
  color: #15803d;
  margin-bottom: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.audio-player {
  width: 100%;
  margin-bottom: 16px;
  border-radius: 8px;
}

.download-btn {
  padding: 12px 28px;
  font-weight: 600;
  box-shadow: var(--shadow-sm);
}

.divider {
  height: 2px;
  background: linear-gradient(90deg, transparent, var(--border-color), transparent);
  margin: 48px 0;
  border-radius: 1px;
}

.section-title {
  font-size: 22px;
  color: var(--text-primary);
  margin-bottom: 24px;
  display: flex;
  align-items: center;
  gap: 12px;
  font-weight: 700;
}

@media (max-width: 768px) {
  .form-row {
    flex-direction: column;
  }
  
  .form-section {
    margin-bottom: 20px;
  }
}
</style>

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
  max-width: 800px;
  margin: 0 auto;
}

.form-section {
  margin-bottom: 20px;
}

.form-label {
  display: block;
  margin-bottom: 8px;
  font-weight: 600;
  color: #374151;
  display: flex;
  align-items: center;
  gap: 8px;
}

.label-icon {
  font-size: 18px;
}

.form-textarea,
.form-input,
.form-select {
  width: 100%;
  padding: 12px 15px;
  border: 2px solid #e5e7eb;
  border-radius: 8px;
  font-size: 14px;
  font-family: inherit;
  transition: border-color 0.3s;
}

.form-textarea {
  resize: vertical;
  min-height: 100px;
}

.form-textarea:focus,
.form-input:focus,
.form-select:focus {
  outline: none;
  border-color: #667eea;
}

.form-row {
  display: flex;
  gap: 20px;
}

.flex-1 {
  flex: 1;
}

.form-range {
  width: 100%;
  height: 6px;
  border-radius: 3px;
  background: #e5e7eb;
  outline: none;
  -webkit-appearance: none;
}

.form-range::-webkit-slider-thumb {
  -webkit-appearance: none;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  cursor: pointer;
  box-shadow: 0 2px 4px rgba(0,0,0,0.2);
}

.form-range::-moz-range-thumb {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  cursor: pointer;
  box-shadow: 0 2px 4px rgba(0,0,0,0.2);
  border: none;
}

.form-file {
  width: 100%;
  padding: 10px;
  border: 2px dashed #e5e7eb;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s;
}

.form-file:hover {
  border-color: #667eea;
  background: rgba(102, 126, 234, 0.05);
}

.file-info {
  margin-top: 8px;
  padding: 8px 12px;
  background: #e0e7ff;
  color: #667eea;
  border-radius: 6px;
  font-size: 14px;
}

.synthesize-btn {
  width: 100%;
  padding: 15px;
  font-size: 16px;
  font-weight: 600;
  margin-top: 10px;
}

.audio-result {
  margin-top: 20px;
  padding: 20px;
  background: #f0fdf4;
  border: 2px solid #86efac;
  border-radius: 12px;
  text-align: center;
}

.result-header {
  font-size: 18px;
  font-weight: 600;
  color: #15803d;
  margin-bottom: 15px;
}

.audio-player {
  width: 100%;
  margin-bottom: 15px;
}

.download-btn {
  padding: 10px 20px;
}

.divider {
  height: 2px;
  background: linear-gradient(90deg, transparent, #e5e7eb, transparent);
  margin: 40px 0;
}

.section-title {
  font-size: 20px;
  color: #374151;
  margin-bottom: 20px;
  display: flex;
  align-items: center;
  gap: 10px;
}

@media (max-width: 768px) {
  .form-row {
    flex-direction: column;
  }
}
</style>

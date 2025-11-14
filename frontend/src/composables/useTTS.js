import { ref } from 'vue'

/**
 * TTS (文本转语音) Composable
 * 
 * 功能说明：
 * 1. 接收后端发送的音频数据
 * 2. 在浏览器中播放音频
 * 3. 支持音频队列管理（多个音频片段依次播放）
 * 4. 支持音量和播放速率控制
 * 
 * 使用场景：
 * - AI回复的语音朗读
 * - 长文本的分段语音播放
 * - 实时流式语音输出
 */
export function useTTS() {
  const isPlaying = ref(false)           // 是否正在播放
  const currentAudio = ref(null)         // 当前音频对象
  const audioQueue = ref([])             // 音频播放队列
  const error = ref(null)                // 错误信息
  const volume = ref(1.0)                // 音量 (0.0 - 1.0)
  const playbackRate = ref(1.0)          // 播放速率 (0.5 - 2.0)

  /**
   * 播放音频
   * 
   * @param {Blob|string} audioData - 音频Blob或Base64字符串
   * @param {Function} onEnd - 播放结束回调
   * 
   * 流程：
   * 1. 如果正在播放，将音频加入队列
   * 2. 将音频数据转换为可播放的URL
   * 3. 创建Audio对象并播放
   * 4. 播放结束后自动播放队列中的下一个
   */
  const playAudio = async (audioData, onEnd = null) => {
    try {
      // 如果正在播放，加入队列
      if (isPlaying.value) {
        audioQueue.value.push({ audioData, onEnd })
        console.log('🔊 音频已加入播放队列，当前队列长度:', audioQueue.value.length)
        return
      }

      let audioUrl

      // 处理不同类型的音频数据
      if (audioData instanceof Blob) {
        // Blob类型 - 直接创建URL
        audioUrl = URL.createObjectURL(audioData)
      } else if (typeof audioData === 'string') {
        // Base64字符串 - 先转换为Blob
        const byteCharacters = atob(audioData)
        const byteNumbers = new Array(byteCharacters.length)
        for (let i = 0; i < byteCharacters.length; i++) {
          byteNumbers[i] = byteCharacters.charCodeAt(i)
        }
        const byteArray = new Uint8Array(byteNumbers)
        const blob = new Blob([byteArray], { type: 'audio/wav' })
        audioUrl = URL.createObjectURL(blob)
      } else {
        throw new Error('不支持的音频数据格式')
      }

      // 创建音频元素
      currentAudio.value = new Audio(audioUrl)
      currentAudio.value.volume = volume.value
      currentAudio.value.playbackRate = playbackRate.value

      // 监听播放结束
      currentAudio.value.onended = () => {
        isPlaying.value = false
        URL.revokeObjectURL(audioUrl)
        
        if (onEnd) {
          onEnd()
        }
        
        // 播放队列中的下一个音频
        playNextInQueue()
      }

      // 监听播放错误
      currentAudio.value.onerror = (err) => {
        console.error('❌ 音频播放失败:', err)
        error.value = '音频播放失败'
        isPlaying.value = false
        URL.revokeObjectURL(audioUrl)
        
        // 尝试播放下一个
        playNextInQueue()
      }

      // 开始播放
      await currentAudio.value.play()
      isPlaying.value = true
      error.value = null
      
      console.log('🔊 TTS音频播放开始')

    } catch (err) {
      console.error('❌ 播放音频失败:', err)
      error.value = err.message || '播放失败'
      isPlaying.value = false
    }
  }

  /**
   * 播放队列中的下一个音频
   */
  const playNextInQueue = () => {
    if (audioQueue.value.length > 0) {
      const { audioData, onEnd } = audioQueue.value.shift()
      console.log('🔊 播放队列中的下一个音频，剩余:', audioQueue.value.length)
      playAudio(audioData, onEnd)
    }
  }

  /**
   * 暂停播放
   */
  const pauseAudio = () => {
    if (currentAudio.value && isPlaying.value) {
      currentAudio.value.pause()
      isPlaying.value = false
      console.log('⏸️ TTS音频已暂停')
    }
  }

  /**
   * 继续播放
   */
  const resumeAudio = () => {
    if (currentAudio.value && !isPlaying.value) {
      currentAudio.value.play()
      isPlaying.value = true
      console.log('▶️ TTS音频继续播放')
    }
  }

  /**
   * 停止播放
   * 
   * 会清空当前播放和整个队列
   */
  const stopAudio = () => {
    if (currentAudio.value) {
      currentAudio.value.pause()
      currentAudio.value.currentTime = 0
      isPlaying.value = false
      console.log('⏹️ TTS音频已停止')
    }
    
    // 清空队列
    audioQueue.value = []
  }

  /**
   * 设置音量
   * @param {number} vol - 音量值 (0.0 - 1.0)
   */
  const setVolume = (vol) => {
    volume.value = Math.max(0, Math.min(1, vol))
    if (currentAudio.value) {
      currentAudio.value.volume = volume.value
    }
  }

  /**
   * 设置播放速率
   * @param {number} rate - 播放速率 (0.5 - 2.0)
   */
  const setPlaybackRate = (rate) => {
    playbackRate.value = Math.max(0.5, Math.min(2, rate))
    if (currentAudio.value) {
      currentAudio.value.playbackRate = playbackRate.value
    }
  }

  /**
   * 处理来自WebSocket的TTS音频数据
   * 
   * @param {Object} message - WebSocket消息对象
   * 
   * 消息格式：
   * {
   *   type: 'tts_audio',
   *   audio: 'base64编码的音频数据',
   *   sentenceId: '句子ID'
   * }
   */
  const handleTTSAudio = (message) => {
    if (message.audio) {
      playAudio(message.audio, () => {
        // 播放完成后通知后端
        if (message.sentenceId) {
          notifyAudioPlaybackCompleted(message.sentenceId)
        }
      })
    }
  }

  /**
   * 通知后端音频播放完成
   * @param {string} sentenceId - 句子ID
   */
  const notifyAudioPlaybackCompleted = (sentenceId) => {
    // 这个函数需要在使用时传入wsManager
    console.log('✅ 音频播放完成:', sentenceId)
  }

  /**
   * 获取队列长度
   */
  const getQueueLength = () => {
    return audioQueue.value.length
  }

  /**
   * 清空队列
   */
  const clearQueue = () => {
    audioQueue.value = []
  }

  return {
    isPlaying,
    error,
    volume,
    playbackRate,
    playAudio,
    pauseAudio,
    resumeAudio,
    stopAudio,
    setVolume,
    setPlaybackRate,
    handleTTSAudio,
    getQueueLength,
    clearQueue
  }
}

export default useTTS


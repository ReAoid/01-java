import { ref } from 'vue'

/**
 * TTS (文本转语音) Composable
 */
export function useTTS() {
  const isPlaying = ref(false)
  const currentAudio = ref(null)
  const audioQueue = ref([])
  const error = ref(null)
  const volume = ref(1.0)
  const playbackRate = ref(1.0)

  /**
   * 播放音频
   * @param {Blob|string} audioData - 音频Blob或Base64字符串
   * @param {Function} onEnd - 播放结束回调
   */
  const playAudio = async (audioData, onEnd = null) => {
    try {
      // 如果正在播放,加入队列
      if (isPlaying.value) {
        audioQueue.value.push({ audioData, onEnd })
        return
      }

      let audioUrl

      // 处理不同类型的音频数据
      if (audioData instanceof Blob) {
        audioUrl = URL.createObjectURL(audioData)
      } else if (typeof audioData === 'string') {
        // 假设是Base64数据
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
   * @param {Object} message - WebSocket消息对象
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


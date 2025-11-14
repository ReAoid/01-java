import { ref } from 'vue'
import wsManager from '@/api/websocket'

/**
 * ASR (自动语音识别) Composable
 * 
 * 功能说明：
 * 1. 使用浏览器的MediaRecorder API录制音频
 * 2. 将录制的音频通过WebSocket发送到后端进行识别
 * 3. 接收并处理后端返回的识别结果
 * 
 * 使用场景：
 * - 语音输入替代键盘输入
 * - 实时语音转文字
 * - 支持长时间连续录音
 */
export function useASR() {
  const isRecording = ref(false)         // 是否正在录音
  const isProcessing = ref(false)        // 是否正在处理识别
  const audioStream = ref(null)          // 音频流对象
  const mediaRecorder = ref(null)        // 媒体录制器
  const audioChunks = ref([])            // 音频数据块
  const recognizedText = ref('')         // 识别结果文本
  const error = ref(null)                // 错误信息

  /**
   * 检查浏览器是否支持音频录制
   */
  const checkSupport = () => {
    if (!navigator.mediaDevices || !navigator.mediaDevices.getUserMedia) {
      error.value = '浏览器不支持音频录制功能'
      return false
    }
    return true
  }

  /**
   * 开始录音
   * 
   * 流程：
   * 1. 请求麦克风权限
   * 2. 创建MediaRecorder实例
   * 3. 开始录制音频
   * 4. 每秒收集一次音频数据
   */
  const startRecording = async () => {
    if (!checkSupport()) {
      return false
    }

    try {
      // 请求麦克风权限并获取音频流
      audioStream.value = await navigator.mediaDevices.getUserMedia({ 
        audio: {
          echoCancellation: true,      // 回声消除
          noiseSuppression: true,      // 噪音抑制
          autoGainControl: true        // 自动增益控制
        } 
      })

      // 创建 MediaRecorder（使用webm格式）
      mediaRecorder.value = new MediaRecorder(audioStream.value, {
        mimeType: 'audio/webm'
      })

      audioChunks.value = []

      // 监听数据可用事件 - 每秒触发一次
      mediaRecorder.value.ondataavailable = (event) => {
        if (event.data.size > 0) {
          audioChunks.value.push(event.data)
        }
      }

      // 监听录制停止事件
      mediaRecorder.value.onstop = async () => {
        isRecording.value = false
        isProcessing.value = true

        // 将音频块合并为 Blob
        const audioBlob = new Blob(audioChunks.value, { type: 'audio/webm' })
        
        // 发送到后端进行识别
        await sendAudioForRecognition(audioBlob)
        
        isProcessing.value = false
      }

      // 开始录制（每1000ms收集一次数据）
      mediaRecorder.value.start(1000)
      isRecording.value = true
      error.value = null
      
      console.log('✅ ASR录音已开始')
      return true

    } catch (err) {
      console.error('❌ 启动录音失败:', err)
      error.value = err.message || '无法访问麦克风'
      return false
    }
  }

  /**
   * 停止录音
   */
  const stopRecording = () => {
    if (mediaRecorder.value && isRecording.value) {
      mediaRecorder.value.stop()
      console.log('🛑 ASR录音已停止')
    }
    
    // 停止音频流
    if (audioStream.value) {
      audioStream.value.getTracks().forEach(track => track.stop())
      audioStream.value = null
    }
  }

  /**
   * 发送音频数据到后端进行识别
   * 
   * 流程：
   * 1. 将Blob转换为Base64编码
   * 2. 通过WebSocket发送到后端
   * 3. 后端调用ASR服务进行识别
   */
  const sendAudioForRecognition = async (audioBlob) => {
    try {
      // 转换为 Base64
      const reader = new FileReader()
      reader.readAsDataURL(audioBlob)
      
      reader.onloadend = () => {
        const base64Audio = reader.result.split(',')[1]
        
        // 通过 WebSocket 发送音频数据
        if (wsManager.isConnected) {
          wsManager.send({
            type: 'asr_audio_chunk',
            audio: base64Audio,
            format: 'webm',
            timestamp: Date.now()
          })
          
          console.log('📤 ASR音频数据已发送')
        } else {
          console.error('❌ WebSocket未连接，无法发送音频')
          error.value = 'WebSocket未连接'
        }
      }
    } catch (err) {
      console.error('❌ 发送音频失败:', err)
      error.value = '发送音频失败'
    }
  }

  /**
   * 处理ASR识别结果
   * 
   * 从WebSocket消息中提取识别文本
   */
  const handleASRResult = (message) => {
    if (message.text) {
      recognizedText.value = message.text
      console.log('🎤 ASR识别结果:', message.text)
    }
  }

  /**
   * 清空识别结果
   */
  const clearResult = () => {
    recognizedText.value = ''
  }

  /**
   * 切换录音状态
   */
  const toggleRecording = async () => {
    if (isRecording.value) {
      stopRecording()
    } else {
      await startRecording()
    }
  }

  return {
    isRecording,
    isProcessing,
    recognizedText,
    error,
    startRecording,
    stopRecording,
    toggleRecording,
    handleASRResult,
    clearResult,
    checkSupport
  }
}

export default useASR


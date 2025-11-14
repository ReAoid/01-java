/**
 * WebSocket 连接管理
 * 用于实时通信、流式响应
 */

class WebSocketManager {
  constructor() {
    this.ws = null
    this.isConnected = false
    this.reconnectAttempts = 0
    this.maxReconnectAttempts = 5
    this.reconnectDelay = 3000
    this.messageHandlers = new Map()
    this.reconnectTimer = null
  }

  /**
   * 连接WebSocket
   */
  connect() {
    return new Promise((resolve, reject) => {
      try {
        const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
        const wsUrl = `${protocol}//${window.location.host}/ws/chat`
        
        console.log('🔌 正在连接WebSocket:', wsUrl)
        
        this.ws = new WebSocket(wsUrl)

        this.ws.onopen = () => {
          console.log('✅ WebSocket连接成功')
          this.isConnected = true
          this.reconnectAttempts = 0
          this.notifyHandlers('connection', { status: 'connected' })
          resolve()
        }

        this.ws.onclose = () => {
          console.log('🔌 WebSocket连接断开')
          this.isConnected = false
          this.notifyHandlers('connection', { status: 'disconnected' })
          this.attemptReconnect()
        }

        this.ws.onerror = (error) => {
          console.error('❌ WebSocket错误:', error)
          this.notifyHandlers('error', { error })
          reject(error)
        }

        this.ws.onmessage = (event) => {
          try {
            const message = JSON.parse(event.data)
            console.log('📨 收到WebSocket消息:', message)
            this.handleMessage(message)
          } catch (error) {
            console.error('❌ 解析WebSocket消息失败:', error)
          }
        }
      } catch (error) {
        console.error('❌ 创建WebSocket失败:', error)
        reject(error)
      }
    })
  }

  /**
   * 断开连接
   */
  disconnect() {
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer)
      this.reconnectTimer = null
    }
    
    if (this.ws) {
      this.ws.close()
      this.ws = null
    }
    
    this.isConnected = false
    this.reconnectAttempts = 0
  }

  /**
   * 尝试重连
   */
  attemptReconnect() {
    if (this.reconnectAttempts >= this.maxReconnectAttempts) {
      console.log('❌ 达到最大重连次数，停止重连')
      this.notifyHandlers('connection', { status: 'failed' })
      return
    }

    this.reconnectAttempts++
    console.log(`🔄 ${this.reconnectDelay / 1000}秒后尝试重连 (${this.reconnectAttempts}/${this.maxReconnectAttempts})`)
    
    this.reconnectTimer = setTimeout(() => {
      this.connect().catch(() => {
        // 重连失败会触发 onclose，继续下一次重连
      })
    }, this.reconnectDelay)
  }

  /**
   * 发送消息
   */
  send(message) {
    if (!this.isConnected || !this.ws) {
      console.error('❌ WebSocket未连接')
      return false
    }

    try {
      const data = typeof message === 'string' ? message : JSON.stringify(message)
      this.ws.send(data)
      console.log('📤 发送WebSocket消息:', message)
      return true
    } catch (error) {
      console.error('❌ 发送WebSocket消息失败:', error)
      return false
    }
  }

  /**
   * 处理收到的消息
   */
  handleMessage(message) {
    const { type } = message

    // 根据消息类型分发
    if (this.messageHandlers.has(type)) {
      const handlers = this.messageHandlers.get(type)
      handlers.forEach(handler => {
        try {
          handler(message)
        } catch (error) {
          console.error(`❌ 处理消息失败 [${type}]:`, error)
        }
      })
    }

    // 通用消息处理器
    if (this.messageHandlers.has('*')) {
      const handlers = this.messageHandlers.get('*')
      handlers.forEach(handler => handler(message))
    }
  }

  /**
   * 注册消息处理器
   * @param {string} type - 消息类型，'*' 表示所有消息
   * @param {Function} handler - 处理函数
   */
  on(type, handler) {
    if (!this.messageHandlers.has(type)) {
      this.messageHandlers.set(type, [])
    }
    this.messageHandlers.get(type).push(handler)
  }

  /**
   * 移除消息处理器
   */
  off(type, handler) {
    if (!this.messageHandlers.has(type)) return
    
    const handlers = this.messageHandlers.get(type)
    const index = handlers.indexOf(handler)
    if (index > -1) {
      handlers.splice(index, 1)
    }
  }

  /**
   * 通知所有处理器
   */
  notifyHandlers(type, data) {
    this.handleMessage({ type, ...data })
  }

  /**
   * 获取连接状态
   */
  getConnectionStatus() {
    return {
      isConnected: this.isConnected,
      readyState: this.ws?.readyState,
      reconnectAttempts: this.reconnectAttempts
    }
  }
}

// 创建单例实例
const wsManager = new WebSocketManager()

export default wsManager


/**
 * 简单的消息提示工具
 * 替代 Element Plus 的 ElMessage
 */

let messageContainer = null

// 创建消息容器
function createMessageContainer() {
  if (!messageContainer) {
    messageContainer = document.createElement('div')
    messageContainer.className = 'message-container'
    messageContainer.style.cssText = `
      position: fixed;
      top: 20px;
      left: 50%;
      transform: translateX(-50%);
      z-index: 9999;
      pointer-events: none;
    `
    document.body.appendChild(messageContainer)
  }
  return messageContainer
}

// 创建消息元素
function createMessage(message, type = 'info') {
  const container = createMessageContainer()
  
  const messageEl = document.createElement('div')
  messageEl.className = `message message-${type}`
  
  // 图标
  const icons = {
    success: '✅',
    error: '❌',
    warning: '⚠️',
    info: 'ℹ️'
  }
  
  // 颜色
  const colors = {
    success: '#67c23a',
    error: '#f56c6c',
    warning: '#e6a23c',
    info: '#909399'
  }
  
  messageEl.innerHTML = `
    <span style="margin-right: 8px;">${icons[type]}</span>
    <span>${message}</span>
  `
  
  messageEl.style.cssText = `
    display: inline-flex;
    align-items: center;
    padding: 12px 20px;
    margin-bottom: 10px;
    background: white;
    color: #333;
    border-radius: 8px;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
    font-size: 14px;
    font-weight: 500;
    pointer-events: auto;
    animation: slideIn 0.3s ease;
    border-left: 4px solid ${colors[type]};
  `
  
  container.appendChild(messageEl)
  
  // 3秒后自动移除
  setTimeout(() => {
    messageEl.style.animation = 'slideOut 0.3s ease'
    setTimeout(() => {
      container.removeChild(messageEl)
    }, 300)
  }, 3000)
}

// 添加动画样式
if (!document.getElementById('message-animations')) {
  const style = document.createElement('style')
  style.id = 'message-animations'
  style.textContent = `
    @keyframes slideIn {
      from {
        opacity: 0;
        transform: translateY(-20px);
      }
      to {
        opacity: 1;
        transform: translateY(0);
      }
    }
    
    @keyframes slideOut {
      from {
        opacity: 1;
        transform: translateY(0);
      }
      to {
        opacity: 0;
        transform: translateY(-20px);
      }
    }
  `
  document.head.appendChild(style)
}

// 导出消息函数
export const Message = {
  success(message) {
    createMessage(message, 'success')
  },
  error(message) {
    createMessage(message, 'error')
  },
  warning(message) {
    createMessage(message, 'warning')
  },
  info(message) {
    createMessage(message, 'info')
  }
}

export default Message


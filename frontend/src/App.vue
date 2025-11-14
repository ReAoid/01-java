<template>
  <div id="app" :class="{ 'dark-mode': isDarkMode }">
    <!-- 左侧Tab导航 -->
    <aside class="sidebar">
      <div class="logo-section">
        <img src="@/assets/favicon.png" alt="Logo" class="logo" />
        <span class="logo-text">AIChat</span>
      </div>
      
      <nav class="nav-buttons">
        <router-link to="/" class="tab-button" title="Chats">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"></path>
          </svg>
          <span class="tab-text">Chats</span>
        </router-link>
        <router-link to="/settings" class="tab-button" title="Settings">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="12" cy="12" r="3"></circle>
            <path d="M12 1v6m0 6v6"></path>
            <path d="M17 12h5M2 12h5"></path>
          </svg>
          <span class="tab-text">Settings</span>
        </router-link>
      </nav>

      <div class="sidebar-footer">
        <!-- User Profile -->
        <div class="user-profile">
          <img src="@/assets/user-avatar.jpg" alt="User" class="user-avatar" />
          <div class="user-info">
            <div class="user-name">Rin</div>
            <div class="user-email">Rin@gmail.com</div>
          </div>
          <button class="profile-menu-btn">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M9 18l6-6-6-6"/>
            </svg>
          </button>
        </div>
      </div>
    </aside>

    <!-- 主内容区域 -->
    <main class="main-content">
      <router-view />
    </main>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'

const isDarkMode = ref(false)

const toggleTheme = () => {
  isDarkMode.value = !isDarkMode.value
  localStorage.setItem('theme', isDarkMode.value ? 'dark' : 'light')
}

onMounted(() => {
  const savedTheme = localStorage.getItem('theme')
  isDarkMode.value = savedTheme === 'dark'
})
</script>

<style>
/* CSS 变量定义 - 动漫插画风格配色 */
:root {
  --primary-gradient: linear-gradient(135deg, #ff9966 0%, #ff8c5a 100%);
  --primary-color: #ff9966;
  --primary-dark: #ff7a45;
  --secondary-color: #ff8c5a;
  
  /* 天空蓝绿背景 */
  --bg-primary: #a8c5c5;
  --bg-secondary: #2a2d3a;
  --bg-tertiary: #353847;
  --bg-gradient: linear-gradient(135deg, #a8c5c5 0%, #9ab8b8 100%);
  
  --text-primary: #ffffff;
  --text-secondary: #e8d4bf;
  --text-tertiary: #b8a89a;
  
  --border-color: #3d4050;
  --border-light: #35384a;
  
  --shadow-sm: 0 1px 2px rgba(0, 0, 0, 0.3);
  --shadow-md: 0 4px 6px -1px rgba(0, 0, 0, 0.4);
  --shadow-lg: 0 10px 15px -3px rgba(0, 0, 0, 0.5);
  --shadow-xl: 0 20px 25px -5px rgba(0, 0, 0, 0.6);
  
  --user-message-bg: linear-gradient(135deg, #ff9966 0%, #ff8c5a 100%);
  --assistant-message-bg: #2a2d3a;
  
  --sidebar-bg: #1f2233;
  --sidebar-hover: rgba(255, 153, 102, 0.15);
  --sidebar-active: rgba(255, 153, 102, 0.25);
  
  /* 卡片背景 */
  --card-bg: rgba(42, 45, 58, 0.5);
  --card-hover: rgba(42, 45, 58, 0.85);
  
  /* 奶油色点缀 */
  --accent-cream: #f5e6d3;
  --accent-light: #e8d4bf;
}

/* 浅色主题（如需要） */
.light-mode {
  --bg-primary: #f8f9fa;
  --bg-secondary: #ffffff;
  --bg-tertiary: #f1f3f5;
  --bg-gradient: linear-gradient(135deg, #f5f7fa 0%, #e3e7ef 100%);
  
  --text-primary: #1a1a1a;
  --text-secondary: #6b7280;
  --text-tertiary: #9ca3af;
  
  --border-color: #e5e7eb;
  --border-light: #f3f4f6;
  
  --assistant-message-bg: #f3f4f6;
  
  --sidebar-bg: rgba(255, 255, 255, 0.95);
  --sidebar-hover: rgba(139, 92, 246, 0.1);
  --sidebar-active: rgba(139, 92, 246, 0.15);
  
  --card-bg: rgba(255, 255, 255, 0.5);
  --card-hover: rgba(255, 255, 255, 0.9);
}

/* 全局样式重置 */
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'Roboto', 'Oxygen',
    'Ubuntu', 'Cantarell', 'Fira Sans', 'Droid Sans', 'Helvetica Neue', sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  margin: 0;
  padding: 0;
  background: var(--bg-primary);
  height: 100vh;
  overflow: hidden;
  color: var(--text-primary);
  transition: background 0.3s ease, color 0.3s ease;
}

#app {
  display: flex;
  height: 100vh;
  overflow: hidden;
  transition: all 0.3s ease;
}

/* 左侧侧边栏 */
.sidebar {
  width: 240px;
  background: var(--sidebar-bg);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-right: 1px solid var(--border-color);
  display: flex;
  flex-direction: column;
  padding: 20px 0;
  z-index: 1000;
  transition: all 0.3s ease;
}

/* Logo区域 */
.logo-section {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 0 20px 30px 20px;
  border-bottom: 1px solid var(--border-light);
  margin-bottom: 20px;
}

.logo {
  width: 32px;
  height: 32px;
  object-fit: contain;
  animation: float 3s ease-in-out infinite;
  display: block;
}

@keyframes float {
  0%, 100% { transform: translateY(0px); }
  50% { transform: translateY(-5px); }
}

.logo-text {
  font-size: 20px;
  font-weight: 700;
  color: var(--primary-color);
  text-shadow: 0 2px 8px rgba(255, 153, 102, 0.3);
}

/* 导航按钮 */
.nav-buttons {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 0 12px;
}

.tab-button {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  border-radius: 12px;
  background: transparent;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  text-decoration: none;
  color: var(--text-secondary);
  font-size: 15px;
  font-weight: 500;
  position: relative;
  overflow: hidden;
}

.tab-button::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  width: 100%;
  height: 100%;
  background: var(--primary-gradient);
  opacity: 0;
  transition: opacity 0.3s ease;
  z-index: -1;
}

.tab-button:hover {
  background: var(--sidebar-hover);
  transform: translateX(4px);
  color: var(--primary-color);
}

.tab-button.router-link-active {
  background: var(--sidebar-active);
  color: var(--primary-color);
  box-shadow: var(--shadow-sm);
}

.tab-button.router-link-active svg {
  stroke: var(--primary-color);
}

.tab-button svg {
  flex-shrink: 0;
  transition: all 0.3s ease;
}

.tab-text {
  white-space: nowrap;
}

/* 侧边栏底部 */
.sidebar-footer {
  padding: 12px;
  margin-top: auto;
}

/* 用户信息 */
.user-profile {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  border-radius: 12px;
  background: var(--bg-tertiary);
  cursor: pointer;
  transition: all 0.3s ease;
}

.user-profile:hover {
  background: var(--sidebar-hover);
}

.user-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  border: 2px solid var(--primary-color);
  flex-shrink: 0;
  box-shadow: 0 2px 8px rgba(255, 153, 102, 0.3);
}

.user-info {
  flex: 1;
  min-width: 0;
}

.user-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.user-email {
  font-size: 12px;
  color: var(--text-tertiary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.profile-menu-btn {
  width: 24px;
  height: 24px;
  border-radius: 6px;
  background: transparent;
  border: none;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--text-tertiary);
  transition: all 0.2s ease;
  flex-shrink: 0;
}

.profile-menu-btn:hover {
  background: var(--sidebar-hover);
  color: var(--text-primary);
}

/* 主内容区域 */
.main-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  position: relative;
  overflow: hidden;
  background: var(--bg-primary);
}

/* 页面容器通用样式 */
.page-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  padding: 30px;
  overflow: hidden;
}

/* 内容卡片 */
.content-card {
  width: 100%;
  max-width: 1200px;
  height: 100%;
  margin: 0 auto;
  background: var(--bg-secondary);
  border-radius: 24px;
  box-shadow: var(--shadow-xl);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  border: 1px solid var(--border-light);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  transition: all 0.3s ease;
}

/* 卡片头部 */
.card-header {
  background: var(--bg-secondary);
  border-bottom: 1px solid var(--border-light);
  color: var(--text-primary);
  padding: 24px 30px;
  flex-shrink: 0;
  transition: all 0.3s ease;
}

.card-header h1 {
  margin: 0;
  font-size: 24px;
  font-weight: 700;
  display: flex;
  align-items: center;
  gap: 10px;
}

.card-header .subtitle {
  margin-top: 8px;
  font-size: 14px;
  color: var(--text-secondary);
  font-weight: 400;
}

/* 卡片主体 */
.card-body {
  flex: 1;
  overflow-y: auto;
  padding: 24px 30px;
  background: var(--bg-secondary);
}

/* 卡片底部 */
.card-footer {
  padding: 20px 30px;
  border-top: 1px solid var(--border-light);
  background: var(--bg-secondary);
  flex-shrink: 0;
}

/* 滚动条样式 */
::-webkit-scrollbar {
  width: 10px;
  height: 10px;
}

::-webkit-scrollbar-track {
  background: var(--bg-tertiary);
  border-radius: 5px;
}

::-webkit-scrollbar-thumb {
  background: var(--text-tertiary);
  border-radius: 5px;
  transition: background 0.3s ease;
}

::-webkit-scrollbar-thumb:hover {
  background: var(--text-secondary);
}

/* 按钮样式 */
button {
  cursor: pointer;
  border: none;
  padding: 10px 20px;
  border-radius: 12px;
  font-size: 14px;
  font-weight: 600;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  font-family: inherit;
}

button.primary {
  background: var(--primary-gradient);
  color: white;
  box-shadow: var(--shadow-md);
}

button.primary:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: var(--shadow-lg);
}

button.primary:active:not(:disabled) {
  transform: translateY(0);
}

button.secondary {
  background: var(--bg-tertiary);
  color: var(--text-primary);
}

button.secondary:hover:not(:disabled) {
  background: var(--border-color);
}

button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  transform: none !important;
}

/* 输入框样式 */
input, textarea, select {
  padding: 12px 16px;
  border: 2px solid var(--border-color);
  border-radius: 12px;
  font-size: 14px;
  transition: all 0.3s ease;
  font-family: inherit;
  background: var(--bg-secondary);
  color: var(--text-primary);
}

input:focus, textarea:focus, select:focus {
  outline: none;
  border-color: var(--primary-color);
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
}

/* 加载动画 */
.loading {
  display: inline-block;
  width: 20px;
  height: 20px;
  border: 3px solid rgba(102, 126, 234, 0.2);
  border-radius: 50%;
  border-top-color: var(--primary-color);
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* 脉冲动画 */
@keyframes pulse {
  0%, 100% { opacity: 0.6; }
  50% { opacity: 1; }
}

/* 光标闪烁动画 */
@keyframes cursor-blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0; }
}

/* 淡入动画 */
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

/* 响应式设计 */
@media (max-width: 768px) {
  .sidebar {
    width: 70px;
  }
  
  .logo-text,
  .tab-text {
    display: none;
  }
  
  .logo-section {
    justify-content: center;
    padding-bottom: 20px;
  }
  
  .tab-button {
    justify-content: center;
  }
  
  .user-email {
    display: none;
  }
  
  .user-profile {
    justify-content: center;
    padding: 8px;
  }
  
  .user-info {
    display: none;
  }
  
  .page-container {
    padding: 15px;
  }
  
  .content-card {
    border-radius: 16px;
  }
}
</style>


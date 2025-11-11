<template>
  <div id="app">
    <!-- 左侧Tab导航 -->
    <aside class="sidebar">
      <router-link to="/" class="tab-button" title="聊天">
        💬
      </router-link>
      <router-link to="/tts" class="tab-button" title="语音合成">
        🎙️
      </router-link>
      <router-link to="/personas" class="tab-button" title="角色管理">
        👥
      </router-link>
      <router-link to="/settings" class="tab-button" title="设置">
        ⚙️
      </router-link>
    </aside>

    <!-- 主内容区域 -->
    <main class="main-content">
      <router-view />
    </main>
  </div>
</template>

<script setup>
// 主应用组件 - 采用左侧导航设计
</script>

<style>
/* 全局样式重置 */
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
  margin: 0;
  padding: 0;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  height: 100vh;
  overflow: hidden;
}

#app {
  display: flex;
  height: 100vh;
  overflow: hidden;
}

/* 左侧Tab导航 */
.sidebar {
  width: 60px;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  box-shadow: 2px 0 10px rgba(0, 0, 0, 0.1);
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 20px 0;
  z-index: 1000;
}

.tab-button {
  width: 40px;
  height: 40px;
  border: none;
  border-radius: 10px;
  background: transparent;
  cursor: pointer;
  margin: 10px 0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  transition: all 0.3s ease;
  position: relative;
  text-decoration: none;
  color: #333;
}

.tab-button:hover {
  background: rgba(103, 126, 234, 0.1);
  transform: scale(1.1);
}

.tab-button.router-link-active {
  background: rgba(103, 126, 234, 0.2);
  color: #667eea;
}

.tab-button.router-link-active::after {
  content: '';
  position: absolute;
  right: -10px;
  width: 3px;
  height: 20px;
  background: #667eea;
  border-radius: 2px;
}

/* 主内容区域 */
.main-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  position: relative;
  overflow: hidden;
}

/* 页面容器通用样式 */
.page-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 20px;
  background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
  overflow: hidden;
}

/* 内容卡片 */
.content-card {
  width: 70%;
  height: 70vh;
  background: white;
  border-radius: 12px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

/* 卡片头部 */
.card-header {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  padding: 20px;
  flex-shrink: 0;
}

.card-header h1 {
  margin: 0;
  font-size: 24px;
  font-weight: 600;
}

.card-header .subtitle {
  margin-top: 5px;
  font-size: 14px;
  opacity: 0.9;
}

/* 卡片主体 */
.card-body {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  background: white;
}

/* 卡片底部 */
.card-footer {
  padding: 20px;
  border-top: 1px solid #eee;
  background: white;
  flex-shrink: 0;
}

/* 滚动条样式 */
::-webkit-scrollbar {
  width: 8px;
  height: 8px;
}

::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 4px;
}

::-webkit-scrollbar-thumb {
  background: #888;
  border-radius: 4px;
}

::-webkit-scrollbar-thumb:hover {
  background: #555;
}

/* 按钮样式 */
button {
  cursor: pointer;
  border: none;
  padding: 10px 20px;
  border-radius: 8px;
  font-size: 14px;
  transition: all 0.3s ease;
  font-weight: 500;
}

button.primary {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

button.primary:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
}

button.secondary {
  background: #e0e7ff;
  color: #667eea;
}

button.secondary:hover:not(:disabled) {
  background: #c7d2fe;
}

button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  transform: none !important;
}

/* 输入框样式 */
input, textarea, select {
  padding: 10px 15px;
  border: 2px solid #e5e7eb;
  border-radius: 8px;
  font-size: 14px;
  transition: border-color 0.3s;
  font-family: inherit;
}

input:focus, textarea:focus, select:focus {
  outline: none;
  border-color: #667eea;
}

/* 加载动画 */
.loading {
  display: inline-block;
  width: 20px;
  height: 20px;
  border: 3px solid rgba(102, 126, 234, 0.3);
  border-radius: 50%;
  border-top-color: #667eea;
  animation: spin 1s ease-in-out infinite;
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
</style>


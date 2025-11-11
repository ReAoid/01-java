import { createRouter, createWebHistory } from 'vue-router'
import ChatView from '../views/ChatView.vue'
import TTSView from '../views/TTSView.vue'
import PersonasView from '../views/PersonasView.vue'
import SettingsView from '../views/SettingsView.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'chat',
      component: ChatView
    },
    {
      path: '/tts',
      name: 'tts',
      component: TTSView
    },
    {
      path: '/personas',
      name: 'personas',
      component: PersonasView
    },
    {
      path: '/settings',
      name: 'settings',
      component: SettingsView
    }
  ]
})

export default router


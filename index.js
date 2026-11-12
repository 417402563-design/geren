import { createRouter, createWebHashHistory } from 'vue-router'
import { store } from '../store'
import Auth from '../views/Auth.vue'
import Documents from '../views/Documents.vue'
import Chat from '../views/Chat.vue'
import History from '../views/History.vue'
import Profile from '../views/Profile.vue'

const router = createRouter({
  history: createWebHashHistory(),
  routes: [
    { path: '/login', component: Auth },
    { path: '/documents', component: Documents, meta: { auth: true } },
    { path: '/chat', component: Chat, meta: { auth: true } },
    { path: '/history', component: History, meta: { auth: true } },
    { path: '/profile', component: Profile, meta: { auth: true } },
    { path: '/', redirect: '/documents' }
  ]
})

router.beforeEach(to => {
  if (to.meta.auth && !store.loggedIn) return '/login'
  if (to.path === '/login' && store.loggedIn) return '/documents'
})

export default router

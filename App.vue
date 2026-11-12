<script setup>
import { useRouter } from 'vue-router'
import { store, clearAuth } from './store'

const router = useRouter()
function logout() {
  clearAuth()
  router.push('/login')
}
</script>

<template>
  <div class="app-shell">
    <header v-if="store.loggedIn" class="top-nav">
      <div class="brand">📚 智能个人知识库</div>
      <nav class="nav-links">
        <router-link to="/documents">知识库管理</router-link>
        <router-link to="/chat">智能问答</router-link>
        <router-link to="/history">对话历史</router-link>
        <router-link to="/profile">个人中心</router-link>
      </nav>
      <div class="user-area">
        <span class="user-name">{{ store.user?.nickname || store.user?.username }}</span>
        <button class="btn btn-plain" @click="logout">退出登录</button>
      </div>
    </header>
    <main class="content">
      <router-view />
    </main>
  </div>
</template>

<style>
* { box-sizing: border-box; margin: 0; padding: 0; }
body {
  font-family: 'Microsoft YaHei', 'PingFang SC', system-ui, sans-serif;
  background: #f2f4f8;
  color: #1f2430;
}
.app-shell { min-height: 100vh; display: flex; flex-direction: column; }
.top-nav {
  display: flex; align-items: center; gap: 24px;
  background: #fff; padding: 0 24px; height: 58px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, .08);
  position: sticky; top: 0; z-index: 10;
}
.brand { font-size: 17px; font-weight: 700; color: #3b5bfd; white-space: nowrap; }
.nav-links { display: flex; gap: 6px; flex: 1; }
.nav-links a {
  text-decoration: none; color: #4a5266; padding: 8px 14px;
  border-radius: 8px; font-size: 14px;
}
.nav-links a:hover { background: #eef1fb; }
.nav-links a.router-link-active { background: #3b5bfd; color: #fff; }
.user-area { display: flex; align-items: center; gap: 12px; }
.user-name { font-size: 14px; color: #4a5266; }
.content { flex: 1; padding: 24px; max-width: 1200px; width: 100%; margin: 0 auto; }

.card {
  background: #fff; border-radius: 12px; padding: 20px;
  box-shadow: 0 2px 8px rgba(31, 41, 82, .06); margin-bottom: 18px;
}
.card h2 { font-size: 17px; margin-bottom: 14px; }
.btn {
  border: none; border-radius: 8px; padding: 9px 18px; font-size: 14px;
  cursor: pointer; background: #eef1fb; color: #334; transition: opacity .15s;
}
.btn:disabled { opacity: .55; cursor: not-allowed; }
.btn-primary { background: #3b5bfd; color: #fff; }
.btn-primary:hover:not(:disabled) { background: #2f4be0; }
.btn-danger { background: #fdeeee; color: #d33; }
.btn-danger:hover { background: #f9dede; }
.btn-plain { background: transparent; color: #667; }
.btn-sm { padding: 5px 12px; font-size: 13px; }
.input, textarea.input {
  width: 100%; border: 1px solid #d8deeb; border-radius: 8px;
  padding: 9px 12px; font-size: 14px; outline: none; background: #fff;
  font-family: inherit;
}
.input:focus { border-color: #3b5bfd; }
.field { margin-bottom: 14px; }
.field label { display: block; font-size: 13px; color: #66708a; margin-bottom: 6px; }
.error-tip { color: #d33; font-size: 13px; margin-top: 8px; white-space: pre-wrap; }
.success-tip { color: #1a9e5a; font-size: 13px; margin-top: 8px; }
.table { width: 100%; border-collapse: collapse; font-size: 14px; }
.table th {
  text-align: left; color: #66708a; font-weight: 500; font-size: 13px;
  padding: 10px 12px; border-bottom: 1px solid #e5e9f2; background: #f8f9fd;
}
.table td { padding: 11px 12px; border-bottom: 1px solid #eef1f6; }
.badge { display: inline-block; padding: 3px 10px; border-radius: 20px; font-size: 12px; }
.badgeREADY { background: #e3f6e9; color: #1a9e5a; }
.badgePARSING, .badgeVECTORIZING { background: #fff5e0; color: #c88400; }
.badgeFAILED { background: #fdeeee; color: #d33; }
.empty { text-align: center; color: #98a1b6; padding: 40px 0; font-size: 14px; }
</style>

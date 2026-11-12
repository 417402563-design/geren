<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import api from '../api'
import { setAuth } from '../store'

const router = useRouter()
const mode = ref('login')
const form = ref({ username: '', password: '', nickname: '' })
const error = ref('')
const loading = ref(false)

async function submit() {
  error.value = ''
  if (!form.value.username || !form.value.password) {
    error.value = '请输入用户名和密码'
    return
  }
  if (mode.value === 'register' && form.value.password.length < 6) {
    error.value = '密码长度至少为6位'
    return
  }
  loading.value = true
  try {
    const url = mode.value === 'login' ? '/auth/login' : '/auth/register'
    const body = mode.value === 'login'
      ? { username: form.value.username, password: form.value.password }
      : form.value
    const res = await api.post(url, body)
    if (res.code === 0) {
      setAuth(res.data.token, res.data.user)
      router.push('/documents')
    } else {
      error.value = res.message
    }
  } catch (e) {
    error.value = e.response?.data?.message || '网络错误，请确认后端服务已启动（端口8080）'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="auth-wrap">
    <div class="auth-card">
      <div class="auth-title">
        <span class="logo">📚</span>
        <h1>智能个人知识库</h1>
        <p>管理与问答系统</p>
      </div>
      <div class="tabs">
        <button :class="{ on: mode === 'login' }" @click="mode = 'login'; error = ''">登 录</button>
        <button :class="{ on: mode === 'register' }" @click="mode = 'register'; error = ''">注 册</button>
      </div>
      <form @submit.prevent="submit">
        <div class="field">
          <label>用户名</label>
          <input v-model.trim="form.username" class="input" placeholder="3-20位字母/数字/下划线/中文" />
        </div>
        <div v-if="mode === 'register'" class="field">
          <label>昵称（可选）</label>
          <input v-model.trim="form.nickname" class="input" placeholder="默认为用户名" />
        </div>
        <div class="field">
          <label>密码</label>
          <input v-model="form.password" type="password" class="input" placeholder="至少6位" />
        </div>
        <button class="btn btn-primary submit" type="submit" :disabled="loading">
          {{ loading ? '处理中…' : (mode === 'login' ? '登录' : '注册并登录') }}
        </button>
        <p v-if="error" class="error-tip">{{ error }}</p>
      </form>
    </div>
  </div>
</template>

<style scoped>
.auth-wrap { min-height: 100vh; display: flex; align-items: center; justify-content: center; background: linear-gradient(135deg, #eef1fb, #dde6ff); }
.auth-card { width: 380px; background: #fff; border-radius: 16px; padding: 34px 32px; box-shadow: 0 10px 40px rgba(59, 91, 253, .12); }
.auth-title { text-align: center; margin-bottom: 22px; }
.auth-title .logo { font-size: 40px; }
.auth-title h1 { font-size: 20px; margin-top: 6px; color: #1f2430; }
.auth-title p { font-size: 13px; color: #8a93a8; margin-top: 4px; }
.tabs { display: flex; margin-bottom: 20px; border-bottom: 2px solid #eef1f6; }
.tabs button {
  flex: 1; background: none; border: none; padding: 10px; font-size: 15px;
  color: #8a93a8; cursor: pointer; border-bottom: 2px solid transparent; margin-bottom: -2px;
}
.tabs button.on { color: #3b5bfd; border-bottom-color: #3b5bfd; font-weight: 600; }
.submit { width: 100%; margin-top: 4px; padding: 11px; }
</style>

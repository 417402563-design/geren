<script setup>
import { ref, onMounted } from 'vue'
import api from '../api'
import { store, updateUser } from '../store'

const profile = ref({ nickname: '', email: '' })
const pwd = ref({ oldPassword: '', newPassword: '', confirmPassword: '' })
const msg = ref('')
const pwdMsg = ref('')
const pwdError = ref('')

onMounted(async () => {
  try {
    const res = await api.get('/user/me')
    if (res.code === 0) {
      profile.value.nickname = res.data.nickname || ''
      profile.value.email = res.data.email || ''
      updateUser(res.data)
    }
  } catch (e) { /* 忽略，使用本地缓存展示 */ }
})

async function saveProfile() {
  msg.value = ''
  try {
    const res = await api.put('/user/profile', profile.value)
    if (res.code === 0) {
      updateUser(res.data)
      msg.value = '资料已保存'
    } else {
      msg.value = res.message
    }
  } catch (e) {
    msg.value = e.response?.data?.message || '保存失败'
  }
}

async function savePwd() {
  pwdMsg.value = ''
  pwdError.value = ''
  if (pwd.value.newPassword !== pwd.value.confirmPassword) {
    pwdError.value = '两次输入的新密码不一致'
    return
  }
  try {
    const res = await api.put('/user/password', {
      oldPassword: pwd.value.oldPassword,
      newPassword: pwd.value.newPassword
    })
    if (res.code === 0) {
      pwdMsg.value = '密码修改成功'
      pwd.value = { oldPassword: '', newPassword: '', confirmPassword: '' }
    } else {
      pwdError.value = res.message
    }
  } catch (e) {
    pwdError.value = e.response?.data?.message || '修改失败'
  }
}
</script>

<template>
  <div>
    <div class="card profile-head">
      <div class="avatar-big">{{ (store.user?.nickname || store.user?.username || 'U').slice(0, 1) }}</div>
      <div>
        <h2>{{ store.user?.nickname || store.user?.username }}</h2>
        <p class="meta">@{{ store.user?.username }} · 注册于 {{ store.user?.createdAt || '-' }}</p>
      </div>
    </div>

    <div class="grid">
      <div class="card">
        <h2>基本资料</h2>
        <div class="field">
          <label>昵称</label>
          <input v-model.trim="profile.nickname" class="input" placeholder="请输入昵称" />
        </div>
        <div class="field">
          <label>邮箱</label>
          <input v-model.trim="profile.email" class="input" placeholder="请输入邮箱（可留空）" />
        </div>
        <button class="btn btn-primary" @click="saveProfile">保存修改</button>
        <p v-if="msg" :class="msg === '资料已保存' ? 'success-tip' : 'error-tip'">{{ msg }}</p>
      </div>

      <div class="card">
        <h2>修改密码</h2>
        <div class="field">
          <label>原密码</label>
          <input v-model="pwd.oldPassword" type="password" class="input" />
        </div>
        <div class="field">
          <label>新密码（至少6位）</label>
          <input v-model="pwd.newPassword" type="password" class="input" />
        </div>
        <div class="field">
          <label>确认新密码</label>
          <input v-model="pwd.confirmPassword" type="password" class="input" />
        </div>
        <button class="btn btn-primary" @click="savePwd">修改密码</button>
        <p v-if="pwdMsg" class="success-tip">{{ pwdMsg }}</p>
        <p v-if="pwdError" class="error-tip">{{ pwdError }}</p>
      </div>
    </div>
  </div>
</template>

<style scoped>
.profile-head { display: flex; align-items: center; gap: 16px; }
.profile-head h2 { margin-bottom: 4px; }
.meta { font-size: 13px; color: #8a93a8; }
.avatar-big {
  width: 56px; height: 56px; border-radius: 50%; background: #3b5bfd; color: #fff;
  display: flex; align-items: center; justify-content: center; font-size: 24px; font-weight: 600;
}
.grid { display: grid; grid-template-columns: 1fr 1fr; gap: 18px; }
@media (max-width: 800px) { .grid { grid-template-columns: 1fr; } }
</style>

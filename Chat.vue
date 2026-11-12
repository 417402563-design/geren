<script setup>
import { ref, onMounted, watch, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import api from '../api'

const route = useRoute()
const conversations = ref([])
const activeId = ref(route.query.open ? Number(route.query.open) : null)
const messages = ref([])
const question = ref('')
const sending = ref(false)
const error = ref('')
const boxEl = ref(null)

async function loadConversations() {
  const res = await api.get('/chat/conversations')
  if (res.code === 0) conversations.value = res.data
}

async function open(id) {
  activeId.value = id
  error.value = ''
  try {
    const res = await api.get(`/chat/conversations/${id}/messages`)
    messages.value = res.code === 0 ? res.data : []
  } catch (e) {
    messages.value = []
  }
  scroll()
}

function newChat() {
  activeId.value = null
  messages.value = []
  question.value = ''
}

async function removeConversation(c) {
  if (!confirm(`删除对话「${c.title}」及其全部记录？`)) return
  await api.delete('/chat/conversations/' + c.id)
  if (activeId.value === c.id) newChat()
  loadConversations()
}

async function send() {
  const q = question.value.trim()
  if (!q || sending.value) return
  question.value = ''
  sending.value = true
  error.value = ''
  messages.value.push({ role: 'user', content: q })
  try {
    const res = await api.post('/chat/ask', { conversationId: activeId.value, question: q })
    if (res.code === 0) {
      activeId.value = res.data.conversationId
      messages.value.push({ role: 'assistant', content: res.data.answer, citations: res.data.citations })
      loadConversations()
    } else {
      error.value = res.message
      messages.value.pop()
    }
  } catch (e) {
    error.value = e.response?.data?.message || '提问失败，请检查后端服务'
    messages.value.pop()
  } finally {
    sending.value = false
    scroll()
  }
}

function scroll() {
  nextTick(() => { if (boxEl.value) boxEl.value.scrollTop = boxEl.value.scrollHeight })
}

onMounted(async () => {
  await loadConversations()
  if (activeId.value) open(activeId.value)
})

watch(() => route.query.open, v => { if (v) open(Number(v)) })
</script>

<template>
  <div class="chat-layout">
    <aside class="card conv-panel">
      <button class="btn btn-primary new-btn" @click="newChat">＋ 新建对话</button>
      <div class="conv-list">
        <div v-for="c in conversations" :key="c.id"
             class="conv-item" :class="{ on: c.id === activeId }" @click="open(c.id)">
          <div class="conv-title">{{ c.title }}</div>
          <div class="conv-meta">
            <span>{{ c.updatedAt }}</span>
            <button class="btn btn-plain btn-sm del" title="删除" @click.stop="removeConversation(c)">✕</button>
          </div>
        </div>
        <div v-if="!conversations.length" class="empty small">暂无对话</div>
      </div>
    </aside>

    <section class="card chat-main">
      <div ref="boxEl" class="msg-box">
        <div v-if="!messages.length" class="empty">
          在下方输入问题，基于你的知识库进行检索增强问答<br />
          <small>（回答末尾附有引用溯源，可定位原文档与片段）</small>
        </div>
        <div v-for="(m, i) in messages" :key="i" class="msg" :class="m.role">
          <div class="avatar">{{ m.role === 'user' ? '我' : 'AI' }}</div>
          <div class="bubble-wrap">
            <div class="bubble">{{ m.content }}</div>
            <div v-if="m.citations && m.citations.length" class="cites">
              <div class="cites-title">🔍 引用溯源</div>
              <div v-for="(c, j) in m.citations" :key="j" class="cite">
                <div class="cite-head">[{{ j + 1 }}]《{{ c.documentName }}》第{{ c.chunkIndex + 1 }}段 · 相似度 {{ (c.score * 100).toFixed(1) }}%</div>
                <div class="cite-snippet">{{ c.snippet }}</div>
              </div>
            </div>
          </div>
        </div>
        <div v-if="sending" class="msg assistant">
          <div class="avatar">AI</div>
          <div class="bubble-wrap"><div class="bubble thinking">正在检索知识库并生成回答…</div></div>
        </div>
      </div>
      <p v-if="error" class="error-tip">{{ error }}</p>
      <div class="input-row">
        <textarea v-model="question" class="input" rows="2" placeholder="输入问题，Enter 发送，Shift+Enter 换行"
                  @keydown.enter.exact.prevent="send"></textarea>
        <button class="btn btn-primary send-btn" :disabled="sending || !question.trim()" @click="send">发送</button>
      </div>
    </section>
  </div>
</template>

<style scoped>
.chat-layout { display: flex; gap: 18px; align-items: flex-start; }
.conv-panel { width: 260px; flex-shrink: 0; display: flex; flex-direction: column; max-height: 78vh; }
.new-btn { margin-bottom: 12px; }
.conv-list { overflow-y: auto; flex: 1; }
.conv-item { padding: 10px 12px; border-radius: 8px; cursor: pointer; margin-bottom: 4px; }
.conv-item:hover { background: #f2f5fd; }
.conv-item.on { background: #e5ebff; }
.conv-title { font-size: 14px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.conv-meta { font-size: 11px; color: #98a1b6; display: flex; justify-content: space-between; align-items: center; margin-top: 4px; }
.conv-meta .del { visibility: hidden; }
.conv-item:hover .del { visibility: visible; }
.empty.small { padding: 20px 0; font-size: 13px; }
.chat-main { flex: 1; display: flex; flex-direction: column; min-height: 78vh; }
.msg-box { flex: 1; overflow-y: auto; padding: 4px; }
.msg { display: flex; gap: 10px; margin-bottom: 16px; }
.msg.user { flex-direction: row-reverse; }
.avatar {
  width: 34px; height: 34px; border-radius: 50%; flex-shrink: 0;
  display: flex; align-items: center; justify-content: center; font-size: 13px;
  background: #dfe6f5; color: #4a5674;
}
.msg.user .avatar { background: #3b5bfd; color: #fff; }
.bubble-wrap { max-width: 75%; }
.bubble {
  background: #f3f5fa; padding: 11px 14px; border-radius: 10px;
  font-size: 14px; line-height: 1.7; white-space: pre-wrap; word-break: break-word;
}
.msg.user .bubble { background: #3b5bfd; color: #fff; }
.thinking { color: #8a93a8; font-style: italic; }
.cites { margin-top: 8px; border-left: 3px solid #3b5bfd; padding-left: 10px; }
.cites-title { font-size: 12px; color: #66708a; margin-bottom: 6px; }
.cite { margin-bottom: 8px; background: #f8f9fd; border-radius: 8px; padding: 8px 10px; }
.cite-head { font-size: 12px; color: #3b5bfd; font-weight: 600; }
.cite-snippet { font-size: 12px; color: #66708a; margin-top: 4px; }
.input-row { display: flex; gap: 10px; margin-top: 12px; }
.input-row .input { resize: none; }
.send-btn { align-self: stretch; padding: 0 22px; }
</style>

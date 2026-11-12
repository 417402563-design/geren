<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import api from '../api'

const router = useRouter()
const conversations = ref([])
const expandedId = ref(null)
const detail = ref([])
const loadingDetail = ref(false)

async function load() {
  const res = await api.get('/chat/conversations')
  if (res.code === 0) conversations.value = res.data
}

async function toggle(c) {
  if (expandedId.value === c.id) {
    expandedId.value = null
    return
  }
  expandedId.value = c.id
  loadingDetail.value = true
  try {
    const res = await api.get(`/chat/conversations/${c.id}/messages`)
    detail.value = res.code === 0 ? res.data : []
  } finally {
    loadingDetail.value = false
  }
}

function continueChat(c) {
  router.push({ path: '/chat', query: { open: c.id } })
}

async function remove(c) {
  if (!confirm(`删除对话「${c.title}」及其全部记录？`)) return
  await api.delete('/chat/conversations/' + c.id)
  if (expandedId.value === c.id) expandedId.value = null
  load()
}

onMounted(load)
</script>

<template>
  <div class="card">
    <h2>对话历史（{{ conversations.length }}）</h2>
    <div v-if="conversations.length">
      <div v-for="c in conversations" :key="c.id" class="hist-item">
        <div class="hist-head" @click="toggle(c)">
          <span class="arrow">{{ expandedId === c.id ? '▼' : '▶' }}</span>
          <div class="hist-info">
            <div class="hist-title">{{ c.title }}</div>
            <div class="hist-time">创建于 {{ c.createdAt }} · 最近更新 {{ c.updatedAt }}</div>
          </div>
          <div class="hist-ops">
            <button class="btn btn-primary btn-sm" @click.stop="continueChat(c)">继续对话</button>
            <button class="btn btn-danger btn-sm" @click.stop="remove(c)">删除</button>
          </div>
        </div>
        <div v-if="expandedId === c.id" class="hist-body">
          <div v-if="loadingDetail" class="empty small">加载中…</div>
          <template v-else>
            <div v-for="m in detail" :key="m.id" class="line" :class="m.role">
              <b>{{ m.role === 'user' ? '问' : '答' }}：</b>{{ m.content }}
              <div v-if="m.citations && m.citations.length" class="cite-tags">
                <span v-for="(ct, i) in m.citations" :key="i" class="cite-tag">
                  [{{ i + 1 }}]《{{ ct.documentName }}》第{{ ct.chunkIndex + 1 }}段
                </span>
              </div>
            </div>
            <div v-if="!detail.length" class="empty small">该对话暂无记录</div>
          </template>
        </div>
      </div>
    </div>
    <div v-else class="empty">暂无历史对话，去「智能问答」提第一个问题吧</div>
  </div>
</template>

<style scoped>
.hist-item { border: 1px solid #eef1f6; border-radius: 10px; margin-bottom: 10px; overflow: hidden; }
.hist-head { display: flex; align-items: center; gap: 12px; padding: 12px 14px; cursor: pointer; }
.hist-head:hover { background: #f8f9fd; }
.arrow { font-size: 11px; color: #98a1b6; }
.hist-info { flex: 1; min-width: 0; }
.hist-title { font-size: 14px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.hist-time { font-size: 12px; color: #98a1b6; margin-top: 3px; }
.hist-ops { display: flex; gap: 8px; }
.hist-body { padding: 12px 16px; background: #fafbfe; border-top: 1px solid #eef1f6; max-height: 320px; overflow-y: auto; }
.line { font-size: 13px; line-height: 1.7; margin-bottom: 8px; white-space: pre-wrap; word-break: break-word; }
.line.user { color: #33416b; }
.line.assistant { color: #333; }
.cite-tags { margin-top: 4px; display: flex; flex-wrap: wrap; gap: 6px; }
.cite-tag { font-size: 11px; background: #e5ebff; color: #3b5bfd; border-radius: 6px; padding: 2px 8px; }
.empty.small { padding: 16px 0; font-size: 13px; }
</style>

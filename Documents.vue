<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import api from '../api'

const docs = ref([])
const fileInputEl = ref(null)
const pickedFile = ref(null)
const uploading = ref(false)
const error = ref('')
let timer = null

async function load() {
  try {
    const res = await api.get('/documents')
    if (res.code === 0) docs.value = res.data
    schedule()
  } catch (e) {
    error.value = '文档列表加载失败'
  }
}

// 状态轮询：存在处理中的文档时每2秒刷新
function schedule() {
  clearTimeout(timer)
  const processing = docs.value.some(d => d.status === 'PARSING' || d.status === 'VECTORIZING')
  if (processing) timer = setTimeout(load, 2000)
}

function pick(e) {
  pickedFile.value = e.target.files[0] || null
  error.value = ''
}

async function upload() {
  if (!pickedFile.value) { error.value = '请先选择要上传的文件'; return }
  error.value = ''
  uploading.value = true
  try {
    const fd = new FormData()
    fd.append('file', pickedFile.value)
    const res = await api.post('/documents', fd)
    if (res.code !== 0) {
      error.value = res.message
    } else if (res.data.status === 'FAILED') {
      error.value = `《${res.data.name}》处理失败：${res.data.errorMsg || '未知原因'}`
    }
    if (fileInputEl.value) fileInputEl.value.value = ''
    pickedFile.value = null
    await load()
  } catch (e) {
    error.value = e.response?.data?.message || '上传失败，请检查网络与后端服务'
  } finally {
    uploading.value = false
  }
}

async function removeDoc(d) {
  if (!confirm(`确定删除《${d.name}》及其全部分片与向量数据？`)) return
  const res = await api.delete('/documents/' + d.id)
  if (res.code !== 0) error.value = res.message
  await load()
}

function fmtSize(n) {
  if (n == null) return '-'
  if (n > 1048576) return (n / 1048576).toFixed(2) + ' MB'
  if (n > 1024) return (n / 1024).toFixed(1) + ' KB'
  return n + ' B'
}

onMounted(load)
onBeforeUnmount(() => clearTimeout(timer))
</script>

<template>
  <div>
    <div class="card">
      <h2>上传文档</h2>
      <div class="upload-row">
        <input ref="fileInputEl" type="file" class="file" accept=".txt,.md,.markdown,.log,.csv,.pdf,.docx" @change="pick" />
        <button class="btn btn-primary" :disabled="uploading || !pickedFile" @click="upload">
          {{ uploading ? '解析并向量化中…' : '上传并解析' }}
        </button>
      </div>
      <p class="hint">支持格式：txt / md / log / csv / pdf / docx，单文件不超过 20MB。上传后自动完成文本提取 → 清洗 → 分割 → 向量化。</p>
      <p v-if="error" class="error-tip">{{ error }}</p>
    </div>

    <div class="card">
      <h2>我的文档（{{ docs.length }}）</h2>
      <div v-if="docs.length" class="table-scroll">
        <table class="table">
          <thead>
            <tr>
              <th>文档名称</th><th>类型</th><th>大小</th><th>状态</th><th>分片数</th><th>上传时间</th><th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="d in docs" :key="d.id">
              <td class="doc-name" :title="d.errorMsg || d.name">{{ d.name }}</td>
              <td>{{ d.fileType }}</td>
              <td>{{ fmtSize(d.fileSize) }}</td>
              <td><span class="badge" :class="'badge' + d.status">{{ d.statusText }}</span></td>
              <td>{{ d.chunkCount }}</td>
              <td>{{ d.createdAt }}</td>
              <td><button class="btn btn-danger btn-sm" @click="removeDoc(d)">删除</button></td>
            </tr>
          </tbody>
        </table>
      </div>
      <div v-else class="empty">暂无文档，上传第一份资料开始构建你的知识库吧</div>
    </div>
  </div>
</template>

<style scoped>
.upload-row { display: flex; gap: 12px; align-items: center; flex-wrap: wrap; }
.file { font-size: 14px; }
.hint { font-size: 12px; color: #8a93a8; margin-top: 10px; }
.doc-name { max-width: 280px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.table-scroll { overflow-x: auto; }
</style>

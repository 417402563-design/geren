import axios from 'axios'

const api = axios.create({ baseURL: '/api', timeout: 120000 })

// 请求拦截：自动携带 JWT
api.interceptors.request.use(config => {
  const token = localStorage.getItem('token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

// 响应拦截：401 自动退回登录页
api.interceptors.response.use(
  res => res.data,
  err => {
    if (err.response && err.response.status === 401) {
      localStorage.removeItem('token')
      localStorage.removeItem('user')
      window.location.hash = '#/login'
    }
    return Promise.reject(err)
  }
)

export default api

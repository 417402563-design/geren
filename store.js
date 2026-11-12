import { reactive } from 'vue'

export const store = reactive({
  loggedIn: !!localStorage.getItem('token'),
  user: JSON.parse(localStorage.getItem('user') || 'null')
})

export function setAuth(token, user) {
  localStorage.setItem('token', token)
  localStorage.setItem('user', JSON.stringify(user))
  store.loggedIn = true
  store.user = user
}

export function updateUser(user) {
  localStorage.setItem('user', JSON.stringify(user))
  store.user = user
}

export function clearAuth() {
  localStorage.removeItem('token')
  localStorage.removeItem('user')
  store.loggedIn = false
  store.user = null
}

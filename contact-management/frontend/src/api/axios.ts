import axios from 'axios'

const api = axios.create({
  baseURL: '/api',
})

const clearAuth = () => {
  localStorage.removeItem('accessToken')
  localStorage.removeItem('tokenType')
  localStorage.removeItem('roles')
  localStorage.removeItem('isLoggedIn')
  localStorage.removeItem('loggedInUser')
}

api.interceptors.request.use((config) => {
  const accessToken = localStorage.getItem('accessToken')
  if (accessToken) config.headers.Authorization = `Bearer ${accessToken}`
  return config
})

api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      clearAuth()
      if (window.location.pathname !== '/login') window.location.replace('/login')
    }
    return Promise.reject(error)
  },
)

export { clearAuth }
export default api
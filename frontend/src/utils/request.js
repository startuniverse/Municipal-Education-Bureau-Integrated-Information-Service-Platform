import axios from 'axios'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/modules/user'

// 创建axios实例
const service = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api/v1',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// 请求拦截器
service.interceptors.request.use(
  (config) => {
    const userStore = useUserStore()
    if (userStore.token) {
      config.headers.Authorization = `Bearer ${userStore.token}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// 响应拦截器
service.interceptors.response.use(
  (response) => {
    const res = response.data
    console.log('=== API Response Debug ===')
    console.log('URL:', response.config.url)
    console.log('Response data:', res)

    // 如果返回的状态码不是200，说明有问题
    if (res.code !== 200) {
      console.error('API Error - Code:', res.code, 'Message:', res.message)
      ElMessage.error(res.message || '系统错误')
      return Promise.reject(new Error(res.message || '系统错误'))
    }

    return res.data
  },
  (error) => {
    let message = '请求失败'

    if (error.response) {
      const status = error.response.status
      switch (status) {
        case 400:
          message = '请求错误'
          break
        case 401:
          message = '未授权，请重新登录'
          // 清除token并跳转到登录页
          const userStore = useUserStore()
          userStore.logout()
          window.location.href = '/login'
          break
        case 403:
          message = '拒绝访问'
          break
        case 404:
          message = '请求地址不存在'
          break
        case 500:
          message = '服务器内部错误'
          break
        default:
          message = `连接错误${status}`
      }
    } else if (error.request) {
      message = '网络请求失败，请检查网络连接'
    } else {
      message = error.message
    }

    ElMessage.error(message)
    return Promise.reject(error)
  }
)

export default service

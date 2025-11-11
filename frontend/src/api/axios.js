import axios from 'axios'

// 创建axios实例
const apiClient = axios.create({
  baseURL: '/api', // 开发环境会被Vite代理到 http://localhost:8080
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// 请求拦截器
apiClient.interceptors.request.use(
  config => {
    // 可以在这里添加token等认证信息
    // const token = localStorage.getItem('token')
    // if (token) {
    //   config.headers.Authorization = `Bearer ${token}`
    // }
    return config
  },
  error => {
    return Promise.reject(error)
  }
)

// 响应拦截器
apiClient.interceptors.response.use(
  response => {
    return response.data
  },
  error => {
    console.error('API Error:', error)
    
    if (error.response) {
      // 服务器返回错误状态码
      const { status, data } = error.response
      
      switch (status) {
        case 400:
          console.error('请求参数错误')
          break
        case 401:
          console.error('未授权,请登录')
          break
        case 403:
          console.error('拒绝访问')
          break
        case 404:
          console.error('请求资源不存在')
          break
        case 500:
          console.error('服务器错误')
          break
        default:
          console.error(`错误: ${status}`)
      }
      
      return Promise.reject(data || error.message)
    } else if (error.request) {
      // 请求已发出但没有收到响应
      console.error('网络错误,请检查网络连接')
      return Promise.reject('网络错误')
    } else {
      // 其他错误
      console.error('请求配置错误:', error.message)
      return Promise.reject(error.message)
    }
  }
)

export default apiClient


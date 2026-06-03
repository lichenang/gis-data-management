import axios, { type AxiosInstance, type AxiosRequestConfig, type AxiosResponse, type InternalAxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'

/**
 * API 请求配置
 * 开发环境使用 Vite 代理（相对路径），生产环境使用环境变量或硬编码地址
 */
const baseURL = import.meta.env.VITE_API_BASE_URL || '/api/v1'

/**
 * 创建 Axios 实例
 */
const service: AxiosInstance = axios.create({
  baseURL,
  timeout: 120000,
  headers: {
    'Content-Type': 'application/json'
  }
})

/**
 * 请求拦截器
 * - 自动携带 JWT Token
 * - 添加租户 ID
 */
service.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = localStorage.getItem('access_token')
    if (token && config.headers) {
      config.headers.Authorization = `Bearer ${token}`
    }
    const tenantId = localStorage.getItem('tenant_id') || 'default'
    if (config.headers) {
      config.headers['X-Tenant-Id'] = tenantId
    }
    return config
  },
  (error) => {
    console.error('Request error:', error)
    return Promise.reject(error)
  }
)

/**
 * 响应拦截器
 * - 处理业务错误
 * - 处理 token 过期
 * - 全局错误提示
 */
service.interceptors.response.use(
  (response: AxiosResponse) => {
    if (response.config.responseType === 'blob') {
      return response.data
    }

    const { code, message } = response.data

    if (code === 200 || code === 0) {
      return response.data
    }

    if (code === 401) {
      ElMessage.error('登录已过期，请重新登录')
      localStorage.removeItem('access_token')
      window.location.href = '/login'
      return Promise.reject(new Error(message || 'Unauthorized'))
    }

    ElMessage.error(message || '请求失败')
    return Promise.reject(new Error(message || 'Request failed'))
  },
  (error) => {
    if (error.response?.config?.responseType === 'blob') {
      ElMessage.error('下载失败')
    } else {
      const message = error.response?.data?.message || error.message || '网络错误'
      ElMessage.error(message)
    }
    return Promise.reject(error)
  }
)

/**
 * 封装 GET 请求
 * @param url 请求地址
 * @param params 请求参数
 * @param config Axios 配置
 */
export function get<T = any>(url: string, params?: object, config?: AxiosRequestConfig): Promise<T> {
  return service.get(url, { params, ...config })
}

/**
 * 封装 POST 请求
 * @param url 请求地址
 * @param data 请求数据
 * @param config Axios 配置
 */
export function post<T = any>(url: string, data?: object, config?: AxiosRequestConfig): Promise<T> {
  return service.post(url, data, config)
}

/**
 * 封装 PUT 请求
 * @param url 请求地址
 * @param data 请求数据
 * @param config Axios 配置
 */
export function put<T = any>(url: string, data?: object, config?: AxiosRequestConfig): Promise<T> {
  return service.put(url, data, config)
}

/**
 * 封装 DELETE 请求
 * @param url 请求地址
 * @param params 请求参数
 * @param config Axios 配置
 */
export function del<T = any>(url: string, params?: object, config?: AxiosRequestConfig): Promise<T> {
  return service.delete(url, { params, ...config })
}

/**
 * 封装 POST 文件上传请求
 * @param url 请求地址
 * @param formData 表单数据
 * @param config Axios 配置
 */
export function upload<T = any>(url: string, formData: FormData, config?: AxiosRequestConfig): Promise<T> {
  return service.post(url, formData, {
    ...config,
    headers: {
      'Content-Type': 'multipart/form-data',
      ...config?.headers
    }
  })
}

export default service

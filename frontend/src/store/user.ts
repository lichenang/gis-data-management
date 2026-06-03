import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { get, post } from '@/api/request'

/**
 * 用户信息接口
 */
export interface UserInfo {
  id: number
  username: string
  nickname: string
  email: string
  phone: string
  avatar: string
  roles: string[]
  permissions: string[]
}

/**
 * 登录请求参数
 */
export interface LoginParams {
  username: string
  password: string
}

/**
 * 登录响应
 */
export interface LoginResult {
  accessToken: string
  refreshToken: string
  expiresIn: number
}

/**
 * 用户状态管理 Store
 */
export const useUserStore = defineStore('user', () => {
  const token = ref<string>(localStorage.getItem('access_token') || '')
  const userInfo = ref<UserInfo | null>(null)

  /**
   * 是否已登录
   */
  const isLoggedIn = computed(() => !!token.value)

  /**
   * 用户角色
   */
  const roles = computed(() => userInfo.value?.roles || [])

  /**
   * 用户权限
   */
  const permissions = computed(() => userInfo.value?.permissions || [])

  /**
   * 是否为管理员
   */
  const isAdmin = computed(() => roles.value.includes('ADMIN'))

  /**
   * 登录
   * @param loginParams 登录参数
   */
  async function login(loginParams: LoginParams): Promise<void> {
    const result = await post<{ code: number; data: LoginResult }>('/auth/login', loginParams)
    if (result.code === 200) {
      const { accessToken, refreshToken, expiresIn } = result.data
      token.value = accessToken
      localStorage.setItem('access_token', accessToken)
      localStorage.setItem('refresh_token', refreshToken)
      localStorage.setItem('token_expires', String(Date.now() + expiresIn * 1000))
    }
  }

  /**
   * 登出
   */
  async function logout(): Promise<void> {
    try {
      await post('/auth/logout')
    } catch {
      // 忽略登出错误
    } finally {
      token.value = ''
      userInfo.value = null
      localStorage.removeItem('access_token')
      localStorage.removeItem('refresh_token')
      localStorage.removeItem('tenant_id')
    }
  }

  /**
   * 获取用户信息
   */
  async function getUserInfo(): Promise<void> {
    const result = await get<{ code: number; data: UserInfo }>('/auth/userinfo')
    if (result.code === 200) {
      userInfo.value = result.data
    }
  }

  return {
    token,
    userInfo,
    isLoggedIn,
    roles,
    permissions,
    isAdmin,
    login,
    logout,
    getUserInfo
  }
})

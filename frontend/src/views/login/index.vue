<template>
  <div class="login-container">
    <div class="login-box">
      <div class="login-header">
        <h1 class="title">GIS Platform</h1>
        <p class="subtitle">地理信息数据管理平台</p>
      </div>

      <el-form
        ref="loginFormRef"
        :model="loginForm"
        :rules="loginRules"
        class="login-form"
        @submit.prevent="handleLogin"
      >
        <el-form-item prop="username">
          <el-input
            v-model="loginForm.username"
            placeholder="请输入用户名"
            prefix-icon="User"
            size="large"
          />
        </el-form-item>

        <el-form-item prop="password">
          <el-input
            v-model="loginForm.password"
            type="password"
            placeholder="请输入密码"
            prefix-icon="Lock"
            size="large"
            show-password
            @keyup.enter="handleLogin"
          />
        </el-form-item>

        <el-form-item>
          <el-button
            type="primary"
            size="large"
            class="login-button"
            :loading="loading"
            @click="handleLogin"
          >
            {{ loading ? '登录中...' : '登 录' }}
          </el-button>
        </el-form-item>
      </el-form>

      <div class="login-footer">
        <span>默认账号：admin</span>
        <span class="divider">|</span>
        <span>密码：admin123</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, FormInstance, FormRules } from 'element-plus'
import { useUserStore } from '@/store/user'

/**
 * 登录组件
 * @description 用户登录页面，包含用户名密码表单和登录逻辑
 */

/**
 * @props - 无外部 Props
 * @emits - 无外部 Emits
 */

const router = useRouter()
const userStore = useUserStore()

/**
 * 登录表单引用
 */
const loginFormRef = ref<FormInstance>()

/**
 * 加载状态
 */
const loading = ref(false)

/**
 * 登录表单数据
 */
const loginForm = reactive({
  username: '',
  password: ''
})

/**
 * 登录表单校验规则
 */
const loginRules: FormRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度至少为6位', trigger: 'blur' }
  ]
}

/**
 * 处理登录
 * @description 校验表单并调用登录接口
 */
const handleLogin = async () => {
  if (!loginFormRef.value) return

  await loginFormRef.value.validate(async (valid) => {
    if (!valid) return

    loading.value = true
    try {
      await userStore.login(loginForm)
      ElMessage.success('登录成功')
      await userStore.getUserInfo()
      router.push('/home')
    } catch (error) {
      console.error('Login error:', error)
    } finally {
      loading.value = false
    }
  })
}
</script>

<style lang="scss" scoped>
.login-container {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.login-box {
  width: 420px;
  padding: 40px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
}

.login-header {
  text-align: center;
  margin-bottom: 40px;
}

.title {
  font-size: 28px;
  font-weight: 600;
  color: #333;
  margin: 0;
}

.subtitle {
  font-size: 14px;
  color: #999;
  margin-top: 8px;
}

.login-form {
  .login-button {
    width: 100%;
    font-size: 16px;
  }
}

.login-footer {
  margin-top: 24px;
  text-align: center;
  font-size: 12px;
  color: #999;

  .divider {
    margin: 0 8px;
  }
}
</style>

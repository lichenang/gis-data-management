<template>
  <div class="home-container">
    <el-container>
      <el-header class="header">
        <div class="header-left">
          <h2>GIS Platform</h2>
        </div>
        <div class="header-right">
          <el-dropdown @command="handleCommand">
            <span class="user-info">
              <el-icon><User /></el-icon>
              <span>{{ userStore.userInfo?.nickname || userStore.userInfo?.username || 'Admin' }}</span>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">个人中心</el-dropdown-item>
                <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <el-container>
        <el-aside width="200px" class="sidebar">
          <el-menu
            :default-active="activeMenu"
            router
            class="sidebar-menu"
          >
            <el-menu-item index="/home">
              <el-icon><HomeFilled /></el-icon>
              <span>仪表盘</span>
            </el-menu-item>
            <el-sub-menu index="/datasets">
              <template #title>
                <el-icon><Folder /></el-icon>
                <span>数据集管理</span>
              </template>
              <el-menu-item index="/datasets/vector">
                <el-icon><MapLocation /></el-icon>
                <span>矢量数据集</span>
              </el-menu-item>
              <el-menu-item index="/datasets/raster">
                <el-icon><Picture /></el-icon>
                <span>影像数据集</span>
              </el-menu-item>
            </el-sub-menu>
            <el-menu-item index="/layers">
              <el-icon><Map /></el-icon>
              <span>图层管理</span>
            </el-menu-item>
            <el-menu-item index="/map">
              <el-icon><Guide /></el-icon>
              <span>地图查看</span>
            </el-menu-item>
            <el-menu-item index="/users" v-if="userStore.isAdmin">
              <el-icon><User /></el-icon>
              <span>用户管理</span>
            </el-menu-item>
            <el-menu-item index="/settings" v-if="userStore.isAdmin">
              <el-icon><Setting /></el-icon>
              <span>系统管理</span>
            </el-menu-item>
          </el-menu>
        </el-aside>

        <el-main class="main-content">
          <div class="dashboard">
            <el-row :gutter="20">
              <el-col :span="6">
                <el-card class="stat-card">
                  <div class="stat-content">
                    <el-icon class="stat-icon" :size="40" color="#409EFF">
                      <Folder />
                    </el-icon>
                    <div class="stat-info">
                      <div class="stat-value">0</div>
                      <div class="stat-label">数据集</div>
                    </div>
                  </div>
                </el-card>
              </el-col>
              <el-col :span="6">
                <el-card class="stat-card">
                  <div class="stat-content">
                    <el-icon class="stat-icon" :size="40" color="#67C23A">
                      <Map />
                    </el-icon>
                    <div class="stat-info">
                      <div class="stat-value">0</div>
                      <div class="stat-label">图层</div>
                    </div>
                  </div>
                </el-card>
              </el-col>
              <el-col :span="6">
                <el-card class="stat-card">
                  <div class="stat-content">
                    <el-icon class="stat-icon" :size="40" color="#E6A23C">
                      <User />
                    </el-icon>
                    <div class="stat-info">
                      <div class="stat-value">0</div>
                      <div class="stat-label">用户</div>
                    </div>
                  </div>
                </el-card>
              </el-col>
              <el-col :span="6">
                <el-card class="stat-card">
                  <div class="stat-content">
                    <el-icon class="stat-icon" :size="40" color="#F56C6C">
                      <Warning />
                    </el-icon>
                    <div class="stat-info">
                      <div class="stat-value">0</div>
                      <div class="stat-label">授权状态</div>
                    </div>
                  </div>
                </el-card>
              </el-col>
            </el-row>

            <el-row :gutter="20" style="margin-top: 20px;">
              <el-col :span="24">
                <el-card class="map-preview">
                  <template #header>
                    <span>地图预览</span>
                  </template>
                  <div class="map-placeholder">
                    <el-icon :size="60" color="#409EFF"><Map /></el-icon>
                    <p>地图组件待集成</p>
                    <p class="hint">请使用 OpenLayers 集成地图功能</p>
                  </div>
                </el-card>
              </el-col>
            </el-row>
          </div>
        </el-main>
      </el-container>
    </el-container>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/user'

/**
 * 首页/仪表盘组件
 * @description 系统首页，展示统计信息和地图预览占位
 */

/**
 * @props - 无外部 Props
 * @emits - 无外部 Emits
 */

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

/**
 * 当前激活菜单
 */
const activeMenu = computed(() => route.path)

/**
 * 处理下拉菜单命令
 */
const handleCommand = async (command: string) => {
  if (command === 'logout') {
    await userStore.logout()
    ElMessage.success('已退出登录')
    router.push('/login')
  } else if (command === 'profile') {
    ElMessage.info('个人中心功能开发中')
  }
}
</script>

<style lang="scss" scoped>
.home-container {
  width: 100%;
  height: 100vh;
}

.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
  padding: 0 20px;
}

.header-left h2 {
  margin: 0;
  font-size: 20px;
  color: #303133;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
}

.sidebar {
  background: #fff;
  border-right: 1px solid #e4e7ed;
}

.sidebar-menu {
  border-right: none;
}

.main-content {
  background: #f5f7fa;
  padding: 20px;
}

.stat-card {
  .stat-content {
    display: flex;
    align-items: center;
    gap: 16px;
  }

  .stat-icon {
    opacity: 0.8;
  }

  .stat-info {
    flex: 1;
  }

  .stat-value {
    font-size: 24px;
    font-weight: 600;
    color: #303133;
  }

  .stat-label {
    font-size: 14px;
    color: #909399;
  }
}

.map-preview {
  min-height: 400px;
}

.map-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 300px;
  color: #909399;

  p {
    margin: 16px 0 0;
  }

  .hint {
    font-size: 12px;
  }
}
</style>

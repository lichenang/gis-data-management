import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { useUserStore } from '@/store/user'

/**
 * 静态路由配置
 */
const staticRoutes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue'),
    meta: { title: '登录', public: true }
  },
  {
    path: '/',
    redirect: '/home'
  },
  {
    path: '/home',
    name: 'Home',
    component: () => import('@/views/home/index.vue'),
    meta: { title: '首页', requiresAuth: true }
  },
  {
    path: '/users',
    name: 'Users',
    component: () => import('@/views/users/index.vue'),
    meta: { title: '用户管理', requiresAuth: true }
  },
  {
    path: '/datasets',
    redirect: '/datasets/vector'
  },
  {
    path: '/datasets/vector',
    name: 'VectorDatasets',
    component: () => import('@/views/datasets/index.vue'),
    meta: { title: '矢量数据集', requiresAuth: true }
  },
  {
    path: '/datasets/raster',
    redirect: '/images'
  },
  {
    path: '/images',
    name: 'ImageManagement',
    component: () => import('@/views/images/index.vue'),
    meta: { title: '影像管理', requiresAuth: true }
  },
  {
    path: '/map',
    name: 'MapViewer',
    component: () => import('@/views/map/index.vue'),
    meta: { title: '地图查看', requiresAuth: true }
  }
]

/**
 * 创建路由实例
 */
const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: staticRoutes
})

/**
 * 路由守卫 - 认证检查
 */
router.beforeEach((to, _from, next) => {
  const title = to.meta.title as string
  if (title) {
    document.title = `${title} - GIS Platform`
  }

  const requiresAuth = to.meta.requiresAuth as boolean
  const isPublic = to.meta.public as boolean
  const userStore = useUserStore()
  const hasToken = !!userStore.token

  if (requiresAuth && !hasToken) {
    next('/login')
  } else if (isPublic && hasToken && to.path === '/login') {
    next('/home')
  } else {
    next()
  }
})

export default router

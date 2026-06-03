# 设计: add-user-menu-item

## 当前菜单结构

```
el-menu
├── 仪表盘       /home      v-if 不限制
├── 数据集管理   /datasets  v-if 不限制
├── 图层管理     /layers    v-if 不限制
├── 地图查看     /map       v-if 不限制
└── 系统管理     /settings  v-if="userStore.isAdmin"  ← 管理员专属
```

## 修改方案

在"系统管理"菜单项之前添加"用户管理"菜单项：

```vue
<el-menu-item index="/users" v-if="userStore.isAdmin">
  <el-icon><User /></el-icon>
  <span>用户管理</span>
</el-menu-item>
```

## 位置选择

放在"系统管理"之前，保持菜单逻辑分组：
- 功能菜单：仪表盘、数据集、图层、地图
- 管理菜单：用户管理、系统管理

## 图标说明

使用 Element Plus 提供的 User 图标，与首页右上角用户图标保持一致。

## 验证步骤

1. 以管理员账号登录
2. 确认左侧菜单显示"用户管理"
3. 点击菜单项跳转到 /users 页面
4. 以普通用户登录（如果有）确认菜单不显示

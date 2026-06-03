# add-user-menu-item

## Why

系统已实现用户列表功能（/users 路由），但左侧菜单栏缺少入口。用户管理是系统基础功能，需要在菜单中提供快捷访问入口。

## What Changes

在 `home/index.vue` 的侧边栏菜单中添加"用户管理"菜单项：
- 菜单项名称：用户管理
- 路由：/users
- 图标：Element Plus User 图标
- 仅管理员可见（使用 `v-if="userStore.isAdmin"`）

## Capabilities

### New Capabilities
- 侧边栏用户管理入口

## Impact

- 修改文件：`frontend/src/views/home/index.vue`
- 影响：管理员可以通过菜单访问用户管理页面

## Non-goals

- 不修改用户管理页面功能
- 不添加其他菜单项

# add-user-list

## Why

系统需要管理员功能，需要展示用户列表供管理员查看和操作。当前系统已有用户基础CRUD接口，但缺少用户列表分页查询功能。

## What Changes

### 后端
- 在 UserController 添加 GET /api/v1/users 接口，支持分页和搜索
- 在 UserService 添加 listUsers 方法，支持分页和用户名搜索
- 返回字段：id、username、email、role(暂时返回null)、status、createTime

### 前端
- 新增 /users 路由，对应用户列表页面
- 使用 Element Plus Table 组件展示用户数据
- 列：用户名、邮箱、角色、状态、创建时间
- 支持分页和用户名搜索功能
- 页面需要管理员权限才能访问（需有 token）

## Capabilities

### New Capabilities
- 用户列表查看
- 用户分页浏览
- 用户名搜索

## Impact

- 新增文件：
  - `frontend/src/views/users/index.vue` - 用户列表页面
- 修改文件：
  - `backend/src/main/java/com/gisplatform/controller/UserController.java` - 添加列表接口
  - `backend/src/main/java/com/gisplatform/service/UserService.java` - 添加列表方法
  - `backend/src/main/java/com/gisplatform/service/impl/UserServiceImpl.java` - 实现列表方法
  - `frontend/src/router/index.ts` - 添加 /users 路由

## Non-goals

- 不实现用户编辑功能
- 不实现用户删除功能
- 不实现角色管理功能
- 不实现导出功能

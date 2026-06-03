## 1. 后端 - UserController 添加列表接口

- [x] 1.1 在 UserController.java 中添加 `list()` 方法
  - 方法签名：`@GetMapping` 映射 `/`
  - 接受参数：`page`, `pageSize`, `username`（使用 `@RequestParam`）
  - 调用 `userService.listUsers(page, pageSize, username)`
  - 返回 `R.ok(page)`

## 2. 后端 - UserService 添加方法声明

- [x] 2.1 在 UserService.java 中添加 `listUsers()` 方法声明
  - 返回类型：`Page<User>`
  - 参数：`int page`, `int pageSize`, `String username`

## 3. 后端 - UserServiceImpl 实现方法

- [x] 3.1 在 UserServiceImpl.java 中实现 `listUsers()` 方法
  - 使用 `Page<User>` 构建分页对象
  - 使用 `LambdaQueryWrapper` 构建查询条件
  - 如果 `username` 不为空，添加模糊查询：`User::getUsername, "%" + username + "%"`
  - 调用 `this.page(page, wrapper)` 执行分页查询
  - 返回分页结果

## 4. 前端 - 添加路由

- [x] 4.1 在 `router/index.ts` 的 `staticRoutes` 中添加 `/users` 路由
  - path: `/users`
  - name: `Users`
  - component: `() => import('@/views/users/index.vue')`
  - meta: `{ title: '用户管理', requiresAuth: true }`

## 5. 前端 - 创建用户列表页面

- [x] 5.1 创建目录 `frontend/src/views/users/`
- [x] 5.2 创建 `index.vue` 文件，实现：
  - 搜索表单：用户名输入框 + 搜索按钮 + 重置按钮
  - 表格：显示用户名、邮箱、角色（显示"-"）、状态（启用/禁用）、创建时间
  - 分页：使用 el-pagination，支持 current-change 事件
  - 加载数据：调用 GET /api/v1/users 接口
  - 搜索功能：输入用户名后点击搜索，重新加载数据
  - 分页变化：更新 page 或 pageSize 后重新加载数据

## 6. 验证

- [ ] 6.1 后端：启动后端服务，访问 `GET /api/v1/users?page=1&pageSize=10` 确认返回分页数据
- [ ] 6.2 前端：启动前端服务，登录后访问 `/users` 页面
- [ ] 6.3 验证表格正确显示用户数据
- [ ] 6.4 验证分页功能正常
- [ ] 6.5 验证搜索功能正常

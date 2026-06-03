# 设计: add-user-list

## 后端设计

### API 接口

**GET /api/v1/users**

请求参数：

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| page | int | 否 | 页码，默认 1 |
| pageSize | int | 否 | 每页条数，默认 10 |
| username | string | 否 | 用户名搜索关键字 |

响应格式：

```json
{
  "code": 200,
  "data": {
    "records": [
      {
        "id": 1,
        "username": "admin",
        "email": "admin@example.com",
        "role": null,
        "status": 1,
        "createTime": "2026-05-25T10:00:00"
      }
    ],
    "total": 100,
    "page": 1,
    "pageSize": 10
  }
}
```

### UserService 接口设计

```java
/**
 * 分页查询用户列表
 *
 * @param page     页码
 * @param pageSize 每页条数
 * @param username 用户名搜索关键字
 * @return 分页结果
 */
Page<User> listUsers(int page, int pageSize, String username);
```

## 前端设计

### 路由配置

```typescript
{
  path: '/users',
  name: 'Users',
  component: () => import('@/views/users/index.vue'),
  meta: { title: '用户管理', requiresAuth: true }
}
```

### 页面布局

```
┌─────────────────────────────────────┐
│  用户管理                           │
├─────────────────────────────────────┤
│  [搜索框: 输入用户名] [搜索]        │
├─────────────────────────────────────┤
│  用户名  │  邮箱  │ 角色 │ 状态 │ 创建时间 │
│  ───────────────────────────────────│
│  admin  │ xxx@.. │  -   │ 启用 │ 2026-.. │
│  ...    │ ...    │ ...  │ ...  │ ...     │
├─────────────────────────────────────┤
│  << < 1 2 3 ... 10 > >>             │
│  共 100 条 每页 10 条               │
└─────────────────────────────────────┘
```

### 组件结构

- 用户管理页面 (`views/users/index.vue`)
- 使用 Element Plus 组件：
  - `el-card` - 卡片容器
  - `el-input` - 搜索输入框
  - `el-button` - 搜索按钮
  - `el-table` - 数据表格
  - `el-pagination` - 分页组件

### 状态映射

| 数值 | 显示 |
|------|------|
| 1 | 启用 |
| 0 | 禁用 |

## 文件修改清单

### 后端

| 文件 | 操作 | 说明 |
|------|------|------|
| `UserController.java` | 修改 | 添加 `list()` 方法 |
| `UserService.java` | 修改 | 添加 `listUsers()` 方法声明 |
| `UserServiceImpl.java` | 修改 | 实现 `listUsers()` 方法 |

### 前端

| 文件 | 操作 | 说明 |
|------|------|------|
| `router/index.ts` | 修改 | 添加 `/users` 路由 |
| `views/users/index.vue` | 新增 | 用户列表页面组件 |

## 验证步骤

1. 启动后端服务
2. 启动前端服务
3. 登录系统（管理员账号）
4. 访问 /users 页面
5. 验证表格显示用户列表
6. 验证分页功能
7. 验证搜索功能

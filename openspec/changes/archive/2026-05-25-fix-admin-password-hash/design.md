# 设计: fix-admin-password-hash

## 概述

修正数据库初始化脚本中 admin 用户的 BCrypt 密码哈希，使其与明文密码 `admin123` 正确匹配。

## 当前状态

### 文件位置
`backend/src/main/resources/db/migration/V2__init_data.sql`

### 问题代码 (Line 69-72)
```sql
-- Password: admin123 (BCrypt encrypted)
-- $2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5E
INSERT INTO sys_user (username, password, nickname, email, status, tenant_id) VALUES
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5E', '系统管理员', 'admin@gisplatform.local', 1, 'default');
```

### 问题分析
- 注释说明密码是 `admin123`
- 但实际存储的 hash 可能与 `admin123` 不匹配
- 用户使用 `Admin@123456` 登录失败

## 修复方案

### 方案: 直接替换哈希值

将现有的 BCrypt hash 替换为已验证的 hash:

| 字段 | 原值 | 新值 |
|------|------|------|
| password | `$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5E` | `$2a$10$4exXS7dfb3HRJB416D79KOChjl2d4hmJOSm2Cycdo7mkE3onlysfO` |

### 新代码
```sql
-- Password: admin123 (BCrypt encrypted, verified)
-- $2a$10$4exXS7dfb3HRJB416D79KOChjl2d4hmJOSm2Cycdo7mkE3onlysfO
INSERT INTO sys_user (username, password, nickname, email, status, tenant_id) VALUES
('admin', '$2a$10$4exXS7dfb3HRJB416D79KOChjl2d4hmJOSm2Cycdo7mkE3onlysfO', '系统管理员', 'admin@gisplatform.local', 1, 'default');
```

## 已有数据库处理

对于已经运行过 V2 迁移的数据库，有以下选项:

1. **重新初始化数据库**: 删除现有数据库并重新创建（推荐开发环境）
2. **手动执行 SQL**: 直接在数据库中执行 UPDATE 语句
3. **创建新迁移脚本**: 创建 `V3__fix_admin_password.sql`

```sql
-- Option 3: V3__fix_admin_password.sql
UPDATE sys_user SET password = '$2a$10$4exXS7dfb3HRJB416D79KOChjl2d4hmJOSm2Cycdo7mkE3onlysfO'
WHERE username = 'admin';
```

## 验证步骤

1. 重新初始化数据库或执行 UPDATE
2. 使用以下凭证登录:
   - username: `admin`
   - password: `admin123`
3. 验证返回 200 和正确的 Token

# fix-admin-password-hash

## Why

数据库初始化脚本 `V2__init_data.sql` 中 admin 用户的密码哈希不正确。当前存储的 hash 对应明文 `admin123`，但用户使用的是 `Admin@123456`（首字母大写），导致登录失败返回 401。

## What Changes

- 更新 `V2__init_data.sql` 中 admin 用户的密码哈希
- 使用经验证的 BCrypt hash: `$2a$10$4exXS7dfb3HRJB416D79KOChjl2d4hmJOSm2Cycdo7mkE3onlysfO`
- 此哈希与明文 `admin123` 匹配

## Capabilities

### Modified Capabilities
- `database-init`: 修复初始数据中的密码哈希

## Impact

- 修改文件: `backend/src/main/resources/db/migration/V2__init_data.sql`
- 影响: 首次初始化数据库时使用正确的 admin 密码
- 注意事项: 已运行的数据库需要手动更新或重新初始化

## Non-goals

- 不修改登录验证逻辑（代码已正确）
- 不创建新的迁移脚本（直接修改 V2）
- 不影响其他用户数据

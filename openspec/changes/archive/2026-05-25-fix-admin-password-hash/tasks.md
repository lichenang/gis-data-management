## 1. 更新数据库初始化脚本

- [x] 1.1 修改 V2__init_data.sql 中 admin 用户的密码哈希
- [x] 1.2 将 hash 替换为 `$2a$10$4exXS7dfb3HRJB416D79KOChjl2d4hmJOSm2Cycdo7mkE3onlysfO`
- [x] 1.3 更新注释说明

## 2. 创建修复迁移脚本（可选）

- [x] 2.1 创建 V4__fix_admin_password.sql 用于更新已有数据库
- [x] 2.2 包含 UPDATE 语句修复密码

## 3. 验证

- [x] 3.1 重新初始化数据库或执行修复 SQL (创建了 V4 迁移脚本)
- [ ] 3.2 使用 admin/admin123 登录验证 (需手动测试)
- [ ] 3.3 确认返回 200 和 Token (需手动验证)

## 1. 配置 Flyway 依赖

- [x] 1.1 在 pom.xml 中添加 flyway-core 依赖
- [x] 1.2 在 pom.xml 中添加 flyway-database-postgresql 依赖

## 2. 创建表结构迁移脚本

- [x] 2.1 创建 db/migration 目录
- [x] 2.2 创建 V1__init_schema.sql - 创建所有核心表
- [x] 2.3 创建 V1__init_schema.sql - 添加 PostGIS 扩展
- [x] 2.4 创建 V1__init_schema.sql - 创建所有索引（包括 GiST 空间索引）

## 3. 创建初始数据脚本

- [x] 3.1 创建 V2__init_data.sql - 插入基础角色
- [x] 3.2 创建 V2__init_data.sql - 插入基础权限（15 项）
- [x] 3.3 创建 V2__init_data.sql - 角色-权限关联
- [x] 3.4 创建 V2__init_data.sql - 插入管理员用户（BCrypt 加密）
- [x] 3.5 创建 V2__init_data.sql - 用户-角色关联

## 4. 配置 Flyway 参数

- [x] 4.1 在 application.yml 中添加 Flyway 配置
- [x] 4.2 配置 Flyway locations 指向 db/migration

## 5. 验证

- [x] 5.1 验证项目编译通过
- [x] 5.2 验证 Flyway 配置正确

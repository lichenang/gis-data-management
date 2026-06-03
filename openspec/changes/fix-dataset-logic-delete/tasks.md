## 1. 检查与修复

- [ ] 1.1 检查 Dataset 实体的 deleted 字段是否有 @TableLogic 注解
- [ ] 1.2 检查 application.yml 中的逻辑删除配置
- [ ] 1.3 检查 MybatisPlusConfig 是否全局注册逻辑删除插件
- [ ] 1.4 如缺少 @TableLogic 注解，添加到 Dataset 实体

## 2. 验证

- [ ] 2.1 Maven 编译验证
- [ ] 2.2 测试删除操作，验证 deleted 字段变为 1

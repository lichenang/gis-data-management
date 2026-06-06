## 1. 前端修改

- [x] 1.1 添加 `sourceSrsRules` 计算属性，当 `missingPrj=true` 时返回必填规则
- [x] 1.2 为源坐标系选择器应用动态验证规则
- [x] 1.3 修改导入按钮的 `disabled` 条件，增加 sourceSrs 校验

## 2. 测试验证

- [x] 2.1 验证 crsDetected=false 时源坐标系为必填
- [x] 2.2 验证 crsDetected=false 时导入按钮被禁用
- [x] 2.3 验证 crsDetected=true 时源坐标系为可选

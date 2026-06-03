## 1. 矢量数据集列表优化

- [x] 1.1 在 datasets/index.vue 中将导出按钮类型从 `success` 改为 `warning`
- [x] 1.2 将发布按钮类型从条件判断（warning/success）改为始终使用 `success`
- [x] 1.3 验证操作列按钮颜色是否符合规范

## 2. 影像管理列表优化

- [x] 2.1 在 images/index.vue 中将三个下载按钮合并为"下载"下拉菜单
- [x] 2.2 实现 handleDownloadCommand 函数处理三种下载方式
- [x] 2.3 将发布按钮类型改为始终使用 `success`
- [x] 2.4 删除不再需要的 handleDownloadMetadata 等独立函数（如有）

## 3. 统一布局样式

- [x] 3.1 确保两个页面的操作列宽度一致（280px）
- [x] 3.2 确保按钮在行内居中对齐
- [x] 3.3 确保按钮间距统一

## 4. 验证

- [x] 4.1 前端编译验证
- [x] 4.2 验证两个页面的操作列样式一致

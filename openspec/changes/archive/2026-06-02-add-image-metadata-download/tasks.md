## 1. 后端接口开发

- [x] 1.1 在 ImageService 接口中添加 `getMetadata(Long id)` 方法
- [x] 1.2 在 ImageServiceImpl 中实现 getMetadata 方法，查询 raster_metadata 和 dataset 表组装元数据
- [x] 1.3 在 ImageController 中添加 `GET /images/{id}/metadata` 接口
- [x] 1.4 Maven 编译验证

## 2. 前端开发

- [x] 2.1 在 src/api 目录下新增 images.ts，添加 downloadMetadata 接口方法
- [x] 2.2 在 images/index.vue 中添加"下载元数据"按钮
- [x] 2.3 实现下载逻辑：获取 Blob 后触发浏览器下载
- [x] 2.4 前端编译验证

## 3. 测试验证

- [ ] 3.1 启动后端服务
- [ ] 3.2 启动前端服务
- [ ] 3.3 测试下载功能：登录后点击下载按钮，验证 JSON 文件正确下载

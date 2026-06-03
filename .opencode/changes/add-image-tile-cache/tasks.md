# 实现任务：影像切片预缓存功能

## 任务列表

### Phase 1: 后端状态轮询

- [x] #### 1.1 修改 GeoServerCacheService

#### 1.2 修改 TileSeedService

### Phase 2: 前端展示

- [x] #### 2.1 添加 retileImage API

- [x] #### 2.2 添加切片状态列

### Phase 3: 验证

- [x] 1. 检查 GeoServerCacheService.getSeedStatus() 是否正确解析 GWC 响应
- [x] 2. 检查 TileSeedService 轮询逻辑是否正确更新数据库
- [x] 3. 检查前端切片状态列是否正确显示
- [x] 4. 检查"重新切片"按钮是否正常工作

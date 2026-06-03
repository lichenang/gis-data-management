## Context

当前 datasets/index.vue 和 images/index.vue 的操作列存在以下问题：

1. **颜色不统一**：导出按钮在矢量列表使用 success 类型，而影像列表使用-primary
2. **功能分散**：影像列表有多个独立的下载按钮，占用过多列宽
3. **对齐不一致**：按钮间距不均匀，部分按钮未居中对齐

## Goals / Non-Goals

**Goals:**
- 统一两个列表的操作按钮颜色规范
- 将影像下载相关的多个按钮合并为下拉菜单
- 保持操作列宽度固定，按钮居中对齐

**Non-Goals:**
- 不修改后端 API
- 不改变下载/导出的功能逻辑

## Decisions

### Decision 1: 按钮颜色规范

统一操作按钮颜色：

| 操作 | 颜色 | Element Plus 类型 |
|------|------|------------------|
| 编辑 | primary | type="primary" link |
| 发布/取消发布 | success | type="success" link (始终) |
| 导出/下载 | warning | type="warning" link |
| 删除 | danger | type="danger" link |

### Decision 2: 影像下载下拉菜单结构

```vue
<el-dropdown @command="(cmd) => handleDownloadCommand(row, cmd)">
  <el-button type="warning" link>
    下载<el-icon><ArrowDown /></el-icon>
  </el-button>
  <template #dropdown>
    <el-dropdown-menu>
      <el-dropdown-item command="original">下载原始影像</el-dropdown-item>
      <el-dropdown-item v-if="row.status === 'published'" command="tiles">下载切片包</el-dropdown-item>
      <el-dropdown-item command="metadata">下载元数据</el-dropdown-item>
    </el-dropdown-menu>
  </template>
</el-dropdown>
```

对应的 handleDownloadCommand 处理函数：
- `original` → 调用 getImageDownloadUrl 后打开新窗口
- `tiles` → 打开切片下载对话框
- `metadata` → 调用 getImageMetadata 后触发 Blob 下载

### Decision 3: 操作列布局

- 操作列固定宽度：280px（矢量列表）/ 280px（影像列表）
- 使用 `justify-content: center` 确保按钮水平居中
- 按钮之间添加 `margin: 0 4px` 统一间距

## Implementation Notes

### datasets/index.vue 改动

当前导出已经是下拉菜单（lines 99-111），只需调整按钮颜色：

```vue
<el-dropdown @command="(format) => handleExport(row, format)">
  <el-button type="warning" link>
    导出<el-icon><ArrowDown /></el-icon>
  </el-button>
  ...
</el-dropdown>
```

发布按钮改为始终使用 success 类型：

```vue
<el-button
  type="success"
  link
  @click="handlePublish(row)"
>
  {{ row.status === 'published' ? '取消发布' : '发布' }}
</el-button>
```

### images/index.vue 改动

1. 替换三个下载按钮为下拉菜单
2. 添加 handleDownloadCommand 函数
3. 调整发布按钮颜色为 success 类型
4. 添加操作列样式确保对齐

## Migration Plan

1. 修改 datasets/index.vue 操作按钮颜色
2. 修改 images/index.vue 将下载按钮合并为下拉菜单
3. 验证两个页面操作列样式一致

无需数据库迁移，回滚只需撤销前端代码改动。


# Proposal: fix-map-template-tags

## Summary

修复 index.vue 模板中 `<div class="map-viewer">` 缺少闭合标签的问题。

## Problem

模板中存在标签不匹配：
- Line 10: `<div class="map-viewer">` 打开
- Line 22: `</el-container>` 关闭了 el-container，但 map-viewer div 未闭合
- Line 24: `<el-dialog>` 开启

缺少 `</div>` 来闭合 `map-viewer`。

## Solution

在 `</el-container>` 后、`</el-dialog>` 前添加 `</div>`：

```vue
    </el-container>
  </div>   ← 添加这个

  <el-dialog ...>
```

## Scope

- File: `frontend/src/views/map/index.vue`

## Success Criteria

- [ ] 模板编译无错误
- [ ] 页面正常渲染

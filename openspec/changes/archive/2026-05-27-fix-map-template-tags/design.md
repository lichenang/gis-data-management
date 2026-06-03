# Design: fix-map-template-tags

## Overview

在 index.vue 模板中添加缺失的闭合标签。

## Current Template Structure (Broken)

```vue
<template>
  <div class="map-page">
    <div class="breadcrumb-bar">...</div>

    <div class="map-viewer">           ← Line 10: 打开
      <el-container>                  ← Line 11: 打开
        <el-aside>...</el-aside>
        <el-main>...</el-main>
      </el-container>                 ← Line 22: 关闭 el-container
    </el-container>  ← 错误！这不是 map-viewer 的闭合

    <el-dialog>...                    ← Line 24: 但 map-viewer 未闭合!
  </div>
</template>
```

## Correct Structure

```vue
<template>
  <div class="map-page">
    <div class="breadcrumb-bar">...</div>

    <div class="map-viewer">
      <el-container>
        <el-aside>...</el-aside>
        <el-main>...</el-main>
      </el-container>
    </div>                              ← 添加这个闭合标签

    <el-dialog>...
  </div>
</template>
```

## Change Location

在 `</el-container>` (line 22) 和 `<el-dialog` (line 24) 之间添加 `</div>`。

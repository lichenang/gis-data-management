# Proposal: fix-map-zoom-timing

## Summary

修复地图自动缩放未生效的问题，使用 setTimeout 延迟调用 fit。

## Problem Statement

GeoJSON 请求成功返回 200 数据，但地图未自动缩放到数据范围。原因是 `source.getExtent()` 在异步加载完成前可能返回 undefined。

## Root Cause

VectorSource 的 loader 是异步的，`source.getExtent()` 需要在 features 完全加载后才能获取正确的范围。

## Goals

使用 setTimeout 延迟调用 fit，确保在 features 添加完成后执行。

## Success Criteria

- 勾选图层后，地图自动缩放到数据范围

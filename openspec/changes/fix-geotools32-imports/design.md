## Context

GeoTools 32.x 包重构：
- DataStore 等类在 org.geotools.data.*
- SimpleFeature 等 API 接口在 org.geotools.api.*
- 需添加 gt-csv, gt-kml, gt-xsd 依赖

## Goals / Non-Goals

Goals: 修复所有导入错误，添加缺少的依赖

Non-Goals: 不修改业务逻辑

## Decisions

根据 openspec/specs/fix-geotools32-imports.md 中的对照表进行修复。

## Migration Plan

1. 添加依赖到 pom.xml
2. 修复各 Java 文件的 import
3. mvn compile 验证

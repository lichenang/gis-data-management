## Context

当前 `pom.xml` 中 `gt-shapefile` 在 `dependencyManagement` 区域已锁定版本 `32.0`，但在 `dependencies` 区域未显式声明 `version`。Maven 命令行（`mvn compile`）通过 `dependencyManagement` 继承版本，可以正常编译。但 IntelliJ IDEA 等 IDE 在解析直接依赖时，部分版本会优先读取 `dependencies` 中的 `<version>` 标签，缺失时无法将 JAR 加入模块类路径，导致 IDE 报 `Cannot resolve symbol 'shapefile'`。

当前状态：
```
<!-- dependencyManagement 中 ✓ -->
<dependency>
    <groupId>org.geotools</groupId>
    <artifactId>gt-shapefile</artifactId>
    <version>32.0</version>
</dependency>

<!-- dependencies 中 ✗ 缺少 version -->
<dependency>
    <groupId>org.geotools</groupId>
    <artifactId>gt-shapefile</artifactId>
</dependency>
```

本次修复只增加一行 `<version>32.0</version>`。

## Goals / Non-Goals

**Goals:**
- 在 `dependencies` 区域为 `gt-shapefile` 添加 `<version>32.0</version>`
- IDE 重新导入后能正确解析 `org.geotools.data.shapefile.*` 中的所有类

**Non-Goals:**
- 不改动 `dependencyManagement` 中的版本锁定
- 不修改其他 GeoTools 模块的依赖声明
- 不影响 Maven 命令行编译（已经能编译通过）
- 不改动任何 Java / 前端代码

## Decisions

### Decision 1：dependencies 中显式声明 version

直接在 `dependencies` 区域添加 `<version>32.0</version>`，与 `gt-referencing`、`gt-coverage`、`gt-api` 等其他 GeoTools 模块保持一致。

- **理由**：这是项目内其他 GeoTools 直接依赖的一致做法。即使 `dependencyManagement` 已有版本锁定，显式声明能消除 IDE 解析歧义。
- **替代方案**：移除 `dependencyManagement` 中的版本锁定，完全依靠显式声明——但这样做过于激进，且与 `gt-main`、`gt-jdbc-postgis` 等模块的管理方式不一致。

### Decision 2：不改动其他模块

不对 `gt-main`、`gt-jdbc-postgis` 等已在 `dependencyManagement` 声明、在 `dependencies` 未显式写版本号的模块做同样修改。因为：
- 它们没有出现 IDE 解析问题
- 改动范围最小化
- 若后续出现相同问题，可在同一 change 中扩展

## Risks / Trade-offs

- **[低风险] 版本号重复维护**：`version` 出现在 `dependencyManagement` 和 `dependencies` 两处。如果将来升级 GeoTools 版本（如 32.0 → 32.x），需要同步修改两个位置。
- **[无风险] 编译冲突**：两个位置的 version 一致（均为 32.0），不存在冲突。Maven 优先使用 `dependencyManagement` 中的版本，但显式声明在 IDE 解析层面有帮助。

# GeoTools 版本与导入修复

## 技术修复说明

本规格文档为技术修复类变更，不涉及业务需求变更。

---

## 技术要求

### 1. 依赖版本统一

- 所有 GeoTools 依赖版本必须为 32.0
- 在 dependencyManagement 中统一指定

### 2. Import 路径修复

将以下包路径替换：
- `org.opengis.*` → `org.geotools.api.*`

### 3. 验证要求

修改完成后必须通过 Maven 编译验证：
```bash
mvn compile -f backend/pom.xml
```

---

## 受影响的文件（计划）

1. pom.xml
2. FormatDetector.java
3. VectorDataStoreFactory.java
4. MultiFormatImportService.java
5. 其他使用 GeoTools 的类

---

## 验证标准

- mvn compile 成功无错误
- 项目可以正常启动
- 已有功能不受影响

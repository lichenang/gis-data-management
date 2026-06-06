# GeoTools 32.x API 迁移修复 v2

技术修复规格。

## 修复内容

1. SimpleFeatureIterator 实例化问题 - 改用 DataStore.getFeatureSource().getFeatures().simpleIterator()
2. PGgeometry.geomFromByteArray 不存在 - 使用正确的方法
3. WKT 解析 - 使用 org(locationtech.jts.geom.impl.WKTReader

## 验证

mvn compile 通过

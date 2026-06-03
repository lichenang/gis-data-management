## 1. 配置文件修改

- [x] 1.1 将 `application.yml` 中 `geoserver.data-dir` 默认值从 `D:\Program Files\geoserver-2.28.3-bin\data_dir` 改为空字符串 `${GEOSERVER_DATA_DIR:}`
- [x] 1.2 确认 application-local.yml 中无 data-dir 覆盖（已确认无覆盖）

## 2. 配置属性类修改

- [x] 2.1 在 `GeoServerProperties.dataDir` 字段添加 `@Builder.Default` 注解，设置默认值为空字符串
- [x] 2.2 `@Builder.Default` 与 `@Data` 共同使用，builder 不设置时默认空字符串（已验证 Lombok 兼容性）

## 3. 服务层校验逻辑修改

- [x] 3.1 在 `TilePackageServiceImpl.packageTiles()` 中，dataDir 校验已为两步（空值检测在第 60-63 行，目录存在性在第 85-88 行），错误消息已更新为包含解决方案提示
- [x] 3.2 两步校验均在 `ZipOutputStream` 创建之前执行（已验证代码结构）

## 4. 编译验证

- [x] 4.1 运行 `mvn compile -f backend/pom.xml` 确认所有修改编译通过（BUILD SUCCESS）
- [x] 4.2 检查无新增编译警告（仅预存的 deprecation/unchecked 警告）

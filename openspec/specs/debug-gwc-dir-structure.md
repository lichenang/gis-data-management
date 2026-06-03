# 诊断报告：GWC 缓存目录结构分析

## 问题背景

`enumerateTileFiles` 返回 0 个瓦片，但 GWC 目录存在：
- 目录路径：`D:\Program Files\geoserver-2.28.3-bin\data_dir\gwc\gisplatform_raster_38`
- 代码假设结构：`{z}/{x}/{y}.png`

## 可能的原因

### GWC 目录结构变体

GeoWebCache 可能使用多种目录组织方式：

| 模式 | 目录结构示例 | 说明 |
|------|-------------|------|
| **A. 标准 GridSet** | `gisplatform_raster_38/5/0/0.png` | zoom/x/y.png |
| **B. EPSG 前缀** | `gisplatform_raster_38/EPSG_3857_5/0/0.png` | EPSG代码_缩放级别 |
| **C. GridSet 命名** | `gisplatform_raster_38/GoogleMapsCompatible/5/0/0.png` | 网格集名称 |
| **D. 无 zoom 子目录** | `gisplatform_raster_38/0_0_0.png` | 直接用层级编号命名 |
| **E. 不同文件格式** | `gisplatform_raster_38/5/0/0.jpg` | 可能是 JPG 或其他格式 |

### GWC 配置文件参考

GeoWebCache 的目录结构由 `gwc-servlet.xml` 或 `geowebcache.xml` 配置决定。典型配置：

```xml
<gridSet>
  <name>EPSG:3857</name>
  <tileWidth>256</tileWidth>
  <tileHeight>256</tileHeight>
  <resolutions>...</resolutions>
  <extent>
    <coords>...</coords>
  </extent>
</gridSet>
```

---

## 诊断方案

### 方案一：手动文件检查（推荐先做）

直接在服务器上列出目录结构：

```powershell
# 1. 查看顶层子目录
Get-ChildItem -Path "D:\Program Files\geoserver-2.28.3-bin\data_dir\gwc\gisplatform_raster_38" -Directory

# 2. 如果有子目录，进入第一层再看
Get-ChildItem -Path "D:\Program Files\geoserver-2.28.3-bin\data_dir\gwc\gisplatform_raster_38\<子目录名>" -Directory

# 3. 查看文件
Get-ChildItem -Path "D:\Program Files\geoserver-2.28.3-bin\data_dir\gwc\gisplatform_raster_38" -File -Recurse | Select-Object -First 20 FullName
```

预期输出示例：

```
Mode                 LastWriteTime         Length Name
----                 -------------         ------ ----
d-----        2024/1/1     10:00                EPSG_3857_0
d-----        2024/1/1     10:00                EPSG_3857_1
...
```

或者：

```
Mode                 LastWriteTime         Length Name
----                 -------------         ------ ----
d-----        2024/1/1     10:00                0
d-----        2024/1/1     10:00                1
...
```

---

### 方案二：添加临时调试端点

在 `TilePackageServiceImpl` 中临时添加一个诊断方法：

```java
/**
 * 临时诊断方法：列出 GWC 目录结构
 */
public Map<String, Object> diagnoseGwcDirectory(Long datasetId) {
    Map<String, Object> result = new HashMap<>();
    
    String dataDir = geoServerProperties.getDataDir();
    String workspace = geoServerProperties.getWorkspace();
    String layerName = "raster_" + datasetId;
    String gwcDirPath = dataDir.replace('\\', '/') + "/gwc/" + workspace + "_" + layerName;
    
    File gwcDir = new File(gwcDirPath);
    result.put("gwcDirPath", gwcDirPath);
    result.put("gwcDirExists", gwcDir.exists());
    
    if (!gwcDir.exists()) {
        return result;
    }
    
    List<Map<String, Object>> structure = new ArrayList<>();
    listDirectoryStructure(gwcDir, structure, 0, 3); // 只列出前3层
    
    result.put("structure", structure);
    result.put("allFiles", listAllPngFiles(gwcDir));
    
    return result;
}

private void listDirectoryStructure(File dir, List<Map<String, Object>> structure, int depth, int maxDepth) {
    if (depth > maxDepth || dir == null) return;
    
    File[] children = dir.listFiles();
    if (children == null) return;
    
    for (File child : children) {
        Map<String, Object> item = new HashMap<>();
        item.put("name", child.getName());
        item.put("isDirectory", child.isDirectory());
        
        if (child.isDirectory()) {
            item.put("subdirs", Arrays.stream(child.listFiles())
                .filter(File::isDirectory)
                .map(File::getName)
                .collect(Collectors.toList()));
            item.put("files", Arrays.stream(child.listFiles())
                .filter(File::isFile)
                .map(File::getName)
                .collect(Collectors.toList()));
        }
        
        structure.add(item);
    }
}

private List<String> listAllPngFiles(File dir) {
    List<String> files = new ArrayList<>();
    collectFiles(dir, files, ".png");
    return files.stream().limit(50).collect(Collectors.toList());
}

private void collectFiles(File dir, List<String> files, String extension) {
    File[] children = dir.listFiles();
    if (children == null) return;
    
    for (File child : children) {
        if (child.isFile() && child.getName().endsWith(extension)) {
            files.add(child.getAbsolutePath());
        } else if (child.isDirectory()) {
            collectFiles(child, files, extension);
        }
    }
}
```

然后在 Controller 中暴露：

```java
@GetMapping("/tiles/diagnose/{datasetId}")
public R<Map<String, Object>> diagnoseGwcDirectory(@PathVariable Long datasetId) {
    return R.ok(tilePackageService.diagnoseGwcDirectory(datasetId));
}
```

---

### 方案三：增强日志输出（无需重启）

修改 `scanAllPngFiles` 方法，输出更多调试信息：

```java
private List<File> scanAllPngFiles(File gwcDir, int zoomStart, int zoomStop) {
    log.info("===== scanAllPngFiles DEBUG =====");
    log.info("Scanning directory: {}", gwcDir.getAbsolutePath());
    
    List<File> files = new ArrayList<>();
    File[] zoomDirs = gwcDir.listFiles();

    if (zoomDirs == null) {
        log.warn("gwcDir.listFiles() returned null!");
        return files;
    }

    log.info("Found {} items in gwcDir:", zoomDirs.length);
    for (File f : zoomDirs) {
        log.info("  [{}] {}", f.isDirectory() ? "DIR" : "FILE", f.getName());
    }

    // ... 原有逻辑 ...
}
```

然后调用接口触发日志输出，检查日志文件。

---

### 方案四：使用 GWC REST API

GeoWebCache 提供 REST API 可查询 tile layers：

```bash
# 获取图层信息
curl "http://localhost:8080/geoserver/gwc/rest/layers/gisplatform:raster_38.json"

# 获取 tile layer 配置
curl "http://localhost:8080/geoserver/gwc/rest/layers/gisplatform:raster_38.xml"
```

响应中包含 `gridSetId` 信息，可确定使用的网格集名称。

---

## 快速验证步骤

### 步骤 1：手动检查目录结构

在 GeoServer 所在服务器的执行：

```powershell
# Windows PowerShell
cd "D:\Program Files\geoserver-2.28.3-bin\data_dir\gwc"
Get-ChildItem -Recurse -Directory | Select-Object FullName
Get-ChildItem -Recurse -File -Filter "*.png" | Select-Object -First 10 FullName
```

### 步骤 2：检查文件扩展名

```
# 可能的结果：
- *.png  → 代码可直接使用
- *.jpg  → 需要修改代码支持 jpg
- *.PNG  → Windows 下大小写不敏感，Linux 可能有问题
```

### 步骤 3：检查 zoom 目录层级

```
# 可能的结构：
# 1 级: 0/0/0.png        → zoom/x/y.png
# 2 级: EPSG_3857_0/0/0.png → gridset_xxx_z/x/y.png  
# 0 级: 0_0_0.png        → 直接文件命名
```

---

## 常见 GWC 目录模式示例

### EPSG:3857 标准结构

```
gwc/
└── gisplatform_raster_38/
    ├── EPSG_3857_0/
    │   └── 0/
    │       └── 0.png
    ├── EPSG_3857_1/
    │   ├── 0/
    │   │   ├── 0.png
    │   │   └── 1.png
    │   └── 1/
    │       └── 0.png
    └── ...
```

### GoogleMapsCompatible 结构

```
gwc/
└── gisplatform_raster_38/
    ├── GoogleMapsCompatible/
    │   └── 0/
    │       └── 0/
    │           └── 0.png
    └── ...
```

### 无 gridset 前缀

```
gwc/
└── gisplatform_raster_38/
    ├── 0/
    │   └── 0/
    │       └── 0.png
    └── ...
```

---

## 预期输出格式

请将手动检查的结果保存，以便进一步分析：

```json
{
  "gwcDirPath": "D:\\Program Files\\geoserver-2.28.3-bin\\data_dir\\gwc\\gisplatform_raster_38",
  "topLevelDirs": ["EPSG_3857_0", "EPSG_3857_1", ...],
  "sampleFilePath": "D:\\Program Files\\geoserver-2.28.3-bin\\data_dir\\gwc\\gisplatform_raster_38\\EPSG_3857_5\\0\\0\\0.png",
  "fileExtension": ".png"
}
```

---

## 后续动作

1. 先执行**方案一**手动检查，确认实际目录结构
2. 根据结果判断是目录结构问题还是其他问题
3. 如需修改代码，创建一个新的 change 进行修复

需要我帮你添加临时诊断代码吗？

# 地理信息数据管理平台规范

## 1. 项目概述

### 1.1 项目背景与目标

构建一个集空间数据管理、服务发布、在线可视化于一体的 Web 平台，支持矢量数据（点、线、面）和影像数据的导入、编辑、查询、切片发布与地图展示，并具备完善的用户管理与数据权限控制。

### 1.2 系统边界

| 组件 | 职责 | 归属 |
|------|------|------|
| 本平台 | 数据管理、权限控制、地图可视化、服务发布触发 | 自主开发 |
| PostgreSQL/PostGIS | 空间数据存储、业务数据存储 | 基础设施 |
| GeoServer | WMS/WMTS 地图服务、矢量切片 | 基础设施 |
| GeoWebCache | 切片缓存管理 | 基础设施 |
| MinIO | 影像原始文件存储 | 基础设施 |
| Nginx | 静态切片文件服务、反向代理 | 基础设施 |

---

## 2. 功能模块划分

### 2.1 模块总览

```
┌─────────────────────────────────────────────────────────────────┐
│                        GIS 平台功能模块                          │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌───────────────┐  ┌───────────────┐  ┌───────────────┐       │
│  │  用户与权限管理  │  │   系统授权     │  │   系统管理     │       │
│  │  - 用户/角色   │  │  - 激活/校验   │  │  - 数据源配置  │       │
│  │  - RBAC       │  │  - 只读模式    │  │  - 操作日志    │       │
│  │  - 数据权限    │  │  - 续期管理    │  │  - 系统监控    │       │
│  └───────┬───────┘  └───────┬───────┘  └───────┬───────┘       │
│          │                  │                  │                │
│  ┌───────┴──────────────────┴──────────────────┴───────┐       │
│  │                      核心业务层                        │       │
│  ├───────────────┬───────────────┬───────────────┬─────┤       │
│  │  矢量数据管理   │  影像数据管理   │   服务发布    │ 切片 │       │
│  │  - 上传解析   │  - 元数据管理   │  - WMS/WMTS  │ 缓存 │       │
│  │  - 属性编辑   │  - 存储管理    │  - REST API  │ 更新 │       │
│  │  - 版本管理   │  - 校验        │              │     │       │
│  └───────────────┴───────────────┴───────────────┴─────┘       │
│                                                                 │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │                    地图可视化                             │   │
│  │  - 底图切换  - 图层管理  - 空间查询  - 测量工具          │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### 2.2 用户与权限管理

#### 2.2.1 用户管理
- 用户注册（可禁用，由管理员分配）
- 用户登录（JWT Token）
- 用户基本信息编辑
- 密码重置

#### 2.2.2 角色管理
预定义角色：
| 角色 | 权限描述 |
|------|----------|
| 管理员 | 系统全部权限，包括数据源配置、用户管理 |
| 数据编辑员 | 数据集管理、编辑、发布、切片管理 |
| 数据查看员 | 只读访问已授权的数据集 |
| 普通用户 | 同数据查看员，可申请数据权限 |

#### 2.2.3 权限模型（RBAC）

```
┌─────────┐       ┌─────────┐       ┌─────────┐
│  User   │──────▶│  Role   │──────▶│Permission│
└─────────┘       └─────────┘       └─────────┘
      │                 │
      │                 │
      ▼                 ▼
┌─────────────────────────────────────┐
│         Data Permission              │
│  (Dataset-Level + Row-Level)        │
└─────────────────────────────────────┘
```

#### 2.2.4 数据权限（重点实现）

**数据集级权限**（第一阶段实现）：
- 管理员为角色分配可访问的数据集列表
- 用户登录后只能看到有权限的数据集
- 权限矩阵：可访问、可编辑、可下载、可发布

**行级权限**（后续迭代）：
- 同一数据集中，按用户/区域分配编辑权限
- 通过 geometry 空间关系或行政区域属性字段判断

#### 2.2.5 多租户预留
- 所有业务表增加 `tenant_id` 字段
- 当前默认值：`default`
- 查询时自动过滤租户数据

### 2.3 矢量数据管理

#### 2.3.1 支持格式
- Shapefile（.shp + .shx + .dbf + .prj）
- GeoJSON（.geojson/.json）
- KML/KMZ

#### 2.3.2 数据上传与解析流程
```
用户上传 ──▶ 服务端解析 ──▶ 坐标转换 ──▶PostGIS存储
                   │
                   ▼
            生成数据集记录
            (名称、范围、要素数、坐标系)
```

#### 2.3.3 在线编辑
- 属性编辑：表单编辑、批量赋值
- 空间编辑：点/线/面绘制、拖拽、节点编辑
- **乐观锁机制**：
  - 要素表增加 `version` 字段
  - 编辑前获取版本号
  - 保存时检查版本，若冲突返回错误提示用户刷新

#### 2.3.4 查询功能
- 属性查询：SQL WHERE 条件
- 空间查询：矩形框选、多边形筛选、圆选
- 组合查询：空间条件 + 属性条件

#### 2.3.5 版本管理
- 数据集版本记录：每次重大修改创建快照
- 版本对比：可查看历史版本的差异

### 2.4 影像数据管理

#### 2.4.1 支持格式
- GeoTIFF（.tif/.tiff）
- COG（Cloud Optimized GeoTIFF）
- 支持 .ovr 概览文件

#### 2.4.2 上传策略
- 用户手动预处理后上传（预建金字塔）
- 服务端仅做校验，不自动构建金字塔

#### 2.4.3 校验内容
- 坐标系合法性（EPSG:4326 等）
- 波段数、像素类型
- 文件完整性

#### 2.4.4 存储管理
- 原始文件存 MinIO
- 元数据存 PostgreSQL（坐标系、分辨率、拍摄时间、波段数等）

### 2.5 服务发布与切片

#### 2.5.1 GeoServer 集成
通过 GeoServer REST API 实现：
- 创建工作区（Workspace）
- 创建数据存储（Store）
- 发布图层（Layer）
- 配置图层样式（SLD）

#### 2.5.2 矢量切片
- 格式：PBF（Protocol Buffer）
- 策略：GeoWebCache 动态生成 + 缓存
- 缓存失效：管理后台手动刷新

#### 2.5.3 影像切片
- 策略：预切片方案
- 流程：上传 GeoTIFF → GeoServer REST API 触发切片任务 → 切片写入本地磁盘
- 服务：Nginx 直接提供静态切片文件
- 更新：管理后台提供"清除缓存/重新切片"按钮

```
用户触发切片 ──▶ GeoServer REST API ──▶ 切片任务
                                            │
                                            ▼
                                    本地磁盘目录
                                            │
                                            ▼
                                    Nginx 静态服务
```

#### 2.5.4 服务状态监控
- GeoServer 状态检测（是否在线）
- 图层发布状态查询
- 切片生成状态跟踪

### 2.6 地图可视化

#### 2.6.1 技术选型
- OpenLayers 6+（推荐）或 Leaflet + Vue-Leaflet

#### 2.6.2 底图支持
- OSM（OpenStreetMap）
- 天地图（需申请 Key）
- 自定义 WMTS 服务

#### 2.6.3 图层管理
- 加载/卸载图层
- 透明度调整（0-100%）
- 图层顺序（z-index）
- 图层分组

#### 2.6.4 交互功能
- 拉框查询（矩形范围选择）
- 点选查询（点击要素弹窗）
- 测量工具（距离、面积）
- 要素高亮显示

### 2.7 系统管理

#### 2.7.1 数据源配置
- PostgreSQL 连接参数
- GeoServer REST API 地址、用户名、密码
- MinIO 连接参数（Endpoint、AccessKey、SecretKey、Bucket）
- Nginx 静态切片目录配置

#### 2.7.2 操作日志与审计追踪
系统自动记录所有变更操作，提供完整的审计追踪能力。

**记录范围**：
- 用户登录/登出
- 数据增删改（数据集、要素、影像）
- 服务发布/切片操作
- 配置变更
- 授权相关操作

**字段设计**：
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGSERIAL | 日志ID |
| user_id | BIGINT | 操作人ID |
| username | VARCHAR(64) | 操作人用户名 |
| module | VARCHAR(64) | 模块（dataset/feature/raster/gs/license等） |
| operation | VARCHAR(128) | 操作类型（create/update/delete/publish等） |
| target_type | VARCHAR(32) | 目标资源类型 |
| target_id | BIGINT | 目标资源ID |
| before_snapshot | JSONB | 变更前快照 |
| after_snapshot | JSONB | 变更后快照 |
| method | VARCHAR(256) | 请求方法+路径 |
| params | JSONB | 请求参数 |
| result | TEXT | 执行结果/错误信息 |
| ip_address | VARCHAR(64) | 客户端IP |
| user_agent | VARCHAR(512) | 浏览器信息 |
| create_time | TIMESTAMP | 操作时间 |

**审计特性**：
- 空间数据变更记录几何信息快照
- 敏感字段（如密码）不记录
- 日志保留时长可配置（默认1年）
- 前端管理后台提供多维度查询：
  - 按时间范围
  - 按操作人
  - 按模块/操作类型
  - 按目标资源
  - 支持导出Excel

#### 2.7.3 系统监控与服务健康

**监控内容**：
| 组件 | 监控指标 | 状态检查方式 |
|------|----------|--------------|
| PostgreSQL | 连接状态、查询延迟、连接池 | JDBC 测试查询 |
| PostGIS | 空间函数可用性 | SELECT postgis_version() |
| GeoServer | 服务状态、图层数量 | REST API /rest/about/version.json |
| MinIO | 服务状态、存储使用量 | S3 API HEAD bucket |
| Nginx | 服务状态、请求延迟 | HTTP 请求 |

**健康检查实现**：
```yaml
# Spring Boot Actuator 配置
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
  endpoint:
    health:
      show-details: always
      probes:
        enabled: true
  health:
    postgis:
      enabled: true
    geoserver:
      enabled: true
    minio:
      enabled: true
```

**管理后台展示**：
- 仪表盘顶部显示健康状态指示器（绿/黄/红）
- 服务状态面板：各组件在线/离线状态
- 数据量统计：数据集数量、要素总数、存储使用
- 图表：近7天数据变更趋势

### 2.9 数据备份与恢复

系统提供多层次的数据备份与恢复机制，确保数据安全。

#### 2.9.1 备份策略

| 数据类型 | 备份方式 | 备份频率 | 保留策略 |
|----------|----------|----------|----------|
| PostgreSQL 业务数据 | pg_dump + WAL 归档 | 每日全量 + 实时 WAL | 保留30天 |
| PostGIS 空间数据 | pg_dump | 每日全量 | 保留30天 |
| MinIO 影像数据 | 桶镜像/定时同步 | 每小时增量 | 保留30天 |
| 切片缓存 | 不备份 | - | 可重新生成 |
| 系统配置 | 配置文件备份 | 变更时 | 保留5个版本 |

#### 2.9.2 备份实现

**PostgreSQL 备份**：
```bash
# 全量备份脚本示例
pg_dump -h localhost -U postgres -d gisdb -Fc -f /backup/gisdb_$(date +%Y%m%d).dump

# WAL 归档（需配置 postgresql.conf）
wal_level = replica
archive_mode = on
archive_command = 'cp %p /backup/wal/%f'
```

**MinIO 影像备份**：
- 方案A：使用 `mc mirror` 定时同步到备份存储
- 方案B：配置 MinIO 桶复制策略

#### 2.9.3 手动备份

管理后台提供手动备份入口：
- 一键备份当前数据库
- 备份文件下载
- 备份记录查询

```
┌─────────────────────────────────────────┐
│         手动备份操作流程                  │
├─────────────────────────────────────────┤
│                                         │
│  用户点击"手动备份" ──▶ 创建备份任务       │
│            │                            │
│            ▼                            │
│  后端执行 pg_dump ──▶ 生成 .dump 文件    │
│            │                            │
│            ▼                            │
│  压缩 + 记录元数据 ──▶ 返回下载链接       │
│                                         │
└─────────────────────────────────────────┘
```

#### 2.9.4 恢复功能

- 从备份列表选择恢复点
- 恢复前自动创建当前数据快照（支持回滚）
- 恢复进度显示
- 恢复后自动刷新缓存

### 2.10 元数据管理

#### 2.10.1 元数据标准
采用 **ISO 19115** 地理信息元数据标准，支持以下核心元素：

| 类别 | 元素 | 说明 |
|------|------|------|
| 标识信息 | title、abstract、date | 数据集名称、描述、日期 |
| 创建者 | originatorcontact | 负责单位、联系人 |
| 地理范围 | geographicBoundingBox | 最小外接矩形 |
| 时间范围 | temporalExtent | 数据时间范围 |
| 主题分类 | topicCategory | 影像/高程/矢量等 |
| 关键字 | keywords | 搜索标签 |
| 数据质量 | lineage、 DQ_* | 数据来源、质量报告 |
| 分发信息 | distributionInfo | 分发格式、访问地址 |
| 坐标系 | referenceSystem | EPSG代码、投影信息 |

#### 2.10.2 功能特性

- **自动提取**：上传数据时自动从文件提取元数据（坐标系、范围、创建时间等）
- **手动编辑**：提供元数据表单，支持手动补充/修改
- **多语言**：支持中文、英文双语
- **预览与导出**：元数据预览、导出为 XML/JSON

#### 2.10.3 前端展示
- 数据集详情页集成元数据显示面板
- 元数据编辑对话框（折叠面板分组展示）
- 支持批量编辑多个数据集元数据

### 2.11 数据质检

#### 2.11.1 质检规则

| 规则类型 | 检查内容 | 检测方式 |
|----------|----------|----------|
| 几何有效性 | 自相交、环闭合、孔洞有效性 | JTS isValid() |
| 几何空值 | 几何字段为空 | SQL IS NULL |
| 坐标系 | 是否为支持的坐标系 | EPSG 白名单 |
| 属性完整性 | 必填字段缺失 | 属性约束配置 |
| 属性类型 | 字段类型与定义不符 | 类型校验 |
| 范围校验 | 几何超过合理范围 | 边界框检查 |
| 重复要素 | 几何和属性完全重复 | 空间 + 属性哈希 |

#### 2.11.2 质检流程

```
┌─────────────────────────────────────────────────────────────────┐
│                      数据质检流程                                │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  用户触发质检 ──▶ 配置质检规则 ──▶ 执行检查 ──▶ 生成报告         │
│       │                                    │                    │
│       │                                    ▼                    │
│       │                          ┌──────────────────┐           │
│       │                          │ 质检报告          │           │
│       │                          │ - 通过/警告/失败  │           │
│       │                          │ - 问题要素列表    │           │
│       │                          │ - 修复建议        │           │
│       │                          └──────────────────┘           │
│       │                                    │                    │
│       ▼                                    ▼                    │
│  选择性修复                            确认导入                   │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

#### 2.11.3 质检报告

- 问题统计：按类型/严重程度分组统计
- 要素定位：点击问题跳转定位到具体要素
- 批量修复：支持一键修复常见问题（如补齐必填字段）
- 质检历史：每次上传的质检记录可追溯

### 2.12 符号化与样式管理

#### 2.12.1 样式类型

| 类型 | 说明 | 适用图层 |
|------|------|----------|
| 单一符号 | 统一渲染样式 | 点/线/面 |
| 分类符号 | 按属性值分类渲染 | 离散值字段 |
| 分段符号 | 按数值区间分段渲染 | 连续值字段 |
| 热力图 | 密度可视化 | 大量点数据 |
| 图标标注 | 文字或图标标注 | 特定要素 |

#### 2.12.2 样式配置

```json
{
  "type": "categorized",
  "field": "land_type",
  "defaultSymbol": {
    "fill": "#cccccc",
    "stroke": "#666666"
  },
  "classes": [
    {"value": "residential", "symbol": {"fill": "#ffcc00", "label": "住宅"}},
    {"value": "commercial", "symbol": {"fill": "#ff6600", "label": "商业"}},
    {"value": "industrial", "symbol": {"fill": "#cc0000", "label": "工业"}}
  ]
}
```

#### 2.12.3 预置模板

系统提供常用行业样式模板：
- 土地利用分类
- 道路等级（国省县道）
- 行政区划边界
-  POI 分类标注

用户可保存自定义样式为模板，供其他数据集复用。

#### 2.12.4 SLD 导出

- 内部样式可导出为 GeoServer SLD 文件
- 支持从现有 SLD 文件导入样式

### 2.13 数据导出与共享

#### 2.13.1 导出格式

| 格式 | 说明 | 适用数据 |
|------|------|----------|
| Shapefile | ESRI 格式，包含 .shp/.shx/.dbf/.prj | 矢量 |
| GeoJSON | JSON 格式，Web 友好 | 矢量 |
| KML/KMZ | Google Earth 格式 | 矢量 |
| GeoTIFF | 带坐标的影像格式 | 影像 |
| CSV | 属性表格（无空间） | 任意 |

#### 2.13.2 导出选项

- **范围选择**：全量导出、框选导出、单个要素
- **字段选择**：选择导出的属性列
- **坐标转换**：导出时目标坐标系选择
- **编码选择**：导出字符编码（UTF-8/GBK）

#### 2.13.3 分享链接

支持生成临时分享链接：

```
分享流程：
1. 用户选择数据集/范围/有效期
2. 系统生成唯一链接 + 提取码
3. 分享给其他人（无需登录即可查看）
4. 链接过期后自动失效
```

**分享链接特性**：
- 可设置有效期（1小时/1天/7天/永久）
- 可设置访问密码
- 可限制访问次数
- 支持查看/下载权限分离

分享链接访问展示：
- 简洁的查看页面
- 在线地图浏览
- 数据表格预览
- 一键下载

### 2.14 系统授权

#### 2.8.1 授权模式概述
系统采用 **离线授权文件 + 在线激活** 双重模式，支持灵活的授权管理：

| 模式 | 适用场景 | 流程 |
|------|----------|------|
| 在线激活 | 服务器可访问互联网 | 输入授权码 → 自动获取授权文件 → 激活成功 |
| 离线激活 | 服务器无法访问互联网 | 导出机器指纹 → 上传至厂商获取授权文件 → 上传激活 |

#### 2.8.2 授权码内容结构
授权信息 JSON 结构如下：
```json
{
  "product": "GIS-Platform",
  "version": "1.0",
  "edition": "professional",
  "issuedAt": "2025-01-01T00:00:00Z",
  "expiresAt": "2026-01-01T00:00:00Z",
  "maxUsers": 100,
  "modules": ["vector", "raster", "wms", "wmts", "analysis"],
  "features": {
    "multiTenant": true,
    "customStyle": true,
    "realtimeSync": false
  },
  "licensee": "测试客户有限公司",
  "machineFingerprint": "sha256:xxxxx"
}
```

#### 2.8.3 授权时限类型
| 类型 | 说明 | 典型场景 |
|------|------|----------|
| 临时版 | 30 天 | 试用/演示 |
| 标准版 | 1 年 | 正式授权 |
| 永久版 | 无过期 | 买断授权 |

#### 2.8.4 机器指纹
用于绑定授权到特定硬件，防止授权文件被复制到其他机器。指纹采集内容：

```
┌─────────────────────────────────────────────────────┐
│               机器指纹采集清单                        │
├─────────────────────────────────────────────────────┤
│  项目           │  获取方式           │  权重       │
├─────────────────┼─────────────────────┼────────────┤
│  MAC 地址       │ network get + hash  │  30%       │
│  CPU 序列号     │ wmic cpu get        │  25%       │
│  主板序列号     │ wmic baseboard      │  25%       │
│  磁盘序列号     │ wmic diskdrive      │  20%       │
└─────────────────┴─────────────────────┴────────────┘

最终指纹 = SHA256(排序拼接 + salt)
```

#### 2.8.5 加密方案
采用 **RSA-2048 + AES-256** 混合加密：

```
┌─────────────────────────────────────────────────────────┐
│                    加密流程                              │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  厂商端：                                               │
│  ┌────────────────┐    AES-256    ┌────────────────┐  │
│  │ 授权信息 JSON  │ ────────────▶ │AES加密内容(enc)│  │
│  └────────────────┘    (随机KEY)   └───────┬────────┘  │
│                                           │            │
│                                           ▼            │
│                                    ┌────────────────┐  │
│                                    │ RSA私钥签名     │  │
│                                    │enc内容+时间戳   │  │
│                                    └───────┬────────┘  │
│                                            │            │
│                                            ▼            │
│                                    .lic 文件格式：       │
│                                    {enc}|{signature}   │
│                                                         │
├─────────────────────────────────────────────────────────┤
│  客户端：                                               │
│  ┌────────────────┐    RSA公钥    ┌────────────────┐  │
│  │  .lic 文件     │ ────────────▶ │ 验证签名       │  │
│  │ enc|signature │   (内置)       │  ──▶ 提取KEY   │  │
│  └───────┬────────                    └───────┬────────┘  │
│          │                                    │           │
│          │                          AES-256   │           │
│          │◀─────────────────────────────────┘           │
│          │                                               │
│          ▼                                               │
│  还原授权信息 JSON                                       │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

#### 2.8.6 授权校验机制

**首次启动流程**：
```
系统启动 ──▶ 检查 license 表 ──▶
                │
                ├─ 无记录 ──▶ 弹窗提示未授权 ──▶ 展示激活向导
                │
                └─ 有记录 ──▶ 校验机器指纹匹配
                                  │
                                  ├─ 匹配 ──▶ 校验授权有效性
                                  │           │
                                  │           ├─ 有效 ──▶ 正常启动
                                  │           │
                                  │           ├─ 过期 ──▶ 进入只读模式
                                  │           │
                                  │           └─ 即将过期(N天) ──▶ 启动+警告提示
                                  │
                                  └─ 不匹配 ──▶ 弹窗提示机器变更 ──▶ 需要重新激活
```

**定期校验**：
- 启动时校验（应用过滤器）
- 定时任务：每天凌晨 3 点校验一次
- 每次调用管理接口时校验（轻量级检查）

#### 2.8.7 只读模式
授权过期后，系统进入只读模式：

| 操作 | 状态 |
|------|------|
| 查看地图 | ✓ 允许 |
| 查询数据 | ✓ 允许 |
| 导出数据 | ✓ 允许 |
| 新增/编辑数据 | ✗ 禁止 |
| 删除数据 | ✗ 禁止 |
| 发布服务 | ✗ 禁止 |
| 系统配置 | ✗ 禁止 |

前端表现：
- 工具栏灰态disable
- 显示"授权已过期，仅提供只读访问"的横幅
- 登录页显示授权到期倒计时

#### 2.8.8 管理后台
在系统管理模块增加授权信息面板：
- 当前授权状态（已激活/未激活/过期）
- 到期时间及剩余天数
- 绑定的机器信息
- 已授权用户数/最大用户数
- 可用功能模块列表
- 手动续期/重新激活按钮

---

## 3. 技术架构设计

### 3.1 技术选型

| 层级 | 技术 | 版本要求 | 说明 |
|------|------|----------|------|
| 后端框架 | Spring Boot | 3.x LTS (3.5+) | Java 17+ |
| ORM | MyBatis-Plus | 3.5.x | 含 GeometryTypeHandler |
| 数据库 | PostgreSQL | 16+ | 需安装 PostGIS 3.4+ |
| 空间库 | GeoTools | 28.x | Java 空间处理 |
| 空间库 | JTS | 1.19.x | 拓扑操作 |
| 对象存储 | MinIO | 最新版 | S3 兼容 |
| 地图服务 | GeoServer | 2.24+ |  |
| 切片缓存 | GeoWebCache | 内置于 GeoServer |  |
| 前端框架 | Vue 3 | 3.4+ |  |
| 构建工具 | Vite | 5.x |  |
| UI 组件 | Element Plus | 2.x |  |
| 地图库 | OpenLayers | 6.x / 7.x |  |
| 认证 | Spring Security + JWT |  |  |
| 健康监控 | Spring Boot Actuator | 3.x |  |
| API 文档 | SpringDoc OpenAPI + Knife4j | 2.x | 开发环境启用 |
| Web 服务器 | Nginx | 1.24+ | 反向代理 + 切片服务 |

### 3.2 模块依赖关系

```
┌─────────────────────────────────────────────────────────────────┐
│                        前端 (Vue3 + Vite)                        │
│   views/  pages/  components/  router/  api/  stores/           │
└───────────────────────────────┬─────────────────────────────────┘
                                │ HTTP / WebSocket
┌───────────────────────────────┴─────────────────────────────────┐
│                     Spring Boot 后端                             │
├─────────────────────────────────────────────────────────────────┤
│  controller层 ──▶ service层 ──▶ mapper层                        │
│       │               │               │                          │
│       │               │               ▼                          │
│       │               │        ┌─────────────┐                   │
│       │               │        │  MyBatis-Plus│                  │
│       │               │        └──────┬──────┘                   │
│       │               │               │                          │
│       ▼               ▼               ▼                          │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │                    空间数据处理层                          │   │
│  │            GeoTools / JTS  ──▶  PostGIS                   │   │
│  └────────────────────────┬─────────────────────────────────┘   │
│                           │                                       │
│  ┌────────────┐  ┌───────┴───────┐  ┌────────────┐              │
│  │  MinIO SDK │  │  GeoServer    │  │  JWT       │              │
│  │            │  │  REST Client  │  │  Security  │              │
│  └────────────┘  └───────────────┘  └────────────┘              │
└─────────────────────────────────────────────────────────────────┘
```

### 3.3 部署架构

```
                              ┌─────────────────┐
                              │    用户浏览器    │
                              └────────┬────────┘
                                       │
                                       │ HTTPS
                                       ▼
┌─────────────────────────────────────────────────────────────────┐
│                         Nginx (反向代理)                         │
│              静态资源 / 切片文件 / API 转发                       │
└───────────────────────────────┬─────────────────────────────────┘
                                │
          ┌─────────────────────┼─────────────────────┐
          │                     │                     │
          ▼                     ▼                     ▼
   ┌────────────┐       ┌────────────┐       ┌────────────┐
   │  GeoWebCache │      │  Spring    │       │   MinIO    │
   │  (切片缓存)  │      │   Boot     │       │ (影像存储) │
   │   PBF/PNG   │◀────▶│   后端     │◀─────▶│            │
   └────────────┘       └──────┬─────┘       └────────────┘
                                │
                                ▼
                         ┌────────────┐
                         │ PostgreSQL │
                         │ + PostGIS  │
                         └────────────┘
```

---

## 4. 数据库设计

### 4.1 表结构总览

```
┌─────────────────────────────────────────────────────────────────┐
│                        核心业务表                                 │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  sys_user          ─ 用户表                                     │
│  sys_role          ─ 角色表                                     │
│  sys_permission    ─ 权限表                                     │
│  sys_user_role     ─ 用户-角色关联                              │
│  sys_role_permission─ 角色-权限关联                             │
│                                                                 │
│  dataset           ─ 数据集表（矢量/影像）                       │
│  dataset_version   ─ 数据集版本记录                             │
│  dataset_permission─ 数据集权限分配                             │
│                                                                 │
│  vector_feature    ─ 矢量要素表（几何+属性）                     │
│  raster_metadata   ─ 影像元数据表                               │
│                                                                 │
│  map_layer         ─ 地图图层配置                               │
│  map_layer_group   ─ 图层分组                                   │
│                                                                 │
│  gs_layer          ─ GeoServer 图层映射                         │
│                                                                 │
│  sys_config        ─ 系统配置                                   │
│  sys_operation_log ─ 操作日志                                   │
│                                                                 │
│  license           ─ 授权信息表                                  │
│  license_machine   ─ 机器指纹记录                               │
│                                                                 │
│  backup_record     ─ 备份记录                                   │
│  backup_settings   ─ 备份配置                                   │
│                                                                 │
│  dataset_metadata  ─ 数据集元数据（ISO 19115）                  │
│  quality_result    ─ 质检结果                                   │
│  quality_rule      ─ 质检规则                                   │
│                                                                 │
│  layer_style       ─ 图层样式                                   │
│  style_template    ─ 样式模板                                   │
│                                                                 │
│  data_export       ─ 导出任务                                   │
│  share_link        ─ 分享链接                                   │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### 4.2 核心表详细设计

#### 4.2.1 用户与角色表

```sql
-- 租户隔离（所有表共用）
ALTER TABLE sys_user ADD COLUMN tenant_id VARCHAR(64) NOT NULL DEFAULT 'default';

-- 用户表
CREATE TABLE sys_user (
    id              BIGSERIAL PRIMARY KEY,
    username        VARCHAR(64) NOT NULL UNIQUE,
    password        VARCHAR(128) NOT NULL,
    nickname        VARCHAR(64),
    email           VARCHAR(128),
    phone           VARCHAR(32),
    avatar          VARCHAR(512),
    status          SMALLINT NOT NULL DEFAULT 1,  -- 1:启用 0:禁用
    tenant_id       VARCHAR(64) NOT NULL DEFAULT 'default',
    create_time     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     TIMESTAMP,
    deleted         SMALLINT NOT NULL DEFAULT 0
);

-- 角色表
CREATE TABLE sys_role (
    id              BIGSERIAL PRIMARY KEY,
    code            VARCHAR(64) NOT NULL UNIQUE,  -- ADMIN, EDITOR, VIEWER
    name            VARCHAR(64) NOT NULL,
    description     VARCHAR(256),
    tenant_id       VARCHAR(64) NOT NULL DEFAULT 'default',
    create_time     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     TIMESTAMP,
    deleted         SMALLINT NOT NULL DEFAULT 0
);

-- 用户-角色关联
CREATE TABLE sys_user_role (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT NOT NULL,
    role_id     BIGINT NOT NULL,
    tenant_id   VARCHAR(64) NOT NULL DEFAULT 'default',
    UNIQUE(user_id, role_id)
);

-- 权限表
CREATE TABLE sys_permission (
    id          BIGSERIAL PRIMARY KEY,
    code        VARCHAR(128) NOT NULL UNIQUE,  -- dataset:read, dataset:write
    name        VARCHAR(64) NOT NULL,
    type        VARCHAR(32) NOT NULL,  -- menu, button, api
    tenant_id   VARCHAR(64) NOT NULL DEFAULT 'default',
    deleted     SMALLINT NOT NULL DEFAULT 0
);

-- 角色-权限关联
CREATE TABLE sys_role_permission (
    id              BIGSERIAL PRIMARY KEY,
    role_id         BIGINT NOT NULL,
    permission_id   BIGINT NOT NULL,
    tenant_id       VARCHAR(64) NOT NULL DEFAULT 'default'
);
```

#### 4.2.2 数据集表

```sql
-- 数据集表
CREATE TABLE dataset (
    id                  BIGSERIAL PRIMARY KEY,
    name                VARCHAR(128) NOT NULL,
    description         VARCHAR(512),
    type                VARCHAR(32) NOT NULL,  -- vector, raster
    geometry_type       VARCHAR(32),  -- Point, LineString, Polygon (矢量)
    srs                 VARCHAR(64) NOT NULL DEFAULT 'EPSG:4326',  -- 坐标系
    
    -- 存储信息
    storage_type        VARCHAR(32) NOT NULL,  -- postgis, minio
    table_name          VARCHAR(128),  -- PostGIS 表名 (矢量)
    minio_key           VARCHAR(512),  -- MinIO 对象路径 (影像)
    
    -- 范围
    extent              JSONB,  -- {"minX":1,"minY":2,"maxX":3,"maxY":4}
    feature_count       INTEGER,
    
    -- 状态
    status              VARCHAR(32) NOT NULL DEFAULT 'draft',  -- draft, published
    version             INTEGER NOT NULL DEFAULT 1,
    
    -- GeoServer 关联
    workspace           VARCHAR(128),
    store_name          VARCHAR(128),
    layer_name          VARCHAR(128),
    
    -- 元数据
    tags                JSONB,
    created_by          BIGINT NOT NULL,
    
    tenant_id           VARCHAR(64) NOT NULL DEFAULT 'default',
    create_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time         TIMESTAMP,
    deleted             SMALLINT NOT NULL DEFAULT 0
);

-- 数据集版本记录
CREATE TABLE dataset_version (
    id              BIGSERIAL PRIMARY KEY,
    dataset_id      BIGINT NOT NULL,
    version         INTEGER NOT NULL,
    snapshot_table  VARCHAR(128),  -- 快照表名
    description     VARCHAR(256),
    created_by      BIGINT NOT NULL,
    create_time     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 数据集权限分配（数据集级）
CREATE TABLE dataset_permission (
    id              BIGSERIAL PRIMARY KEY,
    dataset_id      BIGINT NOT NULL,
    role_id         BIGINT,  -- 角色授权
    user_id         BIGINT,  -- 用户直接授权
    
    -- 权限标志
    can_access      BOOLEAN DEFAULT TRUE,
    can_edit        BOOLEAN DEFAULT FALSE,
    can_download    BOOLEAN DEFAULT FALSE,
    can_publish     BOOLEAN DEFAULT FALSE,
    
    -- 行级权限（后续迭代）
    row_filter      JSONB,  -- {"region":"xxx"} 或空间范围
    
    tenant_id       VARCHAR(64) NOT NULL DEFAULT 'default',
    create_time     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by      BIGINT NOT NULL
);
```

#### 4.2.3 矢量要素表（动态创建）

```sql
-- 每个矢量数据集对应一个要素表（动态创建）
-- 示例：dataset_001 的要素表

CREATE TABLE vector_features_001 (
    id                  BIGSERIAL PRIMARY KEY,
    dataset_id          BIGINT NOT NULL,
    
    -- 几何字段（PostGIS）
    geometry            GEOMETRY(GEOMETRY, 4326) NOT NULL,
    
    -- 属性字段（JSONB 存储灵活属性）
    properties          JSONB,
    
    -- 乐观锁
    version             INTEGER NOT NULL DEFAULT 1,
    
    -- 审计
    create_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time         TIMESTAMP,
    create_by           BIGINT,
    update_by           BIGINT,
    
    deleted             SMALLINT NOT NULL DEFAULT 0
);

-- 空间索引
CREATE INDEX idx_vector_features_001_geom ON vector_features_001 USING GIST(geometry);
CREATE INDEX idx_vector_features_001_dataset ON vector_features_001(dataset_id);
```

#### 4.2.4 影像元数据表

```sql
-- 影像元数据
CREATE TABLE raster_metadata (
    id                  BIGSERIAL PRIMARY KEY,
    dataset_id          BIGINT NOT NULL UNIQUE,
    
    -- 文件信息
    file_name           VARCHAR(256) NOT NULL,
    file_size           BIGINT,
    minio_bucket        VARCHAR(128),
    minio_key           VARCHAR(512),
    
    -- 影像属性
    width               INTEGER,
    height              INTEGER,
    bands               INTEGER,
    pixel_type          VARCHAR(32),  -- Float32, UInt16, Byte
    no_data_value       DOUBLE PRECISION,
    
    -- 地理信息
    crs                 VARCHAR(64),
    transform           JSONB,  -- GeoTIFF 转换矩阵
    
    -- 金字塔信息
    overviews           JSONB,  -- {"0": {"width":...,"height":...}}
    
    -- 时间信息
    capture_time        TIMESTAMP,
    
    -- 校验状态
    validation_status   VARCHAR(32) DEFAULT 'pending',  -- pending, valid, invalid
    validation_message  VARCHAR(512),
    
    create_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time         TIMESTAMP
);
```

#### 4.2.5 地图图层配置

```sql
-- 地图图层
CREATE TABLE map_layer (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(128) NOT NULL,
    type            VARCHAR(32) NOT NULL,  -- vector, raster, wms, wmts, base
    
    -- 数据源
    dataset_id      BIGINT,  -- 关联数据集
    source_type     VARCHAR(32),  -- geoserver, tile, xyz
    source_url      VARCHAR(512),
    
    -- 样式
    style           JSONB,
    
    -- 显示属性
    visible         BOOLEAN DEFAULT TRUE,
    opacity         DECIMAL(5,2) DEFAULT 1.0,
    z_index         INTEGER DEFAULT 0,
    
    -- 最小/最大显示级别
    min_zoom        INTEGER DEFAULT 0,
    max_zoom        INTEGER DEFAULT 18,
    
    -- 权限控制
    public_access   BOOLEAN DEFAULT TRUE,
    required_role   VARCHAR(64),
    
    tenant_id       VARCHAR(64) NOT NULL DEFAULT 'default',
    create_time     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     TIMESTAMP,
    deleted         SMALLINT NOT NULL DEFAULT 0
);

-- 图层分组
CREATE TABLE map_layer_group (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(128) NOT NULL,
    parent_id       BIGINT,
    order_index     INTEGER DEFAULT 0,
    expanded        BOOLEAN DEFAULT TRUE,
    tenant_id       VARCHAR(64) NOT NULL DEFAULT 'default'
);
```

#### 4.2.6 切片缓存管理

```sql
-- GeoServer 图层映射
CREATE TABLE gs_layer (
    id              BIGSERIAL PRIMARY KEY,
    dataset_id      BIGINT NOT NULL,
    workspace       VARCHAR(128) NOT NULL,
    store_name      VARCHAR(128) NOT NULL,
    layer_name      VARCHAR(128) NOT NULL,
    
    -- 服务类型
    service_type    VARCHAR(16) NOT NULL,  -- wms, wmts, vector
    
    -- 切片配置（影像）
    tile_cache_dir  VARCHAR(512),
    tile_format     VARCHAR(16),
    
    -- 状态
    published       BOOLEAN DEFAULT FALSE,
    last_tiled      TIMESTAMP,
    tile_status     VARCHAR(32),  -- none, pending, complete, failed
    
    create_time     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     TIMESTAMP
);
```

#### 4.2.7 系统配置与日志

```sql
-- 系统配置（数据源配置）
CREATE TABLE sys_config (
    id              BIGSERIAL PRIMARY KEY,
    config_key      VARCHAR(128) NOT NULL UNIQUE,
    config_value    TEXT,
    value_type      VARCHAR(32) DEFAULT 'string',  -- string, json, number
    description     VARCHAR(256),
    tenant_id       VARCHAR(64) NOT NULL DEFAULT 'default',
    update_time     TIMESTAMP,
    updated_by      BIGINT
);

-- 预设配置键：
-- db.postgres.url, db.postgres.username, db.postgres.password
-- geoserver.url, geoserver.username, geoserver.password
-- minio.endpoint, minio.accessKey, minio.secretKey, minio.bucket
-- nginx.tileCachePath

-- 操作日志
CREATE TABLE sys_operation_log (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT,
    username        VARCHAR(64),
    module          VARCHAR(64),
    operation       VARCHAR(128),
    method          VARCHAR(256),
    params          JSONB,
    result          TEXT,
    ip_address      VARCHAR(64),
    user_agent      VARCHAR(512),
    create_time     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

#### 4.2.8 授权管理表

```sql
-- 授权信息表
CREATE TABLE license (
    id                  BIGSERIAL PRIMARY KEY,
    license_key         VARCHAR(512),  -- 授权码/激活码
    license_file        TEXT,  -- 完整授权文件内容 (.lic)
    
    -- 解析后的授权信息
    product             VARCHAR(64) NOT NULL,
    version             VARCHAR(32) NOT NULL,
    edition             VARCHAR(32),  -- professional, standard, basic
    issued_at           TIMESTAMP NOT NULL,
    expires_at          TIMESTAMP,  -- NULL 表示永久
    max_users           INTEGER NOT NULL,
    modules             JSONB,  -- ["vector", "raster", "wms"]
    features            JSONB,  -- 特性开关
    
    -- 机器绑定
    machine_fingerprint VARCHAR(128) NOT NULL,
    machine_info        JSONB,  -- 采集时的机器信息摘要
    
    -- 状态
    status              VARCHAR(32) NOT NULL DEFAULT 'active',  -- active, expired, revoked
    is_readonly_mode    BOOLEAN DEFAULT FALSE,  -- 只读模式
    
    -- 校验
    last_check_time     TIMESTAMP,
    check_interval_hours INTEGER DEFAULT 24,
    
    -- 激活信息
    activation_time     TIMESTAMP,
    activation_method   VARCHAR(16),  -- online, offline
    activation_device   VARCHAR(256),  -- 激活时的主机名/IP
    
    create_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time         TIMESTAMP
);

-- 机器指纹历史（用于审计）
CREATE TABLE license_machine (
    id                  BIGSERIAL PRIMARY KEY,
    license_id          BIGINT NOT NULL,
    
    -- 指纹详情
    fingerprint         VARCHAR(128) NOT NULL,
    mac_addresses       JSONB,  -- ["xx:xx:xx:xx:xx:xx"]
    cpu_serial          VARCHAR(128),
    motherboard_serial  VARCHAR(128),
    disk_serial         VARCHAR(128),
    
    -- 采集信息
    hostname            VARCHAR(256),
    os_version          VARCHAR(128),
    capture_time        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- 验证结果
    is_match            BOOLEAN DEFAULT TRUE,
    mismatch_reason     VARCHAR(256)
);

-- 授权变更日志
CREATE TABLE license_log (
    id                  BIGSERIAL PRIMARY KEY,
    license_id          BIGINT,
    event_type          VARCHAR(32) NOT NULL,  -- activate, renew, expire, revoke, check
    event_time          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    detail              JSONB,
    ip_address          VARCHAR(64),
    operator            VARCHAR(64)
);

#### 4.2.9 备份管理表

```sql
-- 备份记录表
CREATE TABLE backup_record (
    id                  BIGSERIAL PRIMARY KEY,
    backup_type         VARCHAR(32) NOT NULL,  -- database, config, full
    file_name           VARCHAR(256),
    file_path           VARCHAR(512),
    file_size           BIGINT,
    status              VARCHAR(32) NOT NULL,  -- pending, running, success, failed
    progress            INTEGER DEFAULT 0,
    error_message       TEXT,
    create_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    complete_time       TIMESTAMP,
    created_by          BIGINT
);

-- 备份配置表
CREATE TABLE backup_settings (
    id                  BIGSERIAL PRIMARY KEY,
    backup_type         VARCHAR(32) NOT NULL,  -- database, minio, all
    enabled             BOOLEAN DEFAULT TRUE,
    scheduleCron        VARCHAR(64),  -- Cron 表达式
    retention_days      INTEGER DEFAULT 30,
    target_path         VARCHAR(512),  -- 备份存储路径
    remote_storage      VARCHAR(128),  -- 远程存储（可选）
    create_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time         TIMESTAMP,
    updated_by          BIGINT
);

#### 4.2.10 元数据与质检表

```sql
-- 数据集元数据（ISO 19115）
CREATE TABLE dataset_metadata (
    id                  BIGSERIAL PRIMARY KEY,
    dataset_id          BIGINT NOT NULL UNIQUE,
    
    -- 标识信息
    title               VARCHAR(256),
    abstract            TEXT,
    date_create         TIMESTAMP,
    date_publish        TIMESTAMP,
    date_modified       TIMESTAMP,
    
    -- 创建者
    originator          VARCHAR(256),
    contact_name        VARCHAR(128),
    contact_email       VARCHAR(128),
    contact_phone       VARCHAR(32),
    
    -- 地理范围
    west_bound          DOUBLE PRECISION,
    east_bound          DOUBLE PRECISION,
    south_bound         DOUBLE PRECISION,
    north_bound         DOUBLE PRECISION,
    
    -- 时间范围
    time_begin          TIMESTAMP,
    time_end            TIMESTAMP,
    
    -- 分类
    topic_category      VARCHAR(64),  -- imagery, elevation, vector, etc.
    keywords            TEXT[],  -- 关键字数组
    
    -- 数据质量
    lineage             TEXT,  -- 数据来源描述
    DQ_reports          JSONB,  -- 质量报告
    
    -- 分发信息
    distribution_format VARCHAR(64),
    access_url          VARCHAR(512),
    
    -- 坐标系
    reference_system    VARCHAR(64),
    
    -- 多语言
    title_en            VARCHAR(256),
    abstract_en         TEXT,
    
    create_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time         TIMESTAMP,
    created_by          BIGINT
);

-- 质检结果
CREATE TABLE quality_result (
    id                  BIGSERIAL PRIMARY KEY,
    dataset_id          BIGINT NOT NULL,
    
    -- 质检状态
    status              VARCHAR(32) NOT NULL,  -- passed, warning, failed
    total_features      INTEGER,
    passed_features     INTEGER,
    warning_count       INTEGER,
    error_count         INTEGER,
    
    -- 错误明细
    errors              JSONB,  -- [{"type":"geometry","count":5,"ids":[1,2,3]}]
    
    create_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    check_duration_ms   INTEGER
);

-- 质检规则
CREATE TABLE quality_rule (
    id                  BIGSERIAL PRIMARY KEY,
    code                VARCHAR(64) NOT NULL UNIQUE,
    name                VARCHAR(128) NOT NULL,
    description         TEXT,
    rule_type           VARCHAR(32) NOT NULL,  -- geometry, attribute, coordinate
    rule_config         JSONB,  -- 规则参数
    enabled             BOOLEAN DEFAULT TRUE,
    severity            VARCHAR(16) NOT NULL,  -- error, warning
    create_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

#### 4.2.11 样式管理表

```sql
-- 图层样式
CREATE TABLE layer_style (
    id                  BIGSERIAL PRIMARY KEY,
    dataset_id          BIGINT NOT NULL,
    name                VARCHAR(128) NOT NULL,
    
    -- 样式定义
    style_type          VARCHAR(32) NOT NULL,  -- single, categorized, interval
    style_config        JSONB NOT NULL,  -- 样式配置
    sld_content         TEXT,  -- SLD 格式（可选）
    
    -- 状态
    is_default          BOOLEAN DEFAULT FALSE,
    
    create_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time         TIMESTAMP,
    created_by          BIGINT
);

-- 样式模板
CREATE TABLE style_template (
    id                  BIGSERIAL PRIMARY KEY,
    name                VARCHAR(128) NOT NULL,
    category            VARCHAR(64),  -- landuse, road, POI, etc.
    
    -- 模板定义
    template_config     JSONB NOT NULL,
    preview_image       VARCHAR(512),
    
    -- 元数据
    description         TEXT,
    tags                TEXT[],
    
    create_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by          BIGINT
);
```

#### 4.2.12 导出与分享表

```sql
-- 导出任务
CREATE TABLE data_export (
    id                  BIGSERIAL PRIMARY KEY,
    dataset_id          BIGINT,
    user_id             BIGINT NOT NULL,
    
    -- 导出参数
    export_type         VARCHAR(32) NOT NULL,  -- shapefile, geojson, kml, csv
    format_options      JSONB,  -- 编码、坐标系等选项
    spatial_filter      JSONB,  -- 空间范围筛选
    fields              TEXT[],  -- 导出字段
    
    -- 执行状态
    status              VARCHAR(32) NOT NULL,  -- pending, processing, success, failed
    file_path           VARCHAR(512),
    file_size           BIGINT,
    progress            INTEGER DEFAULT 0,
    error_message       TEXT,
    
    create_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    complete_time       TIMESTAMP
);

-- 分享链接
CREATE TABLE share_link (
    id                  BIGSERIAL PRIMARY KEY,
    token               VARCHAR(64) NOT NULL UNIQUE,
    dataset_id          BIGINT,
    data_scope          JSONB,  -- {"type":"all"} 或 {"type":"bbox","bounds":...}
    
    -- 权限配置
    password            VARCHAR(128),  -- 可选访问密码
    expires_at          TIMESTAMP,  -- 过期时间，NULL 为永久
    max_access_count    INTEGER,  -- 最大访问次数
    access_count        INTEGER DEFAULT 0,
    
    -- 权限
    can_view            BOOLEAN DEFAULT TRUE,
    can_download        BOOLEAN DEFAULT TRUE,
    
    -- 元数据
    created_by          BIGINT NOT NULL,
    create_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

## 5. API 设计思路

### 5.1 API 分组

| 前缀 | 模块 | 说明 |
|------|------|------|
| `/api/auth/**` | 认证 | 登录、登出、刷新 Token |
| `/api/license/**` | 授权管理 | 激活、状态、续期 |
| `/api/sys/**` | 系统管理 | 用户、角色、配置、日志、监控 |
| `/api/backup/**` | 备份管理 | 备份、恢复、配置 |
| `/api/dataset/**` | 数据集管理 | CRUD、导入、导出 |
| `/api/feature/**` | 要素管理 | 矢量数据增删改查 |
| `/api/raster/**` | 影像管理 | 元数据、校验 |
| `/api/metadata/**` | 元数据管理 | ISO 19115 元数据编辑 |
| `/api/quality/**` | 数据质检 | 规则配置、执行、报告 |
| `/api/style/**` | 样式管理 | 样式配置、模板 |
| `/api/export/**` | 数据导出 | 导出任务、下载 |
| `/api/share/**` | 分享管理 | 链接生成、访问 |
| `/api/gs/**` | GeoServer | 发布、切片 |
| `/api/map/**` | 地图配置 | 图层、分组 |
| `/api/layer/**` | 地图服务 | WMS/WMTS 访问 |

### 5.2 核心接口列表

#### 认证接口
```
POST   /api/auth/login          登录
POST   /api/auth/logout         登出
POST   /api/auth/refresh        刷新 Token
GET    /api/auth/me             获取当前用户信息
```

#### 授权管理
```
GET    /api/license/status      获取授权状态
GET    /api/license/info        获取授权详细信息
POST   /api/license/activate    在线激活（传入授权码）
POST   /api/license/activate/offline  离线激活（上传.lic文件）
GET    /api/license/machine/fingerprint  获取当前机器指纹
POST   /api/license/export      导出机器指纹（用于离线激活）
POST   /api/license/renew       续期（传入新授权码）
POST   /api/license/check       手动触发授权校验
GET    /api/license/log         授权变更日志
```

#### 用户与权限
```
GET    /api/sys/user            用户列表
POST   /api/sys/user            创建用户
PUT    /api/sys/user/{id}       更新用户
DELETE /api/sys/user/{id}       删除用户
GET    /api/sys/role            角色列表
POST   /api/sys/role            创建角色
PUT    /api/sys/role/{id}       更新角色
POST   /api/sys/role/{id}/permission  分配权限
POST   /api/sys/dataset/permission    分配数据集权限
```

#### 数据集管理
```
GET    /api/dataset             数据集列表
POST   /api/dataset             创建数据集
GET    /api/dataset/{id}        数据集详情
PUT    /api/dataset/{id}        更新数据集
DELETE /api/dataset/{id}        删除数据集

POST   /api/dataset/{id}/import 导入矢量数据（Shapefile/GeoJSON）
POST   /api/dataset/{id}/import-raster  导入影像数据
GET    /api/dataset/{id}/export 导出数据
GET    /api/dataset/{id}/versions  版本列表
POST   /api/dataset/{id}/version  创建版本快照
```

#### 要素管理
```
GET    /api/feature/{datasetId} 查询要素（支持空间/属性查询）
GET    /api/feature/{datasetId}/{id}  获取单个要素
POST   /api/feature/{datasetId} 创建要素
PUT    /api/feature/{datasetId}/{id}  更新要素（含版本检查）
DELETE /api/feature/{datasetId}/{id}  删除要素
POST   /api/feature/{datasetId}/batch  批量操作
```

#### 服务发布
```
POST   /api/gs/workspace        创建工作区
POST   /api/gs/store            创建数据存储
POST   /api/gs/publish          发布图层
DELETE /api/gs/layer/{id}       取消发布
GET    /api/gs/layer/{id}/status  图层状态
POST   /api/gs/tile/refresh     刷新切片缓存
POST   /api/gs/tile/generate    触发影像切片
```

#### 地图配置
```
GET    /api/map/layer           图层列表
POST   /api/map/layer           创建图层
PUT    /api/map/layer/{id}      更新图层
DELETE /api/map/layer/{id}      删除图层
GET    /api/map/group           分组列表
```

#### 元数据管理
```
GET    /api/metadata/{datasetId}    获取数据集元数据
PUT    /api/metadata/{datasetId}    更新元数据
GET    /api/metadata/{datasetId}/export  导出元数据（XML/JSON）
```

#### 数据质检
```
GET    /api/quality/rule         质检规则列表
POST   /api/quality/rule         创建质检规则
PUT    /api/quality/rule/{id}    更新质检规则
DELETE /api/quality/rule/{id}    删除质检规则
POST   /api/quality/check/{datasetId}  执行质检
GET    /api/quality/result/{datasetId}  获取质检结果
GET    /api/quality/history/{datasetId}  质检历史记录
```

#### 样式管理
```
GET    /api/style/layer/{datasetId}  获取图层样式
POST   /api/style/layer/{datasetId}  创建图层样式
PUT    /api/style/layer/{id}      更新样式
DELETE /api/style/layer/{id}      删除样式
POST   /api/style/layer/{id}/set-default  设为默认样式
POST   /api/style/export/sld/{id}  导出为 SLD
POST   /api/style/import/sld      从 SLD 导入样式

GET    /api/style/template       样式模板列表
POST   /api/style/template       创建模板
DELETE /api/style/template/{id}  删除模板
```

#### 数据导出
```
POST   /api/export/start         创建导出任务
GET    /api/export/{id}          获取导出任务状态
GET    /api/export/{id}/download 下载导出文件
GET    /api/export/list          导出历史记录
```

#### 分享管理
```
POST   /api/share/create         创建分享链接
GET    /api/share/{token}        获取分享信息（无需登录）
POST   /api/share/{token}/access 访问分享（验证密码）
GET    /api/share/{token}/data   获取分享数据
POST   /api/share/{token}/download  下载分享数据
DELETE /api/share/{id}           删除分享链接
GET    /api/share/my             我的分享列表
```

---

## 6. 前端路由与组件

### 6.1 菜单结构

```
├── 首页 / 仪表盘
│
├── 系统管理
│   ├── 用户管理 /sys/user
│   ├── 角色管理 /sys/role
│   ├── 数据集权限 /sys/dataset-permission
│   ├── 系统配置 /sys/config
│   └── 授权管理 /sys/license
│
├── 数据管理
│   ├── 矢量数据集 /data/vector
│   ├── 影像数据集 /data/raster
│   └── 数据导入 /data/import
│
├── 服务发布
│   ├── 图层发布 /service/publish
│   ├── 切片管理 /service/tile
│   └── 服务状态 /service/status
│
└── 地图展示
    ├── 地图查看 /map/view
    └── 地图配置 /map/config
```

### 6.2 核心页面组件

| 页面 | 路由 | 核心组件 |
|------|------|----------|
| 登录 | `/login` | LoginForm, LicenseStatusBanner |
| 激活向导 | `/activate` | ActivationWizard, FingerprintDisplay, LicenseUpload |
| 仪表盘 | `/` | Dashboard, StatsCards |
| 用户管理 | `/sys/user` | UserTable, UserDialog, RoleSelect |
| 数据集管理 | `/data/vector` | DatasetTable, DatasetDetail, ImportDialog |
| 数据导入 | `/data/import` | UploadZone, ParsePreview, FieldMapper |
| 地图查看 | `/map/view` | MapContainer, LayerPanel, ToolBar |
| 图层配置 | `/map/config` | LayerForm, StyleEditor |
| 服务发布 | `/service/publish` | LayerList, PublishDialog |
| 授权管理 | `/sys/license` | LicensePanel, LicenseInfo, RenewalDialog |

### 6.3 地图组件设计

```vue
MapContainer
├── MapView (OpenLayers)
│   ├── BaseLayer (OSM/天地图)
│   ├── TileLayer[] (叠加图层)
│   ├── VectorLayer[] (矢量图层)
│   └── HighlightLayer (选中高亮)
├── LayerPanel (右侧)
│   ├── LayerTree
│   └── LayerItem (透明度、显隐、顺序)
├── ToolBar (顶部/左侧)
│   ├── PanTool
│   ├── SelectTool (点选/框选)
│   ├── QueryTool
│   ├── MeasureTool
│   └── DrawTool (点/线/面)
└── Popup (要素信息弹窗)
    └── FeatureInfo
```

---

## 7. 数据流设计

### 7.1 矢量数据上传流程

```
┌─────────────────────────────────────────────────────────────────┐
│                    矢量数据导入流程                               │
└─────────────────────────────────────────────────────────────────┘

  1. 用户上传文件
     (.shp / .geojson / .kml)
            │
            ▼
  2. 前端分片上传到后端
     (大文件分片，避免超时)
            │
            ▼
  3. 后端解析文件
     - 读取几何和属性
     - 坐标转换（如需）
     - 投影变换
            │
            ▼
  4. 存入 PostGIS
     - 创建/更新要素表
     - 写入 geometry 字段
     - 写入 JSONB 属性
            │
            ▼
  5. 生成数据集记录
     - 记录名称、范围、坐标系、要素数
            │
            ▼
  6. 返回结果
     - 成功：数据集 ID
     - 失败：错误信息
```

### 7.2 影像数据上传流程

```
┌─────────────────────────────────────────────────────────────────┐
│                    影像数据导入流程                               │
└─────────────────────────────────────────────────────────────────┘

  1. 用户上传 GeoTIFF
     (可带 .ovr 概览文件)
            │
            ▼
  2. 存入 MinIO
     /raster/{tenant}/{uuid}/filename.tif
            │
            ▼
  3. 元数据提取
     - 读取 GeoTIFF 头信息
     - 提取坐标、分辨率、波段
            │
            ▼
  4. 校验
     - 坐标系是否支持
     - 波段数/类型是否合规
            │
            ├── 合规 ──▶ 5. 创建数据集记录
            │
            └── 不合规 ──▶ 提示用户
```

### 7.3 服务发布流程

```
┌─────────────────────────────────────────────────────────────────┐
│                    服务发布流程                                   │
└─────────────────────────────────────────────────────────────────┘

  1. 用户点击"发布"
     (选择数据集)
            │
  2. 后端调用 GeoServer REST API
     │
     ├── POST /workspaces/{workspace}
     ├── POST /workspaces/{workspace}/datastores
     └── POST /workspaces/{workspace}/datastores/{store}/featurety...
            │
            ▼
  3. GeoServer 创建图层
            │
            ▼
  4. 本地记录映射
     (gs_layer 表)
            │
            ▼
  5. 返回 WMTS/WMS 地址
     http://host/geoserver/workspace/layer/{layer}/wms
```

### 7.4 地图加载流程

```
┌─────────────────────────────────────────────────────────────────┐
│                    地图数据加载流程                               │
└─────────────────────────────────────────────────────────────────┘

  1. 前端请求图层列表
     GET /api/map/layer?visible=true
            │
            ▼
  2. 后端返回图层配置
     (包含数据源类型、URL、样式)
            │
            ▼
  3. 前端创建 OpenLayers 图层
     │
     ├── 矢量：VectorSource + VectorLayer
     ├── WMS：ImageWMS / TileWMS
     └── WMTS：WMTS
            │
            ▼
  4. 浏览器请求地图瓦片
     │
     ├── 矢量：/api/layer/{id}/features (GeoJSON)
     ├── WMS：GeoServer WMS GetMap
     └── WMTS：GeoServer WMTS / Nginx 静态切片
            │
            ▼
  5. 地图渲染显示
```

---

## 8. 安全设计

### 8.1 认证流程

```
┌─────────────────────────────────────────────────────────────────┐
│                      JWT 认证流程                                │
└─────────────────────────────────────────────────────────────────┘

  1. 用户登录
     POST /api/auth/login
     body: {username, password}
            │
            ▼
  2. 验证凭证
     (Spring Security UserDetailsService)
            │
            ▼
  3. 生成 JWT
     ├── access_token (短期，15分钟)
     └── refresh_token (长期，7天)
            │
            ▼
  4. 返回 Token
     {access_token, refresh_token, expires_in}
            │
            ▼
  5. 后续请求
     Header: Authorization: Bearer {access_token}
            │
            ▼
  6. JWT 验证过滤
     (JwtAuthenticationFilter)
     - 验证签名
     - 检查过期
     - 加载用户权限
     - 设置 SecurityContext
```

### 8.2 权限控制层次

```
请求 ──▶
        │
        ▼
   ┌─────────┐
   │ 接口权限 │  ← @PreAuthorize("hasAuthority('dataset:read')")
   └────┬────┘
        │
        ▼
   ┌─────────┐
   │ 数据权限 │  ← 查询时过滤 tenant_id + dataset_id
   └────┬────┘
        │
        ▼
   ┌─────────┐
   │ 行级权限 │  ← 当前版本暂未实现，后续迭代
   └─────────┘
```

### 8.3 数据权限过滤实现

```java
// MyBatis-Plus 租户/数据权限拦截器
@DataPermissionInterceptor
public class DataPermissionInterceptor implements InnerInterceptor {
    
    // 自动注入 tenant_id
    // 自动关联查询数据集权限
    
    @Override
    public void beforeQuery(Executor invoker, BoundSql boundSql) {
        // 1. 追加 tenant_id 过滤
        // 2. 追加数据集权限过滤（INNER JOIN dataset_permission）
    }
}
```

---

## 9. 待实现事项与风险

### 9.1 第一阶段（MVP）核心功能

| 序号 | 功能 | 优先级 | 预估工作量 |
|------|------|--------|------------|
| 1 | 用户认证（登录/登出/JWT） | P0 | 2d |
| 2 | 用户与角色管理 | P0 | 3d |
| 3 | 数据集 CRUD | P0 | 3d |
| 4 | 矢量数据上传解析 | P0 | 5d |
| 5 | 要素查询（属性+空间） | P0 | 4d |
| 6 | 影像数据上传存储 | P0 | 3d |
| 7 | GeoServer 集成发布 | P0 | 4d |
| 8 | 地图可视化基础 | P0 | 5d |
| 9 | 数据集权限分配 | P1 | 3d |
| 10 | 系统配置管理 | P1 | 2d |

### 9.2 后续迭代

- 行级数据权限
- 数据集版本管理
- 影像预切片 + Nginx 服务
- 在线图形编辑
- 操作日志审计
- 系统监控面板

### 9.3 潜在风险

| 风险 | 影响 | 缓解措施 |
|------|------|----------|
| GeoServer REST API 版本兼容性 | 发布失败 | 封装适配层，版本兼容测试 |
| 大文件上传超时 | 导入失败 | 分片上传、异步处理 |
| 空间查询性能 | 响应慢 | 空间索引优化、分页查询 |
| 同时编辑冲突 | 数据覆盖 | 乐观锁提示用户重试 |

---

## 10. 附录

### 10.1 配置示例

```yaml
# application.yml 关键配置

spring:
  datasource:
    postgis:
      url: jdbc:postgresql://localhost:5432/gisdb
      username: postgres
      password: ${DB_PASSWORD}
  
  data:
    redis:
      host: localhost
      port: 6379

geoserver:
  url: http://localhost:8080/geoserver
  username: admin
  password: ${GEOSERVER_PASSWORD}
  workspace: gisplatform

minio:
  endpoint: http://localhost:9000
  accessKey: ${MINIO_ACCESS_KEY}
  secretKey: ${MINIO_SECRET_KEY}
  bucket: gis-platform
  bucket-raster: gis-raster

nginx:
  tileCachePath: /var/cache/geowebcache
  
app:
  jwt:
    secret: ${JWT_SECRET}
    accessTokenExpire: 15m
    refreshTokenExpire: 7d
```

### 10.2 GeoServer REST API 常用端点

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /rest/workspaces.json | 列出工作区 |
| POST | /rest/workspaces | 创建工作区 |
| GET | /rest/workspaces/{ws}/datastores.json | 列出数据存储 |
| POST | /rest/workspaces/{ws}/datastores | 创建数据存储 |
| POST | /rest/workspaces/{ws}/datastores/{store}/featuretypes | 发布矢量图层 |
| POST | /rest/workspaces/{ws}/coveragestores | 创建影像存储 |
| POST | /rest/workspaces/{ws}/coveragestores/{store}/coverages | 发布影像图层 |
| POST | /rest/layers/{ws}:{layer}/refresh | 刷新图层缓存 |

---

*文档版本：v1.0*  
*创建时间：2025-05-22*

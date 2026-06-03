package com.gisplatform.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.gisplatform.common.handler.PGobjectJsonbTypeHandler;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("dataset")
@Schema(description = "数据集实体")
public class Dataset {

    @TableId(type = IdType.AUTO)
    @Schema(description = "数据集ID")
    private Long id;

    @Schema(description = "数据集名称")
    private String name;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "类型: vector/raster")
    private String type;

    @Schema(description = "几何类型: Point/LineString/Polygon")
    private String geometryType;

    @Schema(description = "坐标系")
    private String srs;

    @Schema(description = "存储类型: postgis/minio")
    private String storageType;

    @Schema(description = "PostGIS表名")
    private String tableName;

    @Schema(description = "MinIO对象路径")
    private String minioKey;

    @Schema(description = "空间范围")
    @TableField(typeHandler = PGobjectJsonbTypeHandler.class)
    private String extent;

    @Schema(description = "要素数量")
    private Integer featureCount;

    @Schema(description = "状态: draft/published")
    private String status;

    @Schema(description = "版本号")
    private Integer version;

    @Schema(description = "GeoServer工作区")
    private String workspace;

    @Schema(description = "GeoServer数据存储")
    private String storeName;

    @Schema(description = "GeoServer图层名")
    private String layerName;

    @Schema(description = "标签")
    private String tags;

    @Schema(description = "创建人ID")
    private Long createdBy;

    @Schema(description = "切片状态: pending/processing/completed/failed")
    private String tileStatus;

    @Schema(description = "切片进度 0-100")
    private Integer tileProgress;

    @Schema(description = "切片任务ID")
    private String tileJobId;

    @Schema(description = "WMS服务地址")
    private String wmsUrl;

    @Schema(description = "WMTS服务地址")
    private String wmtsUrl;

    @Schema(description = "GeoWebCache切片状态: idle/seeding/seeded")
    private String cacheSeedStatus;

    private String tenantId;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}

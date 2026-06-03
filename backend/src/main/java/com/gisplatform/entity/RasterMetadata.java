package com.gisplatform.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.gisplatform.common.handler.PGobjectJsonbTypeHandler;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("raster_metadata")
@Schema(description = "影像元数据")
public class RasterMetadata {

    @TableId(type = IdType.AUTO)
    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "关联数据集ID")
    private Long datasetId;

    @Schema(description = "原始文件名")
    private String fileName;

    @Schema(description = "文件大小（字节）")
    private Long fileSize;

    @Schema(description = "MinIO存储桶名称")
    private String minioBucket;

    @Schema(description = "MinIO对象路径")
    private String minioKey;

    @Schema(description = "影像宽度（像素）")
    private Integer width;

    @Schema(description = "影像高度（像素）")
    private Integer height;

    @Schema(description = "波段数")
    private Integer bands;

    @Schema(description = "像素类型：Float32、UInt16、Byte")
    private String pixelType;

    @Schema(description = "无效值")
    private Double noDataValue;

    @Schema(description = "坐标参考系统")
    private String crs;

    @TableField(typeHandler = PGobjectJsonbTypeHandler.class)
    @Schema(description = "GeoTIFF转换矩阵")
    private String transform;

    @TableField(typeHandler = PGobjectJsonbTypeHandler.class)
    @Schema(description = "金字塔信息")
    private String overviews;

    @Schema(description = "拍摄时间")
    private LocalDateTime captureTime;

    @Schema(description = "校验状态：pending、valid、invalid")
    private String validationStatus;

    @Schema(description = "校验消息")
    private String validationMessage;

    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}

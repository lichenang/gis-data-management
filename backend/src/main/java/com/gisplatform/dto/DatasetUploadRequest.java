package com.gisplatform.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
@Schema(description = "数据集文件上传请求")
public class DatasetUploadRequest {

    @Schema(description = "数据集名称", required = true)
    private String name;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "类型: vector/raster", required = true)
    private String type;

    @Schema(description = "空间数据文件（支持 Shapefile, GeoJSON, KML, GeoTIFF）")
    private MultipartFile file;

    @Schema(description = "坐标系 (默认 EPSG:4326)")
    private String srs;

    @Schema(description = "存储类型: postgis/minio")
    private String storageType;
}

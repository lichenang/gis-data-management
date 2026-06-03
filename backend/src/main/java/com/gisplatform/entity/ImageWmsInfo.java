package com.gisplatform.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "影像图层WMS信息")
public class ImageWmsInfo {

    @Schema(description = "GeoServer WMS 地址")
    private String wmsUrl;

    @Schema(description = "GeoServer 图层名称")
    private String layerName;

    @Schema(description = "坐标参考系统")
    private String crs;

    @Schema(description = "透明度（0-1）")
    private Double opacity;

    @Schema(description = "影像覆盖范围 [minX, minY, maxX, maxY]")
    private double[] extent;
}

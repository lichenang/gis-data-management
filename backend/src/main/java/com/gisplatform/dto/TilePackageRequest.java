package com.gisplatform.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "切片包下载请求")
public class TilePackageRequest {

    @Schema(description = "起始缩放级别，默认 0")
    private Integer zoomStart;

    @Schema(description = "结束缩放级别，默认 14")
    private Integer zoomStop;

    @Schema(description = "地理范围（WGS84），为空时使用数据集范围")
    private Bounds bounds;

    @Data
    @Schema(description = "地理范围")
    public static class Bounds {
        @Schema(description = "最小经度")
        private Double minX;
        @Schema(description = "最小纬度")
        private Double minY;
        @Schema(description = "最大经度")
        private Double maxX;
        @Schema(description = "最大纬度")
        private Double maxY;
    }
}

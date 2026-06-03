package com.gisplatform.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "影像下载URL响应")
public class ImageDownloadUrlResponse {

    @Schema(description = "MinIO 预签名下载地址")
    private String downloadUrl;

    @Schema(description = "原始文件名")
    private String fileName;

    @Schema(description = "文件大小（字节）")
    private Long fileSize;

    @Schema(description = "URL过期时间（秒）")
    private Integer expiresIn;
}

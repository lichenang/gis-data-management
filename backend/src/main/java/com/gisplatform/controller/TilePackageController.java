package com.gisplatform.controller;

import com.gisplatform.dto.TilePackageRequest;
import com.gisplatform.service.TilePackageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 切片包下载控制器。
 * <p>
 * 提供影像切片包的打包下载接口，将 GeoWebCache 缓存瓦片流式输出为 ZIP 文件。
 * </p>
 */
@RestController
@RequestMapping("/api/v1/images")
@Tag(name = "切片包下载", description = "影像切片包打包下载接口")
public class TilePackageController {

    @Autowired
    private TilePackageService tilePackageService;

    @PostMapping("/{id}/tile-package")
    @Operation(summary = "下载切片包", description = "将指定影像图层的 GWC 缓存切片打包为 ZIP 文件下载，支持指定缩放级别范围")
    public void downloadTilePackage(
            @Parameter(description = "影像数据集 ID") @PathVariable Long id,
            @RequestBody TilePackageRequest request,
            HttpServletResponse response) throws Exception {
        String filename = "tiles_" + id + "_z"
                + (request.getZoomStart() != null ? request.getZoomStart() : 0)
                + "-z"
                + (request.getZoomStop() != null ? request.getZoomStop() : 14) + ".zip";

        try (OutputStream os = response.getOutputStream()) {
            response.setContentType("application/zip");
            response.setHeader("Content-Disposition", "attachment; filename=\""
                    + URLEncoder.encode(filename, StandardCharsets.UTF_8) + "\"");

            tilePackageService.packageTiles(id, request, os);
            os.flush();
        }
    }
}

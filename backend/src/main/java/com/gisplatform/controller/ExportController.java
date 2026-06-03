package com.gisplatform.controller;

import com.gisplatform.common.exception.BusinessException;
import com.gisplatform.entity.Dataset;
import com.gisplatform.service.DatasetService;
import com.gisplatform.util.ExportUtil;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/v1/datasets")
@Tag(name = "数据导出", description = "数据集导出接口")
public class ExportController {

    @Autowired
    private DatasetService datasetService;

    @Autowired
    private MinioClient minioClient;

    @Qualifier("rasterBucket")
    @Autowired
    private String rasterBucket;

    @GetMapping("/{id}/export")
    @Operation(summary = "导出数据集", description = "支持矢量数据导出为 GeoJSON/Shapefile/KML，影像数据导出为 GeoTIFF")
    public void exportDataset(
            @PathVariable Long id,
            @RequestParam String format,
            HttpServletResponse response) {
        if (format == null || format.isEmpty()) {
            format = "geojson";
        }

        Dataset dataset = datasetService.getById(id);
        if (dataset == null || dataset.getDeleted() == 1) {
            writeErrorJson(response, 404, "数据集不存在");
            return;
        }

        if ("vector".equals(dataset.getType()) && (dataset.getTableName() == null || dataset.getTableName().isEmpty())) {
            writeErrorJson(response, 400, "该数据集未导入空间数据，无法导出");
            return;
        }

        try {
            if ("vector".equals(dataset.getType())) {
                if ("geojson".equals(format)) {
                    exportGeoJson(dataset, response);
                } else if ("shapefile".equals(format)) {
                    exportShapefile(dataset, response);
                } else if ("kml".equals(format)) {
                    exportKml(dataset, response);
                } else {
                    writeErrorJson(response, 400, "不支持的导出格式: " + format);
                }
            } else if ("raster".equals(dataset.getType())) {
                if ("geotiff".equals(format)) {
                    exportGeoTiff(dataset, response);
                } else {
                    writeErrorJson(response, 400, "不支持的导出格式: " + format);
                }
            } else {
                writeErrorJson(response, 400, "未知的数据集类型");
            }
        } catch (BusinessException e) {
            writeErrorJson(response, e.getCode(), e.getMessage());
        } catch (Exception e) {
            writeErrorJson(response, 500, "导出失败: " + e.getMessage());
        }
    }

    private void exportGeoJson(Dataset dataset, HttpServletResponse response) throws Exception {
        String geojson = datasetService.getDatasetAsGeoJSON(dataset.getId());
        String filename = ExportUtil.sanitizeFilename(dataset.getName()) + ".geojson";

        response.setContentType("application/geo+json");
        response.setHeader("Content-Disposition", "attachment; filename=\"" +
                URLEncoder.encode(filename, StandardCharsets.UTF_8) + "\"");
        response.getWriter().write(geojson);
    }

    private void exportKml(Dataset dataset, HttpServletResponse response) throws Exception {
        String kml = datasetService.getDatasetAsKML(dataset.getId());
        String filename = ExportUtil.sanitizeFilename(dataset.getName()) + ".kml";

        response.setContentType("application/vnd.google-earth.kml+xml");
        response.setHeader("Content-Disposition", "attachment; filename=\"" +
                URLEncoder.encode(filename, StandardCharsets.UTF_8) + "\"");
        response.getWriter().write(kml);
    }

    private void exportShapefile(Dataset dataset, HttpServletResponse response) throws Exception {
        String filename = ExportUtil.sanitizeFilename(dataset.getName()) + ".zip";

        response.setContentType("application/zip");
        response.setHeader("Content-Disposition", "attachment; filename=\"" +
                URLEncoder.encode(filename, StandardCharsets.UTF_8) + "\"");

        try (OutputStream os = response.getOutputStream()) {
            datasetService.exportShapefileAsZip(dataset.getId(), os);
        }
    }

    private void exportGeoTiff(Dataset dataset, HttpServletResponse response) throws Exception {
        String minioKey = dataset.getMinioKey();
        if (minioKey == null || minioKey.isEmpty()) {
            writeErrorJson(response, 400, "影像文件不存在");
            return;
        }

        String filename = ExportUtil.sanitizeFilename(dataset.getName()) + ".tiff";

        response.setContentType("image/tiff");
        response.setHeader("Content-Disposition", "attachment; filename=\"" +
                URLEncoder.encode(filename, StandardCharsets.UTF_8) + "\"");

        GetObjectArgs args = GetObjectArgs.builder()
                .bucket(rasterBucket)
                .object(minioKey)
                .build();

        try (InputStream is = minioClient.getObject(args);
             OutputStream os = response.getOutputStream()) {
            is.transferTo(os);
        }
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private void writeErrorJson(HttpServletResponse response, int code, String message) {
        try {
            response.setStatus(code);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":" + code + ",\"message\":\"" + escapeJson(message) + "\",\"data\":null}");
        } catch (IOException ignored) {
        }
    }
}

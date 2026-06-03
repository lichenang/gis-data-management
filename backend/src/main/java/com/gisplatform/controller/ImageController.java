package com.gisplatform.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gisplatform.common.R;
import com.gisplatform.dto.ImageDownloadUrlResponse;
import com.gisplatform.entity.Dataset;
import com.gisplatform.entity.ImageWmsInfo;
import com.gisplatform.service.ImageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/images")
@Tag(name = "影像管理", description = "影像数据上传与查询")
public class ImageController {

    @Autowired
    private ImageService imageService;

    /**
     * 上传影像文件。
     * <p>
     * 接收 GeoTIFF 文件，上传到 MinIO 并解析元数据，创建 Dataset 和 raster_metadata 记录。
     * </p>
     *
     * @param file        影像文件（必需，.tif 或 .tiff 格式）
     * @param name        影像名称（可选，为空时使用文件名）
     * @param description 描述信息（可选）
     * @return 上传成功返回 Dataset 对象，失败返回错误信息
     */
    @PostMapping("/upload")
    @Operation(summary = "上传影像文件", description = "接收 GeoTIFF 文件，上传到 MinIO 并解析元数据")
    public R<Dataset> upload(
            @Parameter(description = "影像文件") @RequestParam("file") MultipartFile file,
            @Parameter(description = "影像名称") @RequestParam(value = "name", required = false) String name,
            @Parameter(description = "描述") @RequestParam(value = "description", required = false) String description) {
        if (file.isEmpty()) {
            return R.fail("文件不能为空");
        }
        String fileName = file.getOriginalFilename();
        if (fileName == null || fileName.isEmpty()) {
            return R.fail("文件名无效");
        }
        String lowerFileName = fileName.toLowerCase();
        if (!lowerFileName.endsWith(".tif") && !lowerFileName.endsWith(".tiff")) {
            return R.fail("仅支持 GeoTIFF 格式 (.tif/.tiff)");
        }
        try {
            Dataset dataset = imageService.uploadImage(file, name, description);
            return R.ok(dataset);
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    /**
     * 获取影像列表。
     * <p>
     * 分页查询 type='raster' 的数据集列表，支持按名称模糊查询。
     * </p>
     *
     * @param page     页码（默认 1）
     * @param pageSize 每页记录数（默认 10）
     * @param name     名称查询条件（可选）
     * @return 分页结果
     */
    @GetMapping
    @Operation(summary = "获取影像列表", description = "分页查询影像数据集")
    public R<Page<Dataset>> list(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页记录数") @RequestParam(defaultValue = "10") int pageSize,
            @Parameter(description = "名称查询") @RequestParam(required = false) String name) {
        Page<Dataset> result = imageService.listImages(page, pageSize, name);
        return R.ok(result);
    }

    /**
     * 获取影像详情。
     *
     * @param id 数据集 ID
     * @return 影像 Dataset 对象
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取影像详情", description = "根据ID获取影像数据集详情")
    public R<Dataset> getById(@Parameter(description = "数据集ID") @PathVariable Long id) {
        Dataset dataset = imageService.getById(id);
        if (dataset == null || !("raster".equals(dataset.getType()))) {
            return R.fail("影像数据集不存在");
        }
        return R.ok(dataset);
    }

    @GetMapping("/{id}/download-url")
    @Operation(summary = "获取影像下载地址", description = "获取原始GeoTIFF文件的预签名下载URL（5分钟有效）")
    public R<ImageDownloadUrlResponse> getDownloadUrl(@Parameter(description = "数据集ID") @PathVariable Long id) {
        try {
            ImageDownloadUrlResponse response = imageService.getDownloadUrl(id);
            return R.ok(response);
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    @PostMapping("/{id}/publish")
    @Operation(summary = "发布影像数据集", description = "发布为GeoServer图层并触发切片")
    public R<Dataset> publish(@Parameter(description = "数据集ID") @PathVariable Long id) {
        try {
            Dataset dataset = imageService.publishImageDataset(id);
            return R.ok(dataset);
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    @DeleteMapping("/{id}/publish")
    @Operation(summary = "取消发布影像数据集", description = "取消发布并删除GeoServer图层")
    public R<Dataset> unpublish(@Parameter(description = "数据集ID") @PathVariable Long id) {
        try {
            Dataset dataset = imageService.unpublishImageDataset(id);
            return R.ok(dataset);
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除影像数据集", description = "根据ID删除影像数据集")
    public R<Void> delete(@Parameter(description = "数据集ID") @PathVariable Long id) {
        try {
            imageService.deleteImage(id);
            return R.ok();
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    @PostMapping("/{id}/retile")
    @Operation(summary = "重新切片", description = "手动触发重新切片")
    public R<Map<String, Object>> retile(@Parameter(description = "数据集ID") @PathVariable Long id) {
        try {
            String jobId = imageService.triggerRetile(id);
            Map<String, Object> result = new java.util.HashMap<>();
            result.put("tile_job_id", jobId);
            result.put("message", "切片任务已重新提交");
            return R.ok(result);
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    @GetMapping("/{id}/tiling-status")
    @Operation(summary = "获取切片状态", description = "查询影像切片进度")
    public R<Map<String, Object>> tilingStatus(@Parameter(description = "数据集ID") @PathVariable Long id) {
        try {
            Map<String, Object> status = imageService.getTilingStatus(id);
            return R.ok(status);
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    @GetMapping("/published")
    @Operation(summary = "获取已发布影像数据集列表", description = "查询所有已发布的影像数据集")
    public R<List<Dataset>> listPublished() {
        List<Dataset> list = imageService.listPublishedImages();
        return R.ok(list);
    }

    @GetMapping("/{id}/wms-url")
    @Operation(summary = "获取影像图层WMS地址", description = "获取影像图层的GeoServer WMS访问地址")
    public R<ImageWmsInfo> getWmsUrl(@Parameter(description = "数据集ID") @PathVariable Long id) {
        try {
            ImageWmsInfo wmsInfo = imageService.getImageWmsInfo(id);
            return R.ok(wmsInfo);
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    @GetMapping("/{id}/metadata")
    @Operation(summary = "获取影像元数据", description = "获取影像的元数据信息（JSON格式）")
    public R<Map<String, Object>> getMetadata(@Parameter(description = "数据集ID") @PathVariable Long id) {
        try {
            Map<String, Object> metadata = imageService.getMetadata(id);
            return R.ok(metadata);
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }
}

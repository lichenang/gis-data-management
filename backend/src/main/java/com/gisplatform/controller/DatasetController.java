package com.gisplatform.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gisplatform.common.R;
import com.gisplatform.dto.DatasetImportResult;
import com.gisplatform.dto.GisDataParseResult;
import com.gisplatform.entity.Dataset;
import com.gisplatform.service.DatasetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/datasets")
@Tag(name = "数据集管理", description = "数据集 CRUD 接口")
public class DatasetController {

    @Autowired
    private DatasetService datasetService;

    @GetMapping
    @Operation(summary = "获取数据集列表", description = "分页查询数据集列表")
    public R<Page<Dataset>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status) {
        Page<Dataset> result = datasetService.listDatasets(page, pageSize, name, type, status);
        return R.ok(result);
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取数据集详情", description = "根据ID获取数据集详情")
    @Parameter(name = "id", description = "数据集ID")
    public R<Dataset> getById(@PathVariable Long id) {
        Dataset dataset = datasetService.getDatasetById(id);
        return R.ok(dataset);
    }

    @PostMapping
    @Operation(summary = "创建数据集", description = "创建新的数据集")
    public R<Boolean> create(@RequestBody Dataset dataset) {
        boolean result = datasetService.createDataset(dataset);
        return R.ok(result);
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新数据集", description = "更新数据集信息")
    @Parameter(name = "id", description = "数据集ID")
    public R<Boolean> update(@PathVariable Long id, @RequestBody Dataset dataset) {
        dataset.setId(id);
        boolean result = datasetService.updateDataset(dataset);
        return R.ok(result);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除数据集", description = "删除数据集（逻辑删除）")
    @Parameter(name = "id", description = "数据集ID")
    public R<Boolean> delete(@PathVariable Long id) {
        boolean result = datasetService.deleteDataset(id);
        return R.ok(result);
    }

    @PostMapping("/parse")
    @Operation(summary = "解析空间数据文件", description = "解析上传的空间数据文件并返回元数据信息")
    public R<GisDataParseResult> parseFile(
            @Parameter(description = "空间数据文件") @RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return R.fail("文件不能为空");
        }
        String fileName = file.getOriginalFilename();
        if (fileName == null || fileName.isEmpty()) {
            return R.fail("文件名无效");
        }
        GisDataParseResult result = datasetService.parseUploadFile(file, fileName);
        if (result.isSuccess()) {
            return R.ok(result);
        } else {
            return R.fail(result.getMessage());
        }
    }

    @PostMapping("/import")
    @Operation(summary = "导入空间数据", description = "上传并导入空间数据文件到PostGIS数据库")
    public R<DatasetImportResult> importDataset(
            @Parameter(description = "空间数据文件") @RequestParam("file") MultipartFile file,
            @Parameter(description = "数据集名称") @RequestParam("name") String name,
            @Parameter(description = "描述") @RequestParam(value = "description", required = false) String description,
            @Parameter(description = "坐标系") @RequestParam(value = "srs", required = false, defaultValue = "EPSG:4326") String srs) {
        if (file.isEmpty()) {
            return R.fail("文件不能为空");
        }
        String fileName = file.getOriginalFilename();
        if (fileName == null || fileName.isEmpty()) {
            return R.fail("文件名无效");
        }
        if (name == null || name.isEmpty()) {
            return R.fail("数据集名称不能为空");
        }
        DatasetImportResult result = datasetService.importDataset(file, fileName, name, description, "vector", srs);
        if (result.isSuccess()) {
            return R.ok(result);
        } else {
            return R.fail(result.getMessage());
        }
    }

    @GetMapping("/published")
    @Operation(summary = "获取已发布数据集列表", description = "返回所有 status=published 的数据集")
    public R<List<Dataset>> listPublished() {
        List<Dataset> list = datasetService.listPublishedDatasets();
        return R.ok(list);
    }

    @GetMapping("/{id}/geojson")
    @Operation(summary = "获取数据集 GeoJSON", description = "将指定数据集导出为 GeoJSON 格式")
    public R<String> getGeoJSON(@PathVariable Long id) {
        try {
            String geojson = datasetService.getDatasetAsGeoJSON(id);
            return R.ok(geojson);
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    @PutMapping("/{id}/publish")
    @Operation(summary = "发布数据集", description = "将数据集状态设置为 published")
    public R<Dataset> publish(@PathVariable Long id) {
        try {
            Dataset dataset = datasetService.publishDataset(id);
            return R.ok(dataset);
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    @PutMapping("/{id}/unpublish")
    @Operation(summary = "取消发布", description = "将数据集状态设置为 draft")
    public R<Dataset> unpublish(@PathVariable Long id) {
        try {
            Dataset dataset = datasetService.unpublishDataset(id);
            return R.ok(dataset);
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }
}

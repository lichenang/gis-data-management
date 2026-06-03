package com.gisplatform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gisplatform.common.exception.BusinessException;
import com.gisplatform.config.GeoServerProperties;
import com.gisplatform.dto.ImageDownloadUrlResponse;
import com.gisplatform.entity.Dataset;
import com.gisplatform.entity.ImageWmsInfo;
import com.gisplatform.entity.RasterMetadata;
import com.gisplatform.mapper.DatasetMapper;
import com.gisplatform.mapper.RasterMetadataMapper;
import com.gisplatform.security.CurrentUserUtils;
import com.gisplatform.service.ImageService;
import com.gisplatform.service.geoserver.GeoServerCacheService;
import com.gisplatform.service.geoserver.GeoServerCoverageStoreService;
import com.gisplatform.service.geoserver.GeoServerLayerService;
import com.gisplatform.service.geoserver.GeoServerWorkspaceService;
import com.gisplatform.service.tiling.TileSeedService;
import com.gisplatform.util.CrsTransformUtil;
import com.gisplatform.util.GeoTiffParser;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.http.Method;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;


/**
 * 影像服务实现类。
 * <p>
 * 实现影像上传、MinIO 存储、元数据解析和数据库持久化。
 * </p>
 *
 * @author GIS Platform Team
 * @since 1.0.0
 */
@Slf4j
@Service
public class ImageServiceImpl extends ServiceImpl<DatasetMapper, Dataset> implements ImageService {

    @Autowired
    private RasterMetadataMapper rasterMetadataMapper;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Autowired
    private CurrentUserUtils currentUserUtils;

    @Autowired
    private MinioClient minioClient;

    @Qualifier("rasterBucket")
    @Autowired
    private String rasterBucket;

    @Autowired
    private GeoServerWorkspaceService workspaceService;

    @Autowired
    private GeoServerCoverageStoreService coverageStoreService;

    @Autowired
    private GeoServerLayerService layerService;

    @Autowired
    private GeoServerCacheService cacheService;

    @Autowired
    private TileSeedService tileSeedService;

    @Autowired
    private GeoServerProperties geoServerProperties;

    /**
     * 上传影像文件。
     * <p>
     * 处理流程：
     * 1. 检查并创建 MinIO 存储桶（如不存在）
     * 2. 生成 UUID 文件名并上传到 MinIO（路径：images/{uuid}.tif）
     * 3. 使用 GeoTools 解析 GeoTIFF 元数据
     * 4. 创建 Dataset 记录（type=raster, storageType=minio）
     * 5. 创建 raster_metadata 记录关联元数据
     * </p>
     *
     * @param file        影像文件（GeoTIFF 格式）
     * @param name        影像名称（可选，为空时使用文件名）
     * @param description 描述信息（可选）
     * @return 上传成功后的 Dataset 对象
     * @throws RuntimeException 上传或解析失败时抛出
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Dataset uploadImage(MultipartFile file, String name, String description) {
        try {
            if (minioClient != null) {
                boolean exists = minioClient.bucketExists(
                        BucketExistsArgs.builder().bucket(rasterBucket).build()
                );
                if (!exists) {
                    minioClient.makeBucket(
                            MakeBucketArgs.builder().bucket(rasterBucket).build()
                    );
                }
            }

            String uuid = UUID.randomUUID().toString();
            String objectName = "images/" + uuid + ".tif";

            if (minioClient != null) {
                minioClient.putObject(
                        PutObjectArgs.builder()
                                .bucket(rasterBucket)
                                .object(objectName)
                                .stream(file.getInputStream(), file.getSize(), -1)
                                .contentType("image/tiff")
                                .build()
                );
            }

            RasterMetadata rasterMetadata = GeoTiffParser.parse(file);
            rasterMetadata.setMinioBucket(rasterBucket);
            rasterMetadata.setMinioKey(objectName);
            rasterMetadata.setValidationStatus("valid");

            Dataset dataset = new Dataset();
            if (name != null && !name.isEmpty()) {
                dataset.setName(name);
            } else {
                dataset.setName(file.getOriginalFilename());
            }
            if (description != null) {
                dataset.setDescription(description);
            }
            dataset.setType("raster");
            dataset.setStorageType("minio");
            dataset.setMinioKey(objectName);
            dataset.setSrs(rasterMetadata.getCrs());
            dataset.setStatus("draft");
            dataset.setVersion(1);
            dataset.setTenantId("default");
            dataset.setCreatedBy(currentUserUtils.getCurrentUserId());
            dataset.setCreateTime(LocalDateTime.now());

            this.save(dataset);

            rasterMetadata.setDatasetId(dataset.getId());
            rasterMetadata.setCreateTime(LocalDateTime.now());

            if (rasterMetadata.getTransform() != null && !(rasterMetadata.getTransform() instanceof String)) {
                try {
                    rasterMetadata.setTransform(OBJECT_MAPPER.writeValueAsString(rasterMetadata.getTransform()));
                } catch (JsonProcessingException e) {
                    throw new RuntimeException("序列化 transform 失败", e);
                }
            }
            if (rasterMetadata.getOverviews() != null && !(rasterMetadata.getOverviews() instanceof String)) {
                try {
                    rasterMetadata.setOverviews(OBJECT_MAPPER.writeValueAsString(rasterMetadata.getOverviews()));
                } catch (JsonProcessingException e) {
                    throw new RuntimeException("序列化 overviews 失败", e);
                }
            }

            rasterMetadataMapper.insert(rasterMetadata);

            return dataset;

        } catch (Exception e) {
            throw new RuntimeException("影像上传失败: " + e.getMessage(), e);
        }
    }

    /**
     * 分页查询影像数据集列表。
     * <p>
     * 查询条件：type='raster' 且 deleted=0，按创建时间倒序排列。
     * </p>
     *
     * @param page     当前页码（从 1 开始）
     * @param pageSize 每页记录数
     * @param name     名称模糊查询条件（可选）
     * @return 分页结果
     */
    @Override
    public Page<Dataset> listImages(int page, int pageSize, String name) {
        Page<Dataset> pageObj = new Page<>(page, pageSize);
        LambdaQueryWrapper<Dataset> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Dataset::getDeleted, 0);
        wrapper.eq(Dataset::getType, "raster");

        if (name != null && !name.isEmpty()) {
            wrapper.like(Dataset::getName, name);
        }

        wrapper.orderByDesc(Dataset::getCreateTime);
        return this.page(pageObj, wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Dataset publishImageDataset(Long id) {
        Dataset dataset = this.getById(id);
        if (dataset == null || dataset.getDeleted() == 1) {
            throw new RuntimeException("数据集不存在");
        }

        if (!"raster".equals(dataset.getType())) {
            throw new RuntimeException("只有影像数据集可以发布");
        }

        if ("published".equals(dataset.getStatus())) {
            throw new RuntimeException("数据集已发布");
        }

        // 获取 raster_metadata 中的 MinIO 信息
        LambdaQueryWrapper<RasterMetadata> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(RasterMetadata::getDatasetId, id);
        RasterMetadata rasterMetadata = rasterMetadataMapper.selectOne(queryWrapper);
        if (rasterMetadata == null) {
            throw new RuntimeException("raster_metadata 不存在");
        }

        String minioBucket = rasterMetadata.getMinioBucket();
        if (minioBucket == null || minioBucket.isEmpty()) {
            minioBucket = rasterBucket;
        }
        String minioKey = rasterMetadata.getMinioKey();
        if (minioKey == null || minioKey.isEmpty()) {
            minioKey = dataset.getMinioKey();
        }

        String workspace = geoServerProperties.getWorkspace();
        String storeName = "raster_" + id;
        String layerName = "raster_" + id;

        // Create workspace if not exists
        workspaceService.createWorkspace(workspace);

        // Download file from MinIO and upload to GeoServer
        byte[] fileData;
        try (InputStream stream = minioClient.getObject(
                GetObjectArgs.builder()
                    .bucket(minioBucket)
                    .object(minioKey)
                    .build())) {
            fileData = stream.readAllBytes();
        } catch (Exception e) {
            throw new RuntimeException("Failed to download file from MinIO: " +
                minioBucket + "/" + minioKey, e);
        }
        log.info("Downloaded " + fileData.length + " bytes from MinIO: " + minioBucket + "/" + minioKey);

        // Delete existing coverage store first to avoid "unable to remove existing" error
        try {
            coverageStoreService.deleteStore(workspace, storeName);
            log.info("Deleted existing coverage store: {}", storeName);
            Thread.sleep(2000);
        } catch (Exception e) {
            log.info("No existing coverage store to delete: {}", storeName);
        }

        coverageStoreService.createImageMosaicStore(workspace, storeName, fileData);

        // Set URL fields
        dataset.setWmsUrl(layerService.getWmsUrl(workspace, layerName));
        dataset.setWmtsUrl(layerService.getWmtsUrl(workspace, layerName));

        // 从 raster_metadata.transform 提取 extent
        String transformJson = rasterMetadata.getTransform();
        if (transformJson != null && !transformJson.isEmpty()) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                Map<String, Object> transform = mapper.readValue(transformJson, Map.class);
                Double minX = ((Number) transform.get("minX")).doubleValue();
                Double minY = ((Number) transform.get("minY")).doubleValue();
                Double maxX = ((Number) transform.get("maxX")).doubleValue();
                Double maxY = ((Number) transform.get("maxY")).doubleValue();
                String extentJson = String.format("{\"minX\":%s,\"minY\":%s,\"maxX\":%s,\"maxY\":%s}",
                        minX, minY, maxX, maxY);
                dataset.setExtent(extentJson);
            } catch (Exception e) {
                log.warn("Failed to parse transform for extent: {}", e.getMessage());
            }
        }

        // Update status
        dataset.setStatus("published");
        dataset.setTileStatus("pending");
        dataset.setCacheSeedStatus("idle");
        dataset.setUpdateTime(LocalDateTime.now());

        this.updateById(dataset);

        // Trigger async tiling
        tileSeedService.triggerSeed(id, geoServerProperties.getTilingMinZoom(), geoServerProperties.getTilingMaxZoom());

        return dataset;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Dataset unpublishImageDataset(Long id) {
        Dataset dataset = this.getById(id);
        if (dataset == null || dataset.getDeleted() == 1) {
            throw new RuntimeException("数据集不存在");
        }

        String workspace = geoServerProperties.getWorkspace();
        String layerName = "raster_" + id;
        String storeName = layerName;

        // Step 1: Delete GWC cache layer (ignore 404)
        cacheService.deleteLayer(workspace, layerName);

        // Step 2: Delete coverage store with all layers
        layerService.deleteCoverageStore(workspace, storeName);

        // Step 3: Reset status
        dataset.setStatus("draft");
        dataset.setTileStatus("pending");
        dataset.setTileProgress(0);
        dataset.setCacheSeedStatus("idle");
        dataset.setUpdateTime(LocalDateTime.now());

        this.updateById(dataset);

        return dataset;
    }

    @Override
    public String triggerRetile(Long id) {
        Dataset dataset = this.getById(id);
        if (dataset == null || dataset.getDeleted() == 1) {
            throw new RuntimeException("数据集不存在");
        }

        if (!"raster".equals(dataset.getType())) {
            throw new RuntimeException("只有影像数据集可以切片");
        }

        return tileSeedService.triggerRetile(id);
    }

    @Override
    public Map<String, Object> getTilingStatus(Long id) {
        Dataset dataset = this.getById(id);
        if (dataset == null || dataset.getDeleted() == 1) {
            throw new RuntimeException("数据集不存在");
        }

        Map<String, Object> status = new HashMap<>();
        status.put("status", dataset.getTileStatus());
        status.put("progress", dataset.getTileProgress() != null ? dataset.getTileProgress() : 0);
        status.put("cache_seed_status", dataset.getCacheSeedStatus());
        status.put("wms_url", dataset.getWmsUrl() != null ? dataset.getWmsUrl() : "");
        status.put("wmts_url", dataset.getWmtsUrl() != null ? dataset.getWmtsUrl() : "");
        status.put("tile_job_id", dataset.getTileJobId() != null ? dataset.getTileJobId() : "");

        return status;
    }

    @Override
    public List<Dataset> listPublishedImages() {
        return this.list(new LambdaQueryWrapper<Dataset>()
                .eq(Dataset::getType, "raster")
                .eq(Dataset::getStatus, "published")
                .eq(Dataset::getDeleted, 0));
    }

    @Override
    public ImageWmsInfo getImageWmsInfo(Long id) {
        Dataset dataset = this.getById(id);
        if (dataset == null || dataset.getDeleted() == 1) {
            throw new RuntimeException("影像数据集不存在");
        }

        if (dataset.getWmsUrl() == null || dataset.getWmsUrl().isEmpty()) {
            throw new RuntimeException("影像数据集未发布到 GeoServer");
        }

        ImageWmsInfo info = new ImageWmsInfo();
        info.setLayerName("raster_" + id);
        info.setOpacity(0.8);
        info.setWmsUrl(dataset.getWmsUrl());

        // 解析 extent 并转换到 EPSG:4326
        if (dataset.getExtent() != null && !dataset.getExtent().isEmpty()) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                Map<String, Object> extentMap = mapper.readValue(dataset.getExtent(), Map.class);
//                double[] extent = new double[4];
//                extent[0] = ((Number) extentMap.get("minX")).doubleValue();
//                extent[1] = ((Number) extentMap.get("minY")).doubleValue();
//                extent[2] = ((Number) extentMap.get("maxX")).doubleValue();
//                extent[3] = ((Number) extentMap.get("maxY")).doubleValue();
                double[] extent = new double[4];
                extent[0] = ((Number) extentMap.get("minX")).doubleValue();  // 正确：minX → lon
                extent[1] = ((Number) extentMap.get("minY")).doubleValue();  // 正确：minY → lat
                extent[2] = ((Number) extentMap.get("maxX")).doubleValue();  // 正确：maxX → lon
                extent[3] = ((Number) extentMap.get("maxY")).doubleValue();  // 正确：maxY → lat
                // 获取源 CRS 并转换 extent 到 EPSG:4326
                String sourceCrs = dataset.getSrs();
                log.info("Image {} extent transformation: sourceCRS={}, extent=[{}, {}, {}, {}]",
                        id, sourceCrs, extent[0], extent[1], extent[2], extent[3]);

                double[] transformedExtent = CrsTransformUtil.transformExtentToWgs84(extent, sourceCrs);

                if (transformedExtent != null) {
                    info.setExtent(transformedExtent);
                    info.setCrs("EPSG:4326");
                    log.info("Successfully transformed extent to EPSG:4326: [{}, {}, {}, {}]",
                            transformedExtent[0], transformedExtent[1], transformedExtent[2], transformedExtent[3]);
                } else {
                    // GeoTools转换失败，使用备用数学换算
                    double[] fallbackExtent = transformExtentSimple(extent);
                    info.setExtent(fallbackExtent);
                    info.setCrs("EPSG:4326");
                    log.warn("Using fallback extent transformation for dataset {}. Fallsback: [{}, {}, {}, {}]",
                            id, fallbackExtent[0], fallbackExtent[1], fallbackExtent[2], fallbackExtent[3]);
                }

            } catch (Exception e) {
                log.warn("Failed to parse extent for dataset {}: {}", id, e.getMessage());
            }
        }

        return info;
    }

    @Override
    public ImageDownloadUrlResponse getDownloadUrl(Long id) {
        Dataset dataset = this.getById(id);
        if (dataset == null || dataset.getDeleted() == 1) {
            throw new RuntimeException("影像数据集不存在");
        }

        if (!"raster".equals(dataset.getType())) {
            throw new RuntimeException("仅支持影像数据集");
        }

        LambdaQueryWrapper<RasterMetadata> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(RasterMetadata::getDatasetId, id);
        RasterMetadata rasterMetadata = rasterMetadataMapper.selectOne(queryWrapper);
        if (rasterMetadata == null) {
            throw new RuntimeException("影像元数据不存在");
        }

        String minioBucket = rasterMetadata.getMinioBucket();
        if (minioBucket == null || minioBucket.isEmpty()) {
            minioBucket = rasterBucket;
        }
        String minioKey = rasterMetadata.getMinioKey();
        if (minioKey == null || minioKey.isEmpty()) {
            minioKey = dataset.getMinioKey();
        }
        if (minioKey == null || minioKey.isEmpty()) {
            throw new RuntimeException("影像文件不存在");
        }

        try {
            String downloadUrl = minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(minioBucket)
                            .object(minioKey)
                            .expiry(300)
                            .build());

            ImageDownloadUrlResponse response = new ImageDownloadUrlResponse();
            response.setDownloadUrl(downloadUrl);
            response.setFileName(rasterMetadata.getFileName());
            response.setFileSize(rasterMetadata.getFileSize());
            response.setExpiresIn(300);
            return response;
        } catch (Exception e) {
            throw new RuntimeException("生成下载地址失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void deleteImage(Long id) {
        Dataset dataset = this.getById(id);
        if (dataset == null || dataset.getDeleted() == 1) {
            throw new BusinessException("数据集不存在或已被删除");
        }
        // 使用 LambdaUpdateWrapper 显式设置 deleted=1
        LambdaUpdateWrapper<Dataset> wrapper = new LambdaUpdateWrapper<>();
        wrapper.set(Dataset::getDeleted, 1)
                .set(Dataset::getUpdateTime, LocalDateTime.now())
                .eq(Dataset::getId, id);
        this.update(wrapper);
    }
//    @Override
//    public void deleteImage(Long id) {
//        Dataset dataset = this.getById(id);
//        if (dataset == null || dataset.getDeleted() == 1) {
//            throw new RuntimeException("数据集不存在");
//        }
//        dataset.setDeleted(1);
//        dataset.setUpdateTime(LocalDateTime.now());
//        this.updateById(dataset);
//    }

    @Override
    public Map<String, Object> getMetadata(Long id) {
        Dataset dataset = this.getById(id);
        if (dataset == null || dataset.getDeleted() == 1 || !"raster".equals(dataset.getType())) {
            throw new RuntimeException("影像数据集不存在");
        }

        LambdaQueryWrapper<RasterMetadata> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(RasterMetadata::getDatasetId, id);
        RasterMetadata rasterMetadata = rasterMetadataMapper.selectOne(queryWrapper);

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("datasetId", dataset.getId());
        metadata.put("name", dataset.getName());
        metadata.put("type", dataset.getType());
        metadata.put("status", dataset.getStatus());
        metadata.put("crs", dataset.getSrs());
        metadata.put("createTime", dataset.getCreateTime());
        metadata.put("updateTime", dataset.getUpdateTime());

        if (rasterMetadata != null) {
            metadata.put("fileName", rasterMetadata.getFileName());
            metadata.put("fileSize", rasterMetadata.getFileSize());
            metadata.put("width", rasterMetadata.getWidth());
            metadata.put("height", rasterMetadata.getHeight());
            metadata.put("bands", rasterMetadata.getBands());
            metadata.put("pixelType", rasterMetadata.getPixelType());
            metadata.put("noDataValue", rasterMetadata.getNoDataValue());
            metadata.put("crs", rasterMetadata.getCrs());

            String transformJson = rasterMetadata.getTransform();
            if (transformJson != null && !transformJson.isEmpty()) {
                try {
                    Map<String, Object> transform = OBJECT_MAPPER.readValue(transformJson, Map.class);
                    Map<String, Object> extent = new HashMap<>();
                    extent.put("minX", transform.get("minX"));
                    extent.put("minY", transform.get("minY"));
                    extent.put("maxX", transform.get("maxX"));
                    extent.put("maxY", transform.get("maxY"));
                    metadata.put("extent", extent);
                } catch (JsonProcessingException e) {
                    log.warn("Failed to parse transform JSON: {}", e.getMessage());
                }
            }
        }

        return metadata;
    }

    private double[] transformExtentSimple(double[] extent) {
        double R = 6378137.0;
        double PI = Math.PI;
        
        double minX_rad = extent[0] / R;
        double maxX_rad = extent[2] / R;
        double minY_rad = Math.atan(Math.exp(extent[1] / R));
        double maxY_rad = Math.atan(Math.exp(extent[3] / R));
        
        double[] result = new double[4];
        result[0] = Math.toDegrees(minX_rad);
        result[1] = Math.toDegrees(2 * minY_rad - PI / 2);
        result[2] = Math.toDegrees(maxX_rad);
        result[3] = Math.toDegrees(2 * maxY_rad - PI / 2);
        
        return result;
    }
}

package com.gisplatform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gisplatform.config.GeoServerProperties;
import com.gisplatform.config.TilePackageProperties;
import com.gisplatform.dto.TilePackageRequest;
import com.gisplatform.entity.Dataset;
import com.gisplatform.entity.RasterMetadata;
import com.gisplatform.mapper.RasterMetadataMapper;
import com.gisplatform.service.DatasetService;
import com.gisplatform.service.TilePackageService;
import com.gisplatform.util.CrsTransformUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 切片包下载服务实现。
 * <p>
 * 通过直接读取 GeoWebCache 磁盘缓存目录，将切片文件流式打包为 ZIP。
 * </p>
 */
@Slf4j
@Service
public class TilePackageServiceImpl implements TilePackageService {

    @Autowired
    private DatasetService datasetService;

    @Autowired
    private GeoServerProperties geoServerProperties;

    @Autowired
    private TilePackageProperties tilePackageProperties;

    @Autowired
    private RasterMetadataMapper rasterMetadataMapper;

    private static final Map<String, GwcDirectoryFormat> formatCache = new ConcurrentHashMap<>();

    private enum GwcDirectoryFormat {
        STANDARD,
        LAYERED_EPSG_4326,
        LAYERED_EPSG_3857,
        UNKNOWN
    }

    private static final Pattern LAYERED_PATTERN = Pattern.compile("^(EPSG_\\d+)_(\\d+)$");

    private GwcDirectoryFormat detectGwcDirectoryFormat(File gwcDir) {
        String key = gwcDir.getAbsolutePath();
        if (formatCache.containsKey(key)) {
            return formatCache.get(key);
        }

        File[] subDirs = gwcDir.listFiles(File::isDirectory);
        if (subDirs == null || subDirs.length == 0) {
            formatCache.put(key, GwcDirectoryFormat.STANDARD);
            return GwcDirectoryFormat.STANDARD;
        }

        for (File subDir : subDirs) {
            String name = subDir.getName();
            Matcher matcher = LAYERED_PATTERN.matcher(name);
            if (matcher.matches()) {
                String epsgCode = matcher.group(1);
                if ("EPSG_4326".equals(epsgCode)) {
                    log.debug("Detected LAYERED_EPSG_4326 format in {}", gwcDir.getAbsolutePath());
                    formatCache.put(key, GwcDirectoryFormat.LAYERED_EPSG_4326);
                    return GwcDirectoryFormat.LAYERED_EPSG_4326;
                } else if ("EPSG_3857".equals(epsgCode)) {
                    log.debug("Detected LAYERED_EPSG_3857 format in {}", gwcDir.getAbsolutePath());
                    formatCache.put(key, GwcDirectoryFormat.LAYERED_EPSG_3857);
                    return GwcDirectoryFormat.LAYERED_EPSG_3857;
                }
            }
            try {
                Integer.parseInt(name);
                log.debug("Detected STANDARD format (numeric zoom dir) in {}", gwcDir.getAbsolutePath());
                formatCache.put(key, GwcDirectoryFormat.STANDARD);
                return GwcDirectoryFormat.STANDARD;
            } catch (NumberFormatException e) {
            }
        }

        log.debug("Detected UNKNOWN format, falling back to STANDARD: {}", gwcDir.getAbsolutePath());
        formatCache.put(key, GwcDirectoryFormat.STANDARD);
        return GwcDirectoryFormat.STANDARD;
    }

    @Override
    public void packageTiles(Long datasetId, TilePackageRequest request, OutputStream outputStream) {
        Dataset dataset = datasetService.getById(datasetId);
        if (dataset == null || dataset.getDeleted() == 1) {
            throw new RuntimeException("影像数据集不存在");
        }
        if (!"raster".equals(dataset.getType())) {
            throw new RuntimeException("只有影像数据集支持切片包下载");
        }
        if (!"published".equals(dataset.getStatus())) {
            throw new RuntimeException("影像未发布，尚无切片缓存");
        }
        if (!"seeded".equals(dataset.getCacheSeedStatus())) {
            String progress = dataset.getTileProgress() != null ? dataset.getTileProgress() + "%" : "未知";
            throw new RuntimeException("切片尚未完成，当前进度 " + progress);
        }

        String dataDir = geoServerProperties.getDataDir();
        if (dataDir == null || dataDir.isEmpty()) {
            throw new RuntimeException("GeoServer data_dir 未配置，请在 application.yml 或环境变量中设置 GEOSERVER_DATA_DIR");
        }

        String workspace = geoServerProperties.getWorkspace();
        if (workspace == null || workspace.isEmpty()) {
            workspace = "gisplatform";
        }
        String layerName = "raster_" + datasetId;

        int zoomStart = request.getZoomStart() != null ? request.getZoomStart() : 0;
        int zoomStop = request.getZoomStop() != null ? request.getZoomStop() : tilePackageProperties.getDefaultZoomStop();

        if (zoomStart < 0 || zoomStop > 18 || zoomStart > zoomStop) {
            throw new RuntimeException("无效的缩放级别范围（zoomStart=" + zoomStart + ", zoomStop=" + zoomStop + "）");
        }

        Bounds bounds = parseBounds(request, dataset);

        long estimatedTiles = estimateTileCount(zoomStart, zoomStop, bounds);
        if (estimatedTiles > tilePackageProperties.getMaxTiles()) {
            throw new RuntimeException("瓦片数量超出限制（预估 " + estimatedTiles + "，上限 " + tilePackageProperties.getMaxTiles() + "），请缩小缩放级别范围");
        }

        String gwcDirPath = dataDir.replace('\\', '/')
                + "/gwc/" + workspace + "_" + layerName;

        File gwcDir = new File(gwcDirPath);
        if (!gwcDir.exists() || !gwcDir.isDirectory()) {
            throw new RuntimeException("GeoServer data_dir 目录不存在: " + gwcDirPath + "，请检查配置路径是否正确");
        }

        List<File> tileFiles = enumerateTileFiles(gwcDir, zoomStart, zoomStop, bounds);
        if (tileFiles.isEmpty()) {
            throw new RuntimeException("该影像尚未生成切片缓存，请先触发切片种子任务");
        }

        try (ZipOutputStream zos = new ZipOutputStream(outputStream)) {
            for (File tileFile : tileFiles) {
                String entryName = gwcDir.toPath().relativize(tileFile.toPath()).toString().replace('\\', '/');
                zos.putNextEntry(new ZipEntry(entryName));
                try (FileInputStream fis = new FileInputStream(tileFile)) {
                    fis.transferTo(zos);
                }
                zos.closeEntry();
            }
            log.info("Tile package created for dataset {}: {} tiles written, zoom {}-{}",
                    datasetId, tileFiles.size(), zoomStart, zoomStop);
        } catch (IOException e) {
            log.error("Failed to create tile package for dataset {}", datasetId, e);
            throw new RuntimeException("切片包打包失败: " + e.getMessage());
        }
    }

    /**
     * 估算指定 zoom 范围内的瓦片总数。
     */
    private long estimateTileCount(int zoomStart, int zoomStop, Bounds bounds) {
        long total = 0;
        for (int z = zoomStart; z <= zoomStop; z++) {
            TileRange range = getTileRange(z, bounds);
            total += (long) (range.xMax - range.xMin + 1) * (range.yMax - range.yMin + 1);
        }
        return total;
    }

    /**
     * 枚举指定 zoom 范围内所有匹配的瓦片文件。
     */
    private List<File> enumerateTileFiles(File gwcDir, int zoomStart, int zoomStop, Bounds bounds) {
        long startTime = System.nanoTime();

        boolean isGlobalBounds = bounds.minX == -180 && bounds.minY == -90
                && bounds.maxX == 180 && bounds.maxY == 90;

        log.debug("===== enumerateTileFiles START =====");
        log.debug("gwcDir: {}", gwcDir.getAbsolutePath());
        log.debug("zoom range: {}-{}", zoomStart, zoomStop);
        log.debug("bounds: minX={}, minY={}, maxX={}, maxY={}, isGlobal={}",
                bounds.minX, bounds.minY, bounds.maxX, bounds.maxY, isGlobalBounds);

        if (!gwcDir.exists() || !gwcDir.isDirectory()) {
            log.warn("GWC directory does not exist or is not a directory: {}", gwcDir.getAbsolutePath());
            return new ArrayList<>();
        }

        GwcDirectoryFormat format = detectGwcDirectoryFormat(gwcDir);
        log.debug("Detected GWC directory format: {}", format);

        List<File> files = new ArrayList<>();

        if (isGlobalBounds || format != GwcDirectoryFormat.STANDARD) {
            log.debug("Using fallback mode: scanning all .png files in GWC directory");
            files = scanAllPngFiles(gwcDir, zoomStart, zoomStop, format);
        } else {
            for (int z = zoomStart; z <= zoomStop; z++) {
                TileRange range = getTileRange(z, bounds);
                log.debug("zoom {}: xRange=[{}, {}], yRange=[{}, {}]", z, range.xMin, range.xMax, range.yMin, range.yMax);
                for (int x = range.xMin; x <= range.xMax; x++) {
                    for (int y = range.yMin; y <= range.yMax; y++) {
                        File tileFile = new File(gwcDir, z + "/" + x + "/" + y + ".png");
                        if (tileFile.exists() && tileFile.isFile()) {
                            files.add(tileFile);
                        }
                    }
                }
            }

            if (files.isEmpty() && zoomStop - zoomStart > 5) {
                log.debug("No tiles found with bounds filtering, trying fallback scan...");
                files = scanAllPngFiles(gwcDir, zoomStart, zoomStop, format);
            }
        }

        long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
        log.debug("enumerateTileFiles END: found {} tiles in {} ms", files.size(), durationMs);
        log.debug("===== enumerateTileFiles END =====");

        return files;
    }

    /**
     * 宽松模式：扫描目录下所有指定 zoom 范围内的 .png 文件
     */
    private List<File> scanAllPngFiles(File gwcDir, int zoomStart, int zoomStop, GwcDirectoryFormat format) {
        List<File> files = new ArrayList<>();

        if (format == GwcDirectoryFormat.STANDARD) {
            return scanAllPngFilesStandard(gwcDir, zoomStart, zoomStop);
        } else if (format == GwcDirectoryFormat.LAYERED_EPSG_4326 || format == GwcDirectoryFormat.LAYERED_EPSG_3857) {
            return scanAllPngFilesLayered(gwcDir, zoomStart, zoomStop, format);
        }

        return scanAllPngFilesStandard(gwcDir, zoomStart, zoomStop);
    }

    /**
     * 标准格式扫描: {gwcDir}/{z}/{x}/{y}.png
     */
    private List<File> scanAllPngFilesStandard(File gwcDir, int zoomStart, int zoomStop) {
        List<File> files = new ArrayList<>();
        File[] zoomDirs = gwcDir.listFiles(File::isDirectory);

        if (zoomDirs == null) {
            return files;
        }

        for (File zoomDir : zoomDirs) {
            try {
                int z = Integer.parseInt(zoomDir.getName());
                if (z < zoomStart || z > zoomStop) {
                    continue;
                }
                files.addAll(scanRecursive(zoomDir));
            } catch (NumberFormatException e) {
                log.trace("Skipping non-numeric directory: {}", zoomDir.getName());
            }
        }

        return files;
    }

    /**
     * 分层格式扫描: {gwcDir}/EPSG_XXXX_z/{x}_{y}/{xx}_{yy}.png
     */
    private List<File> scanAllPngFilesLayered(File gwcDir, int zoomStart, int zoomStop, GwcDirectoryFormat format) {
        List<File> files = new ArrayList<>();
        String prefix = format == GwcDirectoryFormat.LAYERED_EPSG_4326 ? "EPSG_4326_" : "EPSG_3857_";

        File[] zoomDirs = gwcDir.listFiles(File::isDirectory);
        if (zoomDirs == null) {
            return files;
        }

        for (File zoomDir : zoomDirs) {
            String name = zoomDir.getName();
            if (!name.startsWith(prefix)) {
                continue;
            }

            try {
                String zStr = name.substring(prefix.length());
                int z = Integer.parseInt(zStr);
                if (z < zoomStart || z > zoomStop) {
                    continue;
                }

                File[] xyDirs = zoomDir.listFiles(File::isDirectory);
                if (xyDirs == null) {
                    continue;
                }

                for (File xyDir : xyDirs) {
                    files.addAll(scanRecursive(xyDir));
                }
            } catch (NumberFormatException e) {
                log.trace("Skipping non-matching directory: {}", name);
            }
        }

        return files;
    }

    /**
     * 递归扫描目录下的所有 .png 文件，保持相对于 gwcDir 的路径结构
     */
    private List<File> scanRecursive(File dir) {
        List<File> files = new ArrayList<>();
        File[] children = dir.listFiles();

        if (children == null) {
            return files;
        }

        for (File child : children) {
            if (child.isFile() && child.getName().endsWith(".png")) {
                files.add(child);
            } else if (child.isDirectory()) {
                files.addAll(scanRecursive(child));
            }
        }

        return files;
    }

    /**
     * 计算指定 zoom 级别下给定地理范围覆盖的瓦片行列范围。
     */
    private TileRange getTileRange(int z, Bounds bounds) {
        int xMin = (int) Math.floor((bounds.minX + 180) / 360 * (1 << z));
        int xMax = (int) Math.floor((bounds.maxX + 180) / 360 * (1 << z));
        int yMin = (int) Math.floor(tileY(bounds.maxY, z));
        int yMax = (int) Math.floor(tileY(bounds.minY, z));

        // 确保范围不超出有效瓦片范围 [0, 2^z - 1]
        int maxTile = (1 << z) - 1;
        xMin = Math.max(0, Math.min(xMin, maxTile));
        xMax = Math.max(0, Math.min(xMax, maxTile));
        yMin = Math.max(0, Math.min(yMin, maxTile));
        yMax = Math.max(0, Math.min(yMax, maxTile));

        return new TileRange(xMin, xMax, yMin, yMax);
    }

    private double tileY(double lat, int z) {
        double latRad = Math.toRadians(lat);
        return (1 - Math.log(Math.tan(latRad) + 1 / Math.cos(latRad)) / Math.PI) / 2 * (1 << z);
    }

    /**
     * 解析请求中的地理范围，为空时从数据集 extent 中获取并转换到 EPSG:4326。
     * 优先级：请求参数 > dataset.extent > raster_metadata.transform > 全球默认范围
     */
    private Bounds parseBounds(TilePackageRequest request, Dataset dataset) {
        if (request.getBounds() != null
                && request.getBounds().getMinX() != null
                && request.getBounds().getMaxX() != null
                && request.getBounds().getMinY() != null
                && request.getBounds().getMaxY() != null) {
            return new Bounds(
                    request.getBounds().getMinX(),
                    request.getBounds().getMinY(),
                    request.getBounds().getMaxX(),
                    request.getBounds().getMaxY()
            );
        }

        if (dataset.getExtent() != null && !dataset.getExtent().isEmpty()) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                Map<String, Object> extentMap = mapper.readValue(dataset.getExtent(), Map.class);
                double[] extent = new double[4];
                extent[0] = ((Number) extentMap.get("minX")).doubleValue();
                extent[1] = ((Number) extentMap.get("minY")).doubleValue();
                extent[2] = ((Number) extentMap.get("maxX")).doubleValue();
                extent[3] = ((Number) extentMap.get("maxY")).doubleValue();

                String sourceCrs = dataset.getSrs();
                if (sourceCrs != null && !sourceCrs.isEmpty() && !"EPSG:4326".equalsIgnoreCase(sourceCrs)) {
                    double[] transformed = CrsTransformUtil.transformExtentToWgs84(extent, sourceCrs);
                    if (transformed != null) {
                        return new Bounds(transformed[0], transformed[1], transformed[2], transformed[3]);
                    }
                    log.warn("CRS transform failed for {} (sourceCrs={}), using original extent values", dataset.getId(), sourceCrs);
                } else if (sourceCrs == null || sourceCrs.isEmpty()) {
                    log.warn("Dataset {} has no SRS, assuming extent is in EPSG:4326", dataset.getId());
                }
                return new Bounds(extent[0], extent[1], extent[2], extent[3]);
            } catch (Exception e) {
                log.warn("Failed to parse dataset extent: {}", e.getMessage());
            }
        }

        Bounds extentFromMetadata = tryGetExtentFromRasterMetadata(dataset.getId());
        if (extentFromMetadata != null) {
            return extentFromMetadata;
        }

        log.warn("Dataset {} has no extent information, using global default bounds (-180,-90,180,90)", dataset.getId());
        return new Bounds(-180, -90, 180, 90);
    }

    /**
     * 尝试从 raster_metadata 表获取影像范围
     */
    private Bounds tryGetExtentFromRasterMetadata(Long datasetId) {
        try {
            LambdaQueryWrapper<RasterMetadata> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(RasterMetadata::getDatasetId, datasetId);
            RasterMetadata rasterMetadata = rasterMetadataMapper.selectOne(queryWrapper);

            if (rasterMetadata == null) {
                log.debug("No raster_metadata found for dataset {}", datasetId);
                return null;
            }

            String transformJson = rasterMetadata.getTransform();
            if (transformJson == null || transformJson.isEmpty()) {
                log.debug("raster_metadata.transform is empty for dataset {}", datasetId);
                return null;
            }

            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> transform = mapper.readValue(transformJson, Map.class);

            Double minX = ((Number) transform.get("minX")).doubleValue();
            Double minY = ((Number) transform.get("minY")).doubleValue();
            Double maxX = ((Number) transform.get("maxX")).doubleValue();
            Double maxY = ((Number) transform.get("maxY")).doubleValue();

            String sourceCrs = rasterMetadata.getCrs();
            double[] extent = new double[]{minX, minY, maxX, maxY};

            if (sourceCrs != null && !sourceCrs.isEmpty() && !"EPSG:4326".equalsIgnoreCase(sourceCrs)) {
                double[] transformed = CrsTransformUtil.transformExtentToWgs84(extent, sourceCrs);
                if (transformed != null) {
                    log.debug("Using extent from raster_metadata with CRS transform: {}", sourceCrs);
                    return new Bounds(transformed[0], transformed[1], transformed[2], transformed[3]);
                }
                log.warn("CRS transform failed for dataset {} (sourceCrs={}), using original extent", datasetId, sourceCrs);
            }

            log.debug("Using extent from raster_metadata: minX={}, minY={}, maxX={}, maxY={}", minX, minY, maxX, maxY);
            return new Bounds(minX, minY, maxX, maxY);
        } catch (Exception e) {
            log.warn("Failed to get extent from raster_metadata for dataset {}: {}", datasetId, e.getMessage());
            return null;
        }
    }

    private static class TileRange {
        final int xMin, xMax, yMin, yMax;

        TileRange(int xMin, int xMax, int yMin, int yMax) {
            this.xMin = xMin;
            this.xMax = xMax;
            this.yMin = yMin;
            this.yMax = yMax;
        }
    }

    private static class Bounds {
        final double minX, minY, maxX, maxY;

        Bounds(double minX, double minY, double maxX, double maxY) {
            this.minX = minX;
            this.minY = minY;
            this.maxX = maxX;
            this.maxY = maxY;
        }
    }
}

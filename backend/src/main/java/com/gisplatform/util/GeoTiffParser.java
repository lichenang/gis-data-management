package com.gisplatform.util;

import com.gisplatform.entity.RasterMetadata;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * GeoTIFF 文件解析工具类。
 * <p>
 * 使用 GeoTools 读取 GeoTIFF 文件的元数据信息。
 * </p>
 *
 * @author GIS Platform Team
 * @since 1.0.0
 */
public class GeoTiffParser {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * 解析 GeoTIFF 文件并提取元数据。
     *
     * @param file 上传的 GeoTIFF 文件
     * @return 包含文件元数据的 RasterMetadata 对象
     * @throws Exception 文件读取或解析失败时抛出
     */
    public static RasterMetadata parse(MultipartFile file) throws Exception {
        RasterMetadata metadata = new RasterMetadata();

        metadata.setFileName(file.getOriginalFilename());
        metadata.setFileSize(file.getSize());
        metadata.setValidationStatus("pending");

        try (InputStream inputStream = file.getInputStream()) {
            GeoTiffReader reader = new GeoTiffReader(inputStream);
            GridCoverage2D coverage = reader.read(null);

            if (coverage == null) {
                throw new Exception("无法读取 GeoTIFF 文件");
            }

            int width = 0;
            int height = 0;
            try {
                Object gridGeom = coverage.getGridGeometry();
                java.lang.reflect.Method getSpan0 = gridGeom.getClass().getMethod("getGridRange");
                Object gridRange = getSpan0.invoke(gridGeom);
                java.lang.reflect.Method getSpan = gridRange.getClass().getMethod("getSpan", int.class);
                width = ((Number) getSpan.invoke(gridRange, 0)).intValue();
                height = ((Number) getSpan.invoke(gridRange, 1)).intValue();
            } catch (Exception e) {
                width = 0;
                height = 0;
            }

            metadata.setWidth(width);
            metadata.setHeight(height);

            int bands = coverage.getNumSampleDimensions();
            metadata.setBands(bands);

            String pixelType = getPixelType(coverage);
            metadata.setPixelType(pixelType);

            if (coverage.getNumSampleDimensions() > 0) {
                try {
                    Object sdObj = coverage.getSampleDimension(0);
                    if (sdObj != null) {
                        java.lang.reflect.Method getNoData = sdObj.getClass().getMethod("getNoDataValues");
                        double[] noDataValues = (double[]) getNoData.invoke(sdObj);
                        if (noDataValues != null && noDataValues.length > 0) {
                            double noData = noDataValues[0];
                            if (!Double.isNaN(noData)) {
                                metadata.setNoDataValue(noData);
                            }
                        }
                    }
                } catch (Exception e) {
                    // 忽略
                }
            }

            Object crsObj = coverage.getCoordinateReferenceSystem();
            String crsCode = extractCrs(crsObj);
            metadata.setCrs(crsCode);

            Object envelope = coverage.getEnvelope();
            Map<String, Object> transformMap = new HashMap<>();
            try {
                java.lang.reflect.Method getMin0 = envelope.getClass().getMethod("getMinimum", int.class);
                java.lang.reflect.Method getMax0 = envelope.getClass().getMethod("getMaximum", int.class);
                java.lang.reflect.Method getSpan0 = envelope.getClass().getMethod("getSpan", int.class);
                transformMap.put("minX", getMin0.invoke(envelope, 0));
                transformMap.put("minY", getMin0.invoke(envelope, 1));
                transformMap.put("maxX", getMax0.invoke(envelope, 0));
                transformMap.put("maxY", getMax0.invoke(envelope, 1));
            } catch (Exception e) {
                transformMap.put("minX", 0);
                transformMap.put("minY", 0);
                transformMap.put("maxX", 0);
                transformMap.put("maxY", 0);
            }
            try {
                metadata.setTransform(MAPPER.writeValueAsString(transformMap));
            } catch (Exception e) {
                metadata.setTransform("{}");
            }

            Map<String, Object> overviewsMap = new HashMap<>();
            double resX = 0;
            double resY = 0;
            try {
                if (width > 0 && transformMap.get("maxX") != null && transformMap.get("minX") != null) {
                    resX = Math.abs(((Number)transformMap.get("maxX")).doubleValue() - ((Number)transformMap.get("minX")).doubleValue()) / width;
                }
                if (height > 0 && transformMap.get("maxY") != null && transformMap.get("minY") != null) {
                    resY = Math.abs(((Number)transformMap.get("maxY")).doubleValue() - ((Number)transformMap.get("minY")).doubleValue()) / height;
                }
            } catch (Exception e) {
                resX = 0;
                resY = 0;
            }
            overviewsMap.put("resolutionX", resX);
            overviewsMap.put("resolutionY", resY);
            try {
                metadata.setOverviews(MAPPER.writeValueAsString(overviewsMap));
            } catch (Exception e) {
                metadata.setOverviews("{}");
            }

            metadata.setValidationStatus("valid");

            reader.dispose();
        }

        return metadata;
    }

    private static String getPixelType(GridCoverage2D coverage) {
        if (coverage.getNumSampleDimensions() == 0) {
            return "Unknown";
        }
        try {
            Object sd = coverage.getSampleDimension(0);
            if (sd != null) {
                java.lang.reflect.Method getType = sd.getClass().getMethod("getSampleType");
                Object type = getType.invoke(sd);
                if (type != null) {
                    return type.toString();
                }
            }
        } catch (Exception e) {
            // 忽略
        }
        return "Unknown";
    }

    private static String extractCrs(Object crsObj) {
        if (crsObj == null) {
            return null;
        }

        String code = null;

        try {
            java.lang.reflect.Method getIds = crsObj.getClass().getMethod("getIdentifiers");
            Object identifiers = getIds.invoke(crsObj);
            if (identifiers != null) {
                java.lang.reflect.Method iterator = identifiers.getClass().getMethod("iterator");
                Object iter = iterator.invoke(identifiers);
                java.lang.reflect.Method hasNext = iter.getClass().getMethod("hasNext");
                java.lang.reflect.Method next = iter.getClass().getMethod("next");
                while ((Boolean) hasNext.invoke(iter)) {
                    Object id = next.invoke(iter);
                    if (id != null) {
                        String idStr = id.toString();
                        if (idStr != null && !idStr.isEmpty()) {
                            if (idStr.startsWith("EPSG:")) {
                                return idStr;
                            }
                            try {
                                java.lang.reflect.Method getCode = id.getClass().getMethod("getCode");
                                String codeStr = (String) getCode.invoke(id);
                                if (codeStr != null && !codeStr.isEmpty()) {
                                    return "EPSG:" + codeStr;
                                }
                            } catch (Exception e) {
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
        }

        try {
            java.lang.reflect.Method getName = crsObj.getClass().getMethod("getName");
            Object name = getName.invoke(crsObj);
            if (name != null) {
                String nameStr = name.toString();
                if (nameStr != null) {
                    Matcher m = Pattern.compile("EPSG[:\\s]*(\\d+)", Pattern.CASE_INSENSITIVE).matcher(nameStr);
                    if (m.find()) {
                        return "EPSG:" + m.group(1);
                    }

                    if (nameStr.contains("WGS 84") && nameStr.contains("Mercator")) {
                        return "EPSG:3857";
                    }
                    if (nameStr.contains("WGS 84") && !nameStr.contains("Mercator")) {
                        return "EPSG:4326";
                    }
                    if (nameStr.contains("Pseudo-Mercator")) {
                        return "EPSG:3857";
                    }
                    if (nameStr.contains("World Geodetic System 1984")) {
                        return "EPSG:4326";
                    }
                }
            }
        } catch (Exception e) {
        }

        return code != null && !code.isEmpty() ? code : "Unknown";
    }

    private static String extractCrsFallback(Object crsObj) {
        String code = null;
        try {
            java.lang.reflect.Method getIds = crsObj.getClass().getMethod("getIdentifiers");
            Object identifiers = getIds.invoke(crsObj);
            if (identifiers != null) {
                java.lang.reflect.Method iterator = identifiers.getClass().getMethod("iterator");
                Object iter = iterator.invoke(identifiers);
                java.lang.reflect.Method hasNext = iter.getClass().getMethod("hasNext");
                java.lang.reflect.Method next = iter.getClass().getMethod("next");
                if ((Boolean) hasNext.invoke(iter)) {
                    code = next.invoke(iter).toString();
                }
            }
        } catch (Exception e) {
        }
        if (code == null || code.isEmpty()) {
            try {
                java.lang.reflect.Method getName = crsObj.getClass().getMethod("getName");
                Object name = getName.invoke(crsObj);
                if (name != null) {
                    java.lang.reflect.Method toString = name.getClass().getMethod("toString");
                    code = toString.invoke(name).toString();
                }
            } catch (Exception e) {
                code = "Unknown";
            }
        }
        return code;
    }
}

package com.gisplatform.util;

import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.referencing.operation.MathTransform;
import org.geotools.api.referencing.IdentifiedObject;
import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.WKBWriter;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

@Slf4j
public class CrsTransformUtil {

    private static final String TARGET_CRS = "EPSG:4326";
    private static final Logger LOGGER = Logger.getLogger(CrsTransformUtil.class.getName());

    private static final Map<String, Integer> CHINA_CRS_MAPPINGS = new HashMap<>();
    static {
        // ==========================================
        // 地理坐标系 (Geographic CS)
        // ==========================================
        CHINA_CRS_MAPPINGS.put("CGCS2000", 4490);
        CHINA_CRS_MAPPINGS.put("GCS_China_Geodetic_Coordinate_System_2000", 4490);
        CHINA_CRS_MAPPINGS.put("China_2000", 4490);
        CHINA_CRS_MAPPINGS.put("GCS_China_2000", 4490);

        CHINA_CRS_MAPPINGS.put("Beijing_1954", 4214);
        CHINA_CRS_MAPPINGS.put("GCS_Beijing_1954", 4214);

        CHINA_CRS_MAPPINGS.put("Xian_1980", 4610);
        CHINA_CRS_MAPPINGS.put("GCS_Xian_1980", 4610);

        CHINA_CRS_MAPPINGS.put("WGS_1984", 4326);
        CHINA_CRS_MAPPINGS.put("GCS_WGS_1984", 4326);

        // ==========================================
        // CGCS2000 高斯-克吕格 3度带投影坐标系
        // 注意：4491-4494 是旧的错误代码，正确的代码是 4524-4533
        // ==========================================

        // 日志中出现过的下划线格式（必须加上！）
        CHINA_CRS_MAPPINGS.put("CGCS2000_3_Degree_GK_Zone_35", 4524); // 中央经线 105°E
        CHINA_CRS_MAPPINGS.put("CGCS2000_3_Degree_GK_Zone_36", 4525); // 中央经线 108°E
        CHINA_CRS_MAPPINGS.put("CGCS2000_3_Degree_GK_Zone_37", 4526); // 中央经线 111°E
        CHINA_CRS_MAPPINGS.put("CGCS2000_3_Degree_GK_Zone_38", 4527); // 中央经线 114°E
        CHINA_CRS_MAPPINGS.put("CGCS2000_3_Degree_GK_Zone_39", 4528); // 中央经线 117°E
        CHINA_CRS_MAPPINGS.put("CGCS2000_3_Degree_GK_Zone_40", 4529); // 中央经线 120°E

        // 更常见的带斜杠格式（兼容）
        CHINA_CRS_MAPPINGS.put("CGCS2000 / 3-degree Gauss-Kruger zone 35", 4524);
        CHINA_CRS_MAPPINGS.put("CGCS2000 / 3-degree Gauss-Kruger zone 36", 4525);
        CHINA_CRS_MAPPINGS.put("CGCS2000 / 3-degree Gauss-Kruger zone 37", 4526);
        CHINA_CRS_MAPPINGS.put("CGCS2000 / 3-degree Gauss-Kruger zone 38", 4527);
        CHINA_CRS_MAPPINGS.put("CGCS2000 / 3-degree Gauss-Kruger zone 39", 4528);
        CHINA_CRS_MAPPINGS.put("CGCS2000 / 3-degree Gauss-Kruger zone 40", 4529);

        // Beijing 1954 投影坐标系
        CHINA_CRS_MAPPINGS.put("Beijing 1954 / 3-degree Gauss-Kruger CM 117E", 2433);
        CHINA_CRS_MAPPINGS.put("Beijing 1954 / 3-degree Gauss-Kruger CM 123E", 2434);
    }

    public static double[] transformExtentToWgs84(double[] extent, String sourceCrs) {
        if (extent == null || extent.length != 4) {
            LOGGER.log(Level.WARNING, "Invalid extent: " + (extent == null ? "null" : "length=" + extent.length));
            return null;
        }

        if (sourceCrs == null || sourceCrs.isEmpty()) {
            LOGGER.log(Level.WARNING, "Source CRS is empty");
            return null;
        }

        if ("EPSG:4326".equalsIgnoreCase(sourceCrs)) {
            return extent;
        }

        try {
            CoordinateReferenceSystem source = CRS.decode(sourceCrs);
            CoordinateReferenceSystem target = CRS.decode(TARGET_CRS);

            if (source.equals(target)) {
                return extent;
            }

            MathTransform mathTransform = CRS.findMathTransform(source, target);

            double[] minPoint = new double[]{extent[0], extent[1]};
            double[] maxPoint = new double[]{extent[2], extent[3]};

            double[] minResult = new double[2];
            double[] maxResult = new double[2];

            mathTransform.transform(minPoint, 0, minResult, 0, 1);
            mathTransform.transform(maxPoint, 0, maxResult, 0, 1);

            double[] result = new double[4];
            result[0] = minResult[1];
            result[1] = minResult[0];
            result[2] = maxResult[1];
            result[3] = maxResult[0];

            LOGGER.info(String.format("Transformed extent from %s to %s: [%.6f, %.6f, %.6f, %.6f] -> [%.6f, %.6f, %.6f, %.6f]",
                    sourceCrs, TARGET_CRS, extent[0], extent[1], extent[2], extent[3],
                    result[0], result[1], result[2], result[3]));

            return result;

        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to transform extent from " + sourceCrs + " to " + TARGET_CRS + ": " + e.getMessage());
            return null;
        }
    }

    public static boolean isValidCrs(String crsCode) {
        if (crsCode == null || crsCode.isEmpty()) {
            return false;
        }
        try {
            CRS.decode(crsCode);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 获取 CoordinateReferenceSystem 对应的 EPSG 代码
     *
     * @param crs 坐标系对象
     * @return EPSG 代码，如果无法识别返回 0
     */
    public static int getEpsgCode(CoordinateReferenceSystem crs) {
        if (crs == null) {
            LOGGER.log(Level.WARNING, "CRS is null, cannot determine EPSG code");
            return 0;
        }

        try {
            if (crs.getIdentifiers() != null && !crs.getIdentifiers().isEmpty()) {
                for (var id : crs.getIdentifiers()) {
                    String code = id.getCode();
                    if (code != null && code.matches("\\d+")) {
                        int epsg = Integer.parseInt(code);
                        LOGGER.log(Level.INFO, "Found EPSG code from identifier: {0}", epsg);
                        return epsg;
                    }
                }
            }

            String name = crs.getName().toString();
            Integer mappedCode = findInChinaMapping(name);
            if (mappedCode != null) {
                LOGGER.log(Level.INFO, "Matched CRS ''{0}'' to EPSG:{1} via China CRS mapping", new Object[]{name, mappedCode});
                return mappedCode;
            }

            if (name.contains("EPSG:")) {
                int idx = name.indexOf("EPSG:") + 5;
                String code = name.substring(idx);
                if (code.matches("\\d+")) {
                    int epsg = Integer.parseInt(code);
                    LOGGER.log(Level.INFO, "Extracted EPSG code from name: {0}", epsg);
                    return epsg;
                }
            }

            LOGGER.log(Level.WARNING, "Cannot determine EPSG code for CRS: ''{0}''. Consider adding it to CHINA_CRS_MAPPINGS.", name);
            return 0;

        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to get EPSG code: " + e.getMessage());
            return 0;
        }
    }

    /**
     * 在中国坐标系映射表中查找
     *
     * @param crsName CRS 名称
     * @return 匹配的 EPSG 代码，若无匹配返回 null
     */
    private static Integer findInChinaMapping(String crsName) {
        if (crsName == null || crsName.isEmpty()) {
            return null;
        }

        if (CHINA_CRS_MAPPINGS.containsKey(crsName)) {
            return CHINA_CRS_MAPPINGS.get(crsName);
        }

        for (Map.Entry<String, Integer> entry : CHINA_CRS_MAPPINGS.entrySet()) {
            if (crsName.contains(entry.getKey())) {
                return entry.getValue();
            }
        }

        return null;
    }

    public static byte[] toWkb(Geometry geometry) {
        if (geometry == null) {
            return null;
        }
        try {
            WKBWriter writer = new WKBWriter();
            return writer.write(geometry);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to write geometry to WKB: " + e.getMessage());
            return null;
        }
    }
}

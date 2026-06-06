package com.gisplatform.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.gisplatform.dto.DatasetImportResult;
import com.gisplatform.dto.GisDataParseResult;
import com.gisplatform.entity.Dataset;
import com.gisplatform.mapper.DatasetMapper;
import com.gisplatform.security.CurrentUserUtils;
import com.gisplatform.service.GisDataParserService;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.geom.impl.CoordinateArraySequence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.sql.DataSource;
import java.io.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class GisDataParserServiceImpl implements GisDataParserService {

    private static final Logger logger = LoggerFactory.getLogger(GisDataParserServiceImpl.class);

    private static final String[] SUPPORTED_FORMATS = {
        "shp", "zip", "geojson", "json"
    };

    @Autowired
    private DataSource dataSource;

    @Autowired
    private DatasetMapper datasetMapper;

    @Autowired
    private CurrentUserUtils currentUserUtils;

    @Value("${gis.datasource.schema:public}")
    private String dbSchema;

    @Override
    public GisDataParseResult parseFile(MultipartFile file, String fileName) {
        try {
            String ext = getFileExtension(fileName);
            if (!isSupportedFormat(ext)) {
                return GisDataParseResult.error("不支持的文件格式: " + ext + "，支持的格式: " + String.join(", ", SUPPORTED_FORMATS));
            }

            String content = new String(file.getBytes(), "UTF-8");

            if ("geojson".equalsIgnoreCase(ext) || "json".equalsIgnoreCase(ext)) {
                return parseGeoJSON(content);
            } else if ("shp".equalsIgnoreCase(ext)) {
                return parseShapefileMetadata(file);
            }

            return GisDataParseResult.error("不支持的格式: " + ext);
        } catch (Exception e) {
            logger.error("解析文件失败", e);
            return GisDataParseResult.error("解析文件失败: " + e.getMessage());
        }
    }

    private GisDataParseResult parseGeoJSON(String content) throws Exception {
        JSONObject json = JSONUtil.parseObj(content);

        String type = json.getStr("type");
        if (!"FeatureCollection".equals(type)) {
            return GisDataParseResult.error("仅支持 GeoJSON FeatureCollection 格式");
        }

        JSONArray features = json.getJSONArray("features");
        if (features == null || features.isEmpty()) {
            return GisDataParseResult.error("GeoJSON 中没有要素");
        }

        int featureCount = features.size();

        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE;
        double maxX = Double.MIN_VALUE, maxY = Double.MIN_VALUE;
        String geometryType = "Unknown";

        for (int i = 0; i < Math.min(featureCount, 100); i++) {
            JSONObject feature = features.getJSONObject(i);
            JSONObject geometry = feature.getJSONObject("geometry");
            if (geometry == null) continue;

            String geomType = geometry.getStr("type");
            if (geometryType.equals("Unknown") || geometryType.isEmpty()) {
                geometryType = geomType;
            }

            JSONArray coordinates = geometry.getJSONArray("coordinates");
            if (coordinates == null) continue;

            double[] bbox = getBoundingBox(geomType, coordinates);
            if (bbox != null) {
                minX = Math.min(minX, bbox[0]);
                minY = Math.min(minY, bbox[1]);
                maxX = Math.max(maxX, bbox[2]);
                maxY = Math.max(maxY, bbox[3]);
            }
        }

        if (minX == Double.MAX_VALUE) {
            minX = minY = maxX = maxY = 0;
        }

        GisDataParseResult result = GisDataParseResult.success("解析成功");
        result.setGeometryType(geometryType);
        result.setSrs("EPSG:4326");
        result.setFeatureCount(featureCount);
        result.setBounds(new double[]{minX, minY, maxX, maxY});
        result.setTableName(generateTableName("geojson"));

        return result;
    }

    private double[] getBoundingBox(String type, JSONArray coordinates) {
        try {
            if ("Point".equals(type)) {
                double x = coordinates.getDouble(0);
                double y = coordinates.getDouble(1);
                return new double[]{x, y, x, y};
            } else if ("LineString".equals(type)) {
                double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE;
                double maxX = Double.MIN_VALUE, maxY = Double.MIN_VALUE;
                for (int i = 0; i < coordinates.size(); i++) {
                    JSONArray coord = coordinates.getJSONArray(i);
                    double x = coord.getDouble(0);
                    double y = coord.getDouble(1);
                    minX = Math.min(minX, x);
                    minY = Math.min(minY, y);
                    maxX = Math.max(maxX, x);
                    maxY = Math.max(maxY, y);
                }
                return new double[]{minX, minY, maxX, maxY};
            } else if ("Polygon".equals(type)) {
                JSONArray rings = coordinates;
                if (rings.size() > 0) {
                    JSONArray ring = rings.getJSONArray(0);
                    double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE;
                    double maxX = Double.MIN_VALUE, maxY = Double.MIN_VALUE;
                    for (int i = 0; i < ring.size(); i++) {
                        JSONArray coord = ring.getJSONArray(i);
                        double x = coord.getDouble(0);
                        double y = coord.getDouble(1);
                        minX = Math.min(minX, x);
                        minY = Math.min(minY, y);
                        maxX = Math.max(maxX, x);
                        maxY = Math.max(maxY, y);
                    }
                    return new double[]{minX, minY, maxX, maxY};
                }
            }
        } catch (Exception e) {
            logger.warn("解析边界失败: " + e.getMessage());
        }
        return null;
    }

    private GisDataParseResult parseShapefileMetadata(MultipartFile file) {
        return GisDataParseResult.success("Shapefile 文件已选择，请导入以获取详细信息");
    }

    @Override
    public DatasetImportResult importToPostGIS(MultipartFile file, String fileName, String datasetName, String targetSrs) {
        Connection conn = null;
        try {
            String ext = getFileExtension(fileName);
            if (!isSupportedFormat(ext)) {
                return DatasetImportResult.error("不支持的文件格式: " + ext);
            }

            String tableName = generateTableName(datasetName);

            conn = dataSource.getConnection();

            if (StrUtil.isBlank(targetSrs)) {
                targetSrs = "EPSG:4326";
            }

            int importedCount;
            if ("geojson".equalsIgnoreCase(ext) || "json".equalsIgnoreCase(ext)) {
                String content = new String(file.getBytes(), "UTF-8");
                importedCount = importGeoJSON(conn, content, tableName, datasetName);
            } else {
                return DatasetImportResult.error("Shapefile 导入功能暂未实现，请使用 GeoJSON 格式");
            }

            Dataset dataset = new Dataset();
            dataset.setName(datasetName);
            dataset.setType("vector");
            dataset.setStorageType("postgis");
            dataset.setTableName(tableName);
            dataset.setSrs(targetSrs);
            dataset.setFeatureCount(importedCount);
            dataset.setGeometryType("MultiPolygon");
            dataset.setStatus("draft");
            dataset.setVersion(1);
            dataset.setTenantId("default");
            dataset.setCreateTime(LocalDateTime.now());
            dataset.setCreatedBy(currentUserUtils.getCurrentUserId());

            datasetMapper.insert(dataset);

            return DatasetImportResult.success(dataset.getId(), importedCount);
        } catch (Exception e) {
            logger.error("导入数据失败", e);
            return DatasetImportResult.error("导入数据失败: " + e.getMessage());
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    logger.error("关闭连接失败", e);
                }
            }
        }
    }

    private int importGeoJSON(Connection conn, String content, String tableName, String datasetName) throws Exception {
        JSONObject json = JSONUtil.parseObj(content);
        JSONArray features = json.getJSONArray("features");

        if (features == null || features.isEmpty()) {
            throw new RuntimeException("GeoJSON 中没有要素");
        }

        JSONObject firstFeature = features.getJSONObject(0);
        JSONObject firstGeometry = firstFeature.getJSONObject("geometry");
        Set<String> propertyNames = new LinkedHashSet<>();

        if (firstFeature.containsKey("properties")) {
            JSONObject props = firstFeature.getJSONObject("properties");
            if (props != null) {
                for (String key : props.keySet()) {
                    propertyNames.add(key);
                }
            }
        }

        createGeoJSONTable(conn, tableName, propertyNames);

        return insertGeoJSONFeatures(conn, features, tableName, propertyNames);
    }

    private void createGeoJSONTable(Connection conn, String tableName, Set<String> propertyNames) throws SQLException {
        StringBuilder sql = new StringBuilder();
        sql.append("CREATE TABLE IF NOT EXISTS \"").append(dbSchema).append("\".\"").append(tableName).append("\" (");

        List<String> columnDefs = new ArrayList<>();
        columnDefs.add("id SERIAL PRIMARY KEY");

        for (String prop : propertyNames) {
            String colName = prop.replaceAll("[^a-zA-Z0-9_]", "_");
            columnDefs.add("\"" + colName + "\" TEXT");
        }

        columnDefs.add("geometry GEOMETRY");

        sql.append(String.join(", ", columnDefs));
        sql.append(")");

        try (PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            stmt.execute();
        }

        String indexSql = "CREATE INDEX IF NOT EXISTS \"" + tableName + "_geom_idx\" ON \"" + dbSchema + "\".\"" + tableName + "\" USING GIST (geometry)";
        try (PreparedStatement stmt = conn.prepareStatement(indexSql)) {
            stmt.execute();
        }
    }

    private int insertGeoJSONFeatures(Connection conn, JSONArray features, String tableName, Set<String> propertyNames) throws SQLException {
        List<String> propertyList = new ArrayList<>(propertyNames);

        StringBuilder sql = new StringBuilder();
        sql.append("INSERT INTO \"").append(dbSchema).append("\".\"").append(tableName).append("\" (");
        sql.append("geometry");
        for (String prop : propertyList) {
            String colName = prop.replaceAll("[^a-zA-Z0-9_]", "_");
            sql.append(", \"").append(colName).append("\"");
        }
        sql.append(") VALUES (ST_GeomFromGeoJSON(?)").append(",?".repeat(propertyList.size())).append(")");

        int count = 0;
        try (PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            conn.setAutoCommit(false);

            for (int i = 0; i < features.size(); i++) {
                JSONObject feature = features.getJSONObject(i);
                JSONObject geometry = feature.getJSONObject("geometry");

                if (geometry != null) {
                    String geoJson = geometry.toString();
                    stmt.setString(1, geoJson);
                } else {
                    stmt.setNull(1, Types.VARCHAR);
                }

                JSONObject props = feature.getJSONObject("properties");
                int paramIndex = 2;
                for (String prop : propertyList) {
                    if (props != null && props.containsKey(prop)) {
                        Object value = props.get(prop);
                        stmt.setString(paramIndex++, value != null ? value.toString() : null);
                    } else {
                        stmt.setNull(paramIndex++, Types.VARCHAR);
                    }
                }

                stmt.addBatch();
                count++;

                if (count % 1000 == 0) {
                    stmt.executeBatch();
                    conn.commit();
                }
            }

            stmt.executeBatch();
            conn.commit();
        } catch (Exception e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }

        return count;
    }

    @Override
    public String[] getSupportedFormats() {
        return SUPPORTED_FORMATS;
    }

    private String getFileExtension(String fileName) {
        if (fileName == null) return "";
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot > 0) {
            return fileName.substring(lastDot + 1).toLowerCase();
        }
        return "";
    }

    private boolean isSupportedFormat(String ext) {
        for (String supported : SUPPORTED_FORMATS) {
            if (supported.equalsIgnoreCase(ext)) {
                return true;
            }
        }
        return false;
    }

    private String generateTableName(String originalName) {
        String sanitized = originalName.replaceAll("[^a-zA-Z0-9_]", "_");
        sanitized = sanitized.toLowerCase();
        if (sanitized.length() > 50) {
            sanitized = sanitized.substring(0, 50);
        }
        return "dataset_" + sanitized + "_" + System.currentTimeMillis() % 10000;
    }
}

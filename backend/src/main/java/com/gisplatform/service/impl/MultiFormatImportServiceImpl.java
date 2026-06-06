package com.gisplatform.service.impl;

import com.gisplatform.common.enums.VectorFileFormat;
import com.gisplatform.dto.DatasetImportResult;
import com.gisplatform.dto.GisDataParseResult;
import com.gisplatform.entity.Dataset;
import com.gisplatform.mapper.DatasetMapper;
import com.gisplatform.security.CurrentUserUtils;
import com.gisplatform.service.FormatDetector;
import com.gisplatform.service.MultiFormatImportService;
import com.gisplatform.service.VectorDataStoreFactory;
import com.gisplatform.util.CrsTransformUtil;
import org.geotools.api.data.DataStore;
import org.geotools.api.data.FeatureSource;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.filter.identity.FeatureId;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.impl.CoordinateArraySequenceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.sql.DataSource;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cn.hutool.core.util.StrUtil;

@Service
public class MultiFormatImportServiceImpl implements MultiFormatImportService {

    private static final Logger logger = LoggerFactory.getLogger(MultiFormatImportServiceImpl.class);

    private static final String[] SUPPORTED_FORMATS = {
            "shp", "zip", "geojson", "json", "kml", "kmz", "gml", "gpx", "csv"
    };

    @Autowired
    private FormatDetector formatDetector;

    @Autowired
    private VectorDataStoreFactory vectorDataStoreFactory;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private DatasetMapper datasetMapper;

    @Autowired
    private CurrentUserUtils currentUserUtils;

    @Value("${gis.datasource.schema:public}")
    private String dbSchema;

    private final GeometryFactory geometryFactory = new GeometryFactory(
            CoordinateArraySequenceFactory.instance()
    );

    @Override
    public GisDataParseResult parseFile(MultipartFile file, String fileName) {
        try {
            VectorFileFormat format = formatDetector.detect(file, fileName);

            if (format == VectorFileFormat.UNKNOWN) {
                return GisDataParseResult.error("不支持的格式或无法识别文件类型");
            }

            if (format == VectorFileFormat.GEOJSON || format == VectorFileFormat.TOPOJSON) {
                return parseJsonMetadata(file);
            }

            GisDataParseResult result = GisDataParseResult.success("文件格式: " + format.getDisplayName());
            result.setFormat(format.name());
            result.setGeometryType("Unknown");
            result.setFeatureCount(0);

            if (format == VectorFileFormat.SHAPEFILE) {
                DataStore dataStore = vectorDataStoreFactory.createDataStore(format, file, fileName);
                String[] typeNames = dataStore.getTypeNames();
                if (typeNames != null && typeNames.length > 0) {
                    var featureSource = dataStore.getFeatureSource(typeNames[0]);
                    var schema = featureSource.getSchema();
                    var nativeCrs = schema.getGeometryDescriptor().getCoordinateReferenceSystem();
                    int nativeSrid = CrsTransformUtil.getEpsgCode(nativeCrs);
                    result.setCrsDetected(nativeSrid > 0);
                    result.setSrs(nativeSrid > 0 ? "EPSG:" + nativeSrid : null);
                } else {
                    result.setCrsDetected(false);
                    result.setSrs(null);
                }
            } else {
                result.setCrsDetected(false);
                result.setSrs("EPSG:4326");
            }

            return result;

        } catch (Exception e) {
            logger.error("解析文件失败", e);
            return GisDataParseResult.error("解析文件失败: " + e.getMessage());
        }
    }

    @Override
    public DatasetImportResult importToPostGIS(MultipartFile file, String fileName, String datasetName, String targetSrs, String sourceSrs) {
        Connection conn = null;
        try {
            if (datasetName == null || datasetName.trim().isEmpty()) {
                return DatasetImportResult.error("数据集名称不能为空");
            }

            VectorFileFormat format = formatDetector.detect(file, fileName);

            if (format == VectorFileFormat.UNKNOWN) {
                return DatasetImportResult.error("不支持的格式或无法识别文件类型");
            }

            String tableName = generateTableName(datasetName);
            conn = dataSource.getConnection();

            if (StrUtil.isBlank(targetSrs)) {
                targetSrs = "EPSG:4326";
            }

            int importedCount;
            if (format == VectorFileFormat.GEOJSON || format == VectorFileFormat.TOPOJSON) {
                String content = new String(file.getBytes(), "UTF-8");
                importedCount = importGeoJSON(conn, content, tableName, datasetName);
            } else {
                importedCount = importUsingDataStore(conn, file, fileName, format, tableName, datasetName, targetSrs, sourceSrs);
            }

            Dataset dataset = new Dataset();
            dataset.setName(datasetName);
            dataset.setType("vector");
            dataset.setStorageType("postgis");
            dataset.setTableName(tableName);
            dataset.setSrs(targetSrs);
            dataset.setFeatureCount(importedCount);
            dataset.setGeometryType(detectGeometryType(conn, tableName));
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
                } catch (Exception e) {
                    logger.error("关闭连接失败", e);
                }
            }
        }
    }

    private int importUsingDataStore(Connection conn, MultipartFile file, String fileName,
                                      VectorFileFormat format, String tableName, String datasetName,
                                      String targetSrs, String sourceSrs) throws Exception {
        DataStore dataStore = vectorDataStoreFactory.createDataStore(format, file, fileName);

        String[] typeNames = dataStore.getTypeNames();
        if (typeNames == null || typeNames.length == 0) {
            throw new IOException("无法获取要素类型");
        }

        FeatureSource<SimpleFeatureType, SimpleFeature> featureSource =
                dataStore.getFeatureSource(typeNames[0]);

        SimpleFeatureType schema = featureSource.getSchema();
        Set<String> propertyNames = new HashSet<>();
        for (var attr : schema.getAttributeDescriptors()) {
            String localName = attr.getName().getLocalPart();
            if (!localName.equalsIgnoreCase("geometry") && !localName.equalsIgnoreCase("the_geom")) {
                propertyNames.add(localName);
            }
        }

        CoordinateReferenceSystem nativeCrs = schema.getGeometryDescriptor().getCoordinateReferenceSystem();
        int nativeSrid = CrsTransformUtil.getEpsgCode(nativeCrs);
        String nativeCrsName = nativeCrs != null ? nativeCrs.getName().toString() : "unknown";

        int targetEpsg = parseTargetSrs(targetSrs);

        int sourceSridForTransform;
        if (nativeSrid > 0) {
            sourceSridForTransform = nativeSrid;
            logger.info("Identified native CRS: {} (EPSG:{}), will transform to EPSG:{}",
                    nativeCrsName, nativeSrid, targetEpsg);
        } else {
            int sourceEpsg = parseTargetSrs(sourceSrs);
            if (sourceEpsg > 0) {
                sourceSridForTransform = sourceEpsg;
                logger.warn("Cannot identify native CRS for '{}'. Using user-specified source SRS: EPSG:{}, will transform to EPSG:{}",
                        nativeCrsName, sourceEpsg, targetEpsg);
            } else if (targetEpsg > 0) {
                logger.warn("Cannot identify native CRS for '{}'. User specified target SRS: {}. " +
                        "Data will be stored in target SRS without transformation. " +
                        "This may cause incorrect positioning if the source data uses a different CRS.",
                        nativeCrsName, targetSrs);
                sourceSridForTransform = targetEpsg;
            } else {
                throw new IllegalStateException(
                        "Cannot determine source CRS for coordinate transformation. " +
                        "Native CRS: '" + nativeCrsName + "' (EPSG code not found). " +
                        "User-specified source SRS: '" + sourceSrs + "' is invalid or not provided. " +
                        "Please ensure your Shapefile has a valid .prj file or specify the correct SRS during upload."
                );
            }
        }

        createTableFromSchema(conn, tableName, propertyNames);

        int count = 0;
        try (org.geotools.feature.FeatureIterator<SimpleFeature> iterator = featureSource.getFeatures().features()) {
            while (iterator.hasNext()) {
                SimpleFeature feature = iterator.next();
                org.locationtech.jts.geom.Geometry geom = (org.locationtech.jts.geom.Geometry) feature.getDefaultGeometry();
                insertFeature(conn, tableName, geom, feature, propertyNames, sourceSridForTransform, targetEpsg);
                count++;
            }
        }

        createSpatialIndex(conn, tableName);

        logger.info("Imported {} features from {} to table '{}'", count, fileName, tableName);
        return count;
    }

    /**
     * 解析 targetSrs 字符串获取 EPSG 代码
     * 支持格式: "EPSG:4490", "4490", "EPSG:4326"
     *
     * @param targetSrs SRS 字符串
     * @return EPSG 代码，默认返回 4326
     */
    private int parseTargetSrs(String targetSrs) {
        if (targetSrs == null || targetSrs.isEmpty()) {
            return 4326;
        }

        try {
            String code = targetSrs.replaceAll("[^0-9]", "");
            if (code.isEmpty()) {
                return 4326;
            }
            return Integer.parseInt(code);
        } catch (NumberFormatException e) {
            logger.warn("Invalid target SRS format: '{}', using default 4326", targetSrs);
            return 4326;
        }
    }

    private void createTableFromSchema(Connection conn, String tableName, Set<String> propertyNames) throws Exception {
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
    }

    /**
     * 插入要素到数据库
     *
     * @param conn 数据库连接
     * @param tableName 表名
     * @param geometry 几何对象
     * @param feature SimpleFeature 对象
     * @param propertyNames 属性名集合
     * @param sourceSrid 源坐标系 EPSG 代码
     * @param targetSrid 目标坐标系 EPSG 代码
     */
    private void insertFeature(Connection conn, String tableName, org.locationtech.jts.geom.Geometry geometry,
                                SimpleFeature feature, Set<String> propertyNames, int sourceSrid, int targetSrid) throws Exception {
        StringBuilder sql = new StringBuilder();
        sql.append("INSERT INTO \"").append(dbSchema).append("\".\"").append(tableName).append("\" (");

        List<String> columnNames = new ArrayList<>();
        columnNames.add("geometry");
        sql.append("geometry");

        List<String> propList = new ArrayList<>(propertyNames);
        for (String prop : propList) {
            String colName = prop.replaceAll("[^a-zA-Z0-9_]", "_");
            columnNames.add(colName);
            sql.append(", \"").append(colName).append("\"");
        }

        String valuesClause;
        if (sourceSrid == targetSrid) {
            valuesClause = ") VALUES (ST_GeomFromWKB(?, " + sourceSrid + ")";
        } else {
            valuesClause = ") VALUES (ST_Transform(ST_GeomFromWKB(?, " + sourceSrid + "), " + targetSrid + ")";
        }

        StringBuilder values = new StringBuilder(valuesClause);
        for (int i = 0; i < propList.size(); i++) {
            values.append(", ?");
        }
        values.append(")");
        sql.append(values.toString());

        String finalSql = sql.toString();
        long placeholderCount = finalSql.chars().filter(ch -> ch == '?').count();
        int columnCount = columnNames.size();

        logger.info("Generated SQL has {} columns and {} placeholders. SQL: {}",
                columnCount, placeholderCount, finalSql);

        if (placeholderCount != columnCount) {
            throw new IllegalStateException("SQL placeholder mismatch: expected " +
                    columnCount + " columns but found " + placeholderCount + " placeholders in SQL: " + finalSql);
        }

        try (PreparedStatement stmt = conn.prepareStatement(finalSql)) {
            if (geometry != null) {
                stmt.setBytes(1, writeWkb(geometry));
//                stmt.setInt(2, sourceSrid);
            } else {
                stmt.setNull(1, Types.OTHER);
                stmt.setNull(2, Types.INTEGER);
            }

            int idx = 2;
            for (String prop : propList) {
                Object value = feature.getAttribute(prop);
                stmt.setString(idx++, value != null ? value.toString() : null);
            }

            stmt.execute();
        }
    }

    private void createSpatialIndex(Connection conn, String tableName) throws Exception {
        String indexSql = "CREATE INDEX IF NOT EXISTS \"" + tableName + "_geom_idx\" ON \"" +
                dbSchema + "\".\"" + tableName + "\" USING GIST (geometry)";
        try (PreparedStatement stmt = conn.prepareStatement(indexSql)) {
            stmt.execute();
        }
    }

    private int importGeoJSON(Connection conn, String content, String tableName, String datasetName) throws Exception {
        cn.hutool.json.JSONObject json = cn.hutool.json.JSONUtil.parseObj(content);
        cn.hutool.json.JSONArray features = json.getJSONArray("features");

        if (features == null || features.isEmpty()) {
            throw new RuntimeException("GeoJSON 中没有要素");
        }

        cn.hutool.json.JSONObject firstFeature = features.getJSONObject(0);
        cn.hutool.json.JSONObject firstGeometry = firstFeature.getJSONObject("geometry");
        Set<String> propertyNames = new LinkedHashSet<>();

        if (firstFeature.containsKey("properties")) {
            cn.hutool.json.JSONObject props = firstFeature.getJSONObject("properties");
            if (props != null) {
                for (String key : props.keySet()) {
                    propertyNames.add(key);
                }
            }
        }

        createGeoJSONTable(conn, tableName, propertyNames);

        return insertGeoJSONFeatures(conn, features, tableName, propertyNames);
    }

    private void createGeoJSONTable(Connection conn, String tableName, Set<String> propertyNames) throws Exception {
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

        String indexSql = "CREATE INDEX IF NOT EXISTS \"" + tableName + "_geom_idx\" ON \"" +
                dbSchema + "\".\"" + tableName + "\" USING GIST (geometry)";
        try (PreparedStatement stmt = conn.prepareStatement(indexSql)) {
            stmt.execute();
        }
    }

    private int insertGeoJSONFeatures(Connection conn, cn.hutool.json.JSONArray features,
                                       String tableName, Set<String> propertyNames) throws Exception {
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
                cn.hutool.json.JSONObject feature = features.getJSONObject(i);
                cn.hutool.json.JSONObject geometry = feature.getJSONObject("geometry");

                if (geometry != null) {
                    stmt.setString(1, geometry.toString());
                } else {
                    stmt.setNull(1, Types.VARCHAR);
                }

                cn.hutool.json.JSONObject props = feature.getJSONObject("properties");
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

    private GisDataParseResult parseJsonMetadata(MultipartFile file) throws Exception {
        String content = new String(file.getBytes(), "UTF-8");
        cn.hutool.json.JSONObject json = cn.hutool.json.JSONUtil.parseObj(content);

        String type = json.getStr("type");
        if (!"FeatureCollection".equals(type)) {
            return GisDataParseResult.error("仅支持 GeoJSON FeatureCollection 格式");
        }

        cn.hutool.json.JSONArray features = json.getJSONArray("features");
        if (features == null || features.isEmpty()) {
            return GisDataParseResult.error("GeoJSON 中没有要素");
        }

        int featureCount = features.size();
        String geometryType = "Unknown";

        for (int i = 0; i < Math.min(featureCount, 100); i++) {
            cn.hutool.json.JSONObject feature = features.getJSONObject(i);
            cn.hutool.json.JSONObject geometry = feature.getJSONObject("geometry");
            if (geometry == null) continue;

            String geomType = geometry.getStr("type");
            if (geometryType.equals("Unknown") || geometryType.isEmpty()) {
                geometryType = geomType;
            }
        }

        GisDataParseResult result = GisDataParseResult.success("解析成功");
        result.setGeometryType(geometryType);
        result.setSrs("EPSG:4326");
        result.setFeatureCount(featureCount);
        result.setTableName(generateTableName("import"));

        return result;
    }

    private String detectGeometryType(Connection conn, String tableName) {
        try {
            String sql = "SELECT GeometryType(geometry) FROM \"" + dbSchema + "\".\"" + tableName + "\" LIMIT 1";
            try (PreparedStatement stmt = conn.prepareStatement(sql);
                 java.sql.ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString(1);
                }
            }
        } catch (Exception e) {
            logger.warn("检测几何类型失败", e);
        }
        return "Unknown";
    }

    private String generateTableName(String originalName) {
        String sanitized = originalName.replaceAll("[^a-zA-Z0-9_]", "_");
        sanitized = sanitized.toLowerCase();
        if (sanitized.length() > 50) {
            sanitized = sanitized.substring(0, 50);
        }
        return "dataset_" + sanitized + "_" + System.currentTimeMillis() % 10000;
    }

    private byte[] writeWkb(org.locationtech.jts.geom.Geometry geometry) throws IOException {
        org.locationtech.jts.io.WKBWriter writer = new org.locationtech.jts.io.WKBWriter();
        return writer.write(geometry);
    }

    @Override
    public String[] getSupportedFormats() {
        return SUPPORTED_FORMATS;
    }
}

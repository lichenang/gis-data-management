package com.gisplatform.service.impl;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gisplatform.common.exception.BusinessException;
import com.gisplatform.dto.DatasetImportResult;
import com.gisplatform.dto.GisDataParseResult;
import com.gisplatform.entity.Dataset;
import com.gisplatform.mapper.DatasetMapper;
import com.gisplatform.security.CurrentUserUtils;
import com.gisplatform.service.DatasetService;
import com.gisplatform.service.GisDataParserService;
import com.gisplatform.service.MultiFormatImportService;
import com.gisplatform.util.ExportUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.sql.DataSource;
import java.io.File;
import java.io.OutputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DatasetServiceImpl extends ServiceImpl<DatasetMapper, Dataset> implements DatasetService {

    @Autowired
    private GisDataParserService gisDataParserService;

    @Autowired
    private MultiFormatImportService multiFormatImportService;

    @Autowired
    private CurrentUserUtils currentUserUtils;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Value("${gis.datasource.schema:public}")
    private String dbSchema;

    @Override
    public Page<Dataset> listDatasets(int page, int pageSize, String name, String type, String status) {
        Page<Dataset> pageObj = new Page<>(page, pageSize);
        LambdaQueryWrapper<Dataset> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Dataset::getDeleted, 0);
        
        if (name != null && !name.isEmpty()) {
            wrapper.like(Dataset::getName, name);
        }
        if (type != null && !type.isEmpty()) {
            wrapper.eq(Dataset::getType, type);
        }
        if (status != null && !status.isEmpty()) {
            wrapper.eq(Dataset::getStatus, status);
        }
        
        wrapper.orderByDesc(Dataset::getCreateTime);
        return this.page(pageObj, wrapper);
    }

    @Override
    public Dataset getDatasetById(Long id) {
        return this.getOne(new LambdaQueryWrapper<Dataset>()
                .eq(Dataset::getId, id)
                .eq(Dataset::getDeleted, 0));
    }

    @Override
    public boolean createDataset(Dataset dataset) {
        if (dataset.getStatus() == null) {
            dataset.setStatus("draft");
        }
        if (dataset.getVersion() == null) {
            dataset.setVersion(1);
        }
        if (dataset.getSrs() == null) {
            dataset.setSrs("EPSG:4326");
        }
        if (dataset.getTenantId() == null) {
            dataset.setTenantId("default");
        }
        if (dataset.getStorageType() == null) {
            dataset.setStorageType("postgis");
        }
        if (dataset.getType() == null) {
            dataset.setType("vector");
        }
        dataset.setCreateTime(LocalDateTime.now());
        dataset.setCreatedBy(currentUserUtils.getCurrentUserId());
        return this.save(dataset);
    }

    @Override
    public boolean updateDataset(Dataset dataset) {
        dataset.setUpdateTime(LocalDateTime.now());
        return this.updateById(dataset);
    }

//    @Override
//    public boolean deleteDataset(Long id) {
//        Dataset dataset = new Dataset();
//        dataset.setId(id);
//        dataset.setDeleted(1);
//        dataset.setUpdateTime(LocalDateTime.now());
//        return this.updateById(dataset);
//    }
    @Override
    public boolean deleteDataset(Long id) {
        Dataset dataset = this.getById(id);
        if (dataset == null || dataset.getDeleted() == 1) {
            throw new BusinessException("数据集不存在或已被删除");
        }
        // 使用 LambdaUpdateWrapper 显式设置 deleted=1
        LambdaUpdateWrapper<Dataset> wrapper = new LambdaUpdateWrapper<>();
        wrapper.set(Dataset::getDeleted, 1)
                .set(Dataset::getUpdateTime, LocalDateTime.now())
                .eq(Dataset::getId, id);
        return this.update(wrapper);
    }

    @Override
    public GisDataParseResult parseUploadFile(MultipartFile file, String fileName) {
        return multiFormatImportService.parseFile(file, fileName);
    }

    @Override
    public DatasetImportResult importDataset(MultipartFile file, String fileName, String name, String description, String type, String srs, String sourceSrs) {
        DatasetImportResult result = multiFormatImportService.importToPostGIS(file, fileName, name, srs, sourceSrs);
        if (result.isSuccess() && description != null && !description.isEmpty()) {
            Dataset dataset = this.getById(result.getDatasetId());
            if (dataset != null) {
                dataset.setDescription(description);
                this.updateById(dataset);
            }
        }
        return result;
    }

    @Override
    public List<Dataset> listPublishedDatasets() {
        return this.list(new LambdaQueryWrapper<Dataset>()
                .eq(Dataset::getStatus, "published")
                .eq(Dataset::getDeleted, 0)
                .eq(Dataset::getType, "vector"));
    }

    @Override
    public String getDatasetAsGeoJSON(Long id) {
        Dataset dataset = this.getById(id);
        if (dataset == null || dataset.getDeleted() == 1) {
            throw BusinessException.notFound("数据集不存在");
        }

        String tableName = dataset.getTableName();
        if (tableName == null || tableName.isEmpty()) {
            throw BusinessException.badRequest("该数据集未导入空间数据，无法导出");
        }

        String sql = "SELECT ST_AsGeoJSON(geometry) as gj FROM \"" + dbSchema + "\".\"" + tableName + "\"";
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);

        List<Map<String, Object>> features = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Object gjObj = row.get("gj");
            if (gjObj == null) {
                continue;
            }

            JSONObject gj = JSONUtil.parseObj(gjObj.toString());

            Map<String, Object> feature = new HashMap<>();
            feature.put("type", "Feature");

            Map<String, Object> properties = new HashMap<>(row);
            properties.remove("gj");
            feature.put("properties", properties);

            Map<String, Object> geometry = new HashMap<>();
            geometry.put("type", gj.get("type"));
            geometry.put("coordinates", gj.get("coordinates"));
            feature.put("geometry", geometry);

            features.add(feature);
        }

        Map<String, Object> featureCollection = new HashMap<>();
        featureCollection.put("type", "FeatureCollection");
        featureCollection.put("features", features);

        return toJsonString(featureCollection);
    }

    @Override
    public String getDatasetAsKML(Long id) {
        Dataset dataset = this.getById(id);
        if (dataset == null || dataset.getDeleted() == 1) {
            throw BusinessException.notFound("数据集不存在");
        }

        String tableName = dataset.getTableName();
        if (tableName == null || tableName.isEmpty()) {
            throw BusinessException.badRequest("该数据集未导入空间数据，无法导出");
        }

        String sql = "SELECT ST_AsKML(geometry) as kml FROM \"" + dbSchema + "\".\"" + tableName + "\"";
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);

        StringBuilder kml = new StringBuilder();
        kml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        kml.append("<kml xmlns=\"http://www.opengis.net/kml/2.2\">");
        kml.append("<Document>");
        kml.append("<name>").append(ExportUtil.escapeXml(dataset.getName())).append("</name>");
        for (Map<String, Object> row : rows) {
            Object kmlObj = row.get("kml");
            if (kmlObj == null) continue;
            kml.append("<Placemark>");
            kml.append(kmlObj.toString());
            kml.append("</Placemark>");
        }
        kml.append("</Document>");
        kml.append("</kml>");
        return kml.toString();
    }

    @Override
    public String getDatasetAsCsv(Long id) {
        Dataset dataset = this.getById(id);
        if (dataset == null || dataset.getDeleted() == 1) {
            throw BusinessException.notFound("数据集不存在");
        }

        String tableName = dataset.getTableName();
        if (tableName == null || tableName.isEmpty()) {
            throw BusinessException.badRequest("该数据集未导入空间数据，无法导出");
        }

        String columnsSql = "SELECT column_name FROM information_schema.columns " +
                "WHERE table_schema = '" + dbSchema + "' AND table_name = '" + tableName + "' " +
                "AND column_name NOT IN ('id', 'geometry') ORDER BY ordinal_position";
        List<Map<String, Object>> columnRows = jdbcTemplate.queryForList(columnsSql);
        
        List<String> columnNames = new ArrayList<>();
        columnNames.add("id");
        for (Map<String, Object> row : columnRows) {
            columnNames.add(row.get("column_name").toString());
        }
        columnNames.add("geometry");
        
        StringBuilder csv = new StringBuilder();
        csv.append(String.join(",", columnNames)).append("\n");
        
        String dataSql = "SELECT id, ST_AsText(geometry) as geometry";
        for (Map<String, Object> row : columnRows) {
            dataSql += ", \"" + row.get("column_name").toString() + "\"";
        }
        dataSql += " FROM \"" + dbSchema + "\".\"" + tableName + "\"";
        
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(dataSql);
        
        for (Map<String, Object> row : rows) {
            List<String> values = new ArrayList<>();
            for (String col : columnNames) {
                Object value = row.get(col);
                String strValue = value != null ? value.toString() : "";
                strValue = strValue.replace("\"", "\"\"");
                if (strValue.contains(",") || strValue.contains("\n") || strValue.contains("\"")) {
                    strValue = "\"" + strValue + "\"";
                }
                values.add(strValue);
            }
            csv.append(String.join(",", values)).append("\n");
        }
        
        return csv.toString();
    }

    @Override
    public void exportShapefileAsZip(Long id, OutputStream outputStream) {
        throw new BusinessException(501, "Shapefile 导出功能暂不可用，请使用 GeoJSON 格式导出");
    }

    @Deprecated
    private Object parseGeoJSONCoordinates(String geojson) {
        try {
            JSONObject obj = JSONUtil.parseObj(geojson);
            return obj.get("coordinates");
        } catch (Exception e) {
            return null;
        }
    }

    private String toJsonString(Map<String, Object> obj) {
        return JSONUtil.toJsonStr(obj);
    }

    @Override
    public Dataset publishDataset(Long id) {
        Dataset dataset = this.getById(id);
        if (dataset == null || dataset.getDeleted() == 1) {
            throw new RuntimeException("数据集不存在");
        }
        dataset.setStatus("published");
        dataset.setUpdateTime(LocalDateTime.now());
        this.updateById(dataset);
        return dataset;
    }

    @Override
    public Dataset unpublishDataset(Long id) {
        Dataset dataset = this.getById(id);
        if (dataset == null || dataset.getDeleted() == 1) {
            throw new RuntimeException("数据集不存在");
        }
        dataset.setStatus("draft");
        dataset.setUpdateTime(LocalDateTime.now());
        this.updateById(dataset);
        return dataset;
    }
}

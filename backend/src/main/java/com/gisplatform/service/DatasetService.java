package com.gisplatform.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.gisplatform.dto.DatasetImportResult;
import com.gisplatform.dto.GisDataParseResult;
import com.gisplatform.entity.Dataset;
import org.springframework.web.multipart.MultipartFile;

import java.io.OutputStream;
import java.util.List;

public interface DatasetService extends IService<Dataset> {

    Page<Dataset> listDatasets(int page, int pageSize, String name, String type, String status);

    Dataset getDatasetById(Long id);

    boolean createDataset(Dataset dataset);

    boolean updateDataset(Dataset dataset);

    boolean deleteDataset(Long id);

    GisDataParseResult parseUploadFile(MultipartFile file, String fileName);

    DatasetImportResult importDataset(MultipartFile file, String fileName, String name, String description, String type, String srs);

    List<Dataset> listPublishedDatasets();

    String getDatasetAsGeoJSON(Long id);

    String getDatasetAsKML(Long id);

    void exportShapefileAsZip(Long id, OutputStream outputStream);

    Dataset publishDataset(Long id);

    Dataset unpublishDataset(Long id);
}

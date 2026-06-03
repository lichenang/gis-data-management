package com.gisplatform.dto;

import lombok.Data;

@Data
public class DatasetImportResult {

    private boolean success;

    private Long datasetId;

    private String message;

    private Integer importedCount;

    public static DatasetImportResult success(Long datasetId, Integer importedCount) {
        DatasetImportResult result = new DatasetImportResult();
        result.setSuccess(true);
        result.setDatasetId(datasetId);
        result.setImportedCount(importedCount);
        result.setMessage("数据集导入成功，共导入 " + importedCount + " 条记录");
        return result;
    }

    public static DatasetImportResult error(String message) {
        DatasetImportResult result = new DatasetImportResult();
        result.setSuccess(false);
        result.setMessage(message);
        return result;
    }
}

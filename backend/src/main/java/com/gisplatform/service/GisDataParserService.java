package com.gisplatform.service;

import com.gisplatform.dto.DatasetImportResult;
import com.gisplatform.dto.GisDataParseResult;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

public interface GisDataParserService {

    /**
     * 解析空间数据文件
     * @param file 空间数据文件
     * @param fileName 文件名
     * @return 解析结果
     */
    GisDataParseResult parseFile(MultipartFile file, String fileName);

    /**
     * 导入空间数据到PostGIS
     * @param file 空间数据文件
     * @param fileName 文件名
     * @param datasetName 数据集名称
     * @param targetSrs 目标坐标系
     * @return 导入结果
     */
    DatasetImportResult importToPostGIS(MultipartFile file, String fileName, String datasetName, String targetSrs);

    /**
     * 获取支持的文件格式
     * @return 支持的格式列表
     */
    String[] getSupportedFormats();
}

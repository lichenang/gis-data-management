package com.gisplatform.service;

import com.gisplatform.dto.DatasetImportResult;
import com.gisplatform.dto.GisDataParseResult;
import org.springframework.web.multipart.MultipartFile;

public interface MultiFormatImportService {

    /**
     * 解析空间数据文件并返回元数据信息
     *
     * @param file     空间数据文件
     * @param fileName 文件名
     * @return 解析结果，包含格式、要素数量、边界等信息
     */
    GisDataParseResult parseFile(MultipartFile file, String fileName);

    /**
     * 导入空间数据到PostGIS数据库
     *
     * @param file        空间数据文件
     * @param fileName    文件名
     * @param datasetName 数据集名称
     * @param targetSrs   目标坐标系（EPSG编码）
     * @param sourceSrs   源坐标系（EPSG编码），当文件本身无法识别坐标系时使用
     * @return 导入结果，包含数据集ID和导入记录数
     */
    DatasetImportResult importToPostGIS(MultipartFile file, String fileName, String datasetName, String targetSrs, String sourceSrs);

    /**
     * 获取支持的文件格式列表
     *
     * @return 支持的格式扩展名数组
     */
    String[] getSupportedFormats();
}

package com.gisplatform.service;

import com.gisplatform.dto.TilePackageRequest;

import java.io.OutputStream;

/**
 * 切片包下载服务接口。
 * <p>
 * 将指定影像图层的 GeoWebCache 缓存切片打包为 ZIP 文件流式输出。
 * </p>
 */
public interface TilePackageService {

    /**
     * 打包指定影像图层的切片为 ZIP 并写入输出流。
     *
     * @param datasetId    影像数据集 ID
     * @param request      请求参数（zoom 范围、地理范围）
     * @param outputStream 输出流（接收 ZIP 数据）
     */
    void packageTiles(Long datasetId, TilePackageRequest request, OutputStream outputStream);
}

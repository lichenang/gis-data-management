package com.gisplatform.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.gisplatform.dto.ImageDownloadUrlResponse;
import com.gisplatform.entity.Dataset;
import com.gisplatform.entity.ImageWmsInfo;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * 影像数据集服务接口。
 * <p>
 * 提供影像上传、元数据解析、列表查询等核心功能。
 * </p>
 *
 * @author GIS Platform Team
 * @since 1.0.0
 */
public interface ImageService extends IService<Dataset> {

    /**
     * 上传影像文件。
     * <p>
     * 流程：解析 GeoTIFF 元数据 → 上传到 MinIO → 写入 Dataset 和 raster_metadata 表
     * </p>
     *
     * @param file        影像文件（GeoTIFF 格式）
     * @param name        影像名称（可选，为空时使用文件名）
     * @param description 描述信息（可选）
     * @return 上传成功后的 Dataset 对象
     */
    Dataset uploadImage(MultipartFile file, String name, String description);

    /**
     * 分页查询影像数据集列表。
     *
     * @param page     当前页码（从 1 开始）
     * @param pageSize 每页记录数
     * @param name     名称模糊查询条件（可选）
     * @return 分页结果
     */
    Page<Dataset> listImages(int page, int pageSize, String name);

    /**
     * 发布影像数据集为 GeoServer 图层并触发切片。
     *
     * @param id 数据集 ID
     * @return 发布后的 Dataset 对象
     */
    Dataset publishImageDataset(Long id);

    /**
     * 取消发布影像数据集。
     *
     * @param id 数据集 ID
     * @return 取消发布后的 Dataset 对象
     */
    Dataset unpublishImageDataset(Long id);

    /**
     * 手动触发重新切片。
     *
     * @param id 数据集 ID
     * @return 切片任务 ID
     */
    String triggerRetile(Long id);

    /**
     * 获取切片状态。
     *
     * @param id 数据集 ID
     * @return 状态信息 Map
     */
    Map<String, Object> getTilingStatus(Long id);

    /**
     * 获取已发布的影像数据集列表。
     *
     * @return 已发布且未删除的影像数据集列表
     */
    List<Dataset> listPublishedImages();

    /**
     * 获取影像图层的 WMS 访问信息。
     *
     * @param id 数据集 ID
     * @return WMS 信息（包含 URL、图层名、坐标系、透明度）
     */
    ImageWmsInfo getImageWmsInfo(Long id);

    /**
     * 获取影像原始文件下载 URL（MinIO 预签名）。
     *
     * @param id 数据集 ID
     * @return 下载 URL 响应
     */
    ImageDownloadUrlResponse getDownloadUrl(Long id);

    /**
     * 删除影像数据集（逻辑删除）。
     *
     * @param id 数据集 ID
     */
    void deleteImage(Long id);

    /**
     * 获取影像元数据。
     *
     * @param id 数据集 ID
     * @return 元数据 Map（包含 dataset 和 raster_metadata 信息）
     */
    Map<String, Object> getMetadata(Long id);
}

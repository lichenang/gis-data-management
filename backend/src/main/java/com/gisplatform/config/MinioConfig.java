package com.gisplatform.config;

import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MinIO 对象存储配置类。
 * <p>
 * 提供 MinioClient 和影像存储桶名称的 Spring Bean。
 * 配置来源：application.yml 中的 minio.* 属性。
 * </p>
 * <p>
 * 配置项说明：
 * <ul>
 *   <li>minio.endpoint - MinIO 服务地址</li>
 *   <li>minio.accessKey - Access Key</li>
 *   <li>minio.secretKey - Secret Key</li>
 *   <li>minio.bucket-raster - 影像存储桶名称（默认 gis-raster）</li>
 * </ul>
 * </p>
 *
 * @author GIS Platform Team
 * @since 1.0.0
 */
@Configuration
public class MinioConfig {

    /**
     * MinIO 服务地址。
     * 配置键：minio.endpoint
     */
    @Value("${minio.endpoint:}")
    private String endpoint;

    /**
     * MinIO Access Key。
     * 配置键：minio.accessKey
     */
    @Value("${minio.accessKey:}")
    private String accessKey;

    /**
     * MinIO Secret Key。
     * 配置键：minio.secretKey
     */
    @Value("${minio.secretKey:}")
    private String secretKey;

    /**
     * 影像存储桶名称。
     * 配置键：minio.bucket-raster，默认值：gis-raster
     */
    @Value("${minio.bucket-raster:gis-raster}")
    private String bucketRaster;

    /**
     * 创建 MinioClient Bean。
     *
     * @return MinioClient 实例（配置为空时返回 null）
     */
    @Bean
    public MinioClient minioClient() {
        if (endpoint == null || endpoint.isEmpty()) {
            return null;
        }
        return MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
    }

    /**
     * 获取影像存储桶名称。
     *
     * @return 桶名称字符串
     */
    @Bean
    public String rasterBucket() {
        return bucketRaster;
    }
}

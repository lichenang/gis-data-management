package com.gisplatform.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 切片包下载配置。
 * <p>
 * 对应 application.yml 中 tile-package 前缀的配置项。
 * </p>
 */
@Component
@ConfigurationProperties(prefix = "tile-package")
@Data
public class TilePackageProperties {

    /**
     * 最大瓦片数量，超过此数量拒绝打包。
     */
    private int maxTiles = 100000;

    /**
     * 默认结束缩放级别。
     */
    private int defaultZoomStop = 14;
}

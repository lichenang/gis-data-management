package com.gisplatform.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "geoserver")
@Data
public class GeoServerProperties {

    private String url = "http://localhost:8080/geoserver";

    private String username = "admin";

    private String password = "geoserver";

    private String workspace = "gisplatform";

    private int tilingMinZoom = 12;

    private int tilingMaxZoom = 18;

    /**
     * GeoServer data_dir 物理路径（如 /opt/geoserver/data_dir）。
     * 用于直接读取 GWC 切片缓存等文件系统操作。
     * 未配置时切片包下载功能不可用。
     */
    private String dataDir = "";
}

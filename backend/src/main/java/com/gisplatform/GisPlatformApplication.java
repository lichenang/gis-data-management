package com.gisplatform;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * GIS 平台应用主类
 * <p>
 * 地理信息数据管理平台，基于 Spring Boot 3.5.x 构建，
 * 集成 PostgreSQL/PostGIS、GeoServer、MinIO 等组件。
 * </p>
 *
 * @author GIS Platform Team
 * @version 1.0.0
 */
@SpringBootApplication
@MapperScan("com.gisplatform.mapper")
public class GisPlatformApplication {

    /**
     * 应用入口方法
     *
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        SpringApplication.run(GisPlatformApplication.class, args);
    }

}

package com.gisplatform.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * SpringDoc OpenAPI + Knife4j 配置类
 * <p>
 * 配置 API 文档的基本信息和安全方案。
 * 访问地址：
 * - /doc.html (Knife4j UI)
 * - /swagger-ui.html (SpringDoc UI)
 * - /v3/api-docs (OpenAPI JSON)
 * </p>
 *
 * @author GIS Platform Team
 * @version 1.0.0
 */
@Configuration
public class OpenApiConfig {

    /**
     * API 标题
     */
    @Value("${springdoc.api.title:GIS Platform API}")
    private String apiTitle;

    /**
     * API 描述
     */
    @Value("${springdoc.api.description:地理信息数据管理平台接口文档}")
    private String apiDescription;

    /**
     * API 版本
     */
    @Value("${springdoc.api.version:1.0.0}")
    private String apiVersion;

    /**
     * 联系人名称
     */
    @Value("${springdoc.api.contact.name:GIS Platform Team}")
    private String contactName;

    /**
     * 创建 OpenAPI 配置
     *
     * @return OpenAPI 配置对象
     */
    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                // API 信息
                .info(new Info()
                        .title(apiTitle)
                        .description(apiDescription)
                        .version(apiVersion)
                        .contact(new Contact()
                                .name(contactName)
                                .email("gis-platform@example.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")))
                // 全局安全方案（JWT）
                .schemaRequirement("Bearer Auth", new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("JWT Token 认证"))
                // 添加全局安全要求
                .addSecurityItem(new SecurityRequirement().addList("Bearer Auth"));
    }

    /**
     * OpenAPI 自定义配置
     * <p>
     * 用于增强 OpenAPI 文档的显示效果。
     * </p>
     *
     * @return OpenApiCustomizer 实例
     */
    @Bean
    public OpenApiCustomizer openApiCustomizer() {
        return openApi -> {
            // 可以在这里添加全局的 tags、paths 等自定义配置
        };
    }

}

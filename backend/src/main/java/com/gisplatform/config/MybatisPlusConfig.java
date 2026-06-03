package com.gisplatform.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.type.TypeHandler;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;

/**
 * MyBatis-Plus 配置类
 * <p>
 * 配置 MyBatis-Plus 的核心组件，包括：
 * 1. 分页插件
 * 2. 自动填充处理器
 * 3. Geometry 类型处理器注册
 * </p>
 *
 * @author GIS Platform Team
 * @version 1.0.0
 */
@Configuration
@MapperScan("com.gisplatform.mapper")
public class MybatisPlusConfig {

    /**
     * MyBatis-Plus 插件配置
     * <p>
     * 配置分页插件，支持分页查询。
     * </p>
     *
     * @return MybatisPlusInterceptor 实例
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // 添加分页插件，指定数据库类型为 PostgreSQL
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.POSTGRE_SQL));
        return interceptor;
    }

    /**
     * 自动填充处理器
     * <p>
     * 在插入和更新时自动填充创建时间、更新时间等字段。
     * </p>
     *
     * @return MetaObjectHandler 实例
     */
    @Bean
    public MetaObjectHandler metaObjectHandler() {
        return new MetaObjectHandler() {
            /**
             * 插入时自动填充
             *
             * @param metaObject 元对象
             */
            @Override
            public void insertFill(MetaObject metaObject) {
                // 填充创建时间
                this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, LocalDateTime.now());
                // 填充更新时间
                this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
            }

            /**
             * 更新时自动填充
             *
             * @param metaObject 元对象
             */
            @Override
            public void updateFill(MetaObject metaObject) {
                // 填充更新时间
                this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
            }
        };
    }

    /**
     * 获取 Geometry 类型处理器
     * <p>
     * 用于处理 PostGIS geometry 类型与 Java 对象的转换。
     * 具体实现需要根据项目需求编写 GeometryTypeHandler 类。
     * </p>
     *
     * @return TypeHandler 实例
     */
    @Bean
    public TypeHandler<?> geometryTypeHandler() {
        // 返回自定义的 GeometryTypeHandler 实例
        // 该处理器需要实现 TypeHandler 接口，处理 org.postgis.Geometry 与 JTS Geometry 的转换
        return new com.gisplatform.common.handler.GeometryTypeHandler();
    }

    /**
     * 获取 Jackson 类型处理器
     * <p>
     * 用于处理 JSON/JSONB 类型与 Java Map 对象的自动转换。
     * 支持实体类中标注 @TableField(typeHandler = JacksonTypeHandler.class) 的字段。
     * </p>
     *
     * @return TypeHandler 实例
     */
    @Bean
    public TypeHandler<?> jacksonTypeHandler() {
        return new JacksonTypeHandler(Object.class);
    }

}

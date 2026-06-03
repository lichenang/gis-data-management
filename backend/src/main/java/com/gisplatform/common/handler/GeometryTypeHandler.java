package com.gisplatform.common.handler;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.io.WKBReader;
import org.locationtech.jts.io.WKBWriter;
import org.locationtech.jts.io.WKTReader;
import org.locationtech.jts.io.WKTWriter;

import java.sql.*;

/**
 * Geometry 类型处理器
 * <p>
 * 用于处理 PostGIS geometry 类型与 JTS Geometry 对象之间的相互转换。
 * 支持 WKB（Well-Known Binary）格式的编解码。
 * </p>
 *
 * @author GIS Platform Team
 * @version 1.0.0
 */
@MappedTypes({Geometry.class})
@MappedJdbcTypes(JdbcType.OTHER)
public class GeometryTypeHandler extends BaseTypeHandler<Geometry> {

    /**
     * JTS GeometryFactory，用于创建几何对象
     */
    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory();

    /**
     * WKT 读写器
     */
    private static final WKTReader WKT_READER = new WKTReader(GEOMETRY_FACTORY);
    private static final WKTWriter WKT_WRITER = new WKTWriter();

    /**
     * WKB 读写器
     */
    private static final WKBReader WKB_READER_3D = new WKBReader(GEOMETRY_FACTORY);
    private static final WKBWriter WKB_WRITER_3D = new WKBWriter(3, true);

    /**
     * 设置非空参数的 PreparedStatement
     *
     * @param ps       PreparedStatement 对象
     * @param parameter Geometry 对象
     * @param i        参数索引
     * @throws SQLException SQL 异常
     */
    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, Geometry parameter, JdbcType jdbcType) throws SQLException {
        // 使用 WKB 格式写入数据库
        byte[] wkb = WKB_WRITER_3D.write(parameter);
        // PostGIS 使用 oid 为 0 的几何类型，驱动会自动处理
        ps.setBytes(i, wkb);
    }

    /**
     * 从 ResultSet 中获取几何对象
     *
     * @param rs         ResultSet 对象
     * @param columnName 列名
     * @return Geometry 对象，可能为 null
     * @throws SQLException SQL 异常
     */
    @Override
    public Geometry getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return parseGeometry(rs.getBytes(columnName));
    }

    /**
     * 从 ResultSet 中获取几何对象（通过列索引）
     *
     * @param rs          ResultSet 对象
     * @param columnIndex 列索引
     * @return Geometry 对象，可能为 null
     * @throws SQLException SQL 异常
     */
    @Override
    public Geometry getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return parseGeometry(rs.getBytes(columnIndex));
    }

    /**
     * 从 CallableStatement 中获取几何对象
     *
     * @param cs          CallableStatement 对象
     * @param columnIndex 列索引
     * @return Geometry 对象，可能为 null
     * @throws SQLException SQL 异常
     */
    @Override
    public Geometry getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return parseGeometry(cs.getBytes(columnIndex));
    }

    /**
     * 解析二进制几何数据
     *
     * @param bytes 二进制数据
     * @return Geometry 对象，解析失败返回 null
     */
    private Geometry parseGeometry(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        try {
            // 使用 WKB 读取器解析
            return WKB_READER_3D.read(bytes);
        } catch (Exception e) {
            // 解析失败记录日志
            return null;
        }
    }

}

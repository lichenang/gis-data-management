package com.gisplatform.service;

import com.gisplatform.common.enums.VectorFileFormat;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * 矢量数据格式自动识别器
 */
@Component
public class FormatDetector {
    
    private static final Logger logger = LoggerFactory.getLogger(FormatDetector.class);
    
    private static final int JSON_PREVIEW_SIZE = 4096;
    
    /**
     * 自动识别文件格式
     *
     * @param file     上传的文件
     * @param filename 文件名
     * @return 识别到的格式
     */
    public VectorFileFormat detect(MultipartFile file, String filename) {
        String extension = FilenameUtils.getExtension(filename);
        VectorFileFormat format = VectorFileFormat.fromExtension(extension);
        
        if (format == VectorFileFormat.UNKNOWN) {
            return VectorFileFormat.UNKNOWN;
        }
        
        if (format == VectorFileFormat.GEOJSON || format == VectorFileFormat.TOPOJSON) {
            return detectJsonType(file);
        }
        
        if (format == VectorFileFormat.SHAPEFILE && filename.toLowerCase().endsWith(".zip")) {
            VectorFileFormat detectedFormat = detectZipContent(file);
            return detectedFormat != null ? detectedFormat : VectorFileFormat.UNKNOWN;
        }
        
        if (format == VectorFileFormat.KML && filename.toLowerCase().endsWith(".kmz")) {
            return VectorFileFormat.KML;
        }
        
        return format;
    }
    
    /**
     * 根据内容检测 JSON 类型 (GeoJSON vs TopoJSON)
     */
    private VectorFileFormat detectJsonType(MultipartFile file) {
        try {
            String preview = readFirstChars(file.getInputStream(), JSON_PREVIEW_SIZE);
            if (preview.contains("\"type\"") && preview.contains("\"Topology\"")) {
                return VectorFileFormat.TOPOJSON;
            }
            if (preview.contains("\"FeatureCollection\"") || preview.contains("\"Feature\"")) {
                return VectorFileFormat.GEOJSON;
            }
            if (preview.contains("\"Point\"") || preview.contains("\"LineString\"") 
                    || preview.contains("\"Polygon\"") || preview.contains("\"Multi")) {
                return VectorFileFormat.GEOJSON;
            }
            return VectorFileFormat.GEOJSON;
        } catch (Exception e) {
            logger.warn("检测 JSON 类型失败", e);
            return VectorFileFormat.GEOJSON;
        }
    }
    
    private static final Charset[] SUPPORTED_CHARSETS = {
        StandardCharsets.UTF_8,
        Charset.forName("GBK"),
        Charset.forName("GB18030"),
        StandardCharsets.ISO_8859_1,
        StandardCharsets.US_ASCII
    };

    public static Charset detectZipCharset(byte[] zipData) {
        for (Charset charset : SUPPORTED_CHARSETS) {
            if (canReadZipEntries(zipData, charset)) {
                logger.debug("检测到 ZIP 编码: {}", charset.name());
                return charset;
            }
        }
        logger.warn("无法检测 ZIP 编码，使用默认 UTF-8");
        return StandardCharsets.UTF_8;
    }

    private static boolean canReadZipEntries(byte[] zipData, Charset charset) {
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipData), charset)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.getName().length() > 0) {
                    return true;
                }
                zis.closeEntry();
            }
        } catch (Exception e) {
            logger.debug("编码 {} 无法读取 ZIP: {}", charset.name(), e.getMessage());
        }
        return false;
    }

    /**
     * 检测 ZIP 包内容
     */
    private VectorFileFormat detectZipContent(MultipartFile file) {
        try {
            byte[] zipData = file.getBytes();
            Charset detectedCharset = detectZipCharset(zipData);
            return detectZipWithCharset(zipData, detectedCharset);
        } catch (Exception e) {
            logger.warn("检测 ZIP 内容失败: {}", e.getMessage());
        }
        return VectorFileFormat.UNKNOWN;
    }

    private VectorFileFormat detectZipWithCharset(MultipartFile file, Charset charset) {
        try (ZipInputStream zis = new ZipInputStream(file.getInputStream(), charset)) {
            return scanZipForFormat(zis);
        } catch (Exception e) {
            logger.debug("使用 {} 检测 ZIP 内容失败: {}", charset.name(), e.getMessage());
        }
        return null;
    }

    private VectorFileFormat detectZipWithCharset(byte[] zipData, Charset charset) {
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipData), charset)) {
            return scanZipForFormat(zis);
        } catch (Exception e) {
            logger.debug("使用 {} 检测 ZIP 内容失败: {}", charset.name(), e.getMessage());
        }
        return null;
    }

    private VectorFileFormat scanZipForFormat(ZipInputStream zis) throws IOException {
        ZipEntry entry;
        while ((entry = zis.getNextEntry()) != null) {
            String name = entry.getName().toLowerCase();
            if (name.endsWith(".shp")) {
                return VectorFileFormat.SHAPEFILE;
            }
            if (name.endsWith(".kml")) {
                return VectorFileFormat.KML;
            }
            zis.closeEntry();
        }
        return null;
    }
    
    /**
     * 读取输入流的前 N 个字符
     */
    private String readFirstChars(InputStream is, int length) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            char[] buffer = new char[length];
            int read = reader.read(buffer);
            if (read > 0) {
                sb.append(buffer, 0, read);
            }
        }
        return sb.toString();
    }
    
    /**
     * 获取文件扩展名
     */
    public String getExtension(String filename) {
        return FilenameUtils.getExtension(filename).toLowerCase();
    }
}

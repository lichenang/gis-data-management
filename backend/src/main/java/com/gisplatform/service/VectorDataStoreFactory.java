package com.gisplatform.service;

import com.gisplatform.common.enums.VectorFileFormat;
import org.apache.commons.io.FileUtils;
import org.geotools.api.data.DataStore;
import org.geotools.api.data.DataStoreFinder;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.impl.CoordinateArraySequenceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Component
public class VectorDataStoreFactory {

    private static final Logger logger = LoggerFactory.getLogger(VectorDataStoreFactory.class);

    private final GeometryFactory geometryFactory = new GeometryFactory(
            CoordinateArraySequenceFactory.instance()
    );

    public DataStore createDataStore(VectorFileFormat format, MultipartFile file, String filename) throws IOException {
        Map<String, Object> params = new HashMap<>();

        switch (format) {
            case SHAPEFILE:
                return createShapefileDataStore(file, filename);
            case GEOJSON:
                return createGeoJSONDataStore(file);
            case KML:
                return createKMLDataStore(file);
            case GML:
                return createGMLDataStore(file);
            case GPX:
                return createGPXDataStore(file);
            case CSV:
                return createCSVDataStore(file);
            default:
                throw new UnsupportedOperationException("不支持的格式: " + format);
        }
    }

    private DataStore createShapefileDataStore(MultipartFile file, String filename) throws IOException {
        File shpFile;
        if (filename != null && filename.toLowerCase().endsWith(".zip")) {
            File tempDir = new File(FileUtils.getTempDirectory(), "shapefile_" + System.currentTimeMillis());
            tempDir.mkdirs();
            byte[] zipData = file.getBytes();
            extractZipToTempDir(zipData, tempDir);
            shpFile = findFile(tempDir, ".shp");
            if (shpFile == null) {
                throw new IOException("ZIP 包中未找到 .shp 文件");
            }
        } else {
            shpFile = extractToTempFile(file, ".shp");
        }

        URI uri = shpFile.toURI();
        Map<String, Object> params = new HashMap<>();
        params.put("url", uri.toURL());

        DataStore dataStore = DataStoreFinder.getDataStore(params);
        if (dataStore == null) {
            throw new IOException("无法创建 ShapefileDataStore");
        }
        return dataStore;
    }

    private DataStore createGeoJSONDataStore(MultipartFile file) throws IOException {
        Map<String, Object> params = new HashMap<>();
        params.put("url", file.getInputStream());
        return DataStoreFinder.getDataStore(params);
    }

    private DataStore createKMLDataStore(MultipartFile file) throws IOException {
        Map<String, Object> params = new HashMap<>();
        params.put("url", file.getInputStream());
        return DataStoreFinder.getDataStore(params);
    }

    private DataStore createGMLDataStore(MultipartFile file) throws IOException {
        Map<String, Object> params = new HashMap<>();
        params.put("url", file.getInputStream());
        params.put("GML_REMOVE_NULL_PROPERTIES", true);
        return DataStoreFinder.getDataStore(params);
    }

    private DataStore createGPXDataStore(MultipartFile file) throws IOException {
        Map<String, Object> params = new HashMap<>();
        params.put("url", file.getInputStream());
        params.put("GPX_USE_EXTENSIONS", true);
        return DataStoreFinder.getDataStore(params);
    }

    private DataStore createCSVDataStore(MultipartFile file) throws IOException {
        Map<String, Object> params = new HashMap<>();
        params.put("url", file.getInputStream());
        params.put("csvfile", file.getInputStream());
        params.put("latfield", "latitude");
        params.put("lonfield", "longitude");
        return DataStoreFinder.getDataStore(params);
    }

    public InputStream getInputStream(MultipartFile file, VectorFileFormat format) throws IOException {
        String filename = file.getOriginalFilename();

        if (format == VectorFileFormat.SHAPEFILE && filename != null && filename.toLowerCase().endsWith(".zip")) {
            File tempDir = new File(FileUtils.getTempDirectory(), "shapefile_" + System.currentTimeMillis());
            tempDir.mkdirs();
            byte[] zipData = file.getBytes();
            extractZipToTempDir(zipData, tempDir);

            File shpFile = findFile(tempDir, ".shp");
            if (shpFile == null) {
                throw new IOException("ZIP 包中未找到 .shp 文件");
            }
            return new FileInputStream(shpFile);
        }

        return file.getInputStream();
    }
    
    private File extractZipToTempDir(byte[] zipData, File tempDir) throws IOException {
        Charset charset = FormatDetector.detectZipCharset(zipData);
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipData), charset)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }
                
                File outFile = new File(tempDir, entry.getName());
                File parent = outFile.getParentFile();
                if (parent != null && !parent.exists()) {
                    parent.mkdirs();
                }
                
                try (FileOutputStream fos = new FileOutputStream(outFile)) {
                    byte[] buffer = new byte[8192];
                    int len;
                    while ((len = zis.read(buffer)) > 0) {
                        fos.write(buffer, 0, len);
                    }
                }
                zis.closeEntry();
            }
        }
        return tempDir;
    }
    
    private File findFile(File dir, String extension) {
        File[] files = dir.listFiles();
        if (files == null) return null;

        for (File file : files) {
            if (file.getName().toLowerCase().endsWith(extension)) {
                return file;
            }
        }
        return null;
    }

    private File extractToTempFile(MultipartFile file, String suffix) throws IOException {
        File tempFile = new File(FileUtils.getTempDirectory(), "upload_" + System.currentTimeMillis() + suffix);
        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
            fos.write(file.getBytes());
        }
        return tempFile;
    }
}

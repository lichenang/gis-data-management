package com.gisplatform.service.geoserver;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class GeoServerCoverageStoreService {

    @Autowired
    private GeoServerClient client;

    /**
     * 创建 GeoTIFF coverage store（文件直传方式）。
     * <p>
     * PUT file → /rest/workspaces/{ws}/coveragestores/{store}/
     * file.geotiff?configure=first&coverageName={store}
     * GeoServer 将文件保存到本地数据目录，自动创建 store、coverage 和 layer。
     *
     * @param workspace 工作区名称
     * @param storeName coverage store 名称
     * @param fileData  GeoTIFF 文件二进制数据
     */
    public void createImageMosaicStore(String workspace, String storeName,
                                        byte[] fileData) {
        String endpoint = "/rest/workspaces/" + workspace +
            "/coveragestores/" + storeName +
            "/file.geotiff?configure=first&coverageName=" + storeName;
        log.info("Uploading GeoTIFF to coverage store: {} ({} bytes)", storeName, fileData.length);
        try {
            client.exchangeWithBinary(endpoint, HttpMethod.PUT, fileData);
            log.info("Coverage store {} created/configured via file upload", storeName);
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload GeoTIFF for coverage store: " +
                storeName, e);
        }
    }

    public void deleteStore(String workspace, String storeName) {
        try {
            client.delete("/rest/workspaces/" + workspace + "/coveragestores/" + storeName, String.class);
            log.info("Deleted coverage store: {}", storeName);
        } catch (Exception e) {
            log.warn("Failed to delete coverage store: {}", storeName, e);
        }
    }
}

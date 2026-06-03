package com.gisplatform.service.geoserver;

import com.gisplatform.config.GeoServerProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class GeoServerLayerService {

    @Autowired
    private GeoServerClient client;

    @Autowired
    private GeoServerProperties props;

    public void publishLayer(String workspace, String storeName, String layerName, String title) {
        if (layerExists(workspace, layerName)) {
            log.info("Layer {} already exists", layerName);
            return;
        }

        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        xml.append("<coverage>");
        xml.append("<name>").append(layerName).append("</name>");
        xml.append("<title>").append(title).append("</title>");
        xml.append("<description>Published from GIS Platform</description>");
        xml.append("<store>").append(storeName).append("</store>");
        xml.append("<nativeCoverageName>").append(storeName).append("</nativeCoverageName>");
        xml.append("<enabled>true</enabled>");
        xml.append("</coverage>");

        try {
            client.post("/rest/workspaces/" + workspace + "/coveragestores/" + storeName + "/coverages", xml.toString(), String.class);
            log.info("Published layer: {} from store: {}", layerName, storeName);
        } catch (Exception e) {
            throw new RuntimeException("Failed to publish layer: " + layerName, e);
        }
    }

    public boolean layerExists(String workspace, String layerName) {
        try {
            client.get("/rest/workspaces/" + workspace + "/layers/" + layerName, String.class);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public void unpublishLayer(String workspace, String layerName) {
        try {
            client.delete("/rest/workspaces/" + workspace + "/layers/" + layerName, String.class);
            log.info("Unpublished layer: {}", layerName);
        } catch (Exception e) {
            log.warn("Failed to unpublish layer: {}", layerName, e);
        }
    }

    public String getWmsUrl(String workspace, String layerName) {
        return "/geoserver" + "/" + workspace + "/" + layerName + "/wms";
    }

    public String getWmtsUrl(String workspace, String layerName) {
        return "/geoserver" + "/" + workspace + "/" + layerName + "/wmts";
    }

    public void deleteCoverageStore(String workspace, String storeName) {
        try {
            client.delete(
                "/rest/workspaces/" + workspace + "/coveragestores/" + storeName + "?recurse=true",
                String.class
            );
            log.info("Deleted coverage store: {} with all layers", storeName);
        } catch (Exception e) {
            log.warn("Failed to delete coverage store: {}", storeName, e);
        }
    }
}

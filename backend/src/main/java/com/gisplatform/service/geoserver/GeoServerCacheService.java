package com.gisplatform.service.geoserver;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gisplatform.config.GeoServerProperties;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import java.util.HashMap;
import java.util.Map;
import org.springframework.util.MultiValueMap;
import org.springframework.util.LinkedMultiValueMap;

@Slf4j
@Service
public class GeoServerCacheService {

    @Autowired
    private GeoServerClient client;

    @Autowired
    private GeoServerProperties props;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Data
    public static class SeedRequest {
        private String name;
        private String zoomStart;
        private String zoomStop;
        private String format;
        private String bounds;
        private String threadCount;
    }

    public void seedLayer(String workspace, String layerName, int minZoom, int maxZoom) {
        String layerId = workspace + ":" + layerName;

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("name", layerId);
        params.add("zoomStart", String.valueOf(minZoom));
        params.add("zoomStop", String.valueOf(maxZoom));
        params.add("format", "image/png");
        params.add("bounds", "-180,-90,180,90");
        params.add("threadCount", "4");
        params.add("type", "seed");

        try {
            client.exchangeWithContentType(
                "/gwc/rest/seed/" + layerId,
                HttpMethod.POST,
                params,
                String.class,
                MediaType.APPLICATION_FORM_URLENCODED
            );
            log.info("Started seed task for layer: {} (zoom levels {}-{})", layerId, minZoom, maxZoom);
        } catch (Exception e) {
            log.error("Failed to start seed task for layer: {}", layerId, e);
            throw new RuntimeException("Failed to start GWC seed task for layer: " + layerId, e);
        }
    }

    public Map<String, Object> getSeedStatus(String workspace, String layerName) {
        String layerId = workspace + ":" + layerName;
        String url = "/gwc/rest/seed/" + layerId + ".json";

        try {
            String response = client.get(url, String.class);
            Map<String, Object> result = new HashMap<>();

            JsonNode root = objectMapper.readTree(response);
            JsonNode longNode = root.get("long");

            if (longNode != null) {
                result.put("status", longNode.has("status") ? longNode.get("status").asText() : "UNKNOWN");
                result.put("tilesTotal", longNode.has("tilesTotal") ? longNode.get("tilesTotal").asLong() : 0L);
                result.put("tilesCached", longNode.has("tilesCached") ? longNode.get("tilesCached").asLong() : 0L);
                result.put("progress", longNode.has("progress") ? longNode.get("progress").asInt() : 0);
            } else {
                result.put("status", "UNKNOWN");
                result.put("progress", 100);
            }

            return result;
        } catch (Exception e) {
            log.warn("Failed to get seed status for layer: {}", layerId, e);
            return null;
        }
    }

    public String getWmtsBaseUrl() {
        return props.getUrl() + "/gwc/service/wmts";
    }

    public String getTileUrl(String workspace, String layerName, int z, int x, int y) {
        return String.format("%s/%s/%s/%d/%d/%d.png",
            getWmtsBaseUrl(), workspace, layerName, z, x, y);
    }

    public void deleteLayer(String workspace, String layerName) {
        String layerId = workspace + ":" + layerName;
        try {
            client.delete("/gwc/rest/layers/" + layerId, String.class);
            log.info("Deleted GWC layer: {}", layerId);
        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().contains("404")) {
                log.info("GWC layer already deleted or not found: {}", layerId);
            } else {
                log.warn("Failed to delete GWC layer: {}", layerId, e);
            }
        }
    }
}

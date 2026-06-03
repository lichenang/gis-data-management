package com.gisplatform.service.tiling;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gisplatform.config.GeoServerProperties;
import com.gisplatform.entity.Dataset;
import com.gisplatform.service.DatasetService;
import com.gisplatform.service.geoserver.GeoServerCacheService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class TileSeedService {

    @Autowired
    private DatasetService datasetService;

    @Autowired
    private GeoServerCacheService cacheService;

    @Autowired
    private GeoServerProperties props;

    private final RestTemplate restTemplate = new RestTemplate();

    @Async("tilingExecutor")
    public void triggerSeed(Long datasetId, Integer minZoom, Integer maxZoom) {
        log.info("Starting GWC seed task for dataset {} (zoom {}-{})", datasetId, minZoom, maxZoom);

        Dataset dataset = datasetService.getById(datasetId);
        if (dataset == null) {
            log.error("Dataset {} not found", datasetId);
            return;
        }

        String jobId = UUID.randomUUID().toString();
        dataset.setTileJobId(jobId);
        dataset.setTileStatus("processing");
        dataset.setTileProgress(0);
        datasetService.updateById(dataset);

        try {
            int startZoom = minZoom != null ? minZoom : props.getTilingMinZoom();
            int endZoom = maxZoom != null ? maxZoom : props.getTilingMaxZoom();

            triggerGwcSeedTask(datasetId, startZoom, endZoom);

            String layerName = "raster_" + datasetId;
            pollSeedStatus(datasetId, layerName);

        } catch (Exception e) {
            log.error("Tile seed failed for dataset {}", datasetId, e);
            dataset.setTileStatus("failed");
            dataset.setCacheSeedStatus("idle");
            dataset.setTileProgress(0);
            datasetService.updateById(dataset);
        }
    }

    private void triggerGwcSeedTask(Long datasetId, Integer minZoom, Integer maxZoom) {
        String layerId = props.getWorkspace() + ":raster_" + datasetId;
        String gwcUrl = props.getUrl() + "/gwc/rest/seed/" + layerId + ".xml";

        double[] extent = getImageExtentInWebMercator(datasetId);
        String minX, minY, maxX, maxY;

        if (extent != null) {
            minX = String.valueOf(extent[0]);
            minY = String.valueOf(extent[1]);
            maxX = String.valueOf(extent[2]);
            maxY = String.valueOf(extent[3]);
            log.info("Using image extent bounds for dataset {}: {},{},{},{}", datasetId, minX, minY, maxX, maxY);
        } else {
            minX = "-180.0";
            minY = "-90.0";
            maxX = "180.0";
            maxY = "90.0";
            log.warn("No extent found for dataset {}, using global bounds", datasetId);
        }

        String xmlBody = String.format(
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
            "<seedRequest>" +
            "<name>%s</name>" +
            "<zoomStart>%d</zoomStart>" +
            "<zoomStop>%d</zoomStop>" +
            "<format>image/png</format>" +
            "<bounds>" +
            "<coords>" +
            "<double>%s</double>" +
            "<double>%s</double>" +
            "<double>%s</double>" +
            "<double>%s</double>" +
            "</coords>" +
            "</bounds>" +
            "<gridSetId>EPSG:900913</gridSetId>" +
            "<threadCount>4</threadCount>" +
            "<type>seed</type>" +
            "</seedRequest>",
            layerId, minZoom, maxZoom, minX, minY, maxX, maxY
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_XML);

        String auth = props.getUsername() + ":" + props.getPassword();
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());
        headers.set("Authorization", "Basic " + encodedAuth);

        HttpEntity<String> request = new HttpEntity<>(xmlBody, headers);

        try {
            restTemplate.exchange(gwcUrl, HttpMethod.POST, request, String.class);
            log.info("GWC seed task started for layer: {} (zoom {}-{})", layerId, minZoom, maxZoom);
        } catch (Exception e) {
            log.error("Failed to start GWC seed task for layer: {}", layerId, e);
            throw new RuntimeException("Failed to start GWC seed task", e);
        }
    }

    private double[] getImageExtentInWebMercator(Long datasetId) {
        Dataset dataset = datasetService.getById(datasetId);
        if (dataset == null || dataset.getExtent() == null) {
            return null;
        }

        try {
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> extent = mapper.readValue(dataset.getExtent(), Map.class);

            double minX = ((Number) extent.get("minX")).doubleValue();
            double minY = ((Number) extent.get("minY")).doubleValue();
            double maxX = ((Number) extent.get("maxX")).doubleValue();
            double maxY = ((Number) extent.get("maxY")).doubleValue();

            if (Math.abs(minY) > 10000 || Math.abs(maxY) > 10000) {
                log.info("Extent already in EPSG:3857, using directly");
                return new double[]{minX, minY, maxX, maxY};
            } else {
                return convertToWebMercator(minX, minY, maxX, maxY);
            }
        } catch (Exception e) {
            log.warn("Failed to parse extent for dataset {}: {}", datasetId, e.getMessage());
            return null;
        }
    }

    private double[] convertToWebMercator(double minX, double minY, double maxX, double maxY) {
        double mercatorMinX = minX * 20037508.34 / 180;
        double mercatorMaxX = maxX * 20037508.34 / 180;
        double mercatorMinY = Math.log(Math.tan((90 + minY) * Math.PI / 360)) / Math.PI / 2 * 20037508.34;
        double mercatorMaxY = Math.log(Math.tan((90 + maxY) * Math.PI / 360)) / Math.PI / 2 * 20037508.34;
        return new double[]{mercatorMinX, mercatorMinY, mercatorMaxX, mercatorMaxY};
    }

    private void pollSeedStatus(Long datasetId, String layerName) {
        int maxAttempts = 120;
        int attempt = 0;

        while (attempt < maxAttempts) {
            try {
                java.util.Map<String, Object> status = cacheService.getSeedStatus(props.getWorkspace(), layerName);

                if (status != null) {
                    Integer progress = (Integer) status.get("progress");
                    String seedStatus = (String) status.get("status");

                    Dataset ds = datasetService.getById(datasetId);
                    if (ds != null) {
                        ds.setTileProgress(progress != null ? progress : 0);
                        ds.setCacheSeedStatus("seeding");
                        datasetService.updateById(ds);

                        if (progress != null && progress >= 100) {
                            log.info("Tile seed completed for dataset {} (progress: {}%)", datasetId, progress);
                            ds.setTileStatus("completed");
                            ds.setCacheSeedStatus("seeded");
                            datasetService.updateById(ds);
                            break;
                        }

                        if ("FINISHED".equals(seedStatus) || "DONE".equals(seedStatus) || "SUCCEEDED".equals(seedStatus)) {
                            log.info("Seed task finished for dataset {}", datasetId);
                            ds.setTileStatus("completed");
                            ds.setCacheSeedStatus("seeded");
                            datasetService.updateById(ds);
                            break;
                        }

                        if ("FAILED".equals(seedStatus) || "ERROR".equals(seedStatus)) {
                            log.error("Seed task failed for dataset {}", datasetId);
                            ds.setTileStatus("failed");
                            ds.setCacheSeedStatus("idle");
                            ds.setTileProgress(0);
                            datasetService.updateById(ds);
                            break;
                        }
                    }
                }

                Thread.sleep(5000);
                attempt++;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.warn("Error polling seed status for dataset {}: {}", datasetId, e.getMessage());
                attempt++;
                if (attempt >= 5) break;
            }
        }
    }

    public String triggerRetile(Long datasetId) {
        Dataset dataset = datasetService.getById(datasetId);
        if (dataset == null) {
            throw new RuntimeException("Dataset not found: " + datasetId);
        }

        // Reset status for a fresh tile job
        dataset.setTileStatus("pending");
        dataset.setCacheSeedStatus("idle");
        dataset.setTileProgress(0);
        datasetService.updateById(dataset);

        // Trigger new tiling (async)
        triggerSeed(datasetId, props.getTilingMinZoom(), props.getTilingMaxZoom());

        return dataset.getTileJobId();
    }
}

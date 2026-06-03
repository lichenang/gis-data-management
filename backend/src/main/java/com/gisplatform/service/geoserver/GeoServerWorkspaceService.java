package com.gisplatform.service.geoserver;

import com.gisplatform.config.GeoServerProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class GeoServerWorkspaceService {

    @Autowired
    private GeoServerClient client;

    @Autowired
    private GeoServerProperties props;

    public void createWorkspace(String workspace) {
        if (workspaceExists(workspace)) {
            log.info("Workspace {} already exists", workspace);
            return;
        }

        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
            "<workspace>" +
            "<name>" + workspace + "</name>" +
            "</workspace>";

        try {
            client.post("/rest/workspaces", xml, String.class);
            log.info("Created workspace: {}", workspace);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create workspace: " + workspace, e);
        }
    }

    public boolean workspaceExists(String workspace) {
        try {
            client.get("/rest/workspaces/" + workspace, String.class);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public String getWorkspace() {
        return props.getWorkspace();
    }
}

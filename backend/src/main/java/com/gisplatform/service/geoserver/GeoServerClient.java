package com.gisplatform.service.geoserver;

import com.gisplatform.config.GeoServerProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;

@Slf4j
@Component
public class GeoServerClient {

    @Autowired
    private GeoServerProperties props;

    private static final MediaType IMAGE_TIFF = MediaType.valueOf("image/tiff");

    private final RestTemplate restTemplate;

    public GeoServerClient() {
        this.restTemplate = new RestTemplate();
    }

    public <T> T exchange(String path, HttpMethod method, Object body, Class<T> responseType) {
        return exchangeWithContentType(path, method, body, responseType, MediaType.APPLICATION_XML);
    }

    public <T> T exchangeWithContentType(String path, HttpMethod method, Object body, Class<T> responseType, MediaType contentType) {
        String url = props.getUrl() + path;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(contentType);
        String auth = props.getUsername() + ":" + props.getPassword();
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());
        headers.set("Authorization", "Basic " + encodedAuth);

        HttpEntity<Object> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<T> response = restTemplate.exchange(url, method, request, responseType);
            log.debug("GeoServer API response: {}", response.getStatusCode());
            return response.getBody();
        } catch (HttpStatusCodeException e) {
            String errorBody = e.getResponseBodyAsString();
            log.error("GeoServer error ({}): {}", e.getStatusCode(), errorBody);
            String geoServerMsg = extractGeoServerErrorMessage(errorBody);
            throw new RuntimeException("GeoServer API failed: " + url +
                ", status: " + e.getStatusCode() +
                ", GeoServer error: " + geoServerMsg, e);
        } catch (Exception e) {
            throw new RuntimeException("GeoServer API call failed: " + url + ", error: " + e.getMessage(), e);
        }
    }

    public void exchangeWithBinary(String path, HttpMethod method, byte[] body) {
        String url = props.getUrl() + path;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(IMAGE_TIFF);
        String auth = props.getUsername() + ":" + props.getPassword();
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());
        headers.set("Authorization", "Basic " + encodedAuth);

        HttpEntity<byte[]> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, method, request, String.class);
            log.debug("GeoServer binary upload response: {}", response.getStatusCode());
        } catch (HttpStatusCodeException e) {
            String errorBody = e.getResponseBodyAsString();
            log.error("GeoServer binary upload error ({}): {}", e.getStatusCode(), errorBody);
            String geoServerMsg = extractGeoServerErrorMessage(errorBody);
            throw new RuntimeException("GeoServer binary upload failed: " + url +
                ", status: " + e.getStatusCode() +
                ", error: " + geoServerMsg, e);
        }
    }

    public <T> T get(String path, Class<T> responseType) {
        return exchange(path, HttpMethod.GET, null, responseType);
    }

    public <T> T post(String path, Object body, Class<T> responseType) {
        return exchange(path, HttpMethod.POST, body, responseType);
    }

    public <T> T delete(String path, Class<T> responseType) {
        return exchange(path, HttpMethod.DELETE, null, responseType);
    }

    private String extractGeoServerErrorMessage(String xmlResponse) {
        if (xmlResponse == null || xmlResponse.isEmpty()) {
            return "no details";
        }
        try {
            int msgStart = xmlResponse.indexOf("<message>");
            int msgEnd = xmlResponse.indexOf("</message>");
            if (msgStart >= 0 && msgEnd > msgStart) {
                return xmlResponse.substring(msgStart + 9, msgEnd);
            }
        } catch (Exception ignored) {}
        try {
            int msgStart = xmlResponse.indexOf("\"message\":\"");
            int msgEnd = xmlResponse.indexOf("\"", msgStart + 11);
            if (msgStart >= 0 && msgEnd > msgStart) {
                return xmlResponse.substring(msgStart + 11, msgEnd);
            }
        } catch (Exception ignored) {}
        return xmlResponse.length() > 200 ? xmlResponse.substring(0, 200) + "..." : xmlResponse;
    }

    public boolean isReachable() {
        try {
            restTemplate.getForObject(props.getUrl() + "/rest/workspaces", String.class);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}

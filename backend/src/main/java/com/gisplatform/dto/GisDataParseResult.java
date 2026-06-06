package com.gisplatform.dto;

import lombok.Data;

@Data
public class GisDataParseResult {

    private boolean success;

    private String message;

    private String geometryType;

    private String srs;

    private int featureCount;

    private double[] bounds;

    private String tableName;

    private String format;

    private java.util.List<String> properties;

    private boolean crsDetected;

    public static GisDataParseResult success(String message) {
        GisDataParseResult result = new GisDataParseResult();
        result.setSuccess(true);
        result.setMessage(message);
        return result;
    }

    public static GisDataParseResult error(String message) {
        GisDataParseResult result = new GisDataParseResult();
        result.setSuccess(false);
        result.setMessage(message);
        return result;
    }
}

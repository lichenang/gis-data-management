package com.gisplatform.common.enums;

/**
 * 矢量数据文件格式枚举
 */
public enum VectorFileFormat {
    GEOJSON("GeoJSON", new String[]{"geojson", "json"}, true),
    SHAPEFILE("Shapefile", new String[]{"shp", "zip"}, true),
    KML("KML", new String[]{"kml", "kmz"}, true),
    GML("GML", new String[]{"gml"}, true),
    GPX("GPX", new String[]{"gpx"}, true),
    CSV("CSV", new String[]{"csv"}, true),
    WKT("WKT", new String[]{"wkt"}, false),
    TOPOJSON("TopoJSON", new String[]{"topojson", "json"}, false),
    UNKNOWN("Unknown", new String[]{}, false);

    private final String displayName;
    private final String[] extensions;
    private final boolean geoToolsSupported;

    VectorFileFormat(String displayName, String[] extensions, boolean geoToolsSupported) {
        this.displayName = displayName;
        this.extensions = extensions;
        this.geoToolsSupported = geoToolsSupported;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String[] getExtensions() {
        return extensions;
    }

    public boolean isGeoToolsSupported() {
        return geoToolsSupported;
    }

    public static VectorFileFormat fromExtension(String extension) {
        if (extension == null || extension.isEmpty()) {
            return UNKNOWN;
        }
        String ext = extension.toLowerCase();
        for (VectorFileFormat format : values()) {
            for (String extension2 : format.extensions) {
                if (extension2.equals(ext)) {
                    return format;
                }
            }
        }
        return UNKNOWN;
    }

    public static VectorFileFormat[] getImportFormats() {
        return new VectorFileFormat[]{GEOJSON, SHAPEFILE, KML, GML, GPX, CSV, WKT, TOPOJSON};
    }

    public static VectorFileFormat[] getExportFormats() {
        return new VectorFileFormat[]{GEOJSON, SHAPEFILE, KML, CSV};
    }
}

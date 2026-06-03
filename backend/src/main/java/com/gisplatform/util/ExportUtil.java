package com.gisplatform.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public final class ExportUtil {

    private ExportUtil() {}

    public static String sanitizeFilename(String name) {
        if (name == null) return "export";
        return name.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
    }

    public static String escapeXml(String value) {
        if (value == null) return "";
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    public static void zipFiles(File[] files, String prefix, OutputStream out) throws IOException {
        try (ZipOutputStream zos = new ZipOutputStream(out)) {
            for (File f : files) {
                if (f.getName().startsWith(prefix)) {
                    zos.putNextEntry(new ZipEntry(f.getName()));
                    try (FileInputStream fis = new FileInputStream(f)) {
                        fis.transferTo(zos);
                    }
                    zos.closeEntry();
                }
            }
        }
    }

    public static void deleteDirectory(File dir) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isDirectory()) {
                    deleteDirectory(f);
                }
                f.delete();
            }
        }
        dir.delete();
    }
}

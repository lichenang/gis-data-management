# Design: fix-crs-transform-imports

## Technical Changes

### 1. 更新 import 语句

**变更前**:
```java
import org.geotools.geometry.DirectPosition2D;
```

**变更后**:
```java
// DirectPosition2D 已移除，不再需要
```

### 2. 使用坐标数组替代 DirectPosition2D

**变更前**:
```java
DirectPosition2D minPoint = new DirectPosition2D(extent[0], extent[1]);
DirectPosition2D maxPoint = new DirectPosition2D(extent[2], extent[3]);

DirectPosition2D minTransformed = mathTransform.transform(minPoint, null);
DirectPosition2D maxTransformed = mathTransform.transform(maxPoint, null);

result[0] = minTransformed.x;
result[1] = minTransformed.y;
result[2] = maxTransformed.x;
result[3] = maxTransformed.y;
```

**变更后**:
```java
double[] minPoint = new double[]{extent[0], extent[1]};
double[] maxPoint = new double[]{extent[2], extent[3]};

double[] minResult = new double[2];
double[] maxResult = new double[2];

mathTransform.transform(minPoint, 0, minResult, 0, 1);
mathTransform.transform(maxPoint, 0, maxResult, 0, 1);

result[0] = minResult[0];
result[1] = minResult[1];
result[2] = maxResult[0];
result[3] = maxResult[1];
```

## 完整修复后代码

```java
package com.gisplatform.util;

import org.geotools.referencing.CRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import lombok.extern.slf4j.Slf4j;

import java.util.logging.Level;
import java.util.logging.Logger;

@Slf4j
public class CrsTransformUtil {

    private static final String TARGET_CRS = "EPSG:4326";
    private static final Logger LOGGER = Logger.getLogger(CrsTransformUtil.class.getName());

    public static double[] transformExtentToWgs84(double[] extent, String sourceCrs) {
        if (extent == null || extent.length != 4) {
            LOGGER.log(Level.WARNING, "Invalid extent: " + (extent == null ? "null" : "length=" + extent.length));
            return null;
        }

        if (sourceCrs == null || sourceCrs.isEmpty()) {
            LOGGER.log(Level.WARNING, "Source CRS is empty");
            return null;
        }

        if ("EPSG:4326".equalsIgnoreCase(sourceCrs)) {
            return extent;
        }

        try {
            CoordinateReferenceSystem source = CRS.decode(sourceCrs);
            CoordinateReferenceSystem target = CRS.decode(TARGET_CRS);

            if (source.equals(target)) {
                return extent;
            }

            MathTransform mathTransform = CRS.findMathTransform(source, target);

            double[] minPoint = new double[]{extent[0], extent[1]};
            double[] maxPoint = new double[]{extent[2], extent[3]};

            double[] minResult = new double[2];
            double[] maxResult = new double[2];

            mathTransform.transform(minPoint, 0, minResult, 0, 1);
            mathTransform.transform(maxPoint, 0, maxResult, 0, 1);

            double[] result = new double[4];
            result[0] = minResult[0];
            result[1] = minResult[1];
            result[2] = maxResult[0];
            result[3] = maxResult[1];

            LOGGER.info(String.format("Transformed extent from %s to %s: [%.6f, %.6f, %.6f, %.6f] -> [%.6f, %.6f, %.6f, %.6f]",
                    sourceCrs, TARGET_CRS, extent[0], extent[1], extent[2], extent[3],
                    result[0], result[1], result[2], result[3]));

            return result;

        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to transform extent from " + sourceCrs + " to " + TARGET_CRS + ": " + e.getMessage());
            return null;
        }
    }

    public static boolean isValidCrs(String crsCode) {
        if (crsCode == null || crsCode.isEmpty()) {
            return false;
        }
        try {
            CRS.decode(crsCode);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
```

## Diff

```diff
-import org.geotools.geometry.DirectPosition2D;
+// DirectPosition2D removed - no longer needed

-            DirectPosition2D minPoint = new DirectPosition2D(extent[0], extent[1]);
-            DirectPosition2D maxPoint = new DirectPosition2D(extent[2], extent[3]);
+            double[] minPoint = new double[]{extent[0], extent[1]};
+            double[] maxPoint = new double[]{extent[2], extent[3]};

-            DirectPosition2D minTransformed = mathTransform.transform(minPoint, null);
-            DirectPosition2D maxTransformed = mathTransform.transform(maxPoint, null);
+            double[] minResult = new double[2];
+            double[] maxResult = new double[2];
+            mathTransform.transform(minPoint, 0, minResult, 0, 1);
+            mathTransform.transform(maxPoint, 0, maxResult, 0, 1);

-            result[0] = minTransformed.x;
-            result[1] = minTransformed.y;
-            result[2] = maxTransformed.x;
-            result[3] = maxTransformed.y;
+            result[0] = minResult[0];
+            result[1] = minResult[1];
+            result[2] = maxResult[0];
+            result[3] = maxResult[1];
```

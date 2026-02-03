package com.garganttua.core.dsl;

import java.util.List;

import com.garganttua.core.dsl.annotations.Scan;
import com.garganttua.core.reflection.IAnnotationScanner;

import lombok.extern.slf4j.Slf4j;

/**
 * Helper class for scanning @Scan annotations and adding packages to packageable builders.
 *
 * <p>
 * This utility scans configured packages for classes annotated with @Scan
 * and automatically adds the specified scan packages to the builder if it
 * implements IPackageableBuilder.
 * </p>
 *
 * @since 2.0.0-ALPHA01
 */
@Slf4j
public class PackageScanHelper {

    private PackageScanHelper() {
        // Utility class
    }

    /**
     * Scans packages for @Scan annotations and adds them to the builder if packageable.
     *
     * <p>
     * This method should be called at the beginning of doAutoDetection() for builders
     * that implement both IPackageableBuilder and IAutomaticBuilder.
     * </p>
     *
     * @param scanner the annotation scanner to use for finding @Scan annotations
     * @param builder the builder to add scan packages to
     * @param basePackages the base packages to scan for @Scan annotations
     * @throws DslException if scanning fails
     */
    public static void scanAndAddPackages(IAnnotationScanner scanner, IBuilder<?> builder, String[] basePackages) throws DslException {
        if (scanner == null || builder == null || basePackages == null || basePackages.length == 0) {
            log.atDebug().log("No scanning needed: scanner, builder or basePackages is null/empty");
            return;
        }

        if (!(builder instanceof IPackageableBuilder)) {
            log.atDebug().log("Builder {} does not implement IPackageableBuilder, skipping scan",
                    builder.getClass().getSimpleName());
            return;
        }

        IPackageableBuilder<?, ?> packageableBuilder = (IPackageableBuilder<?, ?>) builder;
        log.atTrace().log("Scanning for @Scan annotations in {} packages", basePackages.length);

        for (String basePackage : basePackages) {
            try {
                List<Class<?>> annotatedClasses = scanner.getClassesWithAnnotation(
                        basePackage, Scan.class);

                log.atDebug().log("Found {} classes with @Scan annotation in package {}",
                        annotatedClasses.size(), basePackage);

                for (Class<?> clazz : annotatedClasses) {
                    Scan scanAnnotation = clazz.getAnnotation(Scan.class);
                    if (scanAnnotation != null) {
                        String scanPackage = scanAnnotation.scan();
                        packageableBuilder.withPackage(scanPackage);
                        log.atDebug().log("Added scan package '{}' from @Scan on class {} to builder {}",
                                scanPackage, clazz.getSimpleName(), builder.getClass().getSimpleName());
                    }
                }
            } catch (Exception e) {
                log.atWarn().log("Failed to scan package {} for @Scan annotations: {}",
                        basePackage, e.getMessage());
                // Continue with other packages even if one fails
            }
        }

        log.atTrace().log("Completed scanning for @Scan annotations");
    }
}

package com.garganttua.core.dsl;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Objects;

import com.garganttua.core.dsl.annotations.Scan;
import com.garganttua.core.reflection.IAnnotationScanner;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IReflection;

import lombok.extern.slf4j.Slf4j;

/**
 * Scans for @Scan annotations and adds discovered packages to packageable builders.
 *
 * <p>
 * Constructed with an {@link IReflection} instance used for annotation scanning.
 * Call {@link #scanAndAddPackages(IBuilder, String[])} to perform the scan.
 * </p>
 *
 * @since 2.0.0-ALPHA01
 */
@Slf4j
public class PackageScanHelper {

    private final IReflection reflection;
    private IClass<? extends Annotation> scanAnnotationClass;

    public PackageScanHelper(IReflection reflection) {
        this.reflection = Objects.requireNonNull(reflection, "IReflection cannot be null");
        this.scanAnnotationClass = this.reflection.getClass(Scan.class);
    }

    /**
     * Scans packages for @Scan annotations and adds them to the builder if packageable.
     *
     * @param builder the builder to add scan packages to
     * @param basePackages the base packages to scan for @Scan annotations
     * @throws DslException if scanning fails
     */
    public void scanAndAddPackages(IBuilder<?> builder, String[] basePackages) throws DslException {
        if (builder == null || basePackages == null || basePackages.length == 0) {
            log.atDebug().log("No scanning needed: builder or basePackages is null/empty");
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
                List<IClass<?>> annotatedClasses = this.reflection.getClassesWithAnnotation(
                        basePackage, this.scanAnnotationClass);

                log.atDebug().log("Found {} classes with @Scan annotation in package {}",
                        annotatedClasses.size(), basePackage);

                for (IClass<?> clazz : annotatedClasses) {
                    Scan scanAnnotation = clazz.getAnnotation(this.reflection.getClass(Scan.class));
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
            }
        }

        log.atTrace().log("Completed scanning for @Scan annotations");
    }

    /**
     * Scans packages for @Scan annotations using a custom scanner instead of the
     * {@link IReflection} instance provided at construction.
     *
     * @param scanner the annotation scanner to use
     * @param builder the builder to add scan packages to
     * @param basePackages the base packages to scan for @Scan annotations
     * @throws DslException if scanning fails
     */
    public void scanAndAddPackages(IAnnotationScanner scanner, IBuilder<?> builder, String[] basePackages) throws DslException {
        Objects.requireNonNull(scanner, "IAnnotationScanner cannot be null");

        if (builder == null || basePackages == null || basePackages.length == 0) {
            log.atDebug().log("No scanning needed: builder or basePackages is null/empty");
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
                List<IClass<?>> annotatedClasses = scanner.getClassesWithAnnotation(basePackage, this.scanAnnotationClass);

                log.atDebug().log("Found {} classes with @Scan annotation in package {}",
                        annotatedClasses.size(), basePackage);

                for (IClass<?> clazz : annotatedClasses) {
                    Scan scanAnnotation = (Scan) clazz.getAnnotation(this.scanAnnotationClass);
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
            }
        }

        log.atTrace().log("Completed scanning for @Scan annotations");
    }
}

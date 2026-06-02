package com.garganttua.core.bootstrap.dsl;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.ServiceLoader;
import java.util.concurrent.atomic.AtomicBoolean;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.observability.Logger;
import com.garganttua.core.reflection.IAnnotationScanner;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IReflectionProvider;
import com.garganttua.core.reflection.dsl.IReflectionBuilder;
import com.garganttua.core.reflection.dsl.ReflectionBuilder;

import jakarta.annotation.Priority;

/**
 * Cold-start SPI discovery for {@link Bootstrap}: assembles a default
 * {@link com.garganttua.core.reflection.IReflection} from {@link ServiceLoader}-
 * discovered {@link IReflectionProvider}s and {@link IAnnotationScanner}s, sorted
 * by {@link Priority} (higher first, default 0). Extracted to keep {@code Bootstrap}
 * focused on orchestration.
 */
final class BootstrapSpiLoader {

    private static final Logger log = Logger.getLogger(BootstrapSpiLoader.class);

    /** Tracks whether the SPI summary line has already been emitted at INFO. */
    private static final AtomicBoolean SPI_SUMMARY_LOGGED = new AtomicBoolean(false);

    private BootstrapSpiLoader() {
    }

    /**
     * Bootstraps a default {@code IReflection} via {@link ServiceLoader} when none
     * has been installed yet. No-op when one is already present, and also no-op when
     * no provider JAR is on the classpath (in which case the downstream
     * {@code IClass.getClass(...)} call surfaces a clear error).
     */
    static void ensureReflectionAvailable() {
        try {
            IClass.getReflection();
            return; // already configured by user code
        } catch (IllegalStateException ignored) {
            // No reflection bound — fall through to SPI bootstrap.
        }
        IReflectionBuilder rb = buildReflectionBuilderFromSpi();
        if (rb != null) {
            try {
                rb.build();
                // ReflectionBuilder.doBuild() calls IClass.setReflection() internally,
                // so the global facade is now configured for subsequent IClass.getClass() calls.
            } catch (DslException e) {
                log.warn("SPI-built ReflectionBuilder failed to build: {}", e.getMessage());
            }
        }
    }

    static IReflectionBuilder buildReflectionBuilderFromSpi() {
        log.trace("Attempting SPI bootstrap of IReflectionBuilder");
        IReflectionBuilder rb = ReflectionBuilder.builder();
        List<String> providerLabels = new ArrayList<>();
        List<String> scannerLabels = new ArrayList<>();

        for (IReflectionProvider provider : sortedByPriority(ServiceLoader.load(IReflectionProvider.class))) {
            int priority = readPriority(provider);
            log.debug("SPI: registering reflection provider {} with priority {}",
                    provider.getClass().getName(), priority);
            rb.withProvider(provider, priority);
            providerLabels.add(provider.getClass().getSimpleName() + "@" + priority);
        }

        for (IAnnotationScanner scanner : sortedByPriority(ServiceLoader.load(IAnnotationScanner.class))) {
            int priority = readPriority(scanner);
            log.debug("SPI: registering annotation scanner {} with priority {}",
                    scanner.getClass().getName(), priority);
            rb.withScanner(scanner, priority);
            scannerLabels.add(scanner.getClass().getSimpleName() + "@" + priority);
        }

        if (providerLabels.isEmpty() && scannerLabels.isEmpty()) {
            log.debug("SPI bootstrap found no IReflectionProvider/IAnnotationScanner on the classpath");
            return null;
        }

        // First invocation logs at INFO (visible by default); subsequent ones at
        // DEBUG to avoid noisy duplicates when Bootstrap re-runs the loader from
        // both the constructor (ensureReflectionAvailable) and the build phase.
        String summary = String.format("SPI bootstrap: providers=%s, scanners=%s", providerLabels, scannerLabels);
        if (SPI_SUMMARY_LOGGED.compareAndSet(false, true)) {
            log.info(summary);
        } else {
            log.debug(summary);
        }
        return rb;
    }

    private static <T> List<T> sortedByPriority(ServiceLoader<T> loader) {
        List<T> list = new ArrayList<>();
        for (T svc : loader) {
            list.add(svc);
        }
        list.sort(Comparator.comparingInt(BootstrapSpiLoader::readPriority).reversed());
        return list;
    }

    private static int readPriority(Object svc) {
        Priority p = svc.getClass().getAnnotation(Priority.class);
        return p == null ? 0 : p.value();
    }
}

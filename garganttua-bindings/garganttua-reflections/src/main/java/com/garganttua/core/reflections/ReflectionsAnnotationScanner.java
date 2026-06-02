package com.garganttua.core.reflections;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.FilterBuilder;

import com.garganttua.core.observability.Logger;
import com.garganttua.core.reflection.IAnnotationScanner;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IMethod;

import jakarta.annotation.Priority;

/**
 * {@link IAnnotationScanner} implementation backed by the
 * <a href="https://github.com/ronmamo/reflections">Reflections</a> library.
 *
 * <p>Discovers classes and methods carrying a given annotation, optionally
 * scoped to a package. {@link Reflections} instances are cached per package to
 * avoid repeated full-classpath scans.
 *
 * <p>Registered as a cold-start SPI provider at {@link Priority} {@code 10}.
 */
@Priority(10)
public class ReflectionsAnnotationScanner implements IAnnotationScanner {
    private static final Logger log = Logger.getLogger(ReflectionsAnnotationScanner.class);

    /**
     * Cache of {@link Reflections} instances keyed by package name. Scanning
     * a package is expensive (50–200ms each) so we instantiate ONCE per
     * package with both TypesAnnotated and MethodsAnnotated scanners
     * pre-enabled, then answer every subsequent query against the cached
     * metadata.
     *
     * <p>Without the cache, BeanProviderBuilder.doAutoDetection — which
     * iterates over every registered qualifier × every scanned package —
     * was triggering a fresh full-classpath scan per call. Easy
     * O(qualifiers × packages) waste.
     */
    private final ConcurrentHashMap<String, Reflections> cache = new ConcurrentHashMap<>();

    private Reflections reflectionsFor(String packageName) {
        return this.cache.computeIfAbsent(packageName == null ? "" : packageName,
                pkg -> {
                    ConfigurationBuilder cfg = new ConfigurationBuilder()
                            .forPackage(pkg)
                            .setScanners(Scanners.TypesAnnotated, Scanners.MethodsAnnotated);
                    // Reflections' forPackage(pkg) only adds the classpath URLs that
                    // contain pkg — it does NOT filter scanned class FQNs. The whole
                    // URL (e.g. target/test-classes/) is then indexed, so siblings
                    // outside pkg leak into every query. Apply an explicit prefix
                    // filter so only classes whose FQN starts with pkg are kept.
                    // Empty pkg means "global scan" — leave the filter off then.
                    if (!pkg.isEmpty()) {
                        cfg.filterInputsBy(new FilterBuilder().includePackage(pkg));
                    }
                    return new Reflections(cfg);
                });
    }

	/**
	 * Returns all classes on the classpath annotated with {@code annotation}.
	 *
	 * @return the matching classes, never {@code null}
	 */
	@Override
	public List<IClass<?>> getClassesWithAnnotation(IClass<? extends Annotation> annotation) {
		return getClassesWithAnnotation("", annotation);
	}

	/**
	 * Returns the classes within {@code packageName} annotated with {@code annotation}.
	 *
	 * @param packageName package to scope the scan to; empty performs a global scan
	 * @return the matching classes, never {@code null}
	 */
	@SuppressWarnings("unchecked")
	@Override
	public List<IClass<?>> getClassesWithAnnotation(String packageName, IClass<? extends Annotation> annotation) {
		log.trace("Entering getClassesWithAnnotation(package={}, annotation={})", packageName, annotation.getName());

		Class<? extends Annotation> rawAnnotation = (Class<? extends Annotation>) annotation.getType();

		log.debug("Scanning package '{}' for classes with annotation '{}'", packageName, annotation.getName());
		Set<Class<?>> annotatedClasses = reflectionsFor(packageName)
		        .getTypesAnnotatedWith(rawAnnotation, true);

		List<IClass<?>> result = new ArrayList<>(annotatedClasses.size());
		for (Class<?> clazz : annotatedClasses) {
			result.add(IClass.getClass(clazz));
		}

		log.debug("Found {} classes annotated with '{}' in package {}", result.size(), annotation.getName(), packageName);
		return result;
	}

	/**
	 * Returns all methods on the classpath annotated with {@code annotation}.
	 *
	 * @return the matching methods, never {@code null}
	 */
	@Override
	public List<IMethod> getMethodsWithAnnotation(IClass<? extends Annotation> annotation) {
		return getMethodsWithAnnotation("", annotation);
	}

	/**
	 * Returns the methods within {@code packageName} annotated with {@code annotation}.
	 *
	 * <p>Methods that cannot be resolved back through the {@link IClass} mirror are
	 * skipped and logged rather than aborting the scan.
	 *
	 * @param packageName package to scope the scan to; empty performs a global scan
	 * @return the matching methods, never {@code null}
	 */
	@SuppressWarnings("unchecked")
	@Override
	public List<IMethod> getMethodsWithAnnotation(String packageName, IClass<? extends Annotation> annotation) {
		log.trace("Entering getMethodsWithAnnotation(package={}, annotation={})", packageName, annotation.getName());

		Class<? extends Annotation> rawAnnotation = (Class<? extends Annotation>) annotation.getType();

		log.debug("Scanning package '{}' for methods with annotation '{}'", packageName, annotation.getName());
		Set<Method> annotatedMethods = reflectionsFor(packageName).getMethodsAnnotatedWith(rawAnnotation);

		List<IMethod> result = new ArrayList<>(annotatedMethods.size());
		for (Method method : annotatedMethods) {
			try {
				IMethod found = IClass.getClass(method.getDeclaringClass()).getMethod(method.getName(), Arrays.stream(method.getParameterTypes()).map(IClass::getClass).toArray(IClass[]::new));
				result.add(found);
			} catch (NoSuchMethodException | SecurityException e) {
				log.warn("Skipping method '{}#{}' that could not be resolved", method.getDeclaringClass().getName(), method.getName(), e);
			}
		}

		log.debug("Found {} methods annotated with '{}' in package {}", result.size(), annotation.getName(), packageName);
		return result;
	}
}

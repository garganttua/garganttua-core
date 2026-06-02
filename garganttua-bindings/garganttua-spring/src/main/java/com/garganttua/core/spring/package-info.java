/**
 * Spring Framework integration for annotation scanning.
 *
 * <h2>Overview</h2>
 * <p>
 * This package provides integration with Spring Framework's annotation scanning
 * capabilities. It enables using Spring's ClassPathScanningCandidateComponentProvider
 * for discovering annotated classes, providing an alternative to the Reflections
 * library integration.
 * </p>
 *
 * <h2>Main Classes</h2>
 * <ul>
 *   <li>{@link com.garganttua.core.spring.SpringAnnotationScanner} - Annotation scanner using Spring's scanning infrastructure</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Create scanner
 * SpringAnnotationScanner scanner = new SpringAnnotationScanner();
 *
 * // Scan a package for classes carrying an annotation
 * List<IClass<?>> serviceClasses = scanner.getClassesWithAnnotation(
 *     "com.myapp.services",
 *     IClass.getClass(Service.class)
 * );
 *
 * // Scan a package for methods carrying an annotation
 * List<IMethod> scheduledMethods = scanner.getMethodsWithAnnotation(
 *     "com.myapp",
 *     IClass.getClass(Scheduled.class)
 * );
 * }</pre>
 *
 * <h2>Features</h2>
 * <ul>
 *   <li>Spring-based annotation scanning</li>
 *   <li>Package-based filtering</li>
 *   <li>Meta-annotation support</li>
 *   <li>Include/exclude filters</li>
 *   <li>Classpath scanning</li>
 * </ul>
 *
 * <h2>Integration</h2>
 * <p>
 * This scanner provides an alternative annotation scanning strategy for
 * the dependency injection framework, particularly useful in Spring-based
 * applications or environments where Spring is already available.
 * </p>
 *
 * @since 2.0.0-ALPHA01
 * @see com.garganttua.core.injection
 */
package com.garganttua.core.spring;

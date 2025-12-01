/**
 * Reflections library integration for annotation scanning.
 *
 * <h2>Overview</h2>
 * <p>
 * This package provides integration with the Reflections library for scanning and
 * discovering annotated classes at runtime. It enables efficient annotation-based
 * component discovery for dependency injection and other frameworks.
 * </p>
 *
 * <h2>Main Classes</h2>
 * <ul>
 *   <li>{@code ReflectionsAnnotationScanner} - Annotation scanner using Reflections library</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Create scanner
 * ReflectionsAnnotationScanner scanner = new ReflectionsAnnotationScanner();
 *
 * // Scan for classes with @Inject annotation
 * Set<Class<?>> injectableClasses = scanner.findAnnotatedClasses(
 *     "com.myapp",
 *     Inject.class
 * );
 *
 * // Scan for classes with @RuntimeDefinition
 * Set<Class<?>> runtimeClasses = scanner.findAnnotatedClasses(
 *     "com.myapp.runtimes",
 *     RuntimeDefinition.class
 * );
 * }</pre>
 *
 * <h2>Features</h2>
 * <ul>
 *   <li>Fast annotation scanning</li>
 *   <li>Package-based filtering</li>
 *   <li>Multiple annotation support</li>
 *   <li>Classpath scanning</li>
 *   <li>Caching for performance</li>
 * </ul>
 *
 * <h2>Integration</h2>
 * <p>
 * This scanner is used by the dependency injection framework for automatic
 * component discovery and bean registration.
 * </p>
 *
 * @since 2.0.0-ALPHA01
 * @see com.garganttua.core.injection
 */
package com.garganttua.core.reflections;

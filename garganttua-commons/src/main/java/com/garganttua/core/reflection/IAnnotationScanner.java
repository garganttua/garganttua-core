package com.garganttua.core.reflection;

import java.lang.annotation.Annotation;
import java.util.List;

/**
 * Scanner interface for discovering classes annotated with specific annotations.
 *
 * <p>
 * {@code IAnnotationScanner} provides classpath scanning capabilities to identify
 * classes decorated with particular annotations. This is fundamental for component
 * scanning in dependency injection frameworks, plugin discovery, and annotation-driven
 * configuration. The scanner operates at the package level, examining all classes
 * within the specified package and its sub-packages.
 * </p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * IAnnotationScanner scanner = ...;
 *
 * // Find all @Component annotated classes in a package
 * List<Class<?>> components = scanner.getClassesWithAnnotation(
 *     "com.example.services",
 *     Component.class
 * );
 *
 * // Find all @Controller classes for web framework
 * List<Class<?>> controllers = scanner.getClassesWithAnnotation(
 *     "com.example.web",
 *     Controller.class
 * );
 *
 * // Register discovered components
 * for (Class<?> componentClass : components) {
 *     context.register(componentClass);
 * }
 * }</pre>
 *
 * <h2>Common Use Cases</h2>
 * <ul>
 *   <li>Component scanning for dependency injection (e.g., @Component, @Service)</li>
 *   <li>Web controller discovery (e.g., @Controller, @RestController)</li>
 *   <li>Entity discovery for ORM frameworks (e.g., @Entity, @Table)</li>
 *   <li>Plugin and extension point discovery</li>
 *   <li>Test class discovery (e.g., @Test, @TestSuite)</li>
 * </ul>
 *
 * <h2>Performance Considerations</h2>
 * <p>
 * Classpath scanning can be expensive, especially for large applications. Implementations
 * typically cache results and may use bytecode analysis rather than class loading
 * to improve performance. It is recommended to scan specific packages rather than
 * the entire classpath when possible.
 * </p>
 *
 * @since 2.0.0-ALPHA01
 */
public interface IAnnotationScanner {

	/**
	 * Finds all classes annotated with the specified annotation in the given package.
	 *
	 * <p>
	 * This method scans the specified package and all its sub-packages, identifying
	 * classes that are annotated with the given annotation type. The scan includes
	 * both direct annotations and meta-annotations (annotations on annotations).
	 * </p>
	 *
	 * <h3>Scanning Behavior</h3>
	 * <ul>
	 *   <li>Scans recursively through all sub-packages</li>
	 *   <li>Includes both public and package-private classes</li>
	 *   <li>May include abstract classes and interfaces if annotated</li>
	 *   <li>Results are typically cached for performance</li>
	 * </ul>
	 *
	 * @param package_ the base package to scan (e.g., "com.example.services")
	 * @param annotation the annotation type to search for
	 * @return a list of classes annotated with the specified annotation (never {@code null}, may be empty)
	 */
	List<Class<?>> getClassesWithAnnotation(String package_, Class<? extends Annotation> annotation);

}

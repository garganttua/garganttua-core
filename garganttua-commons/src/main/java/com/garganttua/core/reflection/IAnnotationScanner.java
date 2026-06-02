package com.garganttua.core.reflection;

import java.lang.annotation.Annotation;
import java.util.List;

/**
 * Discovers classes and methods carrying a given annotation, optionally scoped
 * to a package. Pluggable providers (classpath scanning at runtime, or
 * compile-time indices for AOT) implement this contract and are prioritized by
 * the {@link IReflection} facade.
 *
 * @since 2.0.0-ALPHA01
 */
public interface IAnnotationScanner {

	// --- IClass<>-based methods (new API for AOT migration) ---

	/**
	 * Finds all classes annotated with the given annotation.
	 *
	 * @param annotation the annotation class to search for
	 * @return list of matching classes (never null)
	 */
	List<IClass<?>> getClassesWithAnnotation(IClass<? extends Annotation> annotation);

	/**
	 * Finds all classes annotated with the given annotation within a package.
	 *
	 * @param packageName the package to restrict the search to
	 * @param annotation  the annotation class to search for
	 * @return list of matching classes (never null)
	 */
	List<IClass<?>> getClassesWithAnnotation(String packageName, IClass<? extends Annotation> annotation);

	/**
	 * Finds all methods annotated with the given annotation.
	 *
	 * @param annotation the annotation class to search for
	 * @return list of matching methods (never null)
	 */
	List<IMethod> getMethodsWithAnnotation(IClass<? extends Annotation> annotation);

	/**
	 * Finds all methods annotated with the given annotation within a package.
	 *
	 * @param packageName the package to restrict the search to
	 * @param annotation  the annotation class to search for
	 * @return list of matching methods (never null)
	 */
	List<IMethod> getMethodsWithAnnotation(String packageName, IClass<? extends Annotation> annotation);

}

package com.garganttua.core.reflection;

import java.lang.annotation.Annotation;
import java.util.List;

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

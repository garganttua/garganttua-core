package com.garganttua.core.reflection.dsl;

import com.garganttua.core.dsl.IObservableBuilder;
import com.garganttua.core.reflection.IAnnotationScanner;
import com.garganttua.core.reflection.IReflection;
import com.garganttua.core.reflection.IReflectionProvider;
import com.garganttua.core.reflection.annotations.Reflected;

/**
 * Fluent builder for assembling a composite {@link IReflection} facade from one
 * or more {@link IReflectionProvider}s and {@link IAnnotationScanner}s.
 *
 * <p>
 * Providers and scanners are registered with a priority; higher priorities win
 * when multiple candidates can satisfy a request. The default priority is
 * {@code 10}. Built results are typically installed via {@code IClass.setReflection(...)}
 * or supplied to consumers such as the injection context builder.
 * </p>
 *
 * @see IReflection
 * @see IReflectionProvider
 * @see IAnnotationScanner
 */
@Reflected
public interface IReflectionBuilder extends IObservableBuilder<IReflectionBuilder, IReflection> {

    /**
     * Registers a reflection provider at the default priority ({@code 10}).
     *
     * @param provider the class-resolution provider to add
     * @return this builder for method chaining
     */
    IReflectionBuilder withProvider(IReflectionProvider provider);

    /**
     * Registers an annotation scanner at the default priority ({@code 10}).
     *
     * @param scanner the annotation scanner to add
     * @return this builder for method chaining
     */
    IReflectionBuilder withScanner(IAnnotationScanner scanner);

    /**
     * Registers a reflection provider at the given priority.
     *
     * @param provider the class-resolution provider to add
     * @param priority the selection priority; higher wins ({@code 10} is the default)
     * @return this builder for method chaining
     */
    IReflectionBuilder withProvider(IReflectionProvider provider, int priority);

    /**
     * Registers an annotation scanner at the given priority.
     *
     * @param scanner  the annotation scanner to add
     * @param priority the selection priority; higher wins ({@code 10} is the default)
     * @return this builder for method chaining
     */
    IReflectionBuilder withScanner(IAnnotationScanner scanner, int priority);

}

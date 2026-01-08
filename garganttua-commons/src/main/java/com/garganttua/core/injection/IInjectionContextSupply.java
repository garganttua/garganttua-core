package com.garganttua.core.injection;

import com.garganttua.core.supply.IContextualSupply;

/**
 * Functional interface for supplying objects within a dependency injection context.
 *
 * <p>
 * {@code IInjectionContextSupply} is a specialized version of {@link IContextualObjectSupply}
 * that specifically works with {@link IInjectionContext}. It enables context-aware object creation
 * where the supplied object can access the injection context during instantiation, allowing for
 * dynamic dependency resolution and context-specific initialization.
 * </p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Create a contextual supplier
 * IInjectionContextSupply<MyService> supplier = context -> {
 *     DataSource ds = context.queryBean(
 *         BeanDefinition.example(DataSource.class,
 *             Optional.empty(),
 *             Optional.empty(),
 *             Set.of())
 *     ).orElseThrow();
 *     return new MyService(ds);
 * };
 *
 * // Use the supplier
 * MyService service = supplier.supply(context);
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * Implementations should be stateless or properly synchronized if they maintain state.
 * </p>
 *
 * @param <Supplied> the type of object this supply operation provides
 * @since 2.0.0-ALPHA01
 * @see IContextualObjectSupply
 * @see IInjectionContext
 */
@FunctionalInterface
public interface IInjectionContextSupply<Supplied> extends IContextualSupply<Supplied, IInjectionContext> {

}

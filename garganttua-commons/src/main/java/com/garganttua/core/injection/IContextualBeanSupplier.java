package com.garganttua.core.injection;

import com.garganttua.core.supply.IContextualSupplier;

/**
 * Bean supplier that resolves instances against an {@link IInjectionContext}.
 *
 * <p>
 * Combines {@link IContextualSupplier} (context-aware supply) with {@link IBeanSupplier}
 * (dependency tracking), so the supplied bean can query the injection context during
 * its own instantiation.
 * </p>
 *
 * @param <Bean> the type of bean this supplier provides
 * @since 2.0.0-ALPHA01
 * @see IBeanSupplier
 * @see IInjectionContext
 */
public interface IContextualBeanSupplier<Bean> extends IContextualSupplier<Bean, IInjectionContext>, IBeanSupplier<Bean> {

}

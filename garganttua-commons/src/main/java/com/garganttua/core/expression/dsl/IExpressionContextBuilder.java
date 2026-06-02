package com.garganttua.core.expression.dsl;

import com.garganttua.core.dsl.IAutomaticBuilder;
import com.garganttua.core.dsl.IObservableBuilder;
import com.garganttua.core.dsl.dependency.IDependentBuilder;
import com.garganttua.core.dsl.IPackageableBuilder;
import com.garganttua.core.expression.context.IExpressionContext;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.supply.ISupplier;
import com.garganttua.core.supply.dsl.ISupplierBuilder;
import com.garganttua.core.reflection.annotations.Reflected;

/**
 * Fluent builder for assembling an {@link IExpressionContext}.
 *
 * <p>Supports automatic detection, package scanning, dependency declaration and
 * observability through the builder interfaces it extends.</p>
 *
 * @since 2.0.0-ALPHA01
 */
@Reflected
public interface IExpressionContextBuilder extends IAutomaticBuilder<IExpressionContextBuilder, IExpressionContext>, IPackageableBuilder<IExpressionContextBuilder, IExpressionContext>, IDependentBuilder<IExpressionContextBuilder, IExpressionContext>, IObservableBuilder<IExpressionContextBuilder, IExpressionContext> {

    /**
     * Begins building a method-binder-backed expression node for the given supplier owner.
     *
     * @param methodOwnerSupplier supplier of the object whose method backs the expression
     * @param supplied            the type the expression evaluates to
     * @param <T>                 the supplied value type
     * @return a nested builder for configuring the method binder expression
     */
    <T> IExpressionMethodBinderBuilder<T> expression(ISupplierBuilder<?, ? extends ISupplier<?>> methodOwnerSupplier, IClass<T> supplied);
}

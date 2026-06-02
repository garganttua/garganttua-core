package com.garganttua.core.expression.dsl;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.expression.IExpressionNode;
import com.garganttua.core.expression.context.IExpressionNodeFactory;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.ObjectAddress;
import com.garganttua.core.reflection.binders.dsl.IMethodBinderBuilder;
import com.garganttua.core.supply.ISupplier;

/**
 * Fluent builder for method binders exposed as expression nodes.
 *
 * <p>
 * Extends {@link IMethodBinderBuilder} to produce {@link IExpressionNode} bindings
 * that evaluate lazily to an {@link ISupplier} of the method's return type, allowing
 * a reflected method to be invoked from within an expression.
 * </p>
 *
 * @param <S> the supplied (return) type of the bound method
 * @since 2.0.0-ALPHA01
 */
public interface IExpressionMethodBinderBuilder<S> extends
        IMethodBinderBuilder<IExpressionNode<S, ISupplier<S>>, IExpressionMethodBinderBuilder<S>, IExpressionContextBuilder, IExpressionNodeFactory<S, ISupplier<S>>> {

    /**
     * Marks the parameter at the given position as nullable.
     *
     * @param i the zero-based parameter index
     * @return this builder for method chaining
     */
    IExpressionMethodBinderBuilder<S> withNullableParam(int i);

    /**
     * Sets the name under which this binding is registered in the expression context.
     *
     * @param name the binding name
     * @return this builder for method chaining
     */
    IExpressionMethodBinderBuilder<S> withName(String name);

    /**
     * Sets a human-readable description for this binding.
     *
     * @param description the description text
     * @return this builder for method chaining
     */
    IExpressionMethodBinderBuilder<S> withDescription(String description);

    /**
     * Binds an encapsulated method identified by its object address.
     *
     * @param methodAddress  the address locating the method to bind
     * @param returnType     the declared return type of the method
     * @param parameterTypes the declared parameter types of the method
     * @return this builder for method chaining
     * @throws DslException if the method cannot be resolved or bound
     */
    IExpressionMethodBinderBuilder<S> encapsulatedMethod(ObjectAddress methodAddress,
            IClass<S> returnType, IClass<?>... parameterTypes) throws DslException;

    /**
     * Binds an encapsulated method identified by its name.
     *
     * @param methodName     the name of the method to bind
     * @param returnType     the declared return type of the method
     * @param parameterTypes the declared parameter types of the method
     * @return this builder for method chaining
     * @throws DslException if the method cannot be resolved or bound
     */
    IExpressionMethodBinderBuilder<S> encapsulatedMethod(String methodName,
            IClass<S> returnType, IClass<?>... parameterTypes) throws DslException;

}

package com.garganttua.core.expression.context;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.expression.ContextualExpressionNode;
import com.garganttua.core.expression.ExpressionException;
import com.garganttua.core.expression.ExpressionNode;
import com.garganttua.core.expression.IExpressionNode;
import com.garganttua.core.reflection.IMethodReturn;
import com.garganttua.core.reflection.ObjectAddress;
import com.garganttua.core.reflection.ReflectionException;
import com.garganttua.core.reflection.binders.ContextualMethodBinder;
import com.garganttua.core.reflection.binders.IMethodBinder;
import com.garganttua.core.reflection.binders.MethodBinder;
import com.garganttua.core.reflection.binders.dsl.AbstractMethodBinderBuilder;
import com.garganttua.core.reflection.methods.MethodResolver;
import com.garganttua.core.reflection.methods.SingleMethodReturn;
import com.garganttua.core.supply.IContextualSupplier;
import com.garganttua.core.supply.ISupplier;
import com.garganttua.core.supply.NullSupplier;
import com.garganttua.core.supply.NullableSupplier;
import com.garganttua.core.supply.SupplyException;
import com.garganttua.core.supply.dsl.FixedSupplierBuilder;
import com.garganttua.core.supply.dsl.ISupplierBuilder;

import lombok.extern.slf4j.Slf4j;

/**
 * Factory for creating expression nodes from method bindings.
 *
 * <p>
 * {@code ExpressionNodeFactory} creates expression nodes (either leaf or
 * non-leaf)
 * by binding Java methods to expression evaluation contexts. It supports both
 * contextual and non-contextual expression nodes based on the evaluation
 * requirements.
 * </p>
 *
 * <h2>Key Features</h2>
 * <ul>
 * <li>Creates leaf nodes for terminal expressions without child
 * dependencies</li>
 * <li>Creates composite nodes for expressions with child node dependencies</li>
 * <li>Supports contextual expressions requiring runtime context resolution</li>
 * <li>Handles nullable parameters through parameter encapsulation</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * 
 * <pre>{@code
 * // Create a leaf factory for a static method
 * ExpressionNodeFactory<String, ISupplier<String>> factory = new ExpressionNodeFactory<>(
 *         MyClass.class,
 *         ISupplier.class,
 *         MyClass.class.getMethod("process", String.class),
 *         new ObjectAddress("process"),
 *         List.of(false), // parameter is not nullable
 *         true, // this is a leaf node
 *         Optional.of("process"), // name
 *         Optional.of("Process a string") // description
 * );
 *
 * // Supply an expression node based on context
 * IExpressionNodeContext context = new ExpressionNodeContext(List.of("input"));
 * Optional<IExpressionNode<String, ISupplier<String>>> node = factory.supply(context);
 * }</pre>
 *
 * @param <R> the return type of the expression evaluation
 * @param <S> the supplier type that wraps the return value
 *
 * @since 2.0.0-ALPHA01
 * @see IExpressionNodeFactory
 * @see IExpressionNode
 * @see IExpressionNodeContext
 */
@Slf4j
public class ExpressionNodeFactory<R, S extends ISupplier<R>>
        extends ContextualMethodBinder<IExpressionNode<R, S>, IExpressionNodeContext>
        implements IExpressionNodeFactory<R, S> {

    // ========== Fields ==========

    private final Method method;
    private final Class<?>[] parameterTypes;
    private final List<Boolean> nullableParameters;
    @SuppressWarnings("java:S1068") // Field kept for API compatibility
    private final ObjectAddress methodAddress;

    private String name;
    private String description;
    private ISupplier<?> methodOwnerSupplier;

    // ========== Constructors ==========

    /**
     * Creates a new ExpressionNodeFactory with the specified configuration.
     *
     * @param methodOwner        the class that declares the target method
     * @param supplied           the supplier type class (unused, kept for API
     *                           compatibility)
     * @param method             the target method to be bound
     * @param methodAddress      the address/identifier of the method
     * @param nullableParameters list indicating which parameters accept null values
     * @param leaf               true if this factory creates leaf nodes, false for
     *                           composite nodes
     * @param name               optional custom name for the expression node
     *                           (defaults to method name)
     * @param description        optional description for the expression node
     *                           (defaults to "No description")
     *
     * @throws ExpressionException  if method is null, nullable parameters list is
     *                              null,
     *                              or parameter count mismatch between method and
     *                              nullable parameters list
     * @throws NullPointerException if methodOwner or method is null
     */
    @SuppressWarnings("unchecked")
    public ExpressionNodeFactory(
            ISupplier<?> methodOwnerSupplier,
            Class<S> supplied,
            Method method,
            ObjectAddress methodAddress,
            List<Boolean> nullableParameters,
            Optional<String> name, Optional<String> description) throws ExpressionException {

        super(methodOwnerSupplier,
                MethodResolver.methodByMethod(methodOwnerSupplier.getSuppliedClass(), method),
                List.of());

        log.atTrace().log("Creating ExpressionNodeFactory: method={}", method.getName());

        this.methodOwnerSupplier = Objects.requireNonNull(methodOwnerSupplier, "Method owner supplier cannot be null");
        this.method = Objects.requireNonNull(method, "Method cannot be null");
        this.name = name.orElse(this.method.getName());
        this.description = description.orElse("No description");
        this.methodAddress = Objects.requireNonNull(methodAddress, "Method address cannot be null");
        this.parameterTypes = this.method.getParameterTypes();
        this.nullableParameters = Objects.requireNonNull(nullableParameters,
                "Nullable parameters list cannot be null");

        validateParameterConfiguration();

        log.atDebug().log("ExpressionNodeFactory created: method={}, parameterCount={}",
                method.getName(), parameterTypes.length);
    }

    /**
     * Private constructor for internal use only.
     *
     * @deprecated This constructor is unused and maintained for potential future
     *             extensions
     */
    @Deprecated
    private ExpressionNodeFactory(
            ISupplier<?> objectSupplier,
            ObjectAddress methodAddr,
            List<ISupplier<?>> parameterSuppliers,
            Class<IExpressionNode<R, S>> returnedClass) throws ExpressionException {

        super(objectSupplier,
                MethodResolver.methodByAddress(objectSupplier.getSuppliedClass(), methodAddr, returnedClass,
                    parameterSuppliers.stream().map(s -> s.getSuppliedClass()).toArray(Class[]::new)),
                parameterSuppliers);

        // Default initialization for deprecated constructor
        this.method = null;
        this.methodAddress = methodAddr;
        this.parameterTypes = new Class<?>[0];
        this.nullableParameters = List.of();
    }

    // ========== Public Methods ==========

    /**
     * Returns the key identifier for this factory.
     *
     * <p>
     * Note: Multiple factories can have the same key if different classes have
     * methods
     * with the same name and parameters. The ExpressionContext handles duplicates
     * by
     * keeping the first factory registered.
     * </p>
     *
     * @return a string in the format "name(Type1,Type2,...)"
     */
    @Override
    public String key() {
        StringBuilder key = new StringBuilder(this.name);
        key.append("(");

        for (int i = 0; i < this.parameterTypes.length; i++) {
            if (i > 0) {
                key.append(",");
            }
            key.append(this.parameterTypes[i].getSimpleName());
        }

        key.append(")");
        String keyString = key.toString();
        log.atDebug().log("Generated factory key: {}", keyString);
        return keyString;
    }

    @Override
    public Optional<IMethodReturn<IExpressionNode<R, S>>> execute(IExpressionNodeContext ownerContext, Object... contexts)
            throws ReflectionException {
        return this.supply(ownerContext, contexts);
    }

    /**
     * Supplies an expression node based on the provided context.
     *
     * <p>
     * This method creates the appropriate type of expression node (leaf or
     * composite,
     * contextual or non-contextual) based on the factory configuration and the
     * evaluation context requirements.
     * </p>
     *
     * @param context       the expression node context containing parameters and
     *                      configuration
     * @param otherContexts additional contexts (currently unused)
     *
     * @return an Optional containing the created expression node wrapped in IMethodReturn,
     *         or empty if context doesn't match the expected parameter types
     *
     * @throws SupplyException if an error occurs during node creation or binding
     */
    @Override
    public Optional<IMethodReturn<IExpressionNode<R, S>>> supply(
            IExpressionNodeContext context,
            Object... otherContexts) throws SupplyException {

        log.atTrace().log("Supplying expression node for contexts {} {}", context, otherContexts);

        if (!context.matches(this.parameterTypes)) {
            log.atDebug().log("Context does not match parameter types, returning empty");
            return Optional.empty();
        }

        IExpressionNode<R, S> expressionNode = context.buildContextual()
                ? createContextualNode(context)
                : createNonContextualNode(context);

        log.atDebug().log("Expression node created: type={}",
                expressionNode != null ? expressionNode.getClass().getSimpleName() : "null");

        return Optional.ofNullable(expressionNode).map(SingleMethodReturn::of);
    }

    // ========== Private Node Creation Methods ==========

    /**
     * Creates a non-contextual expression node.
     *
     * @param context the expression context
     * @return the created node
     */
    @SuppressWarnings("unchecked")
    private IExpressionNode<R, S> createNonContextualNode(IExpressionNodeContext context) {
        return (IExpressionNode<R, S>) new ExpressionNode<>(
                getExecutableReference(),
                this::bindNode,
                getReturnType(),
                context.parameters());
    }

    /**
     * Creates a contextual expression node.
     *
     * @param context the expression context
     * @return the created contextual node
     */
    @SuppressWarnings("unchecked")
    private IExpressionNode<R, S> createContextualNode(IExpressionNodeContext context) {
        return (IExpressionNode<R, S>) new ContextualExpressionNode<>(
                getExecutableReference(),
                (c, params) -> this.bindContextualNode(params),
                getReturnType(),
                context.parameters());
    }

    // ========== Private Binding Methods ==========

    /**
     * Binds a contextual method for composite node evaluation.
     *
     * @param parameters the supplier parameters to bind
     * @return a contextual supplier that invokes the bound method
     */
    private IContextualSupplier<R, IExpressionContext> bindContextualNode(Object... parameters) {
        log.atTrace().log("Binding contextual node with {} parameters", parameters.length);

        List<ISupplier<?>> encapsulatedParams = encapsulateParameters(parameters);

        ContextualMethodBinder<R, IExpressionContext> binder = new ContextualMethodBinder<>(
                this.methodOwnerSupplier,
                MethodResolver.methodByMethod(this.methodOwnerSupplier.getSuppliedClass(), this.method),
                encapsulatedParams);

        // Wrap to extract value from IMethodReturn
        return new MethodReturnUnwrappingContextualSupplier<>(binder, getReturnType());
    }

    /**
     * Binds a non-contextual method for composite node evaluation.
     *
     * @param parameters the supplier parameters to bind
     * @return a supplier that invokes the bound method
     * @throws DslException if binding fails
     */
    private ISupplier<R> bindNode(Object... parameters) throws DslException {
        log.atTrace().log("Binding non-contextual node with {} parameters", parameters.length);

        List<ISupplier<?>> encapsulatedParams = encapsulateParameters(parameters);

        MethodBinder<R> binder = new MethodBinder<>(
                this.methodOwnerSupplier,
                MethodResolver.methodByMethod(this.methodOwnerSupplier.getSuppliedClass(), this.method),
                encapsulatedParams);

        // Wrap to extract value from IMethodReturn
        return new MethodReturnUnwrappingSupplier<>(binder, getReturnType());
    }

    // ========== Private Utility Methods ==========

    /**
     * Encapsulates parameters with nullable wrappers based on configuration.
     *
     * @param parameters the parameters to encapsulate
     * @return a list of encapsulated suppliers
     */
    private List<ISupplier<?>> encapsulateParameters(Object[] parameters) {
        List<ISupplier<?>> encapsulated = new ArrayList<>(parameters.length);

        for (int i = 0; i < parameters.length; i++) {
            ISupplier<?> supplier = null;
            if (!(parameters[i] instanceof ISupplier<?>)) {
                supplier = new FixedSupplierBuilder<>(parameters[i]).build();
            } else {
                supplier = (ISupplier<?>) parameters[i];
            }

            boolean nullable = this.nullableParameters.get(i);
            encapsulated.add(new NullableSupplier<>(supplier, nullable));
        }

        return encapsulated;
    }

    /**
     * Gets the return type of the bound method.
     *
     * @return the return type class
     */
    @SuppressWarnings("unchecked")
    private Class<R> getReturnType() {
        return (Class<R>) this.method.getReturnType();
    }

    /**
     * Validates that the parameter configuration is consistent.
     *
     * @throws ExpressionException if validation fails
     */
    private void validateParameterConfiguration() throws ExpressionException {
        if (this.parameterTypes.length != this.nullableParameters.size()) {
            String errorMsg = String.format(
                    "Expression parameters size mismatch: parameterTypes [%d] vs nullableParameters [%d]",
                    this.parameterTypes.length,
                    this.nullableParameters.size());

            log.atError().log(errorMsg);
            throw new ExpressionException(errorMsg);
        }
    }

    // ========== Inner Classes ==========

    /**
     * Wrapper that extracts the value from IMethodReturn for non-contextual suppliers.
     */
    private static class MethodReturnUnwrappingSupplier<T> implements ISupplier<T> {
        private final ISupplier<IMethodReturn<T>> delegate;
        private final Class<T> returnType;

        MethodReturnUnwrappingSupplier(ISupplier<IMethodReturn<T>> delegate, Class<T> returnType) {
            this.delegate = delegate;
            this.returnType = returnType;
        }

        @Override
        public Optional<T> supply() throws SupplyException {
            return delegate.supply()
                    .flatMap(methodReturn -> {
                        if (methodReturn.hasException()) {
                            throw new RuntimeException(new SupplyException(
                                    "Method invocation failed", methodReturn.getException()));
                        }
                        return methodReturn.firstOptional();
                    });
        }

        @Override
        public java.lang.reflect.Type getSuppliedType() {
            return returnType;
        }
    }

    /**
     * Wrapper that extracts the value from IMethodReturn for contextual suppliers.
     */
    private static class MethodReturnUnwrappingContextualSupplier<T, C> implements IContextualSupplier<T, C> {
        private final IContextualSupplier<IMethodReturn<T>, C> delegate;
        private final Class<T> returnType;

        MethodReturnUnwrappingContextualSupplier(IContextualSupplier<IMethodReturn<T>, C> delegate, Class<T> returnType) {
            this.delegate = delegate;
            this.returnType = returnType;
        }

        @Override
        public Optional<T> supply(C context, Object... otherContexts) throws SupplyException {
            return delegate.supply(context, otherContexts)
                    .flatMap(methodReturn -> {
                        if (methodReturn.hasException()) {
                            throw new RuntimeException(new SupplyException(
                                    "Method invocation failed", methodReturn.getException()));
                        }
                        return methodReturn.firstOptional();
                    });
        }

        @Override
        public Class<C> getOwnerContextType() {
            return delegate.getOwnerContextType();
        }

        @Override
        public java.lang.reflect.Type getSuppliedType() {
            return returnType;
        }
    }

    /**
     * Internal builder for creating method binders for expression evaluation.
     *
     * <p>
     * This builder is used specifically for binding leaf node methods with
     * fixed parameter values during expression evaluation.
     * </p>
     */
    class ExpressionMethodBinderBuilder
            extends AbstractMethodBinderBuilder<R, ExpressionMethodBinderBuilder, Object, IMethodBinder<R>> {

        /**
         * Creates a new ExpressionMethodBinderBuilder.
         *
         * @param up       the parent object (unused)
         * @param supplier the object supplier for the method owner
         * @throws DslException if builder creation fails
         */
        protected ExpressionMethodBinderBuilder(Object up, ISupplierBuilder<?, ?> supplier) throws DslException {
            super(up, supplier);
        }

        /**
         * Auto-detection is not implemented for expression binders.
         *
         * @throws DslException never thrown in this implementation
         */
        @Override
        protected void doAutoDetection() throws DslException {
            // No auto-detection needed for expression binders
        }
    }

    @Override
    public String description() {
        return this.description;
    }

    /**
     * Generates a manual page (man-style) documentation for this expression node
     * factory.
     *
     * <p>
     * The manual includes:
     * </p>
     * <ul>
     * <li>NAME - The function name</li>
     * <li>SYNOPSIS - The function signature with parameter types</li>
     * <li>DESCRIPTION - A detailed description of what the function does</li>
     * <li>PARAMETERS - List of parameter types and nullability</li>
     * <li>RETURN VALUE - The return type of the function</li>
     * </ul>
     *
     * @return a formatted manual page string
     */
    @Override
    public String man() {
        StringBuilder manual = new StringBuilder();

        // NAME section
        manual.append("NAME\n");
        manual.append("    ").append(this.name).append(" - ").append("\n\n");

        // SYNOPSIS section
        manual.append("SYNOPSIS\n");
        manual.append("    ").append(this.method.getReturnType().getSimpleName()).append(" ");
        manual.append(this.name).append("(");

        for (int i = 0; i < this.parameterTypes.length; i++) {
            if (i > 0) {
                manual.append(", ");
            }
            manual.append(this.parameterTypes[i].getSimpleName());
            manual.append(" ").append(this.method.getParameters()[i].getName());
        }
        manual.append(")\n\n");

        // DESCRIPTION section
        manual.append("DESCRIPTION\n");
        manual.append("    ").append(this.description).append("\n\n");

        // PARAMETERS section
        if (this.parameterTypes.length > 0) {
            manual.append("PARAMETERS\n");
            for (int i = 0; i < this.parameterTypes.length; i++) {
                manual.append("    ").append(this.method.getParameters()[i].getName()).append(" : ");
                manual.append(this.parameterTypes[i].getSimpleName());

                if (this.nullableParameters.get(i).booleanValue()) {
                    manual.append(" (nullable)");
                } else {
                    manual.append(" (required)");
                }
                manual.append("\n");
            }
            manual.append("\n");
        }

        // RETURN VALUE section
        manual.append("RETURN VALUE\n");
        manual.append("    ").append(this.method.getReturnType().getSimpleName()).append("\n");

        return manual.toString();
    }
}

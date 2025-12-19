package com.garganttua.core.expression.context;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.expression.ContextualExpressionNode;
import com.garganttua.core.expression.ExpressionException;
import com.garganttua.core.expression.ExpressionLeaf;
import com.garganttua.core.expression.ExpressionNode;
import com.garganttua.core.expression.IExpressionNode;
import com.garganttua.core.reflection.ObjectAddress;
import com.garganttua.core.reflection.binders.ContextualMethodBinder;
import com.garganttua.core.reflection.binders.IMethodBinder;
import com.garganttua.core.reflection.binders.MethodBinder;
import com.garganttua.core.reflection.binders.dsl.AbstractMethodBinderBuilder;
import com.garganttua.core.supply.IContextualSupplier;
import com.garganttua.core.supply.ISupplier;
import com.garganttua.core.supply.NullSupplier;
import com.garganttua.core.supply.NullableSupplier;
import com.garganttua.core.supply.SupplyException;
import com.garganttua.core.supply.dsl.FixedSupplierBuilder;
import com.garganttua.core.supply.dsl.ISupplierBuilder;
import com.garganttua.core.supply.dsl.NullSupplierBuilder;

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

    private final boolean leaf;
    private final Method method;
    private final Class<?>[] parameterTypes;
    private final List<Boolean> nullableParameters;
    private final ObjectAddress methodAddress;

    private String name;
    private String description;

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
     * @param name               optional custom name for the expression node (defaults to method name)
     * @param description        optional description for the expression node (defaults to "No description")
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
            Boolean leaf, Optional<String> name, Optional<String> description) throws ExpressionException {

        super(methodOwnerSupplier,
                methodAddress,
                List.of(),
                (Class<IExpressionNode<R, S>>) (Class<?>) IExpressionNode.class);

        log.atTrace().log("Creating ExpressionNodeFactory: method={}, leaf={}", method.getName(), leaf);

        this.method = Objects.requireNonNull(method, "Method cannot be null");
        this.name = name.orElse(this.method.getName());
        this.description = description.orElse("No description");
        this.methodAddress = Objects.requireNonNull(methodAddress, "Method address cannot be null");
        this.parameterTypes = this.method.getParameterTypes();
        this.nullableParameters = Objects.requireNonNull(nullableParameters,
                "Nullable parameters list cannot be null");

        validateParameterConfiguration();

        this.leaf = Objects.requireNonNull(leaf, "Leaf flag cannot be null");

        log.atDebug().log("ExpressionNodeFactory created: method={}, parameterCount={}, leaf={}",
                method.getName(), parameterTypes.length, leaf);
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
            ObjectAddress method,
            List<ISupplier<?>> parameterSuppliers,
            Class<IExpressionNode<R, S>> returnedClass) throws ExpressionException {

        super(objectSupplier, method, parameterSuppliers, returnedClass);

        // Default initialization for deprecated constructor
        this.method = null;
        this.methodAddress = method;
        this.parameterTypes = new Class<?>[0];
        this.nullableParameters = List.of();
        this.leaf = false;
    }

    // ========== Public Methods ==========

    /**
     * Returns the key identifier for this factory.
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
     * @return an Optional containing the created expression node, or empty if
     *         context
     *         doesn't match the expected parameter types
     *
     * @throws SupplyException if an error occurs during node creation or binding
     */
    @Override
    public Optional<IExpressionNode<R, S>> supply(
            IExpressionNodeContext context,
            Object... otherContexts) throws SupplyException {

        log.atTrace().log("Supplying expression node for context");

        if (!context.matches(this.parameterTypes)) {
            log.atDebug().log("Context does not match parameter types, returning empty");
            return Optional.empty();
        }

        IExpressionNode<R, S> expressionNode = this.leaf
                ? createLeafNode(context)
                : createCompositeNode(context);

        log.atDebug().log("Expression node created: type={}",
                expressionNode != null ? expressionNode.getClass().getSimpleName() : "null");

        return Optional.ofNullable(expressionNode);
    }

    // ========== Private Node Creation Methods ==========

    /**
     * Creates a leaf expression node.
     *
     * @param context the expression context
     * @return the created leaf node
     */
    @SuppressWarnings("unchecked")
    private IExpressionNode<R, S> createLeafNode(IExpressionNodeContext context) {
        log.atTrace().log("Creating leaf expression node");

        return (IExpressionNode<R, S>) new ExpressionLeaf<R>(
                getExecutableReference(),
                this::bindLeaf,
                getReturnType(),
                context.leafParameters());
    }

    /**
     * Creates a composite expression node (either contextual or non-contextual).
     *
     * @param context the expression context
     * @return the created composite node
     */
    private IExpressionNode<R, S> createCompositeNode(IExpressionNodeContext context) {
        log.atTrace().log("Creating composite expression node: contextual={}", context.buildContextual());

        return context.buildContextual()
                ? createNonContextualNode(context)
                : createContextualNode(context);
    }

    /**
     * Creates a non-contextual expression node.
     *
     * @param context the expression context
     * @return the created node
     */
    @SuppressWarnings("unchecked")
    private IExpressionNode<R, S> createNonContextualNode(IExpressionNodeContext context) {
        return (IExpressionNode<R, S>) new ExpressionNode<R>(
                getExecutableReference(),
                this::bindNode,
                getReturnType(),
                context.nodeChilds());
    }

    /**
     * Creates a contextual expression node.
     *
     * @param context the expression context
     * @return the created contextual node
     */
    @SuppressWarnings("unchecked")
    private IExpressionNode<R, S> createContextualNode(IExpressionNodeContext context) {
        return (IExpressionNode<R, S>) new ContextualExpressionNode<R>(
                getExecutableReference(),
                (c, params) -> this.bindContextualNode(params),
                getReturnType(),
                context.nodeChilds());
    }

    // ========== Private Binding Methods ==========

    /**
     * Binds a contextual method for composite node evaluation.
     *
     * @param parameters the supplier parameters to bind
     * @return a contextual supplier that invokes the bound method
     */
    private IContextualSupplier<R, IExpressionContext> bindContextualNode(ISupplier<?>[] parameters) {
        log.atTrace().log("Binding contextual node with {} parameters", parameters.length);

        List<ISupplier<?>> encapsulatedParams = encapsulateParameters(parameters);

        return new ContextualMethodBinder<>(
                createMethodOwnerSupplier(),
                this.methodAddress,
                encapsulatedParams,
                getReturnType());
    }

    /**
     * Binds a non-contextual method for composite node evaluation.
     *
     * @param parameters the supplier parameters to bind
     * @return a supplier that invokes the bound method
     * @throws DslException if binding fails
     */
    private ISupplier<R> bindNode(ISupplier<?>[] parameters) throws DslException {
        log.atTrace().log("Binding non-contextual node with {} parameters", parameters.length);

        List<ISupplier<?>> encapsulatedParams = encapsulateParameters(parameters);

        return new MethodBinder<R>(
                createMethodOwnerSupplier(),
                this.methodAddress,
                encapsulatedParams,
                getReturnType());
    }

    /**
     * Binds a leaf node method for direct evaluation.
     *
     * @param parameters the concrete parameter values
     * @return a method binder that can execute the bound method
     * @throws ExpressionException if binding fails
     */
    private IMethodBinder<R> bindLeaf(Object[] parameters) {
        log.atTrace().log("Binding leaf with {} parameters", parameters.length);

        try {
            ExpressionMethodBinderBuilder builder = new ExpressionMethodBinderBuilder(
                    new Object(),
                    new NullSupplierBuilder<>(this.method.getDeclaringClass()));

            builder.method(this.method)
                    .withReturn(getReturnType());

            for (int i = 0; i < parameters.length; i++) {
                builder.withParam(i,
                        new FixedSupplierBuilder<>(parameters[i]),
                        this.nullableParameters.get(i));
            }

            return builder.build();

        } catch (DslException e) {
            String errorMsg = "Failed to bind leaf method: " + method.getName() + " - " + e.getMessage();
            log.atError().log(errorMsg, e);
            throw new ExpressionException(errorMsg);
        }
    }

    // ========== Private Utility Methods ==========

    /**
     * Encapsulates parameters with nullable wrappers based on configuration.
     *
     * @param parameters the parameters to encapsulate
     * @return a list of encapsulated suppliers
     */
    private List<ISupplier<?>> encapsulateParameters(ISupplier<?>[] parameters) {
        List<ISupplier<?>> encapsulated = new ArrayList<>(parameters.length);

        for (int i = 0; i < parameters.length; i++) {
            boolean nullable = this.nullableParameters.get(i);
            encapsulated.add(new NullableSupplier<>(parameters[i], nullable));
        }

        return encapsulated;
    }

    /**
     * Creates a supplier for the method owner.
     *
     * @return a null supplier for the declaring class
     */
    private ISupplier<?> createMethodOwnerSupplier() {
        return new NullSupplier<>(this.method.getDeclaringClass());
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
}

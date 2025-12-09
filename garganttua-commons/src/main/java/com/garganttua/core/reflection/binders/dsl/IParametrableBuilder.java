package com.garganttua.core.reflection.binders.dsl;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.dsl.IAutomaticBuilder;
import com.garganttua.core.supply.ISupplier;
import com.garganttua.core.supply.dsl.ISupplierBuilder;

/**
 * Builder interface for configuring parameters of executable elements (methods/constructors).
 *
 * <p>
 * {@code IParametrableBuilder} provides a fluent API for specifying parameter values
 * for methods and constructors during binder construction. Parameters can be specified
 * using direct values or suppliers, by position (index), by name, or sequentially.
 * The builder supports nullable parameters and automatic parameter detection.
 * </p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Constructor with positional parameters
 * IConstructorBinder<Database> db = ConstructorBinder
 *     .forClass(Database.class)
 *     .withParam(0, "jdbc:mysql://localhost:3306/db")
 *     .withParam(1, 3306)
 *     .build();
 *
 * // Method with named parameters
 * IMethodBinder<Void> method = MethodBinder
 *     .forClass(UserService.class)
 *     .method("updateUser")
 *     .withParam("userId", 123)
 *     .withParam("userName", "Alice")
 *     .build();
 *
 * // Sequential parameter specification
 * IConstructorBinder<Logger> logger = ConstructorBinder
 *     .forClass(Logger.class)
 *     .withParam("ApplicationLogger")      // First parameter
 *     .withParam(Level.INFO)               // Second parameter
 *     .build();
 *
 * // Using suppliers for dynamic values
 * IConstructorBinder<Service> service = ConstructorBinder
 *     .forClass(Service.class)
 *     .withParam(SupplierBuilder.forType(Config.class).withContext(...))
 *     .build();
 *
 * // Nullable parameters
 * IMethodBinder<Response> handler = MethodBinder
 *     .forInstance(controller)
 *     .method("handle")
 *     .withParam("request", request)
 *     .withParam("user", null, true)  // Accepts null
 *     .build();
 * }</pre>
 *
 * <h2>Parameter Specification Methods</h2>
 * <ul>
 *   <li><b>By position</b>: {@code withParam(int, Object)} - Specify parameter at exact index</li>
 *   <li><b>By name</b>: {@code withParam(String, Object)} - Specify parameter by name (requires debug symbols)</li>
 *   <li><b>Sequential</b>: {@code withParam(Object)} - Add parameters in order</li>
 *   <li><b>Supplier-based</b>: All methods accept {@link ISupplierBuilder} for dynamic resolution</li>
 * </ul>
 *
 * @param <Builder> the concrete builder type for method chaining
 * @param <Built> the type of binder being constructed
 * @since 2.0.0-ALPHA01
 * @see IExecutableBinderBuilder
 * @see ISupplierBuilder
 */
public interface IParametrableBuilder<Builder, Built> extends IAutomaticBuilder<Builder, Built> {

    /**
     * Specifies a parameter value at the given position.
     *
     * @param i the zero-based parameter index
     * @param parameter the parameter value (may be {@code null} if allowed)
     * @return this builder instance for method chaining
     * @throws DslException if the index is invalid or the parameter type is incompatible
     */
    Builder withParam(int i, Object parameter) throws DslException;

    /**
     * Specifies a parameter supplier at the given position.
     *
     * @param i the zero-based parameter index
     * @param supplier the supplier that will provide the parameter value
     * @return this builder instance for method chaining
     * @throws DslException if the index is invalid or the supplier type is incompatible
     */
    Builder withParam(int i, ISupplierBuilder<?,? extends ISupplier<?>> supplier) throws DslException;

    /**
     * Specifies a parameter value by name.
     *
     * @param paramName the parameter name (requires debug symbols/reflection capabilities)
     * @param parameter the parameter value (may be {@code null} if allowed)
     * @return this builder instance for method chaining
     * @throws DslException if the parameter name is not found or the type is incompatible
     */
    Builder withParam(String paramName, Object parameter) throws DslException;

    /**
     * Specifies a parameter supplier by name.
     *
     * @param paramName the parameter name (requires debug symbols/reflection capabilities)
     * @param supplier the supplier that will provide the parameter value
     * @return this builder instance for method chaining
     * @throws DslException if the parameter name is not found or the supplier type is incompatible
     */
    Builder withParam(String paramName, ISupplierBuilder<?,? extends ISupplier<?>> supplier) throws DslException;

    /**
     * Specifies the next sequential parameter value.
     *
     * @param parameter the parameter value (may be {@code null} if allowed)
     * @return this builder instance for method chaining
     * @throws DslException if too many parameters are specified or the type is incompatible
     */
    Builder withParam(Object parameter) throws DslException;

    /**
     * Specifies the next sequential parameter supplier.
     *
     * @param supplier the supplier that will provide the parameter value
     * @return this builder instance for method chaining
     * @throws DslException if too many parameters are specified or the supplier type is incompatible
     */
    Builder withParam(ISupplierBuilder<?,? extends ISupplier<?>> supplier) throws DslException;

    /**
     * Specifies a parameter value at the given position with nullable control.
     *
     * @param i the zero-based parameter index
     * @param parameter the parameter value
     * @param acceptNullable whether {@code null} is acceptable for this parameter
     * @return this builder instance for method chaining
     * @throws DslException if the index is invalid, parameter is null when not allowed,
     *                     or the type is incompatible
     */
    Builder withParam(int i, Object parameter, boolean acceptNullable) throws DslException;

    /**
     * Specifies a parameter supplier at the given position with nullable control.
     *
     * @param i the zero-based parameter index
     * @param supplier the supplier that will provide the parameter value
     * @param acceptNullable whether the supplier can return empty/null
     * @return this builder instance for method chaining
     * @throws DslException if the index is invalid or the supplier type is incompatible
     */
    Builder withParam(int i, ISupplierBuilder<?,? extends ISupplier<?>> supplier, boolean acceptNullable) throws DslException;

    /**
     * Specifies a parameter value by name with nullable control.
     *
     * @param paramName the parameter name
     * @param parameter the parameter value
     * @param acceptNullable whether {@code null} is acceptable for this parameter
     * @return this builder instance for method chaining
     * @throws DslException if the parameter name is not found, parameter is null when
     *                     not allowed, or the type is incompatible
     */
    Builder withParam(String paramName, Object parameter, boolean acceptNullable) throws DslException;

    /**
     * Specifies a parameter supplier by name with nullable control.
     *
     * @param paramName the parameter name
     * @param supplier the supplier that will provide the parameter value
     * @param acceptNullable whether the supplier can return empty/null
     * @return this builder instance for method chaining
     * @throws DslException if the parameter name is not found or the supplier type is incompatible
     */
    Builder withParam(String paramName, ISupplierBuilder<?,? extends ISupplier<?>> supplier, boolean acceptNullable) throws DslException;

    /**
     * Specifies the next sequential parameter value with nullable control.
     *
     * @param parameter the parameter value
     * @param acceptNullable whether {@code null} is acceptable for this parameter
     * @return this builder instance for method chaining
     * @throws DslException if too many parameters are specified, parameter is null when
     *                     not allowed, or the type is incompatible
     */
    Builder withParam(Object parameter, boolean acceptNullable) throws DslException;

    /**
     * Specifies the next sequential parameter supplier with nullable control.
     *
     * @param supplier the supplier that will provide the parameter value
     * @param acceptNullable whether the supplier can return empty/null
     * @return this builder instance for method chaining
     * @throws DslException if too many parameters are specified or the supplier type is incompatible
     */
    Builder withParam(ISupplierBuilder<?,? extends ISupplier<?>> supplier, boolean acceptNullable) throws DslException;
}
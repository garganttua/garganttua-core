/**
 * Fluent builder APIs for constructing reflection-based binders.
 *
 * <h2>Overview</h2>
 * <p>
 * This package provides fluent DSL interfaces for building field, method, and constructor
 * binders. It offers a type-safe, readable way to configure complex reflection-based
 * binding scenarios with parameter mapping, value injection, and context awareness.
 * </p>
 *
 * <h2>Core Builder Interfaces</h2>
 * <ul>
 *   <li>{@link com.garganttua.core.reflection.binders.dsl.IFieldBinderBuilder} - Builds field binders</li>
 *   <li>{@link com.garganttua.core.reflection.binders.dsl.IMethodBinderBuilder} - Builds method binders</li>
 *   <li>{@link com.garganttua.core.reflection.binders.dsl.IConstructorBinderBuilder} - Builds constructor binders</li>
 *   <li>{@link com.garganttua.core.reflection.binders.dsl.IExecutableBinderBuilder} - Common builder for executables</li>
 * </ul>
 *
 * <h2>Configuration Builders</h2>
 * <ul>
 *   <li>{@link com.garganttua.core.reflection.binders.dsl.IParametrableBuilder} - Configures method/constructor parameters</li>
 *   <li>{@link com.garganttua.core.reflection.binders.dsl.IValuableBuilder} - Configures value binding strategies</li>
 * </ul>
 *
 * <h2>Usage Example: Field Binder</h2>
 * <pre>{@code
 * // Build a field binder with the fluent API
 * IFieldBinder<UserService, String> binder = FieldBinderBuilder
 *     .forClass(UserService.class)
 *     .field("apiUrl")
 *     .withValue("https://api.example.com")
 *     .build();
 *
 * binder.setValue();
 * }</pre>
 *
 * <h2>Usage Example: Constructor Binder</h2>
 * <pre>{@code
 * // Build a constructor binder
 * IConstructorBinder<DatabaseService> binder = ConstructorBinderBuilder
 *     .forClass(DatabaseService.class)
 *     .withParam("jdbc:mysql://localhost:3306/mydb")
 *     .withParam("admin")
 *     .withParam("secret")
 *     .build();
 *
 * Optional<IMethodReturn<DatabaseService>> service = binder.execute();
 * }</pre>
 *
 * <h2>Usage Example: Method Binder</h2>
 * <pre>{@code
 * // Build a method binder
 * IMethodBinder<Void> binder = MethodBinderBuilder
 *     .forInstance(emailService)
 *     .method("sendEmail")
 *     .withParam("user@example.com")
 *     .withParam("Welcome!")
 *     .withParam("Thank you for joining.")
 *     .build();
 *
 * binder.execute();
 * }</pre>
 *
 * <h2>Usage Example: Supplier-Backed Parameter Binding</h2>
 * <pre>{@code
 * // Mix direct values with supplier-resolved parameters
 * IMethodBinder<Order> binder = MethodBinderBuilder
 *     .forInstance(orderService)
 *     .method("processOrder")
 *     .withParam(SupplierBuilder.forType(OrderRepository.class).withContext(...))
 *     .withParam("STANDARD")
 *     .withParam(null, true)   // nullable parameter
 *     .build();
 * }</pre>
 *
 * <h2>Value Binding Strategies</h2>
 * <p>
 * The {@link com.garganttua.core.reflection.binders.dsl.IValuableBuilder} supports:
 * </p>
 * <ul>
 *   <li>{@code withValue(Object)} - direct value assignment</li>
 *   <li>{@code withValue(ISupplierBuilder)} - deferred, possibly context-aware resolution</li>
 *   <li>{@code allowNull(boolean)} - whether {@code null}/empty values are accepted</li>
 * </ul>
 *
 * <h2>Parameter Configuration</h2>
 * <p>
 * The {@link com.garganttua.core.reflection.binders.dsl.IParametrableBuilder} enables:
 * </p>
 * <ul>
 *   <li>Positional parameter selection ({@code withParam(int, ...)})</li>
 *   <li>Named parameter selection ({@code withParam(String, ...)})</li>
 *   <li>Sequential parameter appending ({@code withParam(Object)})</li>
 *   <li>Supplier-backed values for deferred resolution</li>
 *   <li>Per-parameter nullable control ({@code acceptNullable})</li>
 * </ul>
 *
 * <h2>Features</h2>
 * <ul>
 *   <li>Fluent, chainable API</li>
 *   <li>Type-safe configuration</li>
 *   <li>Direct and supplier-backed value sources</li>
 *   <li>Null safety with explicit opt-in</li>
 *   <li>Clear error messages via {@link com.garganttua.core.dsl.DslException}</li>
 * </ul>
 *
 * <h2>Builder Pattern</h2>
 * <p>
 * All builders follow these conventions:
 * </p>
 * <ul>
 *   <li>Method chaining for fluent configuration</li>
 *   <li>{@code up()} returns to the parent builder</li>
 *   <li>{@code build()} creates the binder</li>
 *   <li>Type parameters preserve compile-time safety</li>
 *   <li>Clear builder hierarchy</li>
 * </ul>
 *
 * @since 2.0.0-ALPHA01
 * @see com.garganttua.core.reflection.binders
 * @see com.garganttua.core.reflection.binders.dsl.IFieldBinderBuilder
 * @see com.garganttua.core.reflection.binders.dsl.IMethodBinderBuilder
 * @see com.garganttua.core.reflection.binders.dsl.IParametrableBuilder
 * @see com.garganttua.core.dsl.IAutomaticBuilder
 */
package com.garganttua.core.reflection.binders.dsl;

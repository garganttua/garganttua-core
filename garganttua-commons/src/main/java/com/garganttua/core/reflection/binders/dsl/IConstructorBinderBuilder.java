package com.garganttua.core.reflection.binders.dsl;

import com.garganttua.core.reflection.binders.IExecutableBinder;

/**
 * Builder interface for constructing constructor binders.
 *
 * <p>
 * {@code IConstructorBinderBuilder} specializes {@link IExecutableBinderBuilder} for
 * constructor-specific binder construction. It inherits all parameter configuration,
 * auto-detection, and navigation capabilities while focusing on object instantiation
 * scenarios. Constructors are identified by their class and parameter types, without
 * needing method names.
 * </p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Simple constructor with direct parameters
 * IConstructorBinder<Database> dbBinder = ConstructorBinderBuilder
 *     .forClass(Database.class)
 *     .withParam("jdbc:mysql://localhost:3306/db")
 *     .withParam(3306)
 *     .build();
 *
 * // Constructor with supplier-based parameters
 * IConstructorBinder<UserService> serviceBinder = ConstructorBinderBuilder
 *     .forClass(UserService.class)
 *     .withParam(SupplierBuilder.forType(UserRepository.class).withContext(...))
 *     .withParam(SupplierBuilder.forType(EmailService.class).withContext(...))
 *     .build();
 *
 * // Auto-detection of no-arg constructor
 * IConstructorBinder<Config> configBinder = ConstructorBinderBuilder
 *     .forClass(Config.class)
 *     .autoDetect(true)  // Finds and uses no-arg constructor
 *     .build();
 *
 * // Nullable parameter support
 * IConstructorBinder<Logger> loggerBinder = ConstructorBinderBuilder
 *     .forClass(Logger.class)
 *     .withParam("AppLogger")
 *     .withParam(null, true)  // Second param can be null
 *     .build();
 * }</pre>
 *
 * <h2>Constructor Resolution</h2>
 * <p>
 * The builder resolves the target constructor based on the number and types of
 * specified parameters. For classes with multiple constructors, the parameter
 * configuration must uniquely identify the desired constructor. Auto-detection
 * can simplify this for common cases like no-argument constructors.
 * </p>
 *
 * <h2>Dependency Injection</h2>
 * <p>
 * Constructor binders are the foundation of constructor-based dependency injection.
 * By combining parameter suppliers with contextual resolution, the builder enables
 * automatic dependency resolution at object instantiation time.
 * </p>
 *
 * @param <Constructed> the type of object the constructor creates
 * @param <Builder> the concrete builder type for method chaining
 * @param <Link> the type of the parent builder for hierarchical navigation
 * @param <Built> the specific constructor binder type being constructed
 * @since 2.0.0-ALPHA01
 * @see IExecutableBinderBuilder
 * @see com.garganttua.core.reflection.binders.IConstructorBinder
 */
public interface IConstructorBinderBuilder<Constructed, Builder extends IConstructorBinderBuilder<?, ?, ?, ?>, Link, Built extends IExecutableBinder<Constructed>>
                extends IExecutableBinderBuilder<Constructed, Builder, Link, Built> {
}
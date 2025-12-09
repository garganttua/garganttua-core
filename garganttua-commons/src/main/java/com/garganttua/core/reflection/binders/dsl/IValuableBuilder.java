package com.garganttua.core.reflection.binders.dsl;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.dsl.IAutomaticLinkedBuilder;
import com.garganttua.core.supply.ISupplier;
import com.garganttua.core.supply.dsl.ISupplierBuilder;

/**
 * Builder interface for configuring single-value elements (fields).
 *
 * <p>
 * {@code IValuableBuilder} provides a fluent API for specifying values in field binders.
 * Values can be provided as direct objects or through suppliers for dynamic resolution.
 * The builder supports nullable value control and hierarchical navigation through
 * {@link IAutomaticLinkedBuilder}.
 * </p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Field binder with direct value
 * IFieldBinder<User, String> nameBinder = FieldBinder
 *     .forClass(User.class)
 *     .field("name")
 *     .withValue("Alice")
 *     .build();
 *
 * // Field binder with supplier
 * IFieldBinder<App, Config> configBinder = FieldBinder
 *     .forClass(App.class)
 *     .field("config")
 *     .withValue(SupplierBuilder
 *         .forType(Config.class)
 *         .withContext(DiContext.class, ctx -> ctx.getBean(Config.class)))
 *     .build();
 *
 * // Nullable field
 * IFieldBinder<User, String> emailBinder = FieldBinder
 *     .forInstance(user)
 *     .field("email")
 *     .allowNull(true)
 *     .withValue(null)  // Allowed because of allowNull
 *     .build();
 * }</pre>
 *
 * <h2>Value Resolution</h2>
 * <p>
 * When using suppliers, value resolution is deferred until the binder is actually used.
 * This enables context-aware field injection where the value depends on runtime state.
 * </p>
 *
 * @param <Builder> the concrete builder type for method chaining
 * @param <Link> the type of the parent builder for hierarchical navigation
 * @param <Built> the type of binder being constructed
 * @since 2.0.0-ALPHA01
 * @see IFieldBinderBuilder
 * @see IAutomaticLinkedBuilder
 */
public interface IValuableBuilder<Builder, Link, Built> extends IAutomaticLinkedBuilder<Builder, Link, Built> {

    /**
     * Specifies a direct value for the field.
     *
     * <p>
     * The provided value will be used when setting the field through the binder.
     * The value type must be compatible with the field type.
     * </p>
     *
     * @param value the value to assign to the field (may be {@code null} if allowed)
     * @return this builder instance for method chaining
     * @throws DslException if the value type is incompatible with the field type,
     *                     or if {@code null} is provided when not allowed
     */
    IValuableBuilder<Builder, Link, Built> withValue(Object value) throws DslException;

    /**
     * Specifies a supplier that will provide the field value dynamically.
     *
     * <p>
     * The supplier will be invoked when the field is set, allowing for context-aware
     * or late-bound value resolution. This is particularly useful for dependency
     * injection scenarios.
     * </p>
     *
     * @param supplier the supplier that will provide the field value
     * @return this builder instance for method chaining
     * @throws DslException if the supplier's type is incompatible with the field type
     */
    IValuableBuilder<Builder, Link, Built> withValue(ISupplierBuilder<?, ? extends ISupplier<?>> supplier) throws DslException;

    /**
     * Configures whether {@code null} values are allowed for the field.
     *
     * <p>
     * When set to {@code true}, the builder will accept {@code null} values or
     * suppliers returning empty. When {@code false} (default), attempting to set
     * a {@code null} value will throw an exception.
     * </p>
     *
     * @param allowNull {@code true} to allow null values, {@code false} to reject them
     * @return this builder instance for method chaining
     * @throws DslException if the configuration is invalid
     */
    IValuableBuilder<Builder, Link, Built> allowNull(boolean allowNull) throws DslException;

}

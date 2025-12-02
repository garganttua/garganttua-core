package com.garganttua.core.reflection.binders.dsl;

import java.lang.reflect.Field;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.reflection.ObjectAddress;
import com.garganttua.core.reflection.binders.IFieldBinder;

/**
 * Builder interface for constructing field binders with various field resolution strategies.
 *
 * <p>
 * {@code IFieldBinderBuilder} extends {@link IValuableBuilder} to provide field-specific
 * configuration methods. Fields can be identified by name, by {@link Field} reference,
 * or by {@link ObjectAddress}. The builder inherits value configuration capabilities
 * from {@link IValuableBuilder}, enabling both direct value assignment and supplier-based
 * dynamic value resolution.
 * </p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Field identification by name
 * IFieldBinder<User, String> nameBinder = FieldBinderBuilder
 *     .forClass(User.class)
 *     .field("name")
 *     .withValue("Alice")
 *     .build();
 *
 * // Field using Field reference
 * Field emailField = User.class.getDeclaredField("email");
 * IFieldBinder<User, String> emailBinder = FieldBinderBuilder
 *     .forInstance(user)
 *     .field(emailField)
 *     .withValue("alice@example.com")
 *     .build();
 *
 * // Field with supplier for dynamic value
 * IFieldBinder<App, Config> configBinder = FieldBinderBuilder
 *     .forClass(App.class)
 *     .field("config")
 *     .withValue(SupplierBuilder
 *         .forType(Config.class)
 *         .withContext(DiContext.class, ctx -> ctx.getBean(Config.class)))
 *     .build();
 *
 * // Nullable field
 * IFieldBinder<User, String> optionalBinder = FieldBinderBuilder
 *     .forInstance(user)
 *     .field("middleName")
 *     .allowNull(true)
 *     .withValue(null)
 *     .build();
 *
 * // Field using ObjectAddress
 * ObjectAddress address = ObjectAddress.of("com.example.User#name");
 * IFieldBinder<User, String> addressBinder = FieldBinderBuilder
 *     .forClass(User.class)
 *     .field(address)
 *     .withValue("Bob")
 *     .build();
 * }</pre>
 *
 * <h2>Field Resolution Strategies</h2>
 * <ul>
 *   <li><b>By name</b>: {@link #field(String)} - Simple field name lookup</li>
 *   <li><b>By reference</b>: {@link #field(Field)} - Direct Field object</li>
 *   <li><b>By address</b>: {@link #field(ObjectAddress)} - Symbolic field address</li>
 * </ul>
 *
 * <h2>Field Injection Pattern</h2>
 * <p>
 * Field binders are commonly used for field-based dependency injection:
 * <ol>
 *   <li>Identify fields requiring injection (via annotations or configuration)</li>
 *   <li>Create field binders for each injectable field</li>
 *   <li>Configure value suppliers (often context-aware)</li>
 *   <li>Execute binders to inject values into target instances</li>
 * </ol>
 * </p>
 *
 * <h2>Private Field Access</h2>
 * <p>
 * The builder and resulting binder automatically handle accessibility for private
 * and protected fields, making them accessible for injection without requiring
 * explicit access modifier changes.
 * </p>
 *
 * @param <FieldType> the type of the field being bound
 * @param <OwnerType> the type of the object that owns the field
 * @param <Builder> the concrete builder type for method chaining
 * @param <Link> the type of the parent builder for hierarchical navigation
 * @since 2.0.0-ALPHA01
 * @see IValuableBuilder
 * @see IFieldBinder
 */
public interface IFieldBinderBuilder<FieldType, OwnerType, Builder, Link>
                extends IValuableBuilder<Builder, Link, IFieldBinder<OwnerType, FieldType>> {

        /**
         * Specifies the field to bind by name.
         *
         * <p>
         * The field will be resolved by searching for a field with the given name
         * in the owner class. This includes inherited fields from superclasses.
         * </p>
         *
         * @param fieldName the name of the field
         * @return this builder instance for method chaining
         * @throws DslException if the field name is invalid or the field cannot be found
         */
        IFieldBinderBuilder<FieldType, OwnerType, Builder, Link> field(String fieldName)
                        throws DslException;

        /**
         * Specifies the field to bind using a {@link Field} reference.
         *
         * <p>
         * This is the most direct method of field specification, using a Field
         * object obtained through reflection. This ensures exact field matching
         * and avoids ambiguity.
         * </p>
         *
         * @param field the Field object to bind
         * @return this builder instance for method chaining
         * @throws DslException if the field is null or incompatible with the owner type
         */
        IFieldBinderBuilder<FieldType, OwnerType, Builder, Link> field(Field field)
                        throws DslException;

        /**
         * Specifies the field to bind using an {@link ObjectAddress}.
         *
         * <p>
         * Object addresses provide a symbolic way to reference fields, often in
         * the form "com.example.Class#fieldName". This is useful for configuration-based
         * field binding where field references are stored as strings.
         * </p>
         *
         * @param address the symbolic address of the field
         * @return this builder instance for method chaining
         * @throws DslException if the address is invalid or the field cannot be resolved
         */
        IFieldBinderBuilder<FieldType, OwnerType, Builder, Link> field(ObjectAddress address)
                        throws DslException;

}

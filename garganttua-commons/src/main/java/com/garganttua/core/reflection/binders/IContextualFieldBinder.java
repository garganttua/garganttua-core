package com.garganttua.core.reflection.binders;

import com.garganttua.core.reflection.ReflectionException;

/**
 * Context-aware field binder for reflective field access with runtime value resolution.
 *
 * <p>
 * {@code IContextualFieldBinder} extends {@link IFieldBinder} to support field
 * injection where the field value is resolved from a runtime context. This enables
 * field-based dependency injection where field values are supplied by a DI container
 * or other context provider. The binder requires both an owner context (providing
 * the target instance) and a value context (providing the field value).
 * </p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Field injection requiring DiContext for value resolution
 * IContextualFieldBinder<UserController, Logger, DiContext, DiContext> loggerField =
 *     ContextualFieldBinder
 *         .forClass(UserController.class)
 *         .field("logger")
 *         .withValueType(Logger.class)
 *         .build();
 *
 * // Set field value from context
 * DiContext context = ...;
 * UserController controller = context.getBean(UserController.class);
 * loggerField.setValue(context, context);
 * // Resolves logger from context and injects into controller.logger
 *
 * // Get field value with context
 * Logger logger = loggerField.getValue(context);
 * }</pre>
 *
 * <h2>Dual Context Model</h2>
 * <p>
 * This interface uses a dual context model:
 * </p>
 * <ul>
 *   <li><b>Owner Context</b>: Provides or identifies the target instance that owns the field</li>
 *   <li><b>Value Context</b>: Provides the value to be injected into the field</li>
 * </ul>
 * <p>
 * In many scenarios, both contexts may be the same (e.g., a DI container providing
 * both the target instance and the field value).
 * </p>
 *
 * <h2>Field Injection Pattern</h2>
 * <p>
 * Common usage in dependency injection:
 * </p>
 * <ol>
 *   <li>Create or retrieve the target instance</li>
 *   <li>Identify fields that require injection (via annotations or configuration)</li>
 *   <li>Create contextual field binders for each injectable field</li>
 *   <li>Resolve field values from the context</li>
 *   <li>Inject resolved values using {@link #setValue(Object, Object)}</li>
 * </ol>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * Contextual field binders are not inherently thread-safe when operating on the
 * same target instance. If multiple threads need to inject fields on the same
 * object, external synchronization is required.
 * </p>
 *
 * @param <OnwerType> the type of the object that owns the field (note: typo in original)
 * @param <FieldType> the type of the field being accessed
 * @param <OwnerContextType> the type of context providing the target instance
 * @param <FieldContextType> the type of context providing the field value
 * @since 2.0.0-ALPHA01
 * @see IFieldBinder
 */
public interface IContextualFieldBinder<OnwerType, FieldType, OwnerContextType, FieldContextType>
        extends IFieldBinder<OnwerType, FieldType> {

    /**
     * Returns the owner context type for this field binder.
     *
     * <p>
     * This method declares the context type that provides or identifies the
     * target instance containing the field to be accessed.
     * </p>
     *
     * @return the {@link Class} object representing the owner context type
     */
    Class<OwnerContextType> getOwnerContextType();

    /**
     * Returns the value context type for this field binder.
     *
     * <p>
     * This method declares the context type that provides the value to be
     * injected into the field. This may be the same as or different from
     * the owner context type.
     * </p>
     *
     * @return the {@link Class} object representing the value context type
     */
    Class<FieldContextType> getValueContextType();

    /**
     * Sets the field value using the provided contexts.
     *
     * <p>
     * This method resolves the target instance from the owner context and
     * the field value from the value context, then performs the field injection.
     * The implementation handles field accessibility and type compatibility.
     * </p>
     *
     * @param ownerContext the context providing the target instance (never {@code null})
     * @param valueContext the context providing the field value (never {@code null})
     * @throws ReflectionException if the field cannot be accessed, the owner or value
     *                            cannot be resolved, the value type is incompatible,
     *                            or the field is final
     */
    void setValue(OwnerContextType ownerContext, FieldContextType valueContext) throws ReflectionException;

    /**
     * Retrieves the current field value using the provided owner context.
     *
     * <p>
     * This method resolves the target instance from the owner context and
     * reads the field value using reflection, returning it with proper type
     * casting.
     * </p>
     *
     * @param ownerContext the context providing the target instance (never {@code null})
     * @return the current field value, or {@code null} if the field is null
     * @throws ReflectionException if the field cannot be accessed, the owner cannot
     *                            be resolved, or the value cannot be retrieved
     */
    FieldType getValue(OwnerContextType ownerContext) throws ReflectionException;

    /**
     * Throws an exception indicating that context is required.
     *
     * <p>
     * This default implementation ensures that contextual field binders cannot
     * be used without providing the required contexts. Callers must use
     * {@link #setValue(Object, Object)} instead.
     * </p>
     *
     * @throws ReflectionException always, indicating that context is required
     */
    @Override
    default void setValue() throws ReflectionException {
        throw new ReflectionException(
                "Owner context of type " + getOwnerContextType().getSimpleName() + " required for this supplier");
    };

    /**
     * Throws an exception indicating that context is required.
     *
     * <p>
     * This default implementation ensures that contextual field binders cannot
     * be used without providing the required contexts. Callers must use
     * {@link #getValue(Object)} instead.
     * </p>
     *
     * @return never returns normally
     * @throws ReflectionException always, indicating that context is required
     */
    @Override
    default FieldType getValue() throws ReflectionException {
        throw new ReflectionException(
                "Owner context of type " + getOwnerContextType().getSimpleName() + " required for this supplier");
    };

}

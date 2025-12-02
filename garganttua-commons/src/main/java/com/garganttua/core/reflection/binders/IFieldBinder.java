package com.garganttua.core.reflection.binders;

import com.garganttua.core.reflection.ReflectionException;

/**
 * Binder interface for reflective field access and modification.
 *
 * <p>
 * {@code IFieldBinder} provides a type-safe abstraction for reading and writing
 * object fields via reflection. It encapsulates field resolution, accessibility
 * handling, and value conversion, offering a fluent API for field manipulation.
 * This is particularly useful for dependency injection, serialization frameworks,
 * and testing scenarios where direct field access is required.
 * </p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Creating a field binder
 * User user = new User();
 * IFieldBinder<User, String> nameBinder = FieldBinder
 *     .forInstance(user)
 *     .field("name")
 *     .withValue("Alice")
 *     .build();
 *
 * // Setting field value
 * nameBinder.setValue();  // Sets user.name = "Alice"
 *
 * // Getting field value
 * String name = nameBinder.getValue();  // Returns "Alice"
 *
 * // Field reference for debugging
 * String ref = nameBinder.getFieldReference();
 * // Returns something like "User.name"
 * }</pre>
 *
 * <h2>Accessibility Handling</h2>
 * <p>
 * Field binders automatically handle accessibility modifiers (private, protected)
 * by temporarily suppressing access checks when necessary. This allows reading
 * and writing of private fields, which is essential for frameworks that need to
 * bypass normal access restrictions.
 * </p>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * Field binders are not inherently thread-safe when operating on the same
 * target instance. If multiple threads need to access or modify fields on
 * the same object, external synchronization is required.
 * </p>
 *
 * @param <OnwerType> the type of the object that owns the field (note: typo in original)
 * @param <FieldType> the type of the field being accessed
 * @since 2.0.0-ALPHA01
 * @see IContextualFieldBinder
 */
public interface IFieldBinder<OnwerType, FieldType> {

    /**
     * Sets the field value on the target instance.
     *
     * <p>
     * This method writes the pre-configured value to the bound field. The value
     * must have been specified during binder construction. This is the non-contextual
     * version suitable for simple field injection scenarios.
     * </p>
     *
     * @throws ReflectionException if the field cannot be accessed, the value type
     *                            is incompatible, or the field is final
     */
    void setValue() throws ReflectionException;

    /**
     * Retrieves the current value of the field from the target instance.
     *
     * <p>
     * This method reads the field value using reflection and returns it with
     * proper type casting. For primitive fields, the value is automatically
     * boxed to the corresponding wrapper type.
     * </p>
     *
     * @return the current field value, or {@code null} if the field is null
     * @throws ReflectionException if the field cannot be accessed or the value
     *                            cannot be retrieved
     */
    FieldType getValue() throws ReflectionException;

    /**
     * Returns a string reference identifying the bound field.
     *
     * <p>
     * This reference is primarily used for debugging, logging, and error messages.
     * The format is typically "ClassName.fieldName" or similar.
     * </p>
     *
     * @return a string identifying the bound field (never {@code null})
     */
    String getFieldReference();

}

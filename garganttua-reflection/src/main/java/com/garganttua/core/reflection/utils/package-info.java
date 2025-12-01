/**
 * Reflection utility classes and helper functions.
 *
 * <h2>Overview</h2>
 * <p>
 * This package provides utility classes and helper functions for common reflection
 * operations. It includes utilities for type conversion, accessibility management,
 * annotation processing, and reflection performance optimization.
 * </p>
 *
 * <h2>Utility Categories</h2>
 * <ul>
 *   <li><b>Type Conversion</b> - Converting between types</li>
 *   <li><b>Accessibility</b> - Managing field/method accessibility</li>
 *   <li><b>Annotation Processing</b> - Extracting annotation metadata</li>
 *   <li><b>Caching</b> - Caching reflection metadata for performance</li>
 *   <li><b>Validation</b> - Validating reflection operations</li>
 * </ul>
 *
 * <h2>Usage Example: Type Conversion</h2>
 * <pre>{@code
 * // Convert string to various types
 * Integer intValue = TypeConverter.convert("42", Integer.class);
 * Boolean boolValue = TypeConverter.convert("true", Boolean.class);
 * LocalDate dateValue = TypeConverter.convert("2025-01-01", LocalDate.class);
 *
 * // Convert with default value
 * int timeout = TypeConverter.convert("invalid", int.class, 30);
 * }</pre>
 *
 * <h2>Usage Example: Accessibility Management</h2>
 * <pre>{@code
 * Field privateField = User.class.getDeclaredField("password");
 *
 * // Make field accessible
 * AccessibilityUtils.makeAccessible(privateField);
 *
 * // Set value
 * privateField.set(user, "newPassword");
 *
 * // Restore accessibility
 * AccessibilityUtils.restoreAccessibility(privateField);
 * }</pre>
 *
 * <h2>Usage Example: Annotation Processing</h2>
 * <pre>{@code
 * // Get annotation value
 * Property propAnnotation = field.getAnnotation(Property.class);
 * String propertyName = AnnotationUtils.getValue(propAnnotation, "value");
 *
 * // Check annotation presence with inheritance
 * boolean hasInject = AnnotationUtils.hasAnnotation(
 *     field,
 *     Inject.class,
 *     true  // check inherited
 * );
 *
 * // Get all annotations
 * List<Annotation> annotations = AnnotationUtils.getAllAnnotations(field);
 * }</pre>
 *
 * <h2>Usage Example: Reflection Caching</h2>
 * <pre>{@code
 * // Cache field lookup
 * Field field = ReflectionCache.getField(User.class, "email");
 *
 * // Cache method lookup
 * Method method = ReflectionCache.getMethod(
 *     UserService.class,
 *     "updateUser",
 *     String.class, String.class
 * );
 *
 * // Cache constructor lookup
 * Constructor<User> constructor = ReflectionCache.getConstructor(
 *     User.class,
 *     String.class, int.class
 * );
 *
 * // Clear cache
 * ReflectionCache.clear();
 * }</pre>
 *
 * <h2>Usage Example: Validation</h2>
 * <pre>{@code
 * // Validate field is accessible
 * ReflectionValidator.validateFieldAccess(field, User.class);
 *
 * // Validate method signature
 * ReflectionValidator.validateMethodSignature(
 *     method,
 *     void.class,
 *     String.class, int.class
 * );
 *
 * // Validate constructor parameters
 * ReflectionValidator.validateConstructorParameters(
 *     constructor,
 *     String.class, int.class
 * );
 * }</pre>
 *
 * <h2>Features</h2>
 * <ul>
 *   <li>Comprehensive type conversion</li>
 *   <li>Accessibility management (setAccessible)</li>
 *   <li>Annotation metadata extraction</li>
 *   <li>Reflection metadata caching</li>
 *   <li>Validation utilities</li>
 *   <li>Generic type resolution</li>
 *   <li>Exception handling helpers</li>
 *   <li>Performance optimization</li>
 * </ul>
 *
 * @since 2.0.0-ALPHA01
 * @see com.garganttua.core.reflection
 */
package com.garganttua.core.reflection.utils;

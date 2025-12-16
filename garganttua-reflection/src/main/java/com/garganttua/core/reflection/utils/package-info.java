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
 * <h2>Usage Example: ObjectReflectionHelper (from ObjectReflectionHelperTest)</h2>
 * <pre>{@code
 * class SuperClass {
 *     Long superField;
 * }
 *
 * class Entity extends SuperClass {
 *     String field;
 *
 *     public String aMethod(String test, SuperClass superClass) {
 *         return null;
 *     }
 * }
 *
 * // Get field from class
 * Field fieldField = ObjectReflectionHelper.getField(Entity.class, "field");
 * assertNotNull(fieldField);
 *
 * // Try to get non-existent field
 * Field testField = ObjectReflectionHelper.getField(Entity.class, "test");
 * assertNull(testField);
 *
 * // Get field from superclass - searches inheritance hierarchy
 * Field superFieldField = ObjectReflectionHelper.getField(Entity.class, "superField");
 * assertNotNull(superFieldField);  // Successfully finds field in parent class
 *
 * // Get method
 * Method method = ObjectReflectionHelper.getMethod(Entity.class, "aMethod");
 * assertNotNull(method);
 * }</pre>
 *
 * <h2>Usage Example: Field Resolution Across Hierarchies</h2>
 * <pre>{@code
 * // ObjectReflectionHelper can find fields in parent classes
 * class Parent {
 *     private Long id;
 * }
 *
 * class Child extends Parent {
 *     private String name;
 * }
 *
 * // Find field in child class
 * Field nameField = ObjectReflectionHelper.getField(Child.class, "name");
 * assertNotNull(nameField);
 *
 * // Find field in parent class through child
 * Field idField = ObjectReflectionHelper.getField(Child.class, "id");
 * assertNotNull(idField);  // Finds field in parent
 * }</pre>
 *
 * <h2>Usage Example: Method Resolution</h2>
 * <pre>{@code
 * class Service {
 *     public void process(String data) {
 *         // implementation
 *     }
 *
 *     private void validate(Object obj) {
 *         // implementation
 *     }
 * }
 *
 * // Get public method
 * Method processMethod = ObjectReflectionHelper.getMethod(Service.class, "process");
 * assertNotNull(processMethod);
 *
 * // Get private method
 * Method validateMethod = ObjectReflectionHelper.getMethod(Service.class, "validate");
 * assertNotNull(validateMethod);
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

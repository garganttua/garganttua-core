package com.garganttua.core.nativve;

import java.util.List;
import java.util.Objects;

/**
 * Interface representing a single reflection configuration entry for GraalVM native image.
 *
 * <p>
 * {@code IReflectionConfigurationEntry} represents a class and its associated reflection
 * metadata (fields, methods, constructors) that should be accessible via reflection in
 * GraalVM native images. This corresponds to a single entry in the reflect-config.json file.
 * </p>
 *
 * <p>
 * <strong>Usage Example:</strong>
 * </p>
 * <pre>{@code
 * IReflectionConfigurationEntry entry = createEntry(MyService.class);
 *
 * // Configure reflection access
 * entry.setQueryAllDeclaredConstructors(true);
 * entry.setQueryAllDeclaredMethods(true);
 * entry.setAllDeclaredFields(true);
 *
 * // Or configure specific members
 * List<Field> fields = List.of(createField("serviceName"), createField("serviceId"));
 * entry.setFields(fields);
 *
 * List<Method> methods = List.of(
 *     createMethod("executeService", List.of("java.lang.String")),
 *     createMethod("initialize", List.of())
 * );
 * entry.setMethods(methods);
 * }</pre>
 *
 * @since 2.0.0-ALPHA01
 * @see IReflectionConfiguration
 * @see IReflectionConfigurationEntryBuilder
 */
public interface IReflectionConfigurationEntry {

    /**
     * Represents a field in the reflection configuration.
     *
     * <p>
     * This class holds metadata about a field that should be accessible via reflection
     * in GraalVM native images.
     * </p>
     *
     * @since 2.0.0-ALPHA01
     */
    public static class Field {
        private String name;

        /**
         * Creates a new Field instance with default settings.
         */
        public Field() {
        }

        /**
         * Returns the name of the field.
         *
         * @return the field name
         */
        public String getName() {
            return name;
        }

        /**
         * Sets the name of the field.
         *
         * @param name the field name
         */
        public void setName(String name) {
            this.name = name;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            Field field = (Field) o;
            return Objects.equals(name, field.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name);
        }
    }

    /**
     * Represents a method in the reflection configuration.
     *
     * <p>
     * This class holds metadata about a method including its name and parameter types
     * that should be accessible via reflection in GraalVM native images.
     * </p>
     *
     * @since 2.0.0-ALPHA01
     */
    public static class Method {
        private String name;
        private List<String> parameterTypes;

        /**
         * Creates a new Method instance with default settings.
         */
        public Method() {
        }

        /**
         * Returns the name of the method.
         *
         * @return the method name
         */
        public String getName() {
            return name;
        }

        /**
         * Sets the name of the method.
         *
         * @param name the method name
         */
        public void setName(String name) {
            this.name = name;
        }

        /**
         * Returns the list of parameter types for the method.
         *
         * @return the list of fully qualified parameter type names
         */
        public List<String> getParameterTypes() {
            return parameterTypes;
        }

        /**
         * Sets the list of parameter types for the method.
         *
         * @param parameterTypes the list of fully qualified parameter type names
         */
        public void setParameterTypes(List<String> parameterTypes) {
            this.parameterTypes = parameterTypes;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            Method method = (Method) o;
            return Objects.equals(name, method.name) &&
                    Objects.equals(parameterTypes, method.parameterTypes);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, parameterTypes);
        }
    }

    /**
     * Returns the fully qualified class name for this reflection entry.
     *
     * @return the class name
     */
    public String getName();

    /**
     * Sets the list of fields to be registered for reflection access.
     *
     * @param fields the list of fields
     */
    public void setFields(List<Field> fields);

    /**
     * Sets the list of methods to be registered for reflection access.
     *
     * @param methods the list of methods
     */
    public void setMethods(List<Method> methods);

    /**
     * Enables or disables querying all declared constructors via reflection.
     *
     * @param value {@code true} to enable, {@code false} to disable
     */
    public void setQueryAllDeclaredConstructors(boolean value);

    /**
     * Enables or disables querying all public constructors via reflection.
     *
     * @param value {@code true} to enable, {@code false} to disable
     */
    public void setQueryAllPublicConstructors(boolean value);

    /**
     * Enables or disables querying all declared methods via reflection.
     *
     * @param value {@code true} to enable, {@code false} to disable
     */
    public void setQueryAllDeclaredMethods(boolean value);

    /**
     * Enables or disables querying all public methods via reflection.
     *
     * @param value {@code true} to enable, {@code false} to disable
     */
    public void setQueryAllPublicMethods(boolean value);

    /**
     * Enables or disables access to all declared inner classes via reflection.
     *
     * @param value {@code true} to enable, {@code false} to disable
     */
    public void setAllDeclaredClasses(boolean value);

    /**
     * Enables or disables access to all public inner classes via reflection.
     *
     * @param value {@code true} to enable, {@code false} to disable
     */
    public void setAllPublicClasses(boolean value);

    /**
     * Enables or disables access to all declared fields via reflection.
     *
     * @param value {@code true} to enable, {@code false} to disable
     */
    public void setAllDeclaredFields(boolean value);

    /**
     * Returns the list of fields registered for reflection access.
     *
     * @return the list of fields, may be empty
     */
    public List<Field> getFields();

    /**
     * Returns the list of methods registered for reflection access.
     *
     * @return the list of methods, may be empty
     */
    public List<Method> getMethods();

    /**
     * Returns the Class object corresponding to this reflection entry.
     *
     * @return the Class object for this entry
     * @throws ClassNotFoundException if the class cannot be found
     */
    public Class<?> getEntryClass() throws ClassNotFoundException;
}

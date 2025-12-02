package com.garganttua.core.utils;

/**
 * Interface for objects that can create deep copies of themselves.
 *
 * <p>
 * {@code Copyable} defines a contract for creating independent copies of objects.
 * Implementing classes should ensure that the copy is a deep copy, meaning that
 * mutable objects referenced by the original are also copied rather than shared.
 * </p>
 *
 * <p>
 * <strong>Usage Examples:</strong>
 * </p>
 * <pre>{@code
 * public class Configuration implements Copyable<Configuration> {
 *     private String name;
 *     private List<String> properties;
 *
 *     @Override
 *     public Configuration copy() throws CopyException {
 *         try {
 *             Configuration copy = new Configuration();
 *             copy.name = this.name;
 *             copy.properties = new ArrayList<>(this.properties);
 *             return copy;
 *         } catch (Exception e) {
 *             throw new CopyException(e);
 *         }
 *     }
 * }
 *
 * // Using the copyable interface
 * Configuration original = new Configuration();
 * original.setName("Production");
 *
 * Configuration clone = original.copy();
 * clone.setName("Development");
 *
 * // original and clone are independent
 * }</pre>
 *
 * @param <T> the type of object being copied
 *
 * @since 2.0.0-ALPHA01
 * @see CopyException
 */
public interface Copyable<T> {

    /**
     * Creates and returns a deep copy of this object.
     *
     * <p>
     * The returned object should be independent of the original, meaning that
     * modifications to the copy should not affect the original object and vice versa.
     * </p>
     *
     * @return a deep copy of this object
     * @throws CopyException if the copy operation fails
     */
    T copy() throws CopyException;

}

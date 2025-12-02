package com.garganttua.core.dsl;

/**
 * Base builder interface for constructing objects using the builder pattern.
 *
 * <p>
 * {@code IBuilder} defines the foundational contract for all builders in the Garganttua
 * ecosystem. Builders accumulate configuration through method chaining and produce
 * the final object via the {@link #build()} method.
 * </p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Simple builder implementation
 * public class UserBuilder implements IBuilder<User> {
 *     private String name;
 *     private int age;
 *
 *     public UserBuilder name(String name) {
 *         this.name = name;
 *         return this;
 *     }
 *
 *     public UserBuilder age(int age) {
 *         this.age = age;
 *         return this;
 *     }
 *
 *     @Override
 *     public User build() throws DslException {
 *         if (name == null) {
 *             throw new DslException("Name is required");
 *         }
 *         return new User(name, age);
 *     }
 * }
 *
 * // Usage
 * User user = new UserBuilder()
 *     .name("Alice")
 *     .age(30)
 *     .build();
 * }</pre>
 *
 * <h2>Design Benefits</h2>
 * <ul>
 *   <li>Fluent, chainable API for configuration</li>
 *   <li>Type-safe object construction</li>
 *   <li>Validation before object creation</li>
 *   <li>Immutable objects produced safely</li>
 * </ul>
 *
 * @param <Built> the type of object this builder constructs
 * @since 2.0.0-ALPHA01
 * @see ILinkedBuilder
 * @see IAutomaticBuilder
 */
public interface IBuilder<Built> {

    /**
     * Constructs and returns the built object.
     *
     * <p>
     * This method validates the accumulated configuration and creates the final object.
     * Once called, the builder may or may not be reusable depending on the implementation.
     * </p>
     *
     * @return the constructed object (never {@code null})
     * @throws DslException if the configuration is invalid or object construction fails
     */
    Built build() throws DslException;

}

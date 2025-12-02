package com.garganttua.core.dsl;

/**
 * Builder interface that supports hierarchical navigation to parent builders.
 *
 * <p>
 * {@code ILinkedBuilder} extends {@link IBuilder} to enable bidirectional navigation
 * in nested builder hierarchies. This allows fluent APIs where child builders can
 * navigate back to their parent builder, enabling complex object graphs to be
 * constructed through intuitive method chaining.
 * </p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Building a nested object structure
 * Company company = new CompanyBuilder()
 *     .name("TechCorp")
 *     .department()
 *         .name("Engineering")
 *         .employee()
 *             .name("Alice")
 *             .role("Developer")
 *             .up()  // Returns to DepartmentBuilder
 *         .employee()
 *             .name("Bob")
 *             .role("Manager")
 *             .up()  // Returns to DepartmentBuilder
 *         .up()  // Returns to CompanyBuilder
 *     .build();
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * Implementations are typically not thread-safe. Each builder instance should be
 * used by a single thread during construction.
 * </p>
 *
 * @param <Link> the type of the parent builder to navigate back to
 * @param <Built> the type of object this builder constructs
 * @since 2.0.0-ALPHA01
 * @see IBuilder
 * @see IAutomaticLinkedBuilder
 */
public interface ILinkedBuilder<Link, Built> extends IBuilder<Built> {

    /**
     * Navigates back to the parent builder.
     *
     * <p>
     * This method allows fluent navigation up the builder hierarchy, enabling
     * the construction of nested object structures through method chaining.
     * </p>
     *
     * @return the parent builder instance
     */
    Link up();

    /**
     * Sets the parent builder for this linked builder.
     *
     * <p>
     * This method establishes the hierarchical relationship between this builder
     * and its parent. It is typically called internally during builder construction
     * and should not be invoked by client code.
     * </p>
     *
     * @param up the parent builder instance
     */
    void setUp(Link up);

}

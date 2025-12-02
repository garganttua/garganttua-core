package com.garganttua.core.injection;

import java.util.Optional;

/**
 * Represents a query operation for retrieving a bean from the dependency injection context.
 *
 * <p>
 * An {@code IBeanQuery} encapsulates the criteria and execution logic for locating
 * a specific bean instance. It provides a deferred execution model where the query
 * can be constructed, passed around, and executed when needed. This interface follows
 * the Command pattern for bean retrieval operations.
 * </p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Build and execute a bean query
 * IBeanQueryBuilder<MyService> builder = context.queryBean();
 * IBeanQuery<MyService> query = builder
 *     .type(MyService.class)
 *     .name("primaryService")
 *     .strategy(BeanStrategy.SINGLETON)
 *     .build();
 *
 * // Execute the query
 * Optional<MyService> service = query.execute();
 * service.ifPresent(s -> s.doWork());
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * Thread safety depends on the underlying context and query implementation.
 * Query execution should be idempotent but may not be thread-safe.
 * </p>
 *
 * @param <Bean> the type of bean this query retrieves
 * @since 2.0.0-ALPHA01
 * @see IBeanQueryBuilder
 * @see IDiContext
 * @see BeanDefinition
 */
public interface IBeanQuery<Bean> {

    /**
     * Executes the query and returns the matching bean if found.
     *
     * <p>
     * This method performs the actual bean lookup based on the criteria
     * configured in this query. If no matching bean is found, an empty
     * Optional is returned. If multiple beans match, the behavior is
     * implementation-specific.
     * </p>
     *
     * @return an {@link Optional} containing the bean if found, or empty if not found
     * @throws DiException if an error occurs during query execution or bean instantiation
     */
    Optional<Bean> execute() throws DiException;

}

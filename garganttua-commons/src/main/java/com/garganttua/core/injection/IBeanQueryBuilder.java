package com.garganttua.core.injection;

import java.lang.annotation.Annotation;

import com.garganttua.core.dsl.IBuilder;

/**
 * Builder interface for constructing bean queries with specific criteria.
 *
 * <p>
 * {@code IBeanQueryBuilder} provides a fluent API for building {@link IBeanQuery} instances
 * with various search criteria including type, name, qualifiers, strategy, and provider.
 * This builder follows the Builder pattern to create complex bean lookup queries in a
 * readable and maintainable way.
 * </p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Build a query for a specific bean
 * IInjectionContext context = ...;
 * IBeanQuery<DataSource> query = context.queryBean()
 *     .type(DataSource.class)
 *     .name("primaryDataSource")
 *     .qualifier(Primary.class)
 *     .strategy(BeanStrategy.SINGLETON)
 *     .provider("dbProvider")
 *     .build();
 *
 * Optional<DataSource> dataSource = query.execute();
 * }</pre>
 *
 * @param <Bean> the type of bean to query for
 * @since 2.0.0-ALPHA01
 * @see IBeanQuery
 * @see BeanDefinition
 * @see IBuilder
 */
public interface IBeanQueryBuilder<Bean> extends IBuilder<IBeanQuery<Bean>>{

    /**
     * Specifies the type of bean to query for.
     *
     * <p>
     * The query will match beans that are instances of or assignable to the specified type.
     * </p>
     *
     * @param type the class of the bean to query
     * @return this builder for method chaining
     */
    IBeanQueryBuilder<Bean> type(Class<Bean> type);

    /**
     * Specifies the name of the bean to query for.
     *
     * <p>
     * The query will match beans with the exact specified name. If not set,
     * the query matches beans regardless of their name.
     * </p>
     *
     * @param name the bean name
     * @return this builder for method chaining
     */
    IBeanQueryBuilder<Bean> name(String name);

    /**
     * Adds a qualifier annotation to the query criteria.
     *
     * <p>
     * The query will only match beans that have the specified qualifier annotation.
     * Multiple qualifiers can be added, and all must match for a bean to be selected.
     * </p>
     *
     * @param qualifier the qualifier annotation class
     * @return this builder for method chaining
     * @throws DiException if the qualifier is invalid or cannot be processed
     */
    IBeanQueryBuilder<Bean> qualifier(Class<? extends Annotation> qualifier) throws DiException;

    /**
     * Specifies the bean strategy (scope) to query for.
     *
     * <p>
     * The query will match only beans with the specified strategy (e.g., SINGLETON, PROTOTYPE).
     * </p>
     *
     * @param strategy the bean strategy
     * @return this builder for method chaining
     * @see BeanStrategy
     */
    IBeanQueryBuilder<Bean> strategy(BeanStrategy strategy);

    /**
     * Specifies the provider from which to retrieve the bean.
     *
     * <p>
     * The query will search only within the specified provider. If not set,
     * the query searches across all available providers.
     * </p>
     *
     * @param provider the provider name
     * @return this builder for method chaining
     */
    IBeanQueryBuilder<Bean> provider(String provider);

}

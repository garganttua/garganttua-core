package com.garganttua.core.injection.context.dsl;

import com.garganttua.core.reflection.binders.Dependent;
import com.garganttua.core.reflection.binders.IConstructorBinder;
import com.garganttua.core.reflection.binders.dsl.IConstructorBinderBuilder;

/**
 * Builder interface for configuring constructor-based bean instantiation with parameter injection.
 *
 * <p>
 * {@code IBeanConstructorBinderBuilder} provides a fluent API for configuring which constructor
 * to use for bean instantiation and how to resolve and inject its parameters. It extends
 * {@link IConstructorBinderBuilder} with DI-specific functionality and tracks dependencies for
 * circular dependency detection. This builder is linked to {@link IBeanFactoryBuilder},
 * enabling seamless integration into bean factory configuration.
 * </p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Configure bean with constructor injection
 * beanProviderBuilder
 *     .withBean(DatabaseService.class)
 *         .strategy(BeanStrategy.singleton)
 *         .constructor()
 *             .withParameter(DataSource.class)
 *                 .fromBean()
 *                     .type(DataSource.class)
 *                     .name("primary")
 *                     .and()
 *                 .and()
 *             .withParameter(Integer.class)
 *                 .fromProperty()
 *                     .key("database.pool.size")
 *                     .and()
 *                 .and()
 *             .and()
 *         .build();
 *
 * // Multiple parameters with annotations
 * beanProviderBuilder
 *     .withBean(EmailService.class)
 *         .constructor()
 *             .withParameter(String.class)
 *                 .fromProperty()
 *                     .key("smtp.host")
 *                     .and()
 *                 .and()
 *             .withParameter(Integer.class)
 *                 .fromProperty()
 *                     .key("smtp.port")
 *                     .and()
 *                 .and()
 *             .and()
 *         .build();
 * }</pre>
 *
 * @param <Bean> the type of bean this constructor builder is for
 * @since 2.0.0-ALPHA01
 * @see IConstructorBinder
 * @see IBeanFactoryBuilder
 * @see Dependent
 */
public interface IBeanConstructorBinderBuilder<Bean> extends
        IConstructorBinderBuilder<Bean, IBeanConstructorBinderBuilder<Bean>, IBeanFactoryBuilder<Bean>, IConstructorBinder<Bean>>, Dependent {

}

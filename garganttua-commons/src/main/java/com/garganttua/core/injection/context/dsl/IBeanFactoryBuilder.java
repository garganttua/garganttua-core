package com.garganttua.core.injection.context.dsl;

import java.lang.annotation.Annotation;
import java.util.Set;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.dsl.IAutomaticBuilder;
import com.garganttua.core.dsl.dependency.IDependentBuilder;
import com.garganttua.core.injection.BeanStrategy;
import com.garganttua.core.injection.IBeanFactory;
import com.garganttua.core.reflection.binders.Dependent;
import com.garganttua.core.supply.dsl.ISupplierBuilder;

/**
 * Builder interface for constructing bean factories with complete bean metadata and dependencies.
 *
 * <p>
 * {@code IBeanFactoryBuilder} provides a fluent API for building {@link IBeanFactory} instances
 * with comprehensive configuration including strategy (scope), name, qualifiers, constructor binding,
 * post-construct methods, and injectable fields. This builder enables fine-grained control over
 * bean instantiation and dependency injection behavior.
 * </p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Build a bean factory with full configuration
 * IBeanFactory<MyService> factory = beanProviderBuilder
 *     .withBean(MyService.class)
 *         .strategy(BeanStrategy.SINGLETON)
 *         .name("primaryService")
 *         .qualifier(Primary.class)
 *         .constructor()
 *             .withParameter(DataSource.class)
 *                 .fromBean()
 *                 .type(DataSource.class)
 *                 .and()
 *             .and()
 *         .field(Logger.class)
 *             .named("logger")
 *             .fromBean()
 *                 .type(Logger.class)
 *                 .and()
 *             .and()
 *         .postConstruction()
 *             .method("initialize")
 *             .and()
 *         .build();
 * }</pre>
 *
 * @param <Bean> the type of bean this factory produces
 * @since 2.0.0-ALPHA01
 * @see IBeanFactory
 * @see BeanStrategy
 * @see ISupplierBuilder
 * @see Dependent
 */
public interface IBeanFactoryBuilder<Bean> extends IAutomaticBuilder<IBeanFactoryBuilder<Bean>, IBeanFactory<Bean>>, ISupplierBuilder<Bean, IBeanFactory<Bean>>, IDependentBuilder<IBeanFactoryBuilder<Bean>, IBeanFactory<Bean>> {


    IBeanFactoryBuilder<Bean> bean(Bean bean) throws DslException;
    
    /**
     * Specifies the bean strategy (scope) for this factory.
     *
     * @param singleton the bean strategy (SINGLETON, PROTOTYPE, etc.)
     * @return this builder for method chaining
     */
    IBeanFactoryBuilder<Bean> strategy(BeanStrategy singleton);

    /**
     * Begins configuration of the constructor binder for bean instantiation.
     *
     * @return a constructor binder builder
     * @throws DslException if the constructor cannot be configured
     */
    IBeanConstructorBinderBuilder<Bean> constructor() throws DslException;

    /**
     * Begins configuration of a post-construct method to be invoked after instantiation.
     *
     * @return a post-construct method binder builder
     * @throws DslException if the post-construct method cannot be configured
     */
    IBeanPostConstructMethodBinderBuilder<Bean> postConstruction() throws DslException;

    /**
     * Begins configuration of an injectable field on the bean.
     *
     * @param <FieldType> the type of the field
     * @param fieldType the class of the field type
     * @return an injectable field builder
     * @throws DslException if the field cannot be configured
     */
    <FieldType> IBeanInjectableFieldBuilder<FieldType, Bean> field(Class<FieldType> fieldType)
            throws DslException;

    /**
     * Specifies the name for this bean.
     *
     * @param name the bean name
     * @return this builder for method chaining
     */
    IBeanFactoryBuilder<Bean> name(String name);

    /**
     * Adds a qualifier annotation to this bean.
     *
     * @param qualifier the qualifier annotation class
     * @return this builder for method chaining
     * @throws DslException if the qualifier cannot be added
     */
    IBeanFactoryBuilder<Bean> qualifier(Class<? extends Annotation> qualifier) throws DslException;

    /**
     * Adds multiple qualifier annotations to this bean.
     *
     * @param qualifiers the set of qualifier annotation classes
     * @return this builder for method chaining
     * @throws DslException if the qualifiers cannot be added
     */
    IBeanFactoryBuilder<Bean> qualifiers(Set<Class<? extends Annotation>> qualifiers) throws DslException;

}

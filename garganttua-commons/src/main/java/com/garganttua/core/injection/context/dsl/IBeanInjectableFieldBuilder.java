package com.garganttua.core.injection.context.dsl;

import java.lang.reflect.Field;

import com.garganttua.core.reflection.binders.Dependent;
import com.garganttua.core.reflection.binders.dsl.IFieldBinderBuilder;
import com.garganttua.core.supply.ISupplier;
import com.garganttua.core.supply.dsl.ISupplierBuilder;

/**
 * Builder interface for configuring field injection on beans.
 *
 * <p>
 * {@code IBeanInjectableFieldBuilder} provides a fluent API for configuring how bean fields
 * should be injected with values from the DI context. It extends {@link IFieldBinderBuilder}
 * with DI-specific functionality and tracks dependencies for circular dependency detection.
 * This builder is linked to {@link IBeanFactoryBuilder}, enabling seamless integration into
 * bean factory configuration. Field injection is applied after constructor injection but
 * before post-construct methods.
 * </p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Configure bean with field injection
 * beanProviderBuilder
 *     .withBean(UserService.class)
 *         .strategy(BeanStrategy.singleton)
 *         .field(Logger.class)
 *             .named("logger")
 *             .fromBean()
 *                 .type(Logger.class)
 *                 .name("applicationLogger")
 *                 .and()
 *             .and()
 *         .field(CacheService.class)
 *             .named("cacheService")
 *             .fromBean()
 *                 .type(CacheService.class)
 *                 .and()
 *             .and()
 *         .build();
 *
 * // Field injection with property
 * beanProviderBuilder
 *     .withBean(EmailService.class)
 *         .field(String.class)
 *             .named("smtpHost")
 *             .fromProperty()
 *                 .key("smtp.host")
 *                 .and()
 *             .and()
 *         .field(Integer.class)
 *             .named("smtpPort")
 *             .fromProperty()
 *                 .key("smtp.port")
 *                 .and()
 *             .and()
 *         .build();
 * }</pre>
 *
 * @param <FieldType> the type of the field to inject
 * @param <BeanType> the type of bean containing the field
 * @since 2.0.0-ALPHA01
 * @see IFieldBinderBuilder
 * @see IBeanFactoryBuilder
 * @see Dependent
 */
public interface IBeanInjectableFieldBuilder<FieldType, BeanType> extends IFieldBinderBuilder<FieldType, BeanType, IBeanInjectableFieldBuilder<FieldType, BeanType>, IBeanFactoryBuilder<BeanType>>, Dependent {


    public IBeanInjectableFieldBuilder<FieldType, BeanType> ownerSupplierBuilder(ISupplierBuilder<BeanType, ? extends ISupplier<BeanType>> ownerSupplierBuilder);

    /**
     * Returns the field that will be injected.
     *
     * <p>
     * This method provides access to the field metadata for reflection operations
     * or validation purposes.
     * </p>
     *
     * @return the field to be injected
     */
    public Field field();

}

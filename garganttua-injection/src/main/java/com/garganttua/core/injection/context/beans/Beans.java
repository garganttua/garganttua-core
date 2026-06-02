package com.garganttua.core.injection.context.beans;

import java.lang.annotation.Annotation;
import java.util.Optional;
import java.util.Set;

import jakarta.annotation.Nullable;

import com.garganttua.core.observability.Logger;
import com.garganttua.core.expression.annotations.Expression;
import com.garganttua.core.injection.BeanReference;
import com.garganttua.core.injection.BeanStrategy;
import com.garganttua.core.injection.IBeanQueryBuilder;
import com.garganttua.core.injection.context.dsl.BeanQueryBuilder;
import com.garganttua.core.injection.context.dsl.BeanSupplierBuilder;
import com.garganttua.core.injection.context.dsl.IBeanSupplierBuilder;
import com.garganttua.core.reflection.IClass;

import com.garganttua.core.reflection.annotations.Reflected;
@Reflected(queryAllDeclaredMethods = true)
public class Beans {
    private static final Logger log = Logger.getLogger(Beans.class);

    @Expression(name = "beanReference", description = "Creates a BeanReference with the specified parameters")
    public static BeanReference<?> beanReference(@Nullable IClass<?> type, Optional<BeanStrategy> strategy, Optional<String> name,
            Set<IClass<? extends Annotation>> qualifiers) {
        log.trace("Creating BeanReference with type: {}, strategy: {}, name: {}, qualifiers: {}", type,
                strategy, name, qualifiers);
        return new BeanReference<>(type, strategy, name, qualifiers);
    }

    @Expression(name = "bean", description = "Creates a BeanSupplierBuilder with the specified parameters")
    public static <Bean> IBeanSupplierBuilder<Bean> bean(Optional<String> provider, BeanReference<Bean> query) {
        log.trace("Creating BeanSupplierBuilder with provider: {} and query: {}", provider, query);
        IBeanSupplierBuilder<Bean> builder = new BeanSupplierBuilder<>(provider, query);
        log.debug("BeanSupplierBuilder created: {}", builder);
        return builder;
    }

    @Expression(name = "bean", description = "Creates a BeanSupplierBuilder with the specified parameters")
    public static <Bean> IBeanSupplierBuilder<Bean> bean(BeanReference<Bean> query) {
        log.trace("Creating BeanSupplierBuilder with query: {}", query);
        IBeanSupplierBuilder<Bean> builder = new BeanSupplierBuilder<>(query);
        log.debug("BeanSupplierBuilder created: {}", builder);
        return builder;
    }

    @Expression(name = "bean", description = "Creates a BeanSupplierBuilder with the specified parameters")
    public static <Bean> IBeanSupplierBuilder<Bean> bean(IClass<Bean> type) {
        log.trace("Creating BeanSupplierBuilder with type: {}", type);
        IBeanSupplierBuilder<Bean> builder = new BeanSupplierBuilder<>(type);
        log.debug("BeanSupplierBuilder created: {}", builder);
        return builder;
    }

    @Expression(name = "bean", description = "Creates a BeanSupplierBuilder with the specified parameters")
    public static <Bean> IBeanSupplierBuilder<Bean> bean(String provider, IClass<Bean> type, String name) {
        log.trace("Creating BeanSupplierBuilder with provider: {}, type: {}, name: {}", provider, type, name);
        IBeanSupplierBuilder<Bean> builder = new BeanSupplierBuilder<>(type).name(name).provider(provider);
        log.debug("BeanSupplierBuilder created: {}", builder);
        return builder;
    }

    @Expression(name = "bean", description = "Creates a BeanSupplierBuilder with the specified parameters")
    public static <Bean> IBeanSupplierBuilder<Bean> bean(IClass<Bean> type, String name) {
        log.trace("Creating BeanSupplierBuilder with type: {} and name: {}", type, name);
        IBeanSupplierBuilder<Bean> builder = new BeanSupplierBuilder<>(type).name(name);
        log.debug("BeanSupplierBuilder created: {}", builder);
        return builder;
    }

    @Expression(name = "bean", description = "Creates a BeanSupplierBuilder with the specified parameters")
    public static <Bean> IBeanSupplierBuilder<Bean> bean(String provider, IClass<Bean> type) {
        log.trace("Creating BeanSupplierBuilder with provider: {} and type: {}", provider, type);
        IBeanSupplierBuilder<Bean> builder = new BeanSupplierBuilder<>(type).provider(provider);
        log.debug("BeanSupplierBuilder created: {}", builder);
        return builder;
    }

    @Expression(name = "query", description = "Creates a BeanQueryBuilder with the specified parameters")
    public static <Bean> IBeanQueryBuilder<Bean> query() {
        log.trace("Creating BeanQueryBuilder");
        IBeanQueryBuilder<Bean> queryBuilder = new BeanQueryBuilder<>();
        log.debug("BeanQueryBuilder created: {}", queryBuilder);
        return queryBuilder;
    }
}

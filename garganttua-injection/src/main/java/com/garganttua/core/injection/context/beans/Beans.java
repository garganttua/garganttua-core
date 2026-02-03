package com.garganttua.core.injection.context.beans;

import java.lang.annotation.Annotation;
import java.util.Optional;
import java.util.Set;

import jakarta.annotation.Nullable;

import com.garganttua.core.expression.annotations.Expression;
import com.garganttua.core.injection.BeanReference;
import com.garganttua.core.injection.BeanStrategy;
import com.garganttua.core.injection.IBeanQueryBuilder;
import com.garganttua.core.injection.context.dsl.BeanQueryBuilder;
import com.garganttua.core.injection.context.dsl.BeanSupplierBuilder;
import com.garganttua.core.injection.context.dsl.IBeanSupplierBuilder;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Beans {

    @Expression(name = "beanReference", description = "Creates a BeanReference with the specified parameters")
    public static BeanReference<?> beanReference(@Nullable Class<?> type, Optional<BeanStrategy> strategy, Optional<String> name,
            Set<Class<? extends Annotation>> qualifiers) {
        log.atTrace().log("Creating BeanReference with type: {}, strategy: {}, name: {}, qualifiers: {}", type,
                strategy, name, qualifiers);
        return new BeanReference<>(type, strategy, name, qualifiers);
    }

    @Expression(name = "bean", description = "Creates a BeanSupplierBuilder with the specified parameters")
    public static <Bean> IBeanSupplierBuilder<Bean> bean(Optional<String> provider, BeanReference<Bean> query) {
        log.atTrace().log("Creating BeanSupplierBuilder with provider: {} and query: {}", provider, query);
        IBeanSupplierBuilder<Bean> builder = new BeanSupplierBuilder<>(provider, query);
        log.atDebug().log("BeanSupplierBuilder created: {}", builder);
        return builder;
    }

    @Expression(name = "bean", description = "Creates a BeanSupplierBuilder with the specified parameters")
    public static <Bean> IBeanSupplierBuilder<Bean> bean(BeanReference<Bean> query) {
        log.atTrace().log("Creating BeanSupplierBuilder with query: {}", query);
        IBeanSupplierBuilder<Bean> builder = new BeanSupplierBuilder<>(query);
        log.atDebug().log("BeanSupplierBuilder created: {}", builder);
        return builder;
    }

    @Expression(name = "bean", description = "Creates a BeanSupplierBuilder with the specified parameters")
    public static <Bean> IBeanSupplierBuilder<Bean> bean(Class<Bean> type) {
        log.atTrace().log("Creating BeanSupplierBuilder with type: {}", type);
        IBeanSupplierBuilder<Bean> builder = new BeanSupplierBuilder<>(type);
        log.atDebug().log("BeanSupplierBuilder created: {}", builder);
        return builder;
    }

    @Expression(name = "bean", description = "Creates a BeanSupplierBuilder with the specified parameters")
    public static <Bean> IBeanSupplierBuilder<Bean> bean(String provider, Class<Bean> type, String name) {
        log.atTrace().log("Creating BeanSupplierBuilder with provider: {}, type: {}, name: {}", provider, type, name);
        IBeanSupplierBuilder<Bean> builder = new BeanSupplierBuilder<>(type).name(name).provider(provider);
        log.atDebug().log("BeanSupplierBuilder created: {}", builder);
        return builder;
    }

    @Expression(name = "bean", description = "Creates a BeanSupplierBuilder with the specified parameters")
    public static <Bean> IBeanSupplierBuilder<Bean> bean(Class<Bean> type, String name) {
        log.atTrace().log("Creating BeanSupplierBuilder with type: {} and name: {}", type, name);
        IBeanSupplierBuilder<Bean> builder = new BeanSupplierBuilder<>(type).name(name);
        log.atDebug().log("BeanSupplierBuilder created: {}", builder);
        return builder;
    }

    @Expression(name = "bean", description = "Creates a BeanSupplierBuilder with the specified parameters")
    public static <Bean> IBeanSupplierBuilder<Bean> bean(String provider, Class<Bean> type) {
        log.atTrace().log("Creating BeanSupplierBuilder with provider: {} and type: {}", provider, type);
        IBeanSupplierBuilder<Bean> builder = new BeanSupplierBuilder<>(type).provider(provider);
        log.atDebug().log("BeanSupplierBuilder created: {}", builder);
        return builder;
    }

    @Expression(name = "query", description = "Creates a BeanQueryBuilder with the specified parameters")
    public static <Bean> IBeanQueryBuilder<Bean> query() {
        log.atTrace().log("Creating BeanQueryBuilder");
        IBeanQueryBuilder<Bean> queryBuilder = new BeanQueryBuilder<>();
        log.atDebug().log("BeanQueryBuilder created: {}", queryBuilder);
        return queryBuilder;
    }
}
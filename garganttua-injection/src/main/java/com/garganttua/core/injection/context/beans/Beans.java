package com.garganttua.core.injection.context.beans;

import java.lang.annotation.Annotation;
import java.util.Optional;
import java.util.Set;

import com.garganttua.core.expression.annotations.ExpressionNode;
import com.garganttua.core.injection.BeanReference;
import com.garganttua.core.injection.BeanStrategy;
import com.garganttua.core.injection.IBeanQueryBuilder;
import com.garganttua.core.injection.context.dsl.BeanQueryBuilder;
import com.garganttua.core.injection.context.dsl.BeanSupplierBuilder;
import com.garganttua.core.injection.context.dsl.IBeanSupplierBuilder;

import jakarta.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Beans {

    @ExpressionNode(name = "beanReference", description = "Creates a BeanReference with the specified parameters")
    public static BeanReference<?> beanReference(@Nullable Class<?> type, @Nullable Optional<BeanStrategy> strategy, @Nullable Optional<String> name,
            Set<Class<? extends Annotation>> qualifiers) {
        log.atTrace().log("Creating BeanReference with type: {}, strategy: {}, name: {}, qualifiers: {}", type,
                strategy, name, qualifiers);
        return new BeanReference<>(type, strategy, name, qualifiers);
    }

    public static <Bean> IBeanSupplierBuilder<Bean> bean(Optional<String> provider, BeanReference<Bean> query) {
        log.atTrace().log("Creating BeanSupplierBuilder with provider: {} and query: {}", provider, query);
        IBeanSupplierBuilder<Bean> builder = new BeanSupplierBuilder<>(provider, query);
        log.atInfo().log("BeanSupplierBuilder created: {}", builder);
        return builder;
    }

    public static <Bean> IBeanSupplierBuilder<Bean> bean(BeanReference<Bean> query) {
        log.atTrace().log("Creating BeanSupplierBuilder with query: {}", query);
        IBeanSupplierBuilder<Bean> builder = new BeanSupplierBuilder<>(query);
        log.atInfo().log("BeanSupplierBuilder created: {}", builder);
        return builder;
    }

    public static <Bean> IBeanSupplierBuilder<Bean> bean(Class<Bean> type) {
        log.atTrace().log("Creating BeanSupplierBuilder with type: {}", type);
        IBeanSupplierBuilder<Bean> builder = new BeanSupplierBuilder<>(type);
        log.atInfo().log("BeanSupplierBuilder created: {}", builder);
        return builder;
    }

    public static <Bean> IBeanSupplierBuilder<Bean> bean(String provider, Class<Bean> type, String name) {
        log.atTrace().log("Creating BeanSupplierBuilder with provider: {}, type: {}, name: {}", provider, type, name);
        IBeanSupplierBuilder<Bean> builder = new BeanSupplierBuilder<>(type).name(name).provider(provider);
        log.atInfo().log("BeanSupplierBuilder created: {}", builder);
        return builder;
    }

    public static <Bean> IBeanSupplierBuilder<Bean> bean(Class<Bean> type, String name) {
        log.atTrace().log("Creating BeanSupplierBuilder with type: {} and name: {}", type, name);
        IBeanSupplierBuilder<Bean> builder = new BeanSupplierBuilder<>(type).name(name);
        log.atInfo().log("BeanSupplierBuilder created: {}", builder);
        return builder;
    }

    public static <Bean> IBeanSupplierBuilder<Bean> bean(String provider, Class<Bean> type) {
        log.atTrace().log("Creating BeanSupplierBuilder with provider: {} and type: {}", provider, type);
        IBeanSupplierBuilder<Bean> builder = new BeanSupplierBuilder<>(type).provider(provider);
        log.atInfo().log("BeanSupplierBuilder created: {}", builder);
        return builder;
    }

    public static <Bean> IBeanQueryBuilder<Bean> query() {
        log.atTrace().log("Creating BeanQueryBuilder");
        IBeanQueryBuilder<Bean> queryBuilder = new BeanQueryBuilder<>();
        log.atInfo().log("BeanQueryBuilder created: {}", queryBuilder);
        return queryBuilder;
    }
}
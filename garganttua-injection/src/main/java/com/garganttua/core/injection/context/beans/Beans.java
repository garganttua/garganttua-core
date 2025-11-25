package com.garganttua.core.injection.context.beans;

import java.util.Optional;

import com.garganttua.core.injection.BeanDefinition;
import com.garganttua.core.injection.IBeanQueryBuilder;
import com.garganttua.core.injection.context.dsl.BeanQueryBuilder;
import com.garganttua.core.injection.context.dsl.BeanSupplierBuilder;
import com.garganttua.core.injection.context.dsl.IBeanSupplierBuilder;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Beans {

    public static <Bean> IBeanSupplierBuilder<Bean> bean(Optional<String> provider, BeanDefinition<Bean> example) {
        log.atTrace().log("Creating BeanSupplierBuilder with provider: {} and example: {}", provider, example);
        IBeanSupplierBuilder<Bean> builder = new BeanSupplierBuilder<>(provider, example);
        log.atInfo().log("BeanSupplierBuilder created: {}", builder);
        return builder;
    }

    public static <Bean> IBeanSupplierBuilder<Bean> bean(BeanDefinition<Bean> example) {
        log.atTrace().log("Creating BeanSupplierBuilder with example: {}", example);
        IBeanSupplierBuilder<Bean> builder = new BeanSupplierBuilder<>(example);
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
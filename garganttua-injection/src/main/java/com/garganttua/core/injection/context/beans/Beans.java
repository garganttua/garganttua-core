package com.garganttua.core.injection.context.beans;

import java.util.Optional;

import com.garganttua.core.injection.BeanDefinition;
import com.garganttua.core.injection.IBeanQueryBuilder;
import com.garganttua.core.injection.context.dsl.BeanQueryBuilder;
import com.garganttua.core.injection.context.dsl.BeanSupplierBuilder;
import com.garganttua.core.injection.context.dsl.IBeanSupplierBuilder;

public class Beans {

    public static <Bean> IBeanSupplierBuilder<Bean> bean(Optional<String> provider, BeanDefinition<Bean> example) {
        return new BeanSupplierBuilder<Bean>(provider, example);
    }

    public static <Bean> IBeanSupplierBuilder<Bean> bean(BeanDefinition<Bean> example) {
        return new BeanSupplierBuilder<Bean>(example);
    }

    public static <Bean> IBeanSupplierBuilder<Bean> bean(Class<Bean> type) {
        return new BeanSupplierBuilder<Bean>(type);
    }

    public static <Bean> IBeanSupplierBuilder<Bean> bean(String provider, Class<Bean> type, String name) {
        return new BeanSupplierBuilder<Bean>(type).name(name).provider(provider);
    }

    public static <Bean> IBeanSupplierBuilder<Bean> bean(Class<Bean> type, String name) {
        return new BeanSupplierBuilder<Bean>(type).name(name);
    }

    public static <Bean> IBeanSupplierBuilder<Bean> bean(String provider, Class<Bean> type) {
        return new BeanSupplierBuilder<Bean>(type).provider(provider);
    }

    public static <Bean> IBeanQueryBuilder<Bean> query(){
        return new BeanQueryBuilder<Bean>();
    }

}

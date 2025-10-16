package com.garganttua.injection;

public class Beans {

    public static <Bean> IBeanSupplierBuilder<Bean> bean(Class<Bean> type) {
        return new BeanSupplierBuilder<Bean>(type);
    }

    public static <Bean> IBeanSupplierBuilder<Bean> bean(String scope, Class<Bean> type, String name) {
        return new BeanSupplierBuilder<Bean>(type).name(name).scope(scope);
    }

    public static <Bean> IBeanSupplierBuilder<Bean> bean(Class<Bean> type, String name) {
        return new BeanSupplierBuilder<Bean>(type).name(name);
    }

    public static <Bean> IBeanSupplierBuilder<Bean> bean(String scope, Class<Bean> type) {
        return new BeanSupplierBuilder<Bean>(type).scope(scope);
    }

}

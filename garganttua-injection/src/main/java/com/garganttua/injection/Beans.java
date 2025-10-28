package com.garganttua.injection;

import com.garganttua.injection.beans.BeanQueryBuilder;
import com.garganttua.injection.beans.IBeanQueryBuilder;

public class Beans {

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

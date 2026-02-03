package com.garganttua.core.injection.context.properties;

import com.garganttua.core.injection.context.dsl.IPropertySupplierBuilder;
import com.garganttua.core.injection.context.dsl.PropertySupplierBuilder;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Properties {

    public static <Property> IPropertySupplierBuilder<Property> property(Class<Property> type) {
        log.atTrace().log("Entering property() with type={}", type);
        IPropertySupplierBuilder<Property> builder = new PropertySupplierBuilder<Property>(type);
        log.atDebug().log("Created PropertySupplierBuilder for type={}", type.getSimpleName());
        log.atTrace().log("Exiting property()");
        return builder;
    }

}

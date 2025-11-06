package com.garganttua.core.injection.context.properties;

import com.garganttua.core.injection.context.dsl.IPropertySupplierBuilder;
import com.garganttua.core.injection.context.dsl.PropertySupplierBuilder;

public class Properties {

    public static <Property> IPropertySupplierBuilder<Property> property(Class<Property> type) {
        return new PropertySupplierBuilder<Property>(type);
    }

}

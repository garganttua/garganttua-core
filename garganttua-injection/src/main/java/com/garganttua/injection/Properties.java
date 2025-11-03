package com.garganttua.injection;

import com.garganttua.core.injection.context.dsl.IPropertySupplierBuilder;

public class Properties {

    public static <Property> IPropertySupplierBuilder<Property> property(Class<Property> type) {
        return new PropertySupplierBuilder<Property>(type);
    }

}

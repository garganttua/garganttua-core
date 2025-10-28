package com.garganttua.injection;

public class Properties {

    public static <Property> IPropertySupplierBuilder<Property> property(Class<Property> type) {
        return new PropertySupplierBuilder<Property>(type);
    }

}

package com.garganttua.core.injection.context.properties;

import com.garganttua.core.observability.Logger;
import com.garganttua.core.injection.context.dsl.IPropertySupplierBuilder;
import com.garganttua.core.injection.context.dsl.PropertySupplierBuilder;
import com.garganttua.core.reflection.IClass;

/**
 * Static entry point for building property suppliers from the active injection context.
 */
public class Properties {
    private static final Logger log = Logger.getLogger(Properties.class);

    /**
     * Starts a fluent {@link IPropertySupplierBuilder} for a property of the given value type.
     *
     * @param type           the property value type
     * @param <Property>     the property value type parameter
     * @return a new property supplier builder
     */
    public static <Property> IPropertySupplierBuilder<Property> property(IClass<Property> type) {
        log.trace("Entering property() with type={}", type);
        IPropertySupplierBuilder<Property> builder = new PropertySupplierBuilder<Property>(type);
        log.debug("Created PropertySupplierBuilder for type={}", type.getSimpleName());
        log.trace("Exiting property()");
        return builder;
    }

}

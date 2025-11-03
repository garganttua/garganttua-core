package com.garganttua.core.injection.context.dsl;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.dsl.IAutomaticLinkedBuilder;
import com.garganttua.core.injection.IPropertyProvider;

public interface IPropertyProviderBuilder extends IAutomaticLinkedBuilder<IPropertyProviderBuilder, IDiContextBuilder, IPropertyProvider>{

    <PropertyType> IPropertyProviderBuilder withProperty(Class<PropertyType> propertyType, String key, PropertyType property) throws DslException;

}

package com.garganttua.injection.spec;

import com.garganttua.dsl.DslException;
import com.garganttua.dsl.IAutomaticLinkedBuilder;

public interface IPropertyProviderBuilder extends IAutomaticLinkedBuilder<IPropertyProviderBuilder, IDiContextBuilder, IPropertyProvider>{

    <PropertyType> IPropertyProviderBuilder withProperty(Class<PropertyType> propertyType, String key, PropertyType property) throws DslException;

}

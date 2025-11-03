package com.garganttua.injection;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.injection.IPropertyProvider;
import com.garganttua.core.injection.context.dsl.IDiContextBuilder;
import com.garganttua.core.injection.context.dsl.IPropertyBuilder;
import com.garganttua.core.injection.context.dsl.IPropertyProviderBuilder;
import com.garganttua.dsl.AbstractAutomaticLinkedBuilder;
import com.garganttua.injection.properties.PropertyProvider;

public class PropertyProviderBuilder extends AbstractAutomaticLinkedBuilder<IPropertyProviderBuilder, IDiContextBuilder, IPropertyProvider> implements IPropertyProviderBuilder {

    private List<IPropertyBuilder<?>> propertyBuilders = new ArrayList<>();

    protected PropertyProviderBuilder(IDiContextBuilder link) {
        super(link);
    }

    @Override
    public <PropertyType> IPropertyProviderBuilder withProperty(Class<PropertyType> propertyType, String key,
            PropertyType property) throws DslException {
        this.propertyBuilders.add(new PropertyBuilder<PropertyType>(key, property));
        return this;
    }

    @Override
    protected IPropertyProvider doBuild() throws DslException {
        return new PropertyProvider(this.propertyBuilders.stream()
        .map(p -> {
            try {
                return p.build();
            } catch (DslException e) {
                throw new RuntimeException("Error building property: " + p, e);
            }
        })
        .filter(Objects::nonNull)
        .collect(Collectors.toMap(
            Map.Entry::getKey,
            Map.Entry::getValue
        )));
    }

    @Override
    protected void doAutoDetection() throws DslException {
        //no auto detection for this simple property provider
    }

}

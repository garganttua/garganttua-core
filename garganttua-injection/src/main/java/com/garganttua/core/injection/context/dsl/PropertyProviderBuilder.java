package com.garganttua.core.injection.context.dsl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import com.garganttua.core.dsl.AbstractAutomaticLinkedBuilder;
import com.garganttua.core.dsl.DslException;
import com.garganttua.core.injection.IPropertyProvider;
import com.garganttua.core.injection.context.properties.PropertyProvider;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PropertyProviderBuilder extends AbstractAutomaticLinkedBuilder<IPropertyProviderBuilder, IInjectionContextBuilder, IPropertyProvider>
        implements IPropertyProviderBuilder {

    private List<IPropertyBuilder<?>> propertyBuilders = new ArrayList<>();

    public PropertyProviderBuilder(IInjectionContextBuilder link) {
        super(link);
        log.atTrace().log("Entering PropertyProviderBuilder constructor with link={}", link);
        log.atTrace().log("Exiting PropertyProviderBuilder constructor");
    }

    @Override
    public <PropertyType> IPropertyProviderBuilder withProperty(Class<PropertyType> propertyType, String key,
                                                                PropertyType property) throws DslException {
        log.atTrace().log("Entering withProperty(propertyType={}, key={}, property={})", propertyType, key, property);
        this.propertyBuilders.add(new PropertyBuilder<>(key, property));
        log.atDebug().log("Added property with key={} and type={}", key, propertyType.getSimpleName());
        log.atTrace().log("Exiting withProperty");
        return this;
    }

    @Override
    protected IPropertyProvider doBuild() throws DslException {
        log.atTrace().log("Entering doBuild()");
        Map<String, Object> properties = this.propertyBuilders.stream()
                .map(p -> {
                    try {
                        log.atDebug().log("Building property: {}", p);
                        return p.build();
                    } catch (DslException e) {
                        log.atError().log("Error building property: {}", p);
                        throw new RuntimeException("Error building property: " + p, e);
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue
                ));
        log.atDebug().log("Built PropertyProvider with {} properties", properties.size());
        IPropertyProvider provider = new PropertyProvider(properties);
        log.atTrace().log("Exiting doBuild()");
        return provider;
    }

    @Override
    protected void doAutoDetection() throws DslException {
        log.atTrace().log("doAutoDetection() called, no auto detection implemented for PropertyProviderBuilder");
    }
}
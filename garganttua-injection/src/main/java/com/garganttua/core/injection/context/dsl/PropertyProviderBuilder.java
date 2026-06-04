package com.garganttua.core.injection.context.dsl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import com.garganttua.core.observability.Logger;
import com.garganttua.core.dsl.AbstractAutomaticLinkedBuilder;
import com.garganttua.core.dsl.DslException;
import com.garganttua.core.injection.IPropertyProvider;
import com.garganttua.core.injection.context.properties.PropertyProvider;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.annotations.Reflected;

/**
 * Builder that collects individual properties and assembles them into an {@link IPropertyProvider}.
 */
@Reflected
public class PropertyProviderBuilder extends AbstractAutomaticLinkedBuilder<IPropertyProviderBuilder, IInjectionContextBuilder, IPropertyProvider>
        implements IPropertyProviderBuilder {
    private static final Logger log = Logger.getLogger(PropertyProviderBuilder.class);

    private List<IPropertyBuilder<?>> propertyBuilders = new ArrayList<>();

    /**
     * Creates a property provider builder linked to its parent injection context builder.
     *
     * @param link the parent injection context builder
     */
    public PropertyProviderBuilder(IInjectionContextBuilder link) {
        super(link);
        log.trace("Entering PropertyProviderBuilder constructor with link={}", link);
        log.trace("Exiting PropertyProviderBuilder constructor");
    }

    /**
     * Registers a property with the provider.
     *
     * @param propertyType   the declared type of the property value
     * @param key            the property key
     * @param property       the property value
     * @param <PropertyType> the property value type parameter
     * @return this builder for chaining
     * @throws DslException if the property cannot be registered
     */
    @Override
    public <PropertyType> IPropertyProviderBuilder withProperty(IClass<PropertyType> propertyType, String key,
                                                                PropertyType property) throws DslException {
        log.trace("Entering withProperty(propertyType={}, key={}, property={})", propertyType, key, property);
        this.propertyBuilders.add(new PropertyBuilder<>(key, property));
        log.debug("Added property with key={} and type={}", key, propertyType.getSimpleName());
        log.trace("Exiting withProperty");
        return this;
    }

    @Override
    public IPropertyProviderBuilder withProperty(String key, String value) throws DslException {
        log.trace("Entering withProperty(key={}, value={})", key, value);
        this.propertyBuilders.add(new PropertyBuilder<>(key, value));
        log.debug("Added string property with key={}", key);
        log.trace("Exiting withProperty");
        return this;
    }

    @Override
    protected IPropertyProvider doBuild() throws DslException {
        log.trace("Entering doBuild()");
        Map<String, Object> properties = this.propertyBuilders.stream()
                .map(p -> {
                    try {
                        log.debug("Building property: {}", p);
                        return p.build();
                    } catch (DslException e) {
                        log.error("Error building property: {}", p);
                        throw new RuntimeException("Error building property: " + p, e);
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue
                ));
        log.debug("Built PropertyProvider with {} properties", properties.size());
        IPropertyProvider provider = new PropertyProvider(properties);
        log.trace("Exiting doBuild()");
        return provider;
    }

    @Override
    protected void doAutoDetection() throws DslException {
        log.trace("doAutoDetection() called, no auto detection implemented for PropertyProviderBuilder");
    }
}

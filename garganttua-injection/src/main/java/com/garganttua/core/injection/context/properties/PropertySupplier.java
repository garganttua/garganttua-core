package com.garganttua.core.injection.context.properties;

import java.lang.reflect.Type;
import java.util.Objects;
import java.util.Optional;

import com.garganttua.core.diagnostic.Diagnostics;
import com.garganttua.core.diagnostic.IDiagnostic;
import com.garganttua.core.injection.DiException;
import com.garganttua.core.injection.IPropertySupplier;
import com.garganttua.core.injection.context.InjectionContext;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.supply.SupplyException;

public class PropertySupplier<Property> implements IPropertySupplier<Property> {
    private static final IDiagnostic log = Diagnostics.of(PropertySupplier.class);

    private String key;
    private Optional<String> provider;
    private IClass<Property> type;

    public PropertySupplier(Optional<String> provider, String key, IClass<Property> type) {
        log.trace("Entering PropertySupplier constructor with provider: {}, key: {}, type: {}", provider, key, type);
        this.key = Objects.requireNonNull(key, "Key cannot be null");
        this.provider = Objects.requireNonNull(provider, "Provider cannot be null");
        this.type = Objects.requireNonNull(type, "Type cannot be null");
        log.debug("PropertySupplier initialized with key: {}, provider: {}, type: {}", this.key, this.provider, this.type);
        log.trace("Exiting PropertySupplier constructor");
    }

    @Override
    public Optional<Property> supply() throws SupplyException {
        log.trace("Entering supply for key: '{}' with provider: {}", key, provider);

        try {
            Optional<Property> result;
            if (this.provider.isPresent()) {
                log.debug("Fetching property using provider: {}", provider.get());
                result = InjectionContext.context.getProperty(this.provider.get(), this.key, this.type);
            } else {
                log.debug("Fetching property without provider");
                result = InjectionContext.context.getProperty(this.key, this.type);
            }

            log.debug("Property supplied for key '{}': {}", key, result.orElse(null));
            log.trace("Exiting supply with result: {}", result);
            return result;

        } catch (DiException e) {
            log.error("Failed to supply property '{}' with provider '{}': {}", key, provider.orElse("N/A"), e.getMessage());
            throw new SupplyException(e);
        }
    }

    @Override
    public Type getSuppliedType() {
        log.trace("Returning supplied type: {}", type);
        return this.type.getType();
    }

    @Override
    public IClass<Property> getSuppliedClass() {
        return this.type;
    }
}

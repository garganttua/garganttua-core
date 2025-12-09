package com.garganttua.core.injection.context.properties;

import java.lang.reflect.Type;
import java.util.Objects;
import java.util.Optional;

import com.garganttua.core.injection.DiException;
import com.garganttua.core.injection.IPropertySupplier;
import com.garganttua.core.injection.context.DiContext;
import com.garganttua.core.supply.SupplyException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PropertySupplier<Property> implements IPropertySupplier<Property> {

    private String key;
    private Optional<String> provider;
    private Class<Property> type;

    public PropertySupplier(Optional<String> provider, String key, Class<Property> type) {
        log.atTrace().log("Entering PropertySupplier constructor with provider: {}, key: {}, type: {}", provider, key, type);
        this.key = Objects.requireNonNull(key, "Key cannot be null");
        this.provider = Objects.requireNonNull(provider, "Provider cannot be null");
        this.type = Objects.requireNonNull(type, "Type cannot be null");
        log.atDebug().log("PropertySupplier initialized with key: {}, provider: {}, type: {}", this.key, this.provider, this.type);
        log.atTrace().log("Exiting PropertySupplier constructor");
    }

    @Override
    public Optional<Property> supply() throws SupplyException {
        log.atTrace().log("Entering supply for key: '{}' with provider: {}", key, provider);

        try {
            Optional<Property> result;
            if (this.provider.isPresent()) {
                log.atDebug().log("Fetching property using provider: {}", provider.get());
                result = DiContext.context.getProperty(this.provider.get(), this.key, this.type);
            } else {
                log.atDebug().log("Fetching property without provider");
                result = DiContext.context.getProperty(this.key, this.type);
            }

            log.atInfo().log("Property supplied for key '{}': {}", key, result.orElse(null));
            log.atTrace().log("Exiting supply with result: {}", result);
            return result;

        } catch (DiException e) {
            log.atError().log("Failed to supply property '{}' with provider '{}': {}", key, provider.orElse("N/A"), e.getMessage());
            throw new SupplyException(e);
        }
    }

    @Override
    public Type getSuppliedType() {
        log.atTrace().log("Returning supplied type: {}", type);
        return this.type;
    }
}
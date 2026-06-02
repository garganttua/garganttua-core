package com.garganttua.core.injection.context.properties;

import java.lang.reflect.Type;
import java.util.Objects;
import java.util.Optional;

import com.garganttua.core.observability.Logger;
import com.garganttua.core.injection.DiException;
import com.garganttua.core.injection.IPropertySupplier;
import com.garganttua.core.injection.context.InjectionContext;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.supply.SupplyException;

/**
 * Lazy {@link IPropertySupplier} that resolves a property from the active
 * {@link InjectionContext} on each {@link #supply()} call, optionally scoped to a named provider.
 *
 * @param <Property> the type of the property value supplied
 */
public class PropertySupplier<Property> implements IPropertySupplier<Property> {
    private static final Logger log = Logger.getLogger(PropertySupplier.class);

    private String key;
    private Optional<String> provider;
    private IClass<Property> type;

    /**
     * Creates a property supplier.
     *
     * @param provider the optional provider scope; must not be {@code null} (may be empty)
     * @param key      the property key; must not be {@code null}
     * @param type     the property value type; must not be {@code null}
     */
    public PropertySupplier(Optional<String> provider, String key, IClass<Property> type) {
        log.trace("Entering PropertySupplier constructor with provider: {}, key: {}, type: {}", provider, key, type);
        this.key = Objects.requireNonNull(key, "Key cannot be null");
        this.provider = Objects.requireNonNull(provider, "Provider cannot be null");
        this.type = Objects.requireNonNull(type, "Type cannot be null");
        log.debug("PropertySupplier initialized with key: {}, provider: {}, type: {}", this.key, this.provider, this.type);
        log.trace("Exiting PropertySupplier constructor");
    }

    /**
     * Resolves the property value from the active injection context.
     *
     * @return the property value, or {@link Optional#empty()} if not found
     * @throws SupplyException if resolution fails
     */
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

    /** {@return the reflective {@link Type} of the property value} */
    @Override
    public Type getSuppliedType() {
        log.trace("Returning supplied type: {}", type);
        return this.type.getType();
    }

    /** {@return the {@link IClass} of the property value} */
    @Override
    public IClass<Property> getSuppliedClass() {
        return this.type;
    }
}

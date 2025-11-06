package com.garganttua.injection;

import java.util.Objects;
import java.util.Optional;

import com.garganttua.core.injection.DiException;
import com.garganttua.core.injection.IPropertySupplier;
import com.garganttua.core.supplying.SupplyException;

public class PropertySupplier<Property> implements IPropertySupplier<Property> {

    private String key;
    private Optional<String> provider;
    private Class<Property> type;

    public PropertySupplier(Optional<String> provider, String key, Class<Property> type) {
        this.key = Objects.requireNonNull(key, "Key cannot be null");
        this.provider = Objects.requireNonNull(provider, "Provider cannot be null");
        this.type = Objects.requireNonNull(type, "Type cannot be null");
    }

    @Override
    public Optional<Property> supply() throws SupplyException {
        try {
            if (this.provider.isPresent())
                return DiContext.context.getProperty(this.provider.get(), this.key, this.type);
            return DiContext.context.getProperty(this.key, this.type);
        } catch (DiException e) {
            throw new SupplyException(e);
        }
    }

    @Override
    public Class<Property> getSuppliedType() {
        return this.type;
    }

}

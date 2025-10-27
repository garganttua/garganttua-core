package com.garganttua.injection.supplier;

import java.util.Objects;
import java.util.Optional;

import com.garganttua.injection.DiException;
import com.garganttua.injection.spec.supplier.IContextualObjectSupplier;
import com.garganttua.injection.spec.supplier.ICustomContextualObjectSupply;

public class CustomContextualObjectSupplier<Supplied, Context> implements IContextualObjectSupplier<Supplied, Context> {

    private ICustomContextualObjectSupply<Supplied, Context> supply;
    private Class<Supplied> suppliedType;
    private Class<Context> contextType;

    public CustomContextualObjectSupplier(ICustomContextualObjectSupply<Supplied, Context> supply,
            Class<Supplied> suppliedType, Class<Context> contextType) {
        this.supply = Objects.requireNonNull(supply, "Contextual supply cannot be null");
        this.suppliedType = Objects.requireNonNull(suppliedType, "Supplied type cannot be null");
        this.contextType = Objects.requireNonNull(contextType, "Context type cannot be null");
    }

    @Override
    public Class<Supplied> getObjectClass() {
        return this.suppliedType;
    }

    @Override
    public Class<Context> getContextClass() {
        return this.contextType;
    }

    @Override
    public Optional<Supplied> getObject(Context context) throws DiException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getObject'");
    }
}

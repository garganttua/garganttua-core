package com.garganttua.dsl;

import java.util.Objects;

public abstract class AbstractAutomaticBuilder<Builder, Built> implements IAutomaticBuilder<Builder, Built> {

    private Boolean autoDetect;
    protected Built built;

    protected AbstractAutomaticBuilder() {
        this.autoDetect = false;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Builder autoDetect(boolean b) throws DslException {
        this.autoDetect = Objects.requireNonNull(b, "AutoDetect cannot be null");
        return (Builder) this;
    }

    @Override
    public Built build() throws DslException {
        if (this.built != null)
            return this.built;
        if (this.autoDetect) {
            this.doAutoDetection();
        }

        return this.built = this.doBuild();
    }

    protected abstract Built doBuild() throws DslException;

    protected abstract void doAutoDetection() throws DslException;
}

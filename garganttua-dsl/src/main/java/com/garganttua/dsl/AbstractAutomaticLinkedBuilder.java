package com.garganttua.dsl;

import java.util.Objects;

public abstract class AbstractAutomaticLinkedBuilder<Builder, Link, Built>
        implements IAutomaticLinkedBuilder<Builder, Link, Built> {

    private Boolean autoDetect;
    private Link link;
    private Built built;

    protected AbstractAutomaticLinkedBuilder(Link link){
        this.link = Objects.requireNonNull(link, "Up cannot be null");
        this.autoDetect = false;
    }

    @Override
    public Link up() {
        return this.link;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Builder autoDetect(boolean b) throws DslException {
        this.autoDetect = Objects.requireNonNull(b, "AutoDetect cannot be null");
        return (Builder) this;
    }

    @Override
    public Built build() throws DslException {
        if( this.built != null )
            return this.built;
        if (this.autoDetect) {
            this.doAutoDetection();
        }

        return this.built = this.doBuild();
    }

    protected abstract Built doBuild() throws DslException;
    protected abstract void doAutoDetection() throws DslException;
}

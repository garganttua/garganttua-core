package com.garganttua.core.dsl;

import com.garganttua.core.diagnostic.Diagnostics;
import com.garganttua.core.diagnostic.IDiagnostic;
import java.util.Objects;

public abstract class AbstractAutomaticLinkedBuilder<Builder, Link, Built> extends AbstractAutomaticBuilder<Builder, Built>
        implements IAutomaticLinkedBuilder<Builder, Link, Built> {
    private static final IDiagnostic log = Diagnostics.of(AbstractAutomaticLinkedBuilder.class);

    private Link link;

    protected AbstractAutomaticLinkedBuilder(Link link){
        log.trace("Entering AbstractAutomaticLinkedBuilder constructor with link: {}", link);
        this.link = Objects.requireNonNull(link, "Up cannot be null");
        this.autoDetect = false;
        log.debug("Link set to: {}, autoDetect initialized to false", this.link);
        log.trace("Exiting constructor");
    }

    @Override
    public Link up() {
        log.trace("Entering up() method");
        log.debug("Returning link: {}", this.link);
        log.trace("Exiting up() method");
        return this.link;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Builder setUp(Link up){
        log.trace("Entering setUp() with link: {}", up);
        try {
            this.link = Objects.requireNonNull(up, "Up cannot be null");
            log.debug("Link updated to: {}", this.link);
            log.trace("Exiting setUp()");
            return (Builder) this;
        } catch (NullPointerException e) {
            log.error("setUp() parameter cannot be null", e);
            throw e;
        }
    }
}
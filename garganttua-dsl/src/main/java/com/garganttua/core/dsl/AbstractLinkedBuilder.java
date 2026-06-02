package com.garganttua.core.dsl;

import com.garganttua.core.observability.Logger;
import java.util.Objects;

public abstract class AbstractLinkedBuilder<Link, Built>
        implements ILinkedBuilder<Link, Built> {
    private static final Logger log = Logger.getLogger(AbstractLinkedBuilder.class);

    private Link link;

    protected AbstractLinkedBuilder(Link link) {
        log.trace("Entering AbstractLinkedBuilder constructor with link: {}", link);
        this.link = Objects.requireNonNull(link, "Up cannot be null");
        log.debug("Link set to: {}", this.link);
        log.trace("Exiting constructor");
    }

    @Override
    public void setUp(Link up){
        log.trace("Entering setUp() with link: {}", up);
        try {
            this.link = Objects.requireNonNull(up, "Up cannot be null");
            log.debug("Link updated to: {}", this.link);
            log.trace("Exiting setUp()");
        } catch (NullPointerException e) {
            log.error("setUp() parameter cannot be null", e);
            throw e;
        }
    }

    @Override
    public Link up() {
        log.trace("Entering up()");
        log.debug("Returning link: {}", this.link);
        log.trace("Exiting up()");
        return this.link;
    }
}

package com.garganttua.core.dsl;

import java.util.Objects;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractAutomaticLinkedBuilder<Builder, Link, Built> extends AbstractAutomaticBuilder<Builder, Built>
        implements IAutomaticLinkedBuilder<Builder, Link, Built> {

    private Link link;

    protected AbstractAutomaticLinkedBuilder(Link link){
        log.atTrace().log("Entering AbstractAutomaticLinkedBuilder constructor with link: {}", link);
        this.link = Objects.requireNonNull(link, "Up cannot be null");
        this.autoDetect = false;
        log.atDebug().log("Link set to: {}, autoDetect initialized to false", this.link);
        log.atTrace().log("Exiting constructor");
    }

    @Override
    public Link up() {
        log.atTrace().log("Entering up() method");
        log.atDebug().log("Returning link: {}", this.link);
        log.atTrace().log("Exiting up() method");
        return this.link;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Builder setUp(Link up){
        log.atTrace().log("Entering setUp() with link: {}", up);
        try {
            this.link = Objects.requireNonNull(up, "Up cannot be null");
            log.atDebug().log("Link updated to: {}", this.link);
            log.atTrace().log("Exiting setUp()");
            return (Builder) this;
        } catch (NullPointerException e) {
            log.atError().log("setUp() parameter cannot be null", e);
            throw e;
        }
    }
}
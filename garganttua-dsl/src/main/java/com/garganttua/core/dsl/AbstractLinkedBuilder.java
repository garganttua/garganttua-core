package com.garganttua.dsl;

import java.util.Objects;

import com.garganttua.core.dsl.ILinkedBuilder;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractLinkedBuilder<Link, Built>
        implements ILinkedBuilder<Link, Built> {

    private Link link;

    protected AbstractLinkedBuilder(Link link) {
        log.atTrace().log("Entering AbstractLinkedBuilder constructor with link: {}", link);
        this.link = Objects.requireNonNull(link, "Up cannot be null");
        log.atDebug().log("Link set to: {}", this.link);
        log.atTrace().log("Exiting constructor");
    }

    @Override
    public void setUp(Link up){
        log.atTrace().log("Entering setUp() with link: {}", up);
        try {
            this.link = Objects.requireNonNull(up, "Up cannot be null");
            log.atDebug().log("Link updated to: {}", this.link);
            log.atTrace().log("Exiting setUp()");
        } catch (NullPointerException e) {
            log.atError().log("setUp() parameter cannot be null", e);
            throw e;
        }
    }

    @Override
    public Link up() {
        log.atTrace().log("Entering up()");
        log.atDebug().log("Returning link: {}", this.link);
        log.atTrace().log("Exiting up()");
        return this.link;
    }
}

package com.garganttua.core.dsl;

import java.util.Objects;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractAutomaticLinkedBuilder<Builder, Link, Built>
        implements IAutomaticLinkedBuilder<Builder, Link, Built> {

    private Boolean autoDetect;
    private Link link;
    private Built built;

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

    @SuppressWarnings("unchecked")
    @Override
    public Builder autoDetect(boolean b) throws DslException {
        log.atTrace().log("Entering autoDetect() with value: {}", b);
        try {
            this.autoDetect = Objects.requireNonNull(b, "AutoDetect cannot be null");
            log.atDebug().log("AutoDetect set to: {}", this.autoDetect);
            log.atTrace().log("Exiting autoDetect()");
            return (Builder) this;
        } catch (NullPointerException e) {
            log.atError().log("AutoDetect parameter cannot be null", e);
            throw new DslException("AutoDetect parameter cannot be null", e);
        }
    }

    @Override
    public Built build() throws DslException {
        log.atTrace().log("Entering build() method");

        if (this.built != null) {
            log.atDebug().log("Returning previously built instance: {}", this.built);
            log.atTrace().log("Exiting build() (cached)");
            return this.built;
        }

        if (this.autoDetect) {
            log.atInfo().log("Auto-detection is enabled, performing auto-detection");
            try {
                this.doAutoDetection();
                log.atDebug().log("Auto-detection completed successfully");
            } catch (DslException e) {
                log.atWarn().log("Non-blocking issue during auto-detection", e);
                // optionally continue to build anyway
            }
        } else {
            log.atDebug().log("Auto-detection is disabled, skipping auto-detection");
        }

        try {
            log.atInfo().log("Building the instance");
            this.built = this.doBuild();
            log.atDebug().log("Built instance: {}", this.built);
            log.atTrace().log("Exiting build() method");
            return this.built;
        } catch (DslException e) {
            log.atError().log("Critical error during build", e);
            throw e;
        }
    }

    protected abstract Built doBuild() throws DslException;

    protected abstract void doAutoDetection() throws DslException;
}
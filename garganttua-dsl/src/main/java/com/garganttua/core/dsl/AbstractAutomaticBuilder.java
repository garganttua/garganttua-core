package com.garganttua.core.dsl;

import java.util.Objects;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractAutomaticBuilder<Builder, Built> implements IAutomaticBuilder<Builder, Built> {

    private Boolean autoDetect;
    protected Built built;

    protected AbstractAutomaticBuilder() {
        log.atTrace().log("Entering AbstractAutomaticBuilder constructor");
        this.autoDetect = false;
        log.atTrace().log("Exiting AbstractAutomaticBuilder constructor, autoDetect set to false");
    }

    @SuppressWarnings("unchecked")
    @Override
    public Builder autoDetect(boolean b) throws DslException {
        log.atTrace().log("Entering autoDetect with value: {}", b);
        try {
            this.autoDetect = Objects.requireNonNull(b, "AutoDetect cannot be null");
            log.atDebug().log("AutoDetect set to: {}", this.autoDetect);
            log.atTrace().log("Exiting autoDetect");
            return (Builder) this;
        } catch (NullPointerException e) {
            log.atError().log("AutoDetect parameter cannot be null", e);
            throw new DslException("AutoDetect parameter cannot be null", e);
        }
    }

    @Override
    public Built build() throws DslException {
        log.atTrace().log("Entering build method");

        if (this.built != null) {
            log.atDebug().log("Returning previously built instance: {}", this.built);
            log.atTrace().log("Exiting build method (cached)");
            return this.built;
        }

        if (this.autoDetect) {
            log.atInfo().log("Auto-detection is enabled, performing auto-detection");
            try {
                this.doAutoDetection();
                log.atDebug().log("Auto-detection completed successfully");
            } catch (DslException e) {
                log.atWarn().log("Non-blocking issue during auto-detection", e);
            }
        } else {
            log.atDebug().log("Auto-detection is disabled, skipping auto-detection");
        }

        try {
            log.atInfo().log("Building the instance");
            this.built = this.doBuild();
            log.atDebug().log("Built instance: {}", this.built);
            log.atTrace().log("Exiting build method");
            return this.built;
        } catch (DslException e) {
            log.atError().log("Critical error during build", e);
            throw e;
        }
    }

    protected abstract Built doBuild() throws DslException;

    protected abstract void doAutoDetection() throws DslException;
}
package com.garganttua.core.bootstrap.dsl;

import java.util.Set;

import com.garganttua.core.dsl.IBuilderObserver;
import com.garganttua.core.dsl.IObservableBuilder;
import com.garganttua.core.dsl.annotations.ConfigurableBuilder;
import com.garganttua.core.dsl.dependency.AbstractAutomaticDependentBuilder;

/**
 * Public, top-level test builder used to verify config-file auto-wiring through a real
 * Bootstrap. Must be public (the configuration populator invokes its methods reflectively).
 */
@ConfigurableBuilder("e2e")
public class ConfigurableBootstrapBuilder
        extends AbstractAutomaticDependentBuilder<ConfigurableBootstrapBuilder, String>
        implements IObservableBuilder<ConfigurableBootstrapBuilder, String> {

    private String name = "default";
    private int count = -1;

    public ConfigurableBootstrapBuilder() {
        super(Set.of());
    }

    /** Config-settable scalar (key {@code name}). */
    public ConfigurableBootstrapBuilder name(String name) {
        this.name = name;
        return this;
    }

    /** Config-settable scalar (key {@code count}). */
    public ConfigurableBootstrapBuilder count(int count) {
        this.count = count;
        return this;
    }

    public String appliedName() {
        return this.name;
    }

    public int appliedCount() {
        return this.count;
    }

    @Override
    public ConfigurableBootstrapBuilder observer(IBuilderObserver<ConfigurableBootstrapBuilder, String> observer) {
        return this;
    }

    @Override
    protected String doBuild() {
        return this.name + ":" + this.count;
    }

    @Override
    protected void doAutoDetection() {
        // no-op
    }

    @Override
    protected void doAutoDetectionWithDependency(Object dependency) {
        // no-op
    }

    @Override
    protected void doPreBuildWithDependency(Object dependency) {
        // no-op
    }

    @Override
    protected void doPostBuildWithDependency(Object dependency) {
        // no-op
    }
}

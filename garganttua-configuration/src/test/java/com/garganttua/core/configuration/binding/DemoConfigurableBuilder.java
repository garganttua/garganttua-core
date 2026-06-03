package com.garganttua.core.configuration.binding;

import com.garganttua.core.dsl.IBuilder;
import com.garganttua.core.dsl.annotations.ConfigurableBuilder;

/**
 * Public top-level test builder for the configuration binding tests (a nested class
 * would not be reflectively accessible by the populator).
 */
@ConfigurableBuilder("demo")
public final class DemoConfigurableBuilder implements IBuilder<String> {

    private String name;
    private int count;
    private boolean enabled;

    public DemoConfigurableBuilder name(String name) {
        this.name = name;
        return this;
    }

    public DemoConfigurableBuilder count(int count) {
        this.count = count;
        return this;
    }

    public DemoConfigurableBuilder enabled(boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    @Override
    public String build() {
        return name + ":" + count + ":" + enabled;
    }
}

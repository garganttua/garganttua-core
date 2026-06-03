package com.garganttua.core.configuration.aot;

import com.garganttua.core.aot.commons.IAOTInfrastructureSeed;
import com.garganttua.core.aot.commons.IAOTSeedContext;
import com.garganttua.core.configuration.annotations.ConfigIgnore;
import com.garganttua.core.configuration.annotations.ConfigProperty;
import com.garganttua.core.configuration.annotations.Configurable;
import com.garganttua.core.configuration.annotations.ConfigurationFormat;
import com.garganttua.core.dsl.annotations.ConfigurableBuilder;

/**
 * Pre-registers the configuration module's annotation surface into the AOT
 * registry for cold-start in pure-AOT consumers.
 *
 * <p>Auto-discovered via
 * {@code META-INF/services/com.garganttua.core.aot.commons.IAOTInfrastructureSeed}.</p>
 *
 * @since 2.0.0-ALPHA02
 */
public class ConfigurationInfrastructureSeed implements IAOTInfrastructureSeed {

    /**
     * Registers the configuration annotation classes with the supplied AOT seed context.
     *
     * @param context the AOT seed context to register the annotation classes into
     */
    @Override
    public void seed(IAOTSeedContext context) {
        context.registerClass(Configurable.class);
        context.registerClass(ConfigProperty.class);
        context.registerClass(ConfigIgnore.class);
        context.registerClass(ConfigurationFormat.class);
        // The bootstrap contributor reads @ConfigurableBuilder off builder classes at
        // runtime — keep the annotation reflectively available in native-image.
        context.registerClass(ConfigurableBuilder.class);
    }
}

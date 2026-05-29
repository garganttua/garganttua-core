package com.garganttua.core.configuration.aot;

import com.garganttua.core.aot.commons.IAOTInfrastructureSeed;
import com.garganttua.core.aot.commons.IAOTSeedContext;
import com.garganttua.core.configuration.annotations.ConfigIgnore;
import com.garganttua.core.configuration.annotations.ConfigProperty;
import com.garganttua.core.configuration.annotations.Configurable;
import com.garganttua.core.configuration.annotations.ConfigurationFormat;

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

    @Override
    public void seed(IAOTSeedContext context) {
        context.registerClass(Configurable.class);
        context.registerClass(ConfigProperty.class);
        context.registerClass(ConfigIgnore.class);
        context.registerClass(ConfigurationFormat.class);
    }
}

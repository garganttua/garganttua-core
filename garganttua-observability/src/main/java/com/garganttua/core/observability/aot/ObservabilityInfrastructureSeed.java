package com.garganttua.core.observability.aot;

import com.garganttua.core.aot.commons.IAOTInfrastructureSeed;
import com.garganttua.core.aot.commons.IAOTSeedContext;
import com.garganttua.core.observability.annotations.Observer;

/**
 * Pre-registers the observability module's annotation surface into the AOT
 * registry for cold-start in pure-AOT consumers.
 *
 * <p>Auto-discovered via
 * {@code META-INF/services/com.garganttua.core.aot.commons.IAOTInfrastructureSeed}.</p>
 *
 * @since 2.0.0-ALPHA02
 */
public class ObservabilityInfrastructureSeed implements IAOTInfrastructureSeed {

    @Override
    public void seed(IAOTSeedContext context) {
        context.registerClass(Observer.class);
    }
}

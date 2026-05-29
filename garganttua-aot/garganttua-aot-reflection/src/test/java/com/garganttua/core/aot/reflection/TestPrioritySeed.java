package com.garganttua.core.aot.reflection;

import com.garganttua.core.aot.commons.IAOTInfrastructureSeed;
import com.garganttua.core.aot.commons.IAOTSeedContext;

import jakarta.annotation.Priority;

/**
 * Higher-priority test seed — runs before {@link TestExtensionSeed} which has
 * no {@link Priority} annotation (default 0). Flips {@link #SEEN_FIRST} only
 * if it sees that the lower-priority seed has not yet run.
 */
@Priority(50)
public class TestPrioritySeed implements IAOTInfrastructureSeed {

    public static volatile boolean SEEN_FIRST = false;

    @Override
    public void seed(IAOTSeedContext context) {
        SEEN_FIRST = !TestExtensionSeed.ran;
        context.registerInterface(TestPriorityMarker.class);
    }
}

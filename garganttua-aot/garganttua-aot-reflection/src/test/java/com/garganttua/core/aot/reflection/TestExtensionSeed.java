package com.garganttua.core.aot.reflection;

import com.garganttua.core.aot.commons.IAOTInfrastructureSeed;
import com.garganttua.core.aot.commons.IAOTSeedContext;

/** Default-priority test seed — exercises the basic SPI path. */
public class TestExtensionSeed implements IAOTInfrastructureSeed {

    public static volatile boolean ran = false;

    @Override
    public void seed(IAOTSeedContext context) {
        context.registerInterface(ITestApiMarker.class);
        ran = true;
    }
}

package com.garganttua.core.aot.reflection;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.garganttua.core.aot.commons.AOTRegistry;
import com.garganttua.core.reflection.IClass;

/**
 * End-to-end test for the {@code IAOTInfrastructureSeed} SPI.
 *
 * <p>Bootstraps {@link CoreInfrastructureSeed} and asserts that the test seed
 * declared in {@code META-INF/services/com.garganttua.core.aot.commons.IAOTInfrastructureSeed}
 * was discovered and successfully registered its marker types into the
 * registry.</p>
 *
 * <p>Covers: SPI discovery, seed execution, descriptor synthesis (forced
 * INTERFACE bit), and {@code @Priority} ordering between two competing
 * seeds.</p>
 */
class ExtensionSeedSpiTest {

    @Test
    void extension_seed_is_discovered_and_seeds_its_types() {
        CoreInfrastructureSeed.bootstrap();

        assertTrue(AOTRegistry.getInstance().contains(ITestApiMarker.class.getName()),
                "Marker interface from TestExtensionSeed must be registered");
        Optional<IClass<ITestApiMarker>> hit =
                AOTRegistry.getInstance().get(ITestApiMarker.class.getName());
        assertTrue(hit.isPresent());
        IClass<ITestApiMarker> desc = hit.get();
        assertNotNull(desc);
        assertTrue(desc.isInterface(),
                "Forced-INTERFACE bit must be honoured by registerInterface()");
    }

    @Test
    void higher_priority_seed_runs_first() {
        CoreInfrastructureSeed.bootstrap();

        // TestPrioritySeed has @Priority(50) and registers TestPriorityMarker.
        // TestExtensionSeed has no @Priority (default 0) and would otherwise
        // race. We only assert presence — order observable side-effect is
        // captured by TestPrioritySeed itself flipping a static flag.
        assertTrue(AOTRegistry.getInstance().contains(TestPriorityMarker.class.getName()));
        assertTrue(TestPrioritySeed.SEEN_FIRST,
                "High-priority seed must run before the default-priority one");
    }
}

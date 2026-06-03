package com.garganttua.core.aot.reflection;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.garganttua.core.aot.commons.AOTRegistry;
import com.garganttua.core.reflection.IClass;

/**
 * Behavioural tests for {@link GeneratedDescriptorLoader}.
 *
 * <p>Exercises the legacy classpath-walk path: a test index file under
 * {@code META-INF/garganttua/aot/classes/} lists {@link LegacyDescriptorFixture}
 * (whose static initializer self-registers a marker) plus a bogus FQN. The
 * loader must {@code Class.forName} the real one (registering the marker) and
 * tolerate the missing one without throwing.</p>
 */
class GeneratedDescriptorLoaderBehaviourTest {

    @Test
    void loadGeneratedDescriptors_forceLoadsLegacyIndexedDescriptor() {
        // Pre-condition: the marker is only registered as a side effect of
        // loading the fixture class via the legacy walk.
        GeneratedDescriptorLoader.loadGeneratedDescriptors();

        assertTrue(AOTRegistry.getInstance().contains(LegacyDescriptorFixture.REGISTERED_NAME),
                "Legacy classpath walk must Class.forName the indexed descriptor, "
                        + "triggering its self-registration");
        Optional<IClass<Object>> hit =
                AOTRegistry.getInstance().get(LegacyDescriptorFixture.REGISTERED_NAME);
        assertTrue(hit.isPresent());
        assertTrue("LegacyLoadedMarker".equals(hit.get().getSimpleName()));
    }

    @Test
    void loadGeneratedDescriptors_toleratesUnresolvableFqnInIndex() {
        // The index file deliberately contains a non-existent FQN. loadEntry
        // must swallow the ClassNotFoundException (logged to stderr) and keep
        // processing — i.e. the whole call completes normally.
        assertDoesNotThrow(GeneratedDescriptorLoader::loadGeneratedDescriptors);
        // And the bogus class is, of course, NOT registered.
        assertTrue(!AOTRegistry.getInstance()
                .contains("com.garganttua.test.DefinitelyDoesNotExistDescriptor"));
    }

    @Test
    void loadGeneratedDescriptors_isRepeatable() {
        GeneratedDescriptorLoader.loadGeneratedDescriptors();
        assertDoesNotThrow(GeneratedDescriptorLoader::loadGeneratedDescriptors);
        assertTrue(AOTRegistry.getInstance().contains(LegacyDescriptorFixture.REGISTERED_NAME));
    }
}

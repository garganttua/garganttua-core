package com.garganttua.core.aot.reflection;

import java.lang.annotation.Annotation;

import com.garganttua.core.aot.commons.AOTRegistry;

/**
 * Test fixture standing in for a generated {@code AOTClass_*}: its static
 * initializer self-registers a marker descriptor into {@link AOTRegistry},
 * exactly like the annotation-processor output. Referenced by FQN from the
 * legacy classpath-walk index file
 * {@code META-INF/garganttua/aot/classes/test-descriptors} so that
 * {@link GeneratedDescriptorLoader#loadGeneratedDescriptors()} force-loads it.
 */
public final class LegacyDescriptorFixture {

    /** The synthetic type name this fixture registers when its class loads. */
    public static final String REGISTERED_NAME = "com.garganttua.test.LegacyLoadedMarker";

    static {
        AOTClass<Object> descriptor = new AOTClass<>(
                REGISTERED_NAME,
                "LegacyLoadedMarker",
                REGISTERED_NAME,
                "com.garganttua.test",
                java.lang.reflect.Modifier.PUBLIC,
                "java.lang.Object",
                new String[0],
                new AOTField[0],
                new AOTMethod[0],
                new AOTConstructor[0],
                new Annotation[0],
                false, false, false, false, false, false,
                false, false, false, false, false, false);
        AOTRegistry.getInstance().register(REGISTERED_NAME, descriptor);
    }

    private LegacyDescriptorFixture() {
    }
}

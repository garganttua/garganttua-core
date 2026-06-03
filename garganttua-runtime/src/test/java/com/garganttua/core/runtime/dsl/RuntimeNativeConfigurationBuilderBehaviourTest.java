package com.garganttua.core.runtime.dsl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.garganttua.core.nativve.IReflectionConfigurationEntryBuilder;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IReflectionUsageReporter;
import com.garganttua.core.reflection.dsl.IReflectionBuilder;
import com.garganttua.core.reflection.dsl.ReflectionBuilder;
import com.garganttua.core.reflection.runtime.RuntimeReflectionProvider;
import com.garganttua.core.reflections.ReflectionsAnnotationScanner;

/**
 * Behaviour tests for {@link RuntimeNativeConfigurationBuilder}: package
 * accumulation semantics, the fixed set of native reflection entries produced
 * by {@code doBuild()}, and the no-op auto-detection path.
 */
class RuntimeNativeConfigurationBuilderBehaviourTest {

    private static IReflectionBuilder reflectionBuilder;

    @BeforeAll
    static void setup() throws Exception {
        reflectionBuilder = ReflectionBuilder.builder()
                .withProvider(new RuntimeReflectionProvider())
                .withScanner(new ReflectionsAnnotationScanner());
        reflectionBuilder.build();
    }

    @AfterAll
    static void tearDown() {
        IClass.setReflection(null);
    }

    @Test
    void withPackage_accumulatesUniquePackages() {
        RuntimeNativeConfigurationBuilder builder = new RuntimeNativeConfigurationBuilder();

        builder.withPackage("com.foo");
        builder.withPackage("com.bar");
        builder.withPackage("com.foo"); // duplicate must be deduplicated by the Set

        Set<String> packages = new HashSet<>(Arrays.asList(builder.getPackages()));
        assertEquals(Set.of("com.foo", "com.bar"), packages);
    }

    @Test
    void withPackage_returnsSameBuilderForChaining() {
        RuntimeNativeConfigurationBuilder builder = new RuntimeNativeConfigurationBuilder();
        assertSame(builder, builder.withPackage("com.x"));
    }

    @Test
    void withPackages_addsAllAndMergesWithExisting() {
        RuntimeNativeConfigurationBuilder builder = new RuntimeNativeConfigurationBuilder();

        builder.withPackage("com.existing");
        RuntimeNativeConfigurationBuilder returned =
                builder.withPackages(new String[] { "com.a", "com.b", "com.existing" });

        assertSame(builder, returned);
        assertEquals(Set.of("com.existing", "com.a", "com.b"),
                new HashSet<>(Arrays.asList(builder.getPackages())));
    }

    @Test
    void withPackages_emptyArray_leavesPackagesUnchanged() {
        RuntimeNativeConfigurationBuilder builder = new RuntimeNativeConfigurationBuilder();
        builder.withPackage("com.only");

        builder.withPackages(new String[0]);

        assertEquals(Set.of("com.only"), new HashSet<>(Arrays.asList(builder.getPackages())));
    }

    @Test
    void getPackages_onFreshBuilder_isEmpty() {
        RuntimeNativeConfigurationBuilder builder = new RuntimeNativeConfigurationBuilder();
        assertEquals(0, builder.getPackages().length);
    }

    @Test
    void build_producesReporterWithExpectedNumberOfReflectionEntries() throws Exception {
        RuntimeNativeConfigurationBuilder builder = new RuntimeNativeConfigurationBuilder();

        IReflectionUsageReporter reporter = builder.build();
        Set<IReflectionConfigurationEntryBuilder> entries = reporter.reflectionUsage();

        // doBuild() issues 27 entries.add(...) calls covering core runtime, step,
        // resolver and DSL-builder classes; each targets a distinct class so the
        // backing HashSet keeps all of them.
        assertEquals(27, entries.size(),
                "doBuild must register one reflection entry per distinct runtime class");
    }

    @Test
    void build_isCachedAndReturnsSameReporterInstance() throws Exception {
        RuntimeNativeConfigurationBuilder builder = new RuntimeNativeConfigurationBuilder();

        IReflectionUsageReporter first = builder.build();
        IReflectionUsageReporter second = builder.build();

        // AbstractAutomaticBuilder caches the built instance.
        assertSame(first, second, "build() must return the cached reporter on subsequent calls");
    }

    @Test
    void build_withAutoDetectEnabled_doesNotThrow_andStillProducesEntries() throws Exception {
        RuntimeNativeConfigurationBuilder builder = new RuntimeNativeConfigurationBuilder();
        builder.autoDetect(true);
        builder.withPackage("com.garganttua.core.runtime");

        assertTrue(builder.isAutoDetected());
        IReflectionUsageReporter reporter = builder.build();
        assertFalse(reporter.reflectionUsage().isEmpty(),
                "auto-detection is a no-op but the static entries must still be produced");
    }
}

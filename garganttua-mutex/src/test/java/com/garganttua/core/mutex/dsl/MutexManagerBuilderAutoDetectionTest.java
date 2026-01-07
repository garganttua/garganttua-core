package com.garganttua.core.mutex.dsl;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.injection.context.dsl.DiContextBuilder;
import com.garganttua.core.injection.context.dsl.IDiContextBuilder;
import com.garganttua.core.mutex.IMutexManager;
import com.garganttua.core.mutex.InterruptibleLeaseMutex;
import com.garganttua.core.mutex.InterruptibleLeaseMutexFactory;
import com.garganttua.core.mutex.MutexException;
import com.garganttua.core.mutex.MutexName;
import com.garganttua.core.reflection.utils.ObjectReflectionHelper;
import com.garganttua.core.reflections.ReflectionsAnnotationScanner;

/**
 * Tests for MutexManagerBuilder auto-detection functionality.
 *
 * <p>
 * This test suite verifies that the builder can automatically discover and
 * register
 * mutex factories annotated with @MutexFactory when scanning specified
 * packages.
 * </p>
 */
class MutexManagerBuilderAutoDetectionTest {

    @BeforeAll
    static void setup() {
        ObjectReflectionHelper.setAnnotationScanner(new ReflectionsAnnotationScanner());
    }

    @Test
    void testAutoDetectionWithNoContext() {
        // Auto-detection without DI context should not throw
        assertDoesNotThrow(() -> {
            MutexManagerBuilder.builder()
                    .withPackage("com.garganttua.core.mutex")
                    .autoDetect(true)
                    .build();
        }, "Auto-detection without context should not throw");
    }

    @Test
    void testAutoDetectionWithContext() throws DslException {
        // Create a DI context builder
        IDiContextBuilder contextBuilder = DiContextBuilder.builder()
                .withPackage("com.garganttua.core.mutex");

        contextBuilder.build();

        // Build with auto-detection enabled
        IMutexManager manager = MutexManagerBuilder.builder(contextBuilder)
                .withPackage("com.garganttua.core.mutex")
                .autoDetect(true)
                .build();

        assertNotNull(manager, "Manager should be built successfully");

        manager.mutex(new MutexName(TestMutex.class, "TestMutexFactory"));
    }

    @Test
    void testAutoDetectionScansMultiplePackages() throws DslException {
        IDiContextBuilder contextBuilder = DiContextBuilder.builder()
                .withPackages(new String[] {
                        "com.garganttua.core.mutex",
                        "com.garganttua.core.mutex.dsl"
                });

        contextBuilder.build();

        IMutexManager manager = MutexManagerBuilder.builder(contextBuilder)
                .withPackages(new String[] {
                        "com.garganttua.core.mutex",
                        "com.garganttua.core.mutex.dsl"
                })
                .autoDetect(true)
                .build();

        assertNotNull(manager, "Manager should be built with multiple packages");
    }

    @Test
    void testWithPackageMethod() {
        IMutexManagerBuilder builder = MutexManagerBuilder.builder()
                .withPackage("com.garganttua.core.mutex");

        String[] packages = builder.getPackages();
        assertEquals(1, packages.length, "Should have one package");
        assertEquals("com.garganttua.core.mutex", packages[0], "Package name should match");
    }

    @Test
    void testWithPackagesMethod() {
        String[] testPackages = {
                "com.garganttua.core.mutex",
                "com.garganttua.core.mutex.dsl",
                "com.garganttua.core.mutex.annotations"
        };

        IMutexManagerBuilder builder = MutexManagerBuilder.builder()
                .withPackages(testPackages);

        String[] packages = builder.getPackages();
        assertEquals(3, packages.length, "Should have three packages");
    }

    @Test
    void testWithPackageReturnsBuilder() {
        IMutexManagerBuilder builder = MutexManagerBuilder.builder();
        IMutexManagerBuilder result = builder.withPackage("com.garganttua.core.mutex");

        assertSame(builder, result, "withPackage should return the builder for chaining");
    }

    @Test
    void testWithPackagesReturnsBuilder() {
        IMutexManagerBuilder builder = MutexManagerBuilder.builder();
        IMutexManagerBuilder result = builder.withPackages(new String[] { "com.garganttua.core.mutex" });

        assertSame(builder, result, "withPackages should return the builder for chaining");
    }

    @Test
    void testWithPackageNullThrows() {
        IMutexManagerBuilder builder = MutexManagerBuilder.builder();

        assertThrows(NullPointerException.class, () -> {
            builder.withPackage(null);
        }, "Should throw when package is null");
    }

    @Test
    void testWithPackagesNullThrows() {
        IMutexManagerBuilder builder = MutexManagerBuilder.builder();

        assertThrows(NullPointerException.class, () -> {
            builder.withPackages(null);
        }, "Should throw when packages array is null");
    }

    @Test
    void testAutoDetectionWithManualFactories() throws DslException, MutexException {
        // Test that auto-detection works alongside manually registered factories
        IDiContextBuilder contextBuilder = DiContextBuilder.builder()
                .withPackage("com.garganttua.core.mutex");

        contextBuilder.build();
        InterruptibleLeaseMutexFactory factory = new InterruptibleLeaseMutexFactory();

        IMutexManager manager = MutexManagerBuilder.builder(contextBuilder)
                .withFactory(InterruptibleLeaseMutex.class, factory)
                .withPackage("com.garganttua.core.mutex")
                .autoDetect(true)
                .build();

        assertNotNull(manager, "Manager should be built");

        // Verify we can create a mutex using the manually registered factory
        MutexName name = MutexName.fromString("InterruptibleLeaseMutex::test");
        assertDoesNotThrow(() -> {
            manager.mutex(name);
        }, "Should be able to create mutex with registered factory");
    }

    @Test
    void testAutoDetectionDisabled() throws DslException {
        // When auto-detection is disabled, only manually registered factories should be
        // used
        IDiContextBuilder contextBuilder = DiContextBuilder.builder()
                .withPackage("com.garganttua.core.mutex");
        contextBuilder.build();

        IMutexManager manager = MutexManagerBuilder.builder(contextBuilder)
                .withPackage("com.garganttua.core.mutex")
                .autoDetect(false)
                .build();

        assertNotNull(manager, "Manager should be built even with auto-detection disabled");
    }

    @Test
    void testGetPackagesReturnsEmptyArrayWhenNoPackages() {
        IMutexManagerBuilder builder = MutexManagerBuilder.builder();

        String[] packages = builder.getPackages();

        assertNotNull(packages, "Should return non-null array");
        assertEquals(0, packages.length, "Should return empty array when no packages configured");
    }

    @Test
    void testPackageConfigurationWithFluentAPI() throws DslException {
        IMutexManager manager = MutexManagerBuilder.builder()
                .withPackage("com.garganttua.core.mutex")
                .withPackage("com.garganttua.core.mutex.dsl")
                .autoDetect(false)
                .build();

        assertNotNull(manager, "Manager should be built with fluent API");
    }

    @Test
    void testAutoDetectionWithContextAndObserver() throws DslException {
        // Create context builder that will build the context
        IDiContextBuilder contextBuilder = DiContextBuilder.builder()
                .withPackage("com.garganttua.core.mutex");

        // Build context to trigger observer
        contextBuilder.build();

        IMutexManager manager = MutexManagerBuilder.builder(contextBuilder)
                .withPackage("com.garganttua.core.mutex")
                .autoDetect(true)
                .build();

        assertNotNull(manager, "Manager should be built after context build");
    }

}

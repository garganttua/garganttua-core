package com.garganttua.core.mutex.dsl;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.injection.context.dsl.InjectionContextBuilder;
import com.garganttua.core.injection.context.dsl.IInjectionContextBuilder;
import com.garganttua.core.mutex.IMutexManager;
import com.garganttua.core.reflection.utils.ObjectReflectionHelper;
import com.garganttua.core.reflections.ReflectionsAnnotationScanner;

/**
 * Tests for package synchronization between MutexManagerBuilder and InjectionContextBuilder.
 *
 * <p>
 * This test suite verifies that packages declared in the InjectionContextBuilder
 * are automatically synchronized to the MutexManagerBuilder during auto-detection.
 * </p>
 */
class MutexManagerBuilderPackageSyncTest {

    @BeforeAll
    static void setup() {
        ObjectReflectionHelper.setAnnotationScanner(new ReflectionsAnnotationScanner());
    }

    @Test
    void testPackageSynchronizationFromContextBuilder() throws DslException {
        // Create a DI context builder with packages
        IInjectionContextBuilder contextBuilder = InjectionContextBuilder.builder()
                .withPackage("com.garganttua.core.mutex")
                .withPackage("com.garganttua.core.mutex.dsl");

        contextBuilder.build();

        // Create MutexManagerBuilder WITHOUT specifying packages
        // It should synchronize packages from InjectionContextBuilder during auto-detection
        IMutexManagerBuilder mutexBuilder = MutexManagerBuilder.builder(contextBuilder)
                .autoDetect(true);

        // Before build, packages should be empty in MutexManagerBuilder
        String[] packagesBefore = mutexBuilder.getPackages();
        assertEquals(0, packagesBefore.length, "MutexManagerBuilder should start with no packages");

        // Build - this should trigger auto-detection and package synchronization
        IMutexManager manager = mutexBuilder.build();

        assertNotNull(manager, "Manager should be built successfully");
    }

    @Test
    void testPackageSynchronizationMergesWithExistingPackages() throws DslException {
        // Create a DI context builder with packages
        IInjectionContextBuilder contextBuilder = InjectionContextBuilder.builder()
                .withPackage("com.garganttua.core.mutex")
                .withPackage("com.garganttua.core.mutex.dsl");

        contextBuilder.build();

        // Create MutexManagerBuilder WITH one package specified
        IMutexManagerBuilder mutexBuilder = MutexManagerBuilder.builder(contextBuilder)
                .withPackage("com.garganttua.core.mutex.annotations")
                .autoDetect(true);

        // Before build, should have only the manually added package
        String[] packagesBefore = mutexBuilder.getPackages();
        assertEquals(1, packagesBefore.length, "Should have one manually added package");
        assertEquals("com.garganttua.core.mutex.annotations", packagesBefore[0]);

        // Build - this should merge InjectionContextBuilder packages with existing ones
        IMutexManager manager = mutexBuilder.build();

        assertNotNull(manager, "Manager should be built successfully");
    }

    @Test
    void testPackageSynchronizationWithLateContextBuilderPackageAddition() throws DslException {
        // Create a DI context builder
        IInjectionContextBuilder contextBuilder = InjectionContextBuilder.builder();

        // Build the context first
        contextBuilder.build();

        // Create MutexManagerBuilder and link it to the context
        IMutexManagerBuilder mutexBuilder = MutexManagerBuilder.builder(contextBuilder);

        // NOW add packages to the InjectionContextBuilder AFTER linking
        contextBuilder.withPackage("com.garganttua.core.mutex")
                .withPackage("com.garganttua.core.mutex.dsl");

        // Enable auto-detection and build
        mutexBuilder.autoDetect(true);
        IMutexManager manager = mutexBuilder.build();

        assertNotNull(manager, "Manager should be built successfully with late-added packages");
    }

    @Test
    void testNoSynchronizationWhenNoContextBuilder() throws DslException {
        // Create MutexManagerBuilder WITHOUT context builder
        IMutexManagerBuilder mutexBuilder = MutexManagerBuilder.builder()
                .withPackage("com.garganttua.core.mutex")
                .autoDetect(false);

        String[] packages = mutexBuilder.getPackages();
        assertEquals(1, packages.length, "Should have only manually added package");

        // Build should work without context
        IMutexManager manager = mutexBuilder.build();
        assertNotNull(manager, "Manager should be built without context");
    }

    @Test
    void testContextBuilderWithNoPackages() throws DslException {
        // Create a DI context builder with NO packages
        IInjectionContextBuilder contextBuilder = InjectionContextBuilder.builder();
        contextBuilder.build();

        // Create MutexManagerBuilder with one package
        IMutexManagerBuilder mutexBuilder = MutexManagerBuilder.builder(contextBuilder)
                .withPackage("com.garganttua.core.mutex")
                .autoDetect(true);

        String[] packages = mutexBuilder.getPackages();
        assertEquals(1, packages.length, "Should have only manually added package");

        IMutexManager manager = mutexBuilder.build();
        assertNotNull(manager, "Manager should be built successfully");
    }
}

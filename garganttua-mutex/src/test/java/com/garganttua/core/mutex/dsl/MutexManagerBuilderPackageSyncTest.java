package com.garganttua.core.mutex.dsl;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.injection.context.dsl.InjectionContextBuilder;
import com.garganttua.core.injection.context.dsl.IInjectionContextBuilder;
import com.garganttua.core.mutex.IMutexManager;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.dsl.IReflectionBuilder;
import com.garganttua.core.reflection.dsl.ReflectionBuilder;
import com.garganttua.core.reflections.ReflectionsAnnotationScanner;
import com.garganttua.core.reflection.runtime.RuntimeReflectionProvider;

class MutexManagerBuilderPackageSyncTest {

    private static IReflectionBuilder reflectionBuilder;

    @BeforeAll
    static void setup() {
        reflectionBuilder = ReflectionBuilder.builder()
                .withProvider(new RuntimeReflectionProvider())
                .withScanner(new ReflectionsAnnotationScanner());
        reflectionBuilder.build();
    }

    @Test
    void testPackageSynchronizationFromContextBuilder() throws DslException {
        IInjectionContextBuilder contextBuilder = InjectionContextBuilder.builder()
                .provide(reflectionBuilder)
                .withPackage("com.garganttua.core.mutex")
                .withPackage("com.garganttua.core.mutex.dsl");

        contextBuilder.build().onInit().onStart();

        IMutexManagerBuilder mutexBuilder = MutexManagerBuilder.builder().provide(contextBuilder)
                .autoDetect(true);

        String[] packagesBefore = mutexBuilder.getPackages();
        assertEquals(0, packagesBefore.length, "MutexManagerBuilder should start with no packages");

        IMutexManager manager = mutexBuilder.build();

        assertNotNull(manager, "Manager should be built successfully");
    }

    @Test
    void testPackageSynchronizationMergesWithExistingPackages() throws DslException {
        IInjectionContextBuilder contextBuilder = InjectionContextBuilder.builder()
                .provide(reflectionBuilder)
                .withPackage("com.garganttua.core.mutex")
                .withPackage("com.garganttua.core.mutex.dsl");

        contextBuilder.build().onInit().onStart();

        IMutexManagerBuilder mutexBuilder = MutexManagerBuilder.builder().provide(contextBuilder)
                .withPackage("com.garganttua.core.mutex.annotations")
                .autoDetect(true);

        String[] packagesBefore = mutexBuilder.getPackages();
        assertEquals(1, packagesBefore.length, "Should have one manually added package");
        assertEquals("com.garganttua.core.mutex.annotations", packagesBefore[0]);

        IMutexManager manager = mutexBuilder.build();

        assertNotNull(manager, "Manager should be built successfully");
    }

    @Test
    void testPackageSynchronizationWithLateContextBuilderPackageAddition() throws DslException {
        IInjectionContextBuilder contextBuilder = InjectionContextBuilder.builder()
                .provide(reflectionBuilder);

        contextBuilder.build().onInit().onStart();

        IMutexManagerBuilder mutexBuilder = MutexManagerBuilder.builder().provide(contextBuilder);

        contextBuilder.withPackage("com.garganttua.core.mutex")
                .withPackage("com.garganttua.core.mutex.dsl");

        mutexBuilder.autoDetect(true);
        IMutexManager manager = mutexBuilder.build();

        assertNotNull(manager, "Manager should be built successfully with late-added packages");
    }

    @Test
    void testNoSynchronizationWhenNoContextBuilder() throws DslException {
        IMutexManagerBuilder mutexBuilder = MutexManagerBuilder.builder()
                .withPackage("com.garganttua.core.mutex")
                .autoDetect(false);

        String[] packages = mutexBuilder.getPackages();
        assertEquals(1, packages.length, "Should have only manually added package");

        IMutexManager manager = mutexBuilder.build();
        assertNotNull(manager, "Manager should be built without context");
    }

    @Test
    void testContextBuilderWithNoPackages() throws DslException {
        IInjectionContextBuilder contextBuilder = InjectionContextBuilder.builder()
                .provide(reflectionBuilder);
        contextBuilder.build().onInit().onStart();

        IMutexManagerBuilder mutexBuilder = MutexManagerBuilder.builder().provide(contextBuilder)
                .withPackage("com.garganttua.core.mutex")
                .autoDetect(true);

        String[] packages = mutexBuilder.getPackages();
        assertEquals(1, packages.length, "Should have only manually added package");

        IMutexManager manager = mutexBuilder.build();
        assertNotNull(manager, "Manager should be built successfully");
    }
}

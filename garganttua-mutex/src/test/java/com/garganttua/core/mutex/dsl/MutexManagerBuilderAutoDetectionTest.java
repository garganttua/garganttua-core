package com.garganttua.core.mutex.dsl;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.injection.context.dsl.InjectionContextBuilder;
import com.garganttua.core.injection.context.dsl.IInjectionContextBuilder;
import com.garganttua.core.mutex.IMutex;
import com.garganttua.core.mutex.IMutexManager;
import com.garganttua.core.mutex.InterruptibleLeaseMutex;
import com.garganttua.core.mutex.InterruptibleLeaseMutexFactory;
import com.garganttua.core.mutex.MutexException;
import com.garganttua.core.mutex.MutexName;
import com.garganttua.core.mutex.dsl.fixtures.TestMutex;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.dsl.IReflectionBuilder;
import com.garganttua.core.reflection.dsl.ReflectionBuilder;
import com.garganttua.core.reflections.ReflectionsAnnotationScanner;
import com.garganttua.core.reflection.runtime.RuntimeReflectionProvider;

class MutexManagerBuilderAutoDetectionTest {

    private static IReflectionBuilder reflectionBuilder;

    @BeforeAll
    static void setup() {
        reflectionBuilder = ReflectionBuilder.builder()
                .withProvider(new RuntimeReflectionProvider())
                .withScanner(new ReflectionsAnnotationScanner());
        reflectionBuilder.build();
    }

    @Test
    void testAutoDetectionWithNoContext() {
        assertDoesNotThrow(() -> {
            MutexManagerBuilder.builder()
                    .withPackage("com.garganttua.core.mutex")
                    .autoDetect(true)
                    .build();
        }, "Auto-detection without context should not throw");
    }

    @Test
    void testAutoDetectionWithContext() throws DslException {
        IInjectionContextBuilder contextBuilder = InjectionContextBuilder.builder()
                .provide(reflectionBuilder)
                .withPackage("com.garganttua.core.mutex");

        contextBuilder.build().onInit().onStart();

        IMutexManager manager = MutexManagerBuilder.builder()
                .withPackage("com.garganttua.core.mutex")
                .autoDetect(true)
                .build();

        assertNotNull(manager, "Manager should be built successfully");

        IMutex mutex = manager.mutex(new MutexName(IClass.getClass(TestMutex.class), "TestMutexFactory"));
        assertNotNull(mutex);
        mutex.acquire(() -> {System.out.println("Hello World");return 2;});
    }

    @Test
    void testAutoDetectionScansMultiplePackages() throws DslException {
        IInjectionContextBuilder contextBuilder = InjectionContextBuilder.builder()
                .provide(reflectionBuilder)
                .withPackages(new String[] {
                        "com.garganttua.core.mutex",
                        "com.garganttua.core.mutex.dsl"
                });

        contextBuilder.build().onInit().onStart();

        IMutexManager manager = MutexManagerBuilder.builder()
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
        IInjectionContextBuilder contextBuilder = InjectionContextBuilder.builder()
                .provide(reflectionBuilder)
                .withPackage("com.garganttua.core.mutex");

        contextBuilder.build().onInit().onStart();
        InterruptibleLeaseMutexFactory factory = new InterruptibleLeaseMutexFactory();

        IMutexManager manager = MutexManagerBuilder.builder()
                .withFactory(IClass.getClass(InterruptibleLeaseMutex.class), factory)
                .withPackage("com.garganttua.core.mutex")
                .autoDetect(true)
                .build();

        assertNotNull(manager, "Manager should be built");

        MutexName name = MutexName.fromString("com.garganttua.core.mutex.InterruptibleLeaseMutex::test");
        assertDoesNotThrow(() -> {
            manager.mutex(name);
        }, "Should be able to create mutex with registered factory");
    }

    @Test
    void testAutoDetectionDisabled() throws DslException {
        IInjectionContextBuilder contextBuilder = InjectionContextBuilder.builder()
                .provide(reflectionBuilder)
                .withPackage("com.garganttua.core.mutex");
        contextBuilder.build().onInit().onStart();

        IMutexManager manager = MutexManagerBuilder.builder()
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
        IInjectionContextBuilder contextBuilder = InjectionContextBuilder.builder()
                .provide(reflectionBuilder)
                .withPackage("com.garganttua.core.mutex");

        contextBuilder.build().onInit().onStart();

        IMutexManager manager = MutexManagerBuilder.builder()
                .withPackage("com.garganttua.core.mutex")
                .autoDetect(true)
                .build();

        assertNotNull(manager, "Manager should be built after context build");
    }

}

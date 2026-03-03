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
import com.garganttua.core.mutex.MutexException;
import com.garganttua.core.mutex.MutexName;
import com.garganttua.core.mutex.dsl.fixtures.TestMutex;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.dsl.IReflectionBuilder;
import com.garganttua.core.reflection.dsl.ReflectionBuilder;
import com.garganttua.core.reflections.ReflectionsAnnotationScanner;
import com.garganttua.core.reflection.runtime.RuntimeReflectionProvider;

class MutexManagerBuilderIntegrationTest {

        private static IReflectionBuilder reflectionBuilder;

        @BeforeAll
        static void setup() {
                reflectionBuilder = ReflectionBuilder.builder()
                                .withProvider(new RuntimeReflectionProvider())
                                .withScanner(new ReflectionsAnnotationScanner());
                reflectionBuilder.build();
        }

        @Test
        void testFullWorkflowWithAutoDetection() throws DslException, MutexException {
                IInjectionContextBuilder contextBuilder = InjectionContextBuilder.builder()
                                .provide(reflectionBuilder)
                                .withPackage("com.garganttua.core.mutex.dsl.fixtures");

                contextBuilder.build().onInit().onStart();

                IMutexManager manager = MutexManagerBuilder.builder()
                                .withPackage("com.garganttua.core.mutex.dsl.fixtures")
                                .autoDetect(true)
                                .build();

                assertNotNull(manager, "Manager should be created");
        }

        @Test
        void testAutoDetectedFactoryCanCreateMutex() throws DslException, MutexException {
                IInjectionContextBuilder contextBuilder = InjectionContextBuilder.builder()
                                .provide(reflectionBuilder)
                                .withPackage("com.garganttua.core.mutex.dsl.fixtures");

                contextBuilder.build().onInit().onStart();

                IMutexManager manager = MutexManagerBuilder.builder()
                                .withPackage("com.garganttua.core.mutex.dsl.fixtures")
                                .autoDetect(true)
                                .build();

                MutexName name = MutexName.fromString("com.garganttua.core.mutex.dsl.fixtures.TestMutex::integration-test");
                IMutex mutex = manager.mutex(name);

                assertNotNull(mutex, "Mutex should be created");
                assertTrue(mutex instanceof InterruptibleLeaseMutex || mutex instanceof TestMutex,
                                "Mutex should be either TestMutex or default InterruptibleLeaseMutex");
        }

        @Test
        void testMixedManualAndAutoDetectedFactories() throws DslException, MutexException {
                IInjectionContextBuilder contextBuilder = InjectionContextBuilder.builder()
                                .provide(reflectionBuilder)
                                .withPackage("com.garganttua.core.mutex")
                                .withPackage("com.garganttua.core.mutex.dsl.fixtures");

                contextBuilder.build().onInit().onStart();

                IMutexManager manager = MutexManagerBuilder.builder()
                                .withPackage("com.garganttua.core.mutex")
                                .withPackage("com.garganttua.core.mutex.dsl.fixtures")
                                .autoDetect(true)
                                .build();

                assertNotNull(manager, "Manager should be created with mixed factories");

                MutexName interruptibleName = MutexName.fromString("com.garganttua.core.mutex.InterruptibleLeaseMutex::test1");
                IMutex interruptibleMutex = manager.mutex(interruptibleName);
                assertNotNull(interruptibleMutex, "Should create InterruptibleLeaseMutex");

                MutexName testName = MutexName.fromString("com.garganttua.core.mutex.dsl.fixtures.TestMutex::test2");
                IMutex testMutex = manager.mutex(testName);
                assertNotNull(testMutex, "Should create TestMutex or fallback");
        }

        @Test
        void testAutoDetectionScansCorrectPackage() throws DslException {
                IInjectionContextBuilder contextBuilder = InjectionContextBuilder.builder()
                                .provide(reflectionBuilder)
                                .withPackage("com.garganttua.core.mutex.dsl.fixtures");

                contextBuilder.build().onInit().onStart();

                IMutexManager manager = MutexManagerBuilder.builder()
                                .withPackage("com.garganttua.core.mutex.dsl.fixtures")
                                .autoDetect(true)
                                .build();

                assertNotNull(manager, "Manager should be created");

                String[] packages = ((MutexManagerBuilder) MutexManagerBuilder.builder()
                                .withPackage("com.garganttua.core.mutex.dsl.fixtures"))
                                .getPackages();

                assertEquals(1, packages.length, "Should have one package");
                assertEquals("com.garganttua.core.mutex.dsl.fixtures", packages[0]);
        }

        @Test
        void testCreatedMutexIsUsable() throws DslException, MutexException {
                IInjectionContextBuilder contextBuilder = InjectionContextBuilder.builder()
                                .provide(reflectionBuilder)
                                .withPackage("com.garganttua.core.mutex");

                contextBuilder.build().onInit().onStart();

                IMutexManager manager = MutexManagerBuilder.builder()
                                .withPackage("com.garganttua.core.mutex")
                                .autoDetect(true)
                                .build();

                MutexName name = MutexName.fromString("com.garganttua.core.mutex.InterruptibleLeaseMutex::functional-test");
                IMutex mutex = manager.mutex(name);

                String result = mutex.acquire(() -> {
                        return "Successfully executed";
                });

                assertEquals("Successfully executed", result, "Mutex should execute function correctly");
        }

        @Test
        void testSameMutexReturnedOnSubsequentCalls() throws DslException, MutexException {
                IInjectionContextBuilder contextBuilder = InjectionContextBuilder.builder()
                                .provide(reflectionBuilder)
                                .withPackage("com.garganttua.core.mutex");

                contextBuilder.build().onInit().onStart();

                IMutexManager manager = MutexManagerBuilder.builder()
                                .withPackage("com.garganttua.core.mutex")
                                .autoDetect(true)
                                .build();

                MutexName name = MutexName.fromString("com.garganttua.core.mutex.InterruptibleLeaseMutex::singleton-test");
                IMutex mutex1 = manager.mutex(name);
                IMutex mutex2 = manager.mutex(name);

                assertSame(mutex1, mutex2, "Same mutex instance should be returned for same name");
        }

        @Test
        void testAutoDetectionWithMultiplePackagesFindsAllFactories() throws DslException {
                String[] packages = {
                                "com.garganttua.core.mutex",
                                "com.garganttua.core.mutex.dsl.fixtures"
                };

                IInjectionContextBuilder contextBuilder = InjectionContextBuilder.builder()
                                .provide(reflectionBuilder)
                                .withPackages(packages);

                contextBuilder.build().onInit().onStart();

                IMutexManager manager = MutexManagerBuilder.builder()
                                .withPackages(packages)
                                .autoDetect(true)
                                .build();

                assertNotNull(manager, "Manager should be created with multiple packages");
        }

}

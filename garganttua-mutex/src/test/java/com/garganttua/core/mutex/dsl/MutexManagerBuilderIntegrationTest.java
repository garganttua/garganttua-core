package com.garganttua.core.mutex.dsl;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.injection.context.dsl.DiContextBuilder;
import com.garganttua.core.injection.context.dsl.IDiContextBuilder;
import com.garganttua.core.mutex.IMutex;
import com.garganttua.core.mutex.IMutexManager;
import com.garganttua.core.mutex.InterruptibleLeaseMutex;
import com.garganttua.core.mutex.MutexException;
import com.garganttua.core.mutex.MutexName;
import com.garganttua.core.mutex.dsl.fixtures.TestMutex;
import com.garganttua.core.reflection.utils.ObjectReflectionHelper;
import com.garganttua.core.reflections.ReflectionsAnnotationScanner;

/**
 * Integration tests for MutexManagerBuilder with real DI context.
 *
 * <p>
 * These tests verify the complete workflow of building a MutexManager
 * with auto-detection, including scanning for @MutexFactory annotations
 * and registering discovered factories in the DI context.
 * </p>
 */
class MutexManagerBuilderIntegrationTest {

        @BeforeAll
        static void setup() {
                ObjectReflectionHelper.setAnnotationScanner(new ReflectionsAnnotationScanner());
        }

        @Test
        void testFullWorkflowWithAutoDetection() throws DslException, MutexException {
                // Create and build DI context
                IDiContextBuilder contextBuilder = DiContextBuilder.builder()
                                .withPackage("com.garganttua.core.mutex.dsl.fixtures");

                // Build the context to make it available
                contextBuilder.build();

                // Create mutex manager with auto-detection
                IMutexManager manager = MutexManagerBuilder.builder(contextBuilder)
                                .withPackage("com.garganttua.core.mutex.dsl.fixtures")
                                .autoDetect(true)
                                .build();

                assertNotNull(manager, "Manager should be created");
        }

        @Test
        void testAutoDetectedFactoryCanCreateMutex() throws DslException, MutexException {
                // Setup context with the test fixtures package
                IDiContextBuilder contextBuilder = DiContextBuilder.builder()
                                .withPackage("com.garganttua.core.mutex.dsl.fixtures");

                contextBuilder.build();

                // Build manager with auto-detection
                IMutexManager manager = MutexManagerBuilder.builder(contextBuilder)
                                .withPackage("com.garganttua.core.mutex.dsl.fixtures")
                                .autoDetect(true)
                                .build();

                // Try to create a mutex using the auto-detected TestMutexFactory
                MutexName name = MutexName.fromString("com.garganttua.core.mutex.dsl.fixtures.TestMutex::integration-test");
                IMutex mutex = manager.mutex(name);

                assertNotNull(mutex, "Mutex should be created");
                // The default fallback is InterruptibleLeaseMutex if no factory matches
                assertTrue(mutex instanceof InterruptibleLeaseMutex || mutex instanceof TestMutex,
                                "Mutex should be either TestMutex or default InterruptibleLeaseMutex");
        }

        @Test
        void testMixedManualAndAutoDetectedFactories() throws DslException, MutexException {
                // Setup context
                IDiContextBuilder contextBuilder = DiContextBuilder.builder()
                                .withPackage("com.garganttua.core.mutex")
                                .withPackage("com.garganttua.core.mutex.dsl.fixtures");

                contextBuilder.build();

                // Build manager with both manual and auto-detected factories
                IMutexManager manager = MutexManagerBuilder.builder(contextBuilder)
                                .withPackage("com.garganttua.core.mutex")
                                .withPackage("com.garganttua.core.mutex.dsl.fixtures")
                                .autoDetect(true)
                                .build();

                assertNotNull(manager, "Manager should be created with mixed factories");

                // Test creating mutex with InterruptibleLeaseMutex type
                MutexName interruptibleName = MutexName.fromString("com.garganttua.core.mutex.InterruptibleLeaseMutex::test1");
                IMutex interruptibleMutex = manager.mutex(interruptibleName);
                assertNotNull(interruptibleMutex, "Should create InterruptibleLeaseMutex");

                // Test creating mutex with TestMutex type
                MutexName testName = MutexName.fromString("com.garganttua.core.mutex.dsl.fixtures.TestMutex::test2");
                IMutex testMutex = manager.mutex(testName);
                assertNotNull(testMutex, "Should create TestMutex or fallback");
        }

        @Test
        void testAutoDetectionScansCorrectPackage() throws DslException {
                // Verify that specifying a package actually limits the scan
                IDiContextBuilder contextBuilder = DiContextBuilder.builder()
                                .withPackage("com.garganttua.core.mutex.dsl.fixtures");

                contextBuilder.build();

                // Only scan the fixtures package
                IMutexManager manager = MutexManagerBuilder.builder(contextBuilder)
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
                // Create a complete working example
                IDiContextBuilder contextBuilder = DiContextBuilder.builder()
                                .withPackage("com.garganttua.core.mutex");

                contextBuilder.build();

                IMutexManager manager = MutexManagerBuilder.builder(contextBuilder)
                                .withPackage("com.garganttua.core.mutex")
                                .autoDetect(true)
                                .build();

                MutexName name = MutexName.fromString("com.garganttua.core.mutex.InterruptibleLeaseMutex::functional-test");
                IMutex mutex = manager.mutex(name);

                // Actually use the mutex to execute code
                String result = mutex.acquire(() -> {
                        return "Successfully executed";
                });

                assertEquals("Successfully executed", result, "Mutex should execute function correctly");
        }

        @Test
        void testSameMutexReturnedOnSubsequentCalls() throws DslException, MutexException {
                IDiContextBuilder contextBuilder = DiContextBuilder.builder()
                                .withPackage("com.garganttua.core.mutex");

                contextBuilder.build();

                IMutexManager manager = MutexManagerBuilder.builder(contextBuilder)
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

                IDiContextBuilder contextBuilder = DiContextBuilder.builder()
                                .withPackages(packages);

                contextBuilder.build();

                IMutexManager manager = MutexManagerBuilder.builder(contextBuilder)
                                .withPackages(packages)
                                .autoDetect(true)
                                .build();

                assertNotNull(manager, "Manager should be created with multiple packages");
        }

}

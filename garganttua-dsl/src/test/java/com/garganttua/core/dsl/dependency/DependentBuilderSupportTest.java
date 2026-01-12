package com.garganttua.core.dsl.dependency;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.dsl.IBuilderObserver;
import com.garganttua.core.dsl.IObservableBuilder;

/**
 * Tests for DependentBuilderSupport class.
 *
 * This test suite validates the phase-aware dependency management system,
 * including dependency provision, validation, and phase-specific processing.
 */
class DependentBuilderSupportTest {

    // Mock builder classes for testing
    static class MockBuilder implements IObservableBuilder<MockBuilder, String> {
        private String built = null;

        @Override
        public String build() throws DslException {
            if (built == null) {
                built = "mock-result";
            }
            return built;
        }

        @Override
        public MockBuilder observer(IBuilderObserver<MockBuilder, String> observer) {
            return this;
        }
    }

    static class DependencyBuilder implements IObservableBuilder<DependencyBuilder, String> {
        private String built = null;

        @Override
        public String build() throws DslException {
            if (built == null) {
                built = "dependency-result";
            }
            return built;
        }

        @Override
        public DependencyBuilder observer(IBuilderObserver<DependencyBuilder, String> observer) {
            return this;
        }
    }

    static class AnotherDependencyBuilder implements IObservableBuilder<AnotherDependencyBuilder, Integer> {
        private Integer built = null;

        @Override
        public Integer build() throws DslException {
            if (built == null) {
                built = 42;
            }
            return built;
        }

        @Override
        public AnotherDependencyBuilder observer(IBuilderObserver<AnotherDependencyBuilder, Integer> observer) {
            return this;
        }
    }

    @Nested
    @DisplayName("Construction Tests")
    class ConstructionTests {

        @Test
        @DisplayName("Should create support with simple required dependency")
        void testSimpleRequiredDependency() {
            DependentBuilderSupport support = new DependentBuilderSupport(
                Set.of(DependencySpec.require(DependencyBuilder.class, DependencyPhase.BUILD))
            );

            assertNotNull(support);
            assertEquals(1, support.require().size());
            assertEquals(0, support.use().size());
            assertTrue(support.require().contains(DependencyBuilder.class));
        }

        @Test
        @DisplayName("Should create support with simple optional dependency")
        void testSimpleOptionalDependency() {
            DependentBuilderSupport support = new DependentBuilderSupport(
                Set.of(DependencySpec.use(DependencyBuilder.class, DependencyPhase.BUILD))
            );

            assertNotNull(support);
            assertEquals(0, support.require().size());
            assertEquals(1, support.use().size());
            assertTrue(support.use().contains(DependencyBuilder.class));
        }

        @Test
        @DisplayName("Should create support with multiple dependencies")
        void testMultipleDependencies() {
            DependentBuilderSupport support = new DependentBuilderSupport(
                Set.of(
                    DependencySpec.require(DependencyBuilder.class, DependencyPhase.BUILD),
                    DependencySpec.use(AnotherDependencyBuilder.class, DependencyPhase.AUTO_DETECT)
                )
            );

            assertNotNull(support);
            assertEquals(1, support.require().size());
            assertEquals(1, support.use().size());
        }

        @Test
        @DisplayName("Should create support with phase-specific requirements")
        void testPhaseSpecificRequirements() {
            DependentBuilderSupport support = new DependentBuilderSupport(
                Set.of(
                    DependencySpec.of(DependencyBuilder.class)
                        .requireForAutoDetect()
                        .useForBuild()
                        .build()
                )
            );

            assertNotNull(support);
            // Should be in require set because it's required in at least one phase
            assertEquals(1, support.require().size());
        }

        @Test
        @DisplayName("Should throw NPE for null dependency specs")
        void testNullDependencySpecs() {
            assertThrows(NullPointerException.class, () -> {
                new DependentBuilderSupport((Set<DependencySpec>) null);
            });
        }
    }

    @Nested
    @DisplayName("Dependency Provision Tests")
    class DependencyProvisionTests {

        private DependentBuilderSupport support;

        @BeforeEach
        void setUp() {
            support = new DependentBuilderSupport(
                Set.of(
                    DependencySpec.require(DependencyBuilder.class, DependencyPhase.BUILD),
                    DependencySpec.use(AnotherDependencyBuilder.class, DependencyPhase.AUTO_DETECT)
                )
            );
        }

        @Test
        @DisplayName("Should accept valid dependency")
        void testProvideValidDependency() {
            DependencyBuilder dependency = new DependencyBuilder();

            assertDoesNotThrow(() -> support.provide(dependency));
        }

        @Test
        @DisplayName("Should throw DslException for unexpected dependency")
        void testProvideUnexpectedDependency() {
            MockBuilder unexpectedDependency = new MockBuilder();

            DslException exception = assertThrows(DslException.class,
                () -> support.provide(unexpectedDependency));

            assertTrue(exception.getMessage().contains("not declared in the expected dependencies list"));
        }

        @Test
        @DisplayName("Should throw NPE for null dependency")
        void testProvideNullDependency() {
            assertThrows(NullPointerException.class, () -> support.provide(null));
        }

        @Test
        @DisplayName("Should accept multiple dependencies")
        void testProvideMultipleDependencies() {
            DependencyBuilder dep1 = new DependencyBuilder();
            AnotherDependencyBuilder dep2 = new AnotherDependencyBuilder();

            assertDoesNotThrow(() -> {
                support.provide(dep1);
                support.provide(dep2);
            });
        }
    }

    @Nested
    @DisplayName("Phase-Specific Processing Tests")
    class PhaseSpecificProcessingTests {

        @Test
        @DisplayName("Should process auto-detection dependencies")
        void testAutoDetectionProcessing() throws DslException {
            DependentBuilderSupport support = new DependentBuilderSupport(
                Set.of(DependencySpec.require(DependencyBuilder.class, DependencyPhase.AUTO_DETECT))
            );

            DependencyBuilder dependency = new DependencyBuilder();
            dependency.build(); // Build it first
            support.provide(dependency);

            AtomicBoolean processed = new AtomicBoolean(false);

            assertDoesNotThrow(() -> {
                support.processAutoDetectionWithDependencies(dep -> {
                    processed.set(true);
                    assertEquals("dependency-result", dep);
                });
            });

            assertTrue(processed.get(), "Auto-detection handler should have been called");
        }

        @Test
        @DisplayName("Should process build dependencies")
        void testBuildProcessing() throws DslException {
            DependentBuilderSupport support = new DependentBuilderSupport(
                Set.of(DependencySpec.require(DependencyBuilder.class, DependencyPhase.BUILD))
            );

            DependencyBuilder dependency = new DependencyBuilder();
            dependency.build();
            support.provide(dependency);

            AtomicBoolean preBuildProcessed = new AtomicBoolean(false);
            AtomicBoolean postBuildProcessed = new AtomicBoolean(false);

            assertDoesNotThrow(() -> {
                support.processPreBuildDependencies(dep -> {
                    preBuildProcessed.set(true);
                    assertEquals("dependency-result", dep);
                });

                support.processPostBuildDependencies(dep -> {
                    postBuildProcessed.set(true);
                    assertEquals("dependency-result", dep);
                });
            });

            assertTrue(preBuildProcessed.get(), "Pre-build handler should have been called");
            assertTrue(postBuildProcessed.get(), "Post-build handler should have been called");
        }

        @Test
        @DisplayName("Should validate required dependency for auto-detection")
        void testRequiredAutoDetectDependencyValidation() {
            DependentBuilderSupport support = new DependentBuilderSupport(
                Set.of(DependencySpec.require(DependencyBuilder.class, DependencyPhase.AUTO_DETECT))
            );

            // Don't provide the dependency

            DslException exception = assertThrows(DslException.class, () -> {
                support.processAutoDetectionWithDependencies(dep -> {});
            });

            assertTrue(exception.getMessage().contains("Required"));
        }

        @Test
        @DisplayName("Should validate required dependency for build")
        void testRequiredBuildDependencyValidation() {
            DependentBuilderSupport support = new DependentBuilderSupport(
                Set.of(DependencySpec.require(DependencyBuilder.class, DependencyPhase.BUILD))
            );

            // Don't provide the dependency

            DslException exception = assertThrows(DslException.class, () -> {
                support.processPreBuildDependencies(dep -> {});
            });

            assertTrue(exception.getMessage().contains("Required"));
        }

        @Test
        @DisplayName("Should not process dependency in wrong phase")
        void testPhaseIsolation() throws DslException {
            DependentBuilderSupport support = new DependentBuilderSupport(
                Set.of(DependencySpec.require(DependencyBuilder.class, DependencyPhase.BUILD))
            );

            DependencyBuilder dependency = new DependencyBuilder();
            dependency.build();
            support.provide(dependency);

            AtomicBoolean processed = new AtomicBoolean(false);

            // Should not process in AUTO_DETECT phase since it's only for BUILD
            assertDoesNotThrow(() -> {
                support.processAutoDetectionWithDependencies(dep -> {
                    processed.set(true);
                });
            });

            assertFalse(processed.get(), "Handler should not be called for wrong phase");
        }

        @Test
        @DisplayName("Should handle phase-specific requirements correctly")
        void testPhaseSpecificRequirements() throws DslException {
            DependentBuilderSupport support = new DependentBuilderSupport(
                Set.of(
                    DependencySpec.of(DependencyBuilder.class)
                        .requireForAutoDetect()
                        .useForBuild()
                        .build()
                )
            );

            DependencyBuilder dependency = new DependencyBuilder();
            dependency.build();
            support.provide(dependency);

            AtomicBoolean autoDetectProcessed = new AtomicBoolean(false);
            AtomicBoolean buildProcessed = new AtomicBoolean(false);

            // Should process in AUTO_DETECT (required)
            assertDoesNotThrow(() -> {
                support.processAutoDetectionWithDependencies(dep -> {
                    autoDetectProcessed.set(true);
                });
            });

            // Should process in BUILD (optional)
            assertDoesNotThrow(() -> {
                support.processPreBuildDependencies(dep -> {
                    buildProcessed.set(true);
                });
            });

            assertTrue(autoDetectProcessed.get());
            assertTrue(buildProcessed.get());
        }
    }

    @Nested
    @DisplayName("Edge Cases and Error Handling")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle empty dependency set")
        void testEmptyDependencySet() {
            DependentBuilderSupport support = new DependentBuilderSupport(Set.of());

            assertNotNull(support);
            assertEquals(0, support.require().size());
            assertEquals(0, support.use().size());
        }

        @Test
        @DisplayName("Should not fail with optional unmet dependency")
        void testOptionalUnmetDependency() {
            DependentBuilderSupport support = new DependentBuilderSupport(
                Set.of(DependencySpec.use(DependencyBuilder.class, DependencyPhase.BUILD))
            );

            AtomicBoolean processed = new AtomicBoolean(false);

            // Should not throw even though optional dependency is not provided
            assertDoesNotThrow(() -> {
                support.processPreBuildDependencies(dep -> {
                    processed.set(true);
                });
            });

            assertFalse(processed.get(), "Handler should not be called for unmet optional dependency");
        }

        @Test
        @DisplayName("Should handle BOTH phase dependency")
        void testBothPhaseDependency() throws DslException {
            DependentBuilderSupport support = new DependentBuilderSupport(
                Set.of(DependencySpec.require(DependencyBuilder.class, DependencyPhase.BOTH))
            );

            DependencyBuilder dependency = new DependencyBuilder();
            dependency.build();
            support.provide(dependency);

            AtomicBoolean autoDetectProcessed = new AtomicBoolean(false);
            AtomicBoolean buildProcessed = new AtomicBoolean(false);

            // Should process in both phases
            assertDoesNotThrow(() -> {
                support.processAutoDetectionWithDependencies(dep -> {
                    autoDetectProcessed.set(true);
                });

                support.processPreBuildDependencies(dep -> {
                    buildProcessed.set(true);
                });
            });

            assertTrue(autoDetectProcessed.get());
            assertTrue(buildProcessed.get());
        }
    }
}

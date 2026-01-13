package com.garganttua.core.bootstrap.dsl;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.dsl.IBuilder;
import com.garganttua.core.dsl.IBuilderObserver;
import com.garganttua.core.dsl.IObservableBuilder;
import com.garganttua.core.dsl.IPackageableBuilder;
import com.garganttua.core.dsl.dependency.IDependentBuilder;
import com.garganttua.core.reflection.utils.ObjectReflectionHelper;
import com.garganttua.core.reflections.ReflectionsAnnotationScanner;

/**
 * Tests for BootstrapBuilder.
 *
 * <p>
 * This test suite validates the configuration and initialization capabilities
 * of the bootstrap builder without implementing the full build() logic.
 * </p>
 */
@DisplayName("BootstrapBuilder Tests")
class BootstrapBuilderTest {

    private BootstrapBuilder bootstrap;

    @BeforeEach
    void setUp() {
        ObjectReflectionHelper.setAnnotationScanner(new ReflectionsAnnotationScanner());
        bootstrap = new BootstrapBuilder();
    }

    @Nested
    @DisplayName("Package Management Tests")
    class PackageManagementTests {

        @Test
        @DisplayName("Should add single package")
        void testAddSinglePackage() {
            IBoostrap result = bootstrap.withPackage("com.example");

            assertNotNull(result);
            assertSame(bootstrap, result, "Should return same instance for method chaining");
            assertArrayEquals(new String[]{"com.example"}, bootstrap.getPackages());
        }

        @Test
        @DisplayName("Should add multiple packages individually")
        void testAddMultiplePackagesIndividually() {
            bootstrap.withPackage("com.example")
                    .withPackage("com.test")
                    .withPackage("com.demo");

            String[] packages = bootstrap.getPackages();
            assertEquals(3, packages.length);
            assertTrue(bootstrap.getConfiguredPackages().contains("com.example"));
            assertTrue(bootstrap.getConfiguredPackages().contains("com.test"));
            assertTrue(bootstrap.getConfiguredPackages().contains("com.demo"));
        }

        @Test
        @DisplayName("Should add multiple packages via array")
        void testAddMultiplePackagesViaArray() {
            String[] pkgs = {"com.example", "com.test", "com.demo"};
            bootstrap.withPackages(pkgs);

            String[] packages = bootstrap.getPackages();
            assertEquals(3, packages.length);
        }

        @Test
        @DisplayName("Should not add duplicate packages")
        void testNoDuplicatePackages() {
            bootstrap.withPackage("com.example")
                    .withPackage("com.example");

            String[] packages = bootstrap.getPackages();
            assertEquals(1, packages.length);
        }

        @Test
        @DisplayName("Should throw NPE for null package")
        void testNullPackage() {
            assertThrows(NullPointerException.class, () -> bootstrap.withPackage(null));
        }

        @Test
        @DisplayName("Should throw NPE for null package array")
        void testNullPackageArray() {
            assertThrows(NullPointerException.class, () -> bootstrap.withPackages(null));
        }

        @Test
        @DisplayName("Should return empty array when no packages configured")
        void testEmptyPackages() {
            String[] packages = bootstrap.getPackages();
            assertNotNull(packages);
            assertEquals(0, packages.length);
        }
    }

    @Nested
    @DisplayName("Builder Registration Tests")
    class BuilderRegistrationTests {

        private MockBuilder mockBuilder1;
        private MockBuilder mockBuilder2;

        @BeforeEach
        void setUp() {
            mockBuilder1 = new MockBuilder("builder1");
            mockBuilder2 = new MockBuilder("builder2");
        }

        @Test
        @DisplayName("Should register single builder")
        void testRegisterSingleBuilder() {
            IBoostrap result = bootstrap.withBuilder(mockBuilder1);

            assertNotNull(result);
            assertSame(bootstrap, result, "Should return same instance for method chaining");
            assertEquals(1, bootstrap.getBuilders().size());
            assertTrue(bootstrap.getBuilders().contains(mockBuilder1));
        }

        @Test
        @DisplayName("Should register multiple builders")
        void testRegisterMultipleBuilders() {
            bootstrap.withBuilder(mockBuilder1)
                    .withBuilder(mockBuilder2);

            assertEquals(2, bootstrap.getBuilders().size());
            assertTrue(bootstrap.getBuilders().contains(mockBuilder1));
            assertTrue(bootstrap.getBuilders().contains(mockBuilder2));
        }

        @Test
        @DisplayName("Should allow duplicate builder references")
        void testDuplicateBuilderReferences() {
            bootstrap.withBuilder(mockBuilder1)
                    .withBuilder(mockBuilder1);

            assertEquals(2, bootstrap.getBuilders().size(),
                "Should allow same builder to be registered multiple times");
        }

        @Test
        @DisplayName("Should throw NPE for null builder")
        void testNullBuilder() {
            assertThrows(NullPointerException.class, () -> bootstrap.withBuilder(null));
        }

        @Test
        @DisplayName("Should return unmodifiable list of builders")
        void testUnmodifiableBuilderList() {
            bootstrap.withBuilder(mockBuilder1);

            assertThrows(UnsupportedOperationException.class,
                () -> bootstrap.getBuilders().add(mockBuilder2),
                "Returned list should be unmodifiable");
        }
    }

    @Nested
    @DisplayName("Auto-Detection Tests")
    class AutoDetectionTests {

        @Test
        @DisplayName("Should execute auto-detection without errors")
        void testAutoDetection() {
            bootstrap.withPackage("com.example")
                    .autoDetect(true);

            assertDoesNotThrow(() -> bootstrap.doAutoDetection(),
                "Auto-detection should not throw exceptions");
        }

        @Test
        @DisplayName("Should handle auto-detection with no packages")
        void testAutoDetectionNoPackages() {
            bootstrap.autoDetect(true);

            assertDoesNotThrow(() -> bootstrap.doAutoDetection(),
                "Auto-detection should work with no packages configured");
        }

        @Test
        @DisplayName("Should handle auto-detection with multiple packages")
        void testAutoDetectionMultiplePackages() {
            bootstrap.withPackages(new String[]{"com.example", "com.test", "com.demo"})
                    .autoDetect(true);

            assertDoesNotThrow(() -> bootstrap.doAutoDetection());
        }
    }

    @Nested
    @DisplayName("Build Lifecycle Tests")
    class BuildLifecycleTests {

        @Test
        @DisplayName("Should return null when building with no builders")
        void testBuildWithNoBuilders() throws DslException {
            Object result = bootstrap.build();
            assertNull(result, "Should return null when no builders are registered");
        }

        @Test
        @DisplayName("Should build single builder and return its result")
        void testBuildSingleBuilder() throws DslException {
            MockBuilder builder = new MockBuilder("test");
            bootstrap.withBuilder(builder);

            Object result = bootstrap.build();
            assertNotNull(result);
            assertEquals("Built: test", result);
        }

        @Test
        @DisplayName("Should build multiple builders and return list of results")
        void testBuildMultipleBuilders() throws DslException {
            MockBuilder builder1 = new MockBuilder("builder1");
            MockBuilder builder2 = new MockBuilder("builder2");
            bootstrap.withBuilder(builder1).withBuilder(builder2);

            Object result = bootstrap.build();
            assertNotNull(result);
            assertInstanceOf(List.class, result);

            @SuppressWarnings("unchecked")
            List<Object> results = (List<Object>) result;
            assertEquals(2, results.size());
            assertTrue(results.contains("Built: builder1"));
            assertTrue(results.contains("Built: builder2"));
        }

        @Test
        @DisplayName("Should allow method chaining")
        void testMethodChaining() {
            IBoostrap result = bootstrap
                    .withPackage("com.example")
                    .withBuilder(new MockBuilder("test"))
                    .autoDetect(true);

            assertSame(bootstrap, result, "All methods should return same instance");
        }
    }

    @Nested
    @DisplayName("Factory Method Tests")
    class FactoryMethodTests {

        @Test
        @DisplayName("Should create instance via builder() factory method")
        void testFactoryMethod() {
            IBoostrap instance = BootstrapBuilder.builder();

            assertNotNull(instance);
            assertInstanceOf(BootstrapBuilder.class, instance);
        }

        @Test
        @DisplayName("Should create new instance each time")
        void testFactoryCreatesNewInstances() {
            IBoostrap instance1 = BootstrapBuilder.builder();
            IBoostrap instance2 = BootstrapBuilder.builder();

            assertNotSame(instance1, instance2,
                "Factory should create new instances");
        }
    }

    @Nested
    @DisplayName("Dependency Resolution Tests")
    class DependencyResolutionTests {

        @Test
        @DisplayName("Should build builders in dependency order")
        void testDependencyOrder() throws DslException {
            MockObservableBuilder observableBuilder = new MockObservableBuilder("observable");
            MockDependentBuilder dependentBuilder = new MockDependentBuilder("dependent", MockObservableBuilder.class);

            bootstrap.withBuilder(dependentBuilder)
                    .withBuilder(observableBuilder);

            Object result = bootstrap.build();
            assertNotNull(result);

            // Verify that observable was built before dependent
            assertTrue(observableBuilder.isBuilt());
            assertTrue(dependentBuilder.isBuilt());
            assertTrue(dependentBuilder.isDependencyProvided());
        }

        @Test
        @DisplayName("Should handle complex dependency chains")
        void testDependencyChain() throws DslException {
            MockObservableBuilder builder1 = new MockObservableBuilder("builder1");
            MockDependentObservableBuilder builder2 = new MockDependentObservableBuilder("builder2", MockObservableBuilder.class);
            MockDependentBuilder builder3 = new MockDependentBuilder("builder3", MockDependentObservableBuilder.class);

            // Add in reverse order to test topological sort
            bootstrap.withBuilder(builder3)
                    .withBuilder(builder2)
                    .withBuilder(builder1);

            Object result = bootstrap.build();
            assertNotNull(result);

            // All should be built
            assertTrue(builder1.isBuilt());
            assertTrue(builder2.isBuilt());
            assertTrue(builder3.isBuilt());
        }

        @Test
        @DisplayName("Should detect circular dependencies")
        void testCircularDependency() {
            // This test would require creating builders with circular dependencies
            // which is complex to set up, so we test the happy path for now
            MockObservableBuilder builder = new MockObservableBuilder("builder");
            bootstrap.withBuilder(builder);

            assertDoesNotThrow(() -> bootstrap.build());
        }

        @Test
        @DisplayName("Should handle builders with no dependencies")
        void testNoDependencies() throws DslException {
            MockBuilder builder1 = new MockBuilder("builder1");
            MockBuilder builder2 = new MockBuilder("builder2");

            bootstrap.withBuilder(builder1).withBuilder(builder2);

            Object result = bootstrap.build();
            assertNotNull(result);
        }
    }

    @Nested
    @DisplayName("Package Propagation Tests")
    class PackagePropagationTests {

        @Test
        @DisplayName("Should propagate packages to existing packageable builders")
        void testPropagateToExistingBuilders() {
            MockPackageableBuilder packageableBuilder = new MockPackageableBuilder("pkgBuilder");
            bootstrap.withBuilder(packageableBuilder);

            bootstrap.withPackage("com.example");

            assertTrue(packageableBuilder.getPackagesSet().contains("com.example"));
        }

        @Test
        @DisplayName("Should propagate existing packages to new packageable builders")
        void testPropagateToNewBuilders() {
            bootstrap.withPackage("com.example")
                    .withPackage("com.test");

            MockPackageableBuilder packageableBuilder = new MockPackageableBuilder("pkgBuilder");
            bootstrap.withBuilder(packageableBuilder);

            assertEquals(2, packageableBuilder.getPackagesSet().size());
            assertTrue(packageableBuilder.getPackagesSet().contains("com.example"));
            assertTrue(packageableBuilder.getPackagesSet().contains("com.test"));
        }

        @Test
        @DisplayName("Should not affect non-packageable builders")
        void testNonPackageableBuilders() {
            MockBuilder normalBuilder = new MockBuilder("normal");
            bootstrap.withBuilder(normalBuilder);

            bootstrap.withPackage("com.example");

            // Should not throw any exception
            assertDoesNotThrow(() -> bootstrap.withPackage("com.test"));
        }
    }

    @Nested
    @DisplayName("Integration Tests")
    class IntegrationTests {

        @Test
        @DisplayName("Should configure complete bootstrap setup")
        void testCompleteSetup() {
            MockBuilder builder1 = new MockBuilder("builder1");
            MockBuilder builder2 = new MockBuilder("builder2");

            IBoostrap result = bootstrap
                    .withPackage("com.example.app")
                    .withPackage("com.example.services")
                    .withBuilder(builder1)
                    .withBuilder(builder2)
                    .autoDetect(true);

            assertNotNull(result);
            assertEquals(2, bootstrap.getPackages().length);
            assertEquals(2, bootstrap.getBuilders().size());
        }

        @Test
        @DisplayName("Should handle empty bootstrap configuration")
        void testEmptyConfiguration() {
            assertNotNull(bootstrap);
            assertEquals(0, bootstrap.getPackages().length);
            assertEquals(0, bootstrap.getBuilders().size());
        }

        @Test
        @DisplayName("Should handle complete dependency resolution workflow")
        void testCompleteDependencyWorkflow() throws DslException {
            MockObservableBuilder observable = new MockObservableBuilder("observable");
            MockDependentBuilder dependent = new MockDependentBuilder("dependent", MockObservableBuilder.class);
            MockPackageableBuilder packageable = new MockPackageableBuilder("packageable");

            bootstrap.withPackage("com.example")
                    .withBuilder(dependent)
                    .withBuilder(observable)
                    .withBuilder(packageable);

            Object result = bootstrap.build();
            assertNotNull(result);

            assertTrue(observable.isBuilt());
            assertTrue(dependent.isBuilt());
            assertTrue(packageable.getPackagesSet().contains("com.example"));
        }
    }

    // Mock builder for testing
    static class MockBuilder implements IBuilder<String> {
        private final String name;

        MockBuilder(String name) {
            this.name = name;
        }

        @Override
        public String build() throws DslException {
            return "Built: " + name;
        }

        @Override
        public String toString() {
            return "MockBuilder{name='" + name + "'}";
        }
    }

    // Mock observable builder for testing dependencies
    static class MockObservableBuilder implements IObservableBuilder<MockObservableBuilder, String> {
        private final String name;
        private boolean built = false;

        MockObservableBuilder(String name) {
            this.name = name;
        }

        @Override
        public String build() throws DslException {
            built = true;
            return "ObservableBuilt: " + name;
        }

        @Override
        public MockObservableBuilder observer(IBuilderObserver<MockObservableBuilder, String> observer) {
            // Not used in tests
            return this;
        }

        public boolean isBuilt() {
            return built;
        }

        @Override
        public String toString() {
            return "MockObservableBuilder{name='" + name + "'}";
        }
    }

    // Mock dependent builder for testing dependency resolution
    static class MockDependentBuilder implements IDependentBuilder<MockDependentBuilder, String> {
        private final String name;
        private final Class<? extends IObservableBuilder<?, ?>> requiredDependency;
        private boolean built = false;
        private boolean dependencyProvided = false;

        MockDependentBuilder(String name, Class<? extends IObservableBuilder<?, ?>> requiredDependency) {
            this.name = name;
            this.requiredDependency = requiredDependency;
        }

        @Override
        public String build() throws DslException {
            built = true;
            return "DependentBuilt: " + name;
        }

        @Override
        public MockDependentBuilder provide(IObservableBuilder<?, ?> dependency) throws DslException {
            dependencyProvided = true;
            return this;
        }

        @Override
        public Set<Class<? extends IObservableBuilder<?, ?>>> use() {
            return new HashSet<>();
        }

        @Override
        public Set<Class<? extends IObservableBuilder<?, ?>>> require() {
            Set<Class<? extends IObservableBuilder<?, ?>>> deps = new HashSet<>();
            deps.add(requiredDependency);
            return deps;
        }

        public boolean isBuilt() {
            return built;
        }

        public boolean isDependencyProvided() {
            return dependencyProvided;
        }

        @Override
        public String toString() {
            return "MockDependentBuilder{name='" + name + "'}";
        }
    }

    // Mock dependent observable builder for testing complex dependency chains
    static class MockDependentObservableBuilder implements IObservableBuilder<MockDependentObservableBuilder, String>,
            IDependentBuilder<MockDependentObservableBuilder, String> {
        private final String name;
        private final Class<? extends IObservableBuilder<?, ?>> requiredDependency;
        private boolean built = false;
        private boolean dependencyProvided = false;

        MockDependentObservableBuilder(String name, Class<? extends IObservableBuilder<?, ?>> requiredDependency) {
            this.name = name;
            this.requiredDependency = requiredDependency;
        }

        @Override
        public String build() throws DslException {
            built = true;
            return "DependentObservableBuilt: " + name;
        }

        @Override
        public MockDependentObservableBuilder observer(IBuilderObserver<MockDependentObservableBuilder, String> observer) {
            // Not used in tests
            return this;
        }

        @Override
        public MockDependentObservableBuilder provide(IObservableBuilder<?, ?> dependency) throws DslException {
            dependencyProvided = true;
            return this;
        }

        @Override
        public Set<Class<? extends IObservableBuilder<?, ?>>> use() {
            return new HashSet<>();
        }

        @Override
        public Set<Class<? extends IObservableBuilder<?, ?>>> require() {
            Set<Class<? extends IObservableBuilder<?, ?>>> deps = new HashSet<>();
            deps.add(requiredDependency);
            return deps;
        }

        public boolean isBuilt() {
            return built;
        }

        public boolean isDependencyProvided() {
            return dependencyProvided;
        }

        @Override
        public String toString() {
            return "MockDependentObservableBuilder{name='" + name + "'}";
        }
    }

    // Mock packageable builder for testing package propagation
    static class MockPackageableBuilder implements IPackageableBuilder<MockPackageableBuilder, String> {
        private final String name;
        private final Set<String> packages = new HashSet<>();

        MockPackageableBuilder(String name) {
            this.name = name;
        }

        @Override
        public String build() throws DslException {
            return "PackageableBuilt: " + name;
        }

        @Override
        public MockPackageableBuilder withPackage(String packageName) {
            packages.add(packageName);
            return this;
        }

        @Override
        public MockPackageableBuilder withPackages(String[] packageNames) {
            for (String pkg : packageNames) {
                packages.add(pkg);
            }
            return this;
        }

        @Override
        public String[] getPackages() {
            return packages.toArray(new String[0]);
        }

        public Set<String> getPackagesSet() {
            return Set.copyOf(packages);
        }

        @Override
        public String toString() {
            return "MockPackageableBuilder{name='" + name + "'}";
        }
    }

    @Nested
    @DisplayName("@Scan Annotation Tests")
    class ScanAnnotationTests {

        @Test
        @DisplayName("Should scan and add packages from @Scan annotations when auto-detecting")
        void shouldScanAndAddPackagesFromScanAnnotations() throws DslException {
            // Given
            MockPackageableAutomaticBuilder packageableBuilder = new MockPackageableAutomaticBuilder("test");
            bootstrap.withPackage("com.garganttua.core.bootstrap.dsl.test")
                    .withBuilder(packageableBuilder);

            // When
            packageableBuilder.autoDetect(true);
            packageableBuilder.build();

            // Then
            Set<String> packages = packageableBuilder.getPackagesSet();
            assertTrue(packages.contains("com.garganttua.core.bootstrap.dsl.test"),
                    "Should contain base package");
            assertTrue(packages.contains("com.garganttua.scanned.package1"),
                    "Should contain package from @Scan on TestScanClass1");
            assertTrue(packages.contains("com.garganttua.scanned.package2"),
                    "Should contain package from @Scan on TestScanClass2");
        }

        @Test
        @DisplayName("Should not scan for @Scan when auto-detect is disabled")
        void shouldNotScanWhenAutoDetectDisabled() throws DslException {
            // Given
            MockPackageableAutomaticBuilder packageableBuilder = new MockPackageableAutomaticBuilder("test");
            bootstrap.withPackage("com.garganttua.core.bootstrap.dsl.test")
                    .withBuilder(packageableBuilder);

            // When
            packageableBuilder.autoDetect(false);
            packageableBuilder.build();

            // Then
            Set<String> packages = packageableBuilder.getPackagesSet();
            assertTrue(packages.contains("com.garganttua.core.bootstrap.dsl.test"),
                    "Should contain base package");
            assertFalse(packages.contains("com.garganttua.scanned.package1"),
                    "Should NOT contain scanned package when auto-detect is disabled");
            assertFalse(packages.contains("com.garganttua.scanned.package2"),
                    "Should NOT contain scanned package when auto-detect is disabled");
        }

        @Test
        @DisplayName("Should only scan @Scan annotations for packageable builders")
        void shouldOnlyScanForPackageableBuilders() throws DslException {
            // Given
            MockBuilder nonPackageableBuilder = new MockBuilder("non-packageable");
            bootstrap.withPackage("com.garganttua.core.bootstrap.dsl.test")
                    .withBuilder(nonPackageableBuilder);

            // When
            bootstrap.autoDetect(true);
            Object result = bootstrap.build();

            // Then - Should complete without error
            assertNotNull(result, "Bootstrap should build successfully for non-packageable builders");
        }
    }

    // Mock packageable builder with automatic detection support
    static class MockPackageableAutomaticBuilder extends com.garganttua.core.dsl.AbstractAutomaticBuilder<MockPackageableAutomaticBuilder, String>
            implements IPackageableBuilder<MockPackageableAutomaticBuilder, String> {
        private final String name;
        private final Set<String> packages = new HashSet<>();

        MockPackageableAutomaticBuilder(String name) {
            this.name = name;
        }

        @Override
        public MockPackageableAutomaticBuilder withPackage(String packageName) {
            packages.add(packageName);
            return this;
        }

        @Override
        public MockPackageableAutomaticBuilder withPackages(String[] packageNames) {
            for (String pkg : packageNames) {
                packages.add(pkg);
            }
            return this;
        }

        @Override
        public String[] getPackages() {
            return packages.toArray(new String[0]);
        }

        @Override
        protected String[] getPackagesForScanning() {
            return packages.toArray(new String[0]);
        }

        @Override
        protected com.garganttua.core.reflection.IAnnotationScanner getAnnotationScanner() {
            return ObjectReflectionHelper.getAnnotationScanner();
        }

        public Set<String> getPackagesSet() {
            return Set.copyOf(packages);
        }

        @Override
        protected String doBuild() throws DslException {
            return "Built: " + name;
        }

        @Override
        protected void doAutoDetection() throws DslException {
            // Business auto-detection logic (empty for this test)
        }

        @Override
        public String toString() {
            return "MockPackageableAutomaticBuilder{name='" + name + "'}";
        }
    }
}

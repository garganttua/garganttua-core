package com.garganttua.core.dsl;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.garganttua.core.dsl.annotations.Scan;
import com.garganttua.core.reflections.ReflectionsAnnotationScanner;

@DisplayName("PackageScanHelper Tests")
class PackageScanHelperTest {

    @Test
    @DisplayName("Should scan and add packages from @Scan annotations")
    void shouldScanAndAddPackages() throws DslException {
        // Given
        MockPackageableBuilder builder = new MockPackageableBuilder();
        String[] basePackages = {"com.garganttua.core.dsl"};
        ReflectionsAnnotationScanner scanner = new ReflectionsAnnotationScanner();

        // When
        PackageScanHelper.scanAndAddPackages(scanner, builder, basePackages);

        // Then
        assertTrue(builder.packages.contains("com.garganttua.core.test.package1"),
                "Should contain package from @Scan annotation on TestClass1");
        assertTrue(builder.packages.contains("com.garganttua.core.test.package2"),
                "Should contain package from @Scan annotation on TestClass2");
    }

    @Test
    @DisplayName("Should do nothing when builder is null")
    void shouldDoNothingWhenBuilderIsNull() {
        // When/Then - Should not throw exception
        try {
            ReflectionsAnnotationScanner scanner = new ReflectionsAnnotationScanner();
            PackageScanHelper.scanAndAddPackages(scanner, null, new String[]{"com.test"});
            assertTrue(true, "No exception should be thrown");
        } catch (Exception e) {
            fail("Should not throw exception when builder is null");
        }
    }

    @Test
    @DisplayName("Should do nothing when basePackages is null")
    void shouldDoNothingWhenBasePackagesIsNull() throws DslException {
        // Given
        MockPackageableBuilder builder = new MockPackageableBuilder();
        ReflectionsAnnotationScanner scanner = new ReflectionsAnnotationScanner();

        // When
        PackageScanHelper.scanAndAddPackages(scanner, builder, null);

        // Then
        assertTrue(builder.packages.isEmpty());
    }

    @Test
    @DisplayName("Should do nothing when basePackages is empty")
    void shouldDoNothingWhenBasePackagesIsEmpty() throws DslException {
        // Given
        MockPackageableBuilder builder = new MockPackageableBuilder();
        ReflectionsAnnotationScanner scanner = new ReflectionsAnnotationScanner();

        // When
        PackageScanHelper.scanAndAddPackages(scanner, builder, new String[0]);

        // Then
        assertTrue(builder.packages.isEmpty());
    }

    @Test
    @DisplayName("Should skip builder that does not implement IPackageableBuilder")
    void shouldSkipNonPackageableBuilder() throws DslException {
        // Given
        MockNonPackageableBuilder builder = new MockNonPackageableBuilder();
        String[] basePackages = {"com.garganttua.core.dsl"};
        ReflectionsAnnotationScanner scanner = new ReflectionsAnnotationScanner();

        // When
        PackageScanHelper.scanAndAddPackages(scanner, builder, basePackages);

        // Then - Should complete without error, no packages added
        assertEquals(0, builder.buildCallCount);
    }

    @Test
    @DisplayName("Should handle package scanning failures gracefully")
    void shouldHandleScanningFailuresGracefully() {
        // Given
        MockPackageableBuilder builder = new MockPackageableBuilder();
        String[] basePackages = {"com.nonexistent.package"};
        ReflectionsAnnotationScanner scanner = new ReflectionsAnnotationScanner();

        // When/Then - Should not throw exception
        try {
            PackageScanHelper.scanAndAddPackages(scanner, builder, basePackages);
            assertTrue(true, "Should handle scanning failures without throwing exception");
        } catch (Exception e) {
            fail("Should not throw exception for non-existent packages");
        }
    }

    // Mock classes for testing

    static class MockPackageableBuilder implements IPackageableBuilder<MockPackageableBuilder, String> {
        Set<String> packages = new HashSet<>();

        @Override
        public MockPackageableBuilder withPackage(String packageName) {
            packages.add(packageName);
            return this;
        }

        @Override
        public MockPackageableBuilder withPackages(String[] packageNames) {
            for (String packageName : packageNames) {
                packages.add(packageName);
            }
            return this;
        }

        @Override
        public String[] getPackages() {
            return packages.toArray(new String[0]);
        }

        @Override
        public String build() throws DslException {
            return "built";
        }
    }

    static class MockNonPackageableBuilder implements IBuilder<String> {
        int buildCallCount = 0;

        @Override
        public String build() throws DslException {
            buildCallCount++;
            return "built";
        }
    }

    // Test classes with @Scan annotations
    @Scan(scan = "com.garganttua.core.test.package1")
    public static class TestClass1 {
    }

    @Scan(scan = "com.garganttua.core.test.package2")
    public static class TestClass2 {
    }
}

package com.garganttua.core.dsl;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.garganttua.core.dsl.annotations.Scan;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IReflection;

@DisplayName("PackageScanHelper Tests")
class PackageScanHelperTest {

    /**
     * Creates a minimal IReflection proxy that supports getClass() and
     * getClassesWithAnnotation(String, IClass) — the two methods used by PackageScanHelper.
     */
    private static IReflection mockReflection(List<IClass<?>> classesToReturn) {
        InvocationHandler handler = (proxy, method, args) -> switch (method.getName()) {
            case "getClass" -> {
                if (args != null && args.length == 1 && args[0] instanceof Class<?> clazz) {
                    yield mockIClass(clazz);
                }
                yield null;
            }
            case "getClassesWithAnnotation" -> {
                if (classesToReturn == null) {
                    throw new RuntimeException("Simulated scanning failure");
                }
                yield classesToReturn;
            }
            default -> throw new UnsupportedOperationException(method.getName());
        };
        return (IReflection) Proxy.newProxyInstance(
                IReflection.class.getClassLoader(),
                new Class[]{IReflection.class},
                handler);
    }
    
    private static <T> IClass<T> mockIClass(Class<T> clazz) {
        InvocationHandler handler = (proxy, method, args) -> switch (method.getName()) {
            case "getName" -> clazz.getName();
            case "getSimpleName" -> clazz.getSimpleName();
            case "getAnnotation" -> {
                if (args != null && args.length == 1) {
                    if (args[0] instanceof Class<?> annClass) {
                        yield clazz.getAnnotation((Class<? extends Annotation>) annClass);
                    }
                    if (args[0] instanceof IClass<?> iAnnClass) {
                        try {
                            Class<?> realClass = Class.forName(iAnnClass.getName());
                            yield clazz.getAnnotation((Class<? extends Annotation>) realClass);
                        } catch (ClassNotFoundException e) {
                            yield null;
                        }
                    }
                }
                yield null;
            }
            default -> throw new UnsupportedOperationException(method.getName());
        };
        return (IClass<T>) Proxy.newProxyInstance(
                IClass.class.getClassLoader(),
                new Class[]{IClass.class},
                handler);
    }

    @Test
    @DisplayName("Should scan and add packages from @Scan annotations")
    void shouldScanAndAddPackages() throws DslException {
        // Given
        MockPackageableBuilder builder = new MockPackageableBuilder();
        String[] basePackages = {"com.garganttua.core.dsl"};
        List<IClass<?>> found = List.of(mockIClass(TestClass1.class), mockIClass(TestClass2.class));
        PackageScanHelper helper = new PackageScanHelper(mockReflection(found));

        // When
        helper.scanAndAddPackages(builder, basePackages);

        // Then
        assertTrue(builder.packages.contains("com.garganttua.core.test.package1"));
        assertTrue(builder.packages.contains("com.garganttua.core.test.package2"));
    }

    @Test
    @DisplayName("Should do nothing when builder is null")
    void shouldDoNothingWhenBuilderIsNull() {
        PackageScanHelper helper = new PackageScanHelper(mockReflection(List.of()));
        assertDoesNotThrow(() -> helper.scanAndAddPackages(null, new String[]{"com.test"}));
    }

    @Test
    @DisplayName("Should do nothing when basePackages is null")
    void shouldDoNothingWhenBasePackagesIsNull() throws DslException {
        MockPackageableBuilder builder = new MockPackageableBuilder();
        PackageScanHelper helper = new PackageScanHelper(mockReflection(List.of()));

        helper.scanAndAddPackages(builder, null);

        assertTrue(builder.packages.isEmpty());
    }

    @Test
    @DisplayName("Should do nothing when basePackages is empty")
    void shouldDoNothingWhenBasePackagesIsEmpty() throws DslException {
        MockPackageableBuilder builder = new MockPackageableBuilder();
        PackageScanHelper helper = new PackageScanHelper(mockReflection(List.of()));

        helper.scanAndAddPackages(builder, new String[0]);

        assertTrue(builder.packages.isEmpty());
    }

    @Test
    @DisplayName("Should skip builder that does not implement IPackageableBuilder")
    void shouldSkipNonPackageableBuilder() throws DslException {
        MockNonPackageableBuilder builder = new MockNonPackageableBuilder();
        PackageScanHelper helper = new PackageScanHelper(mockReflection(List.of()));

        helper.scanAndAddPackages(builder, new String[]{"com.garganttua.core.dsl"});

        assertEquals(0, builder.buildCallCount);
    }

    @Test
    @DisplayName("Should handle package scanning failures gracefully")
    void shouldHandleScanningFailuresGracefully() {
        MockPackageableBuilder builder = new MockPackageableBuilder();
        PackageScanHelper helper = new PackageScanHelper(mockReflection(null)); // will throw

        assertDoesNotThrow(() -> helper.scanAndAddPackages(builder, new String[]{"com.nonexistent"}));
    }

    // --- Mock builders ---

    static class MockPackageableBuilder implements IPackageableBuilder<MockPackageableBuilder, String> {
        Set<String> packages = new HashSet<>();

        @Override
        public MockPackageableBuilder withPackage(String packageName) {
            packages.add(packageName);
            return this;
        }

        @Override
        public MockPackageableBuilder withPackages(String[] packageNames) {
            for (String p : packageNames) packages.add(p);
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

    // --- @Scan annotated test classes ---

    @Scan(scan = "com.garganttua.core.test.package1")
    public static class TestClass1 {}

    @Scan(scan = "com.garganttua.core.test.package2")
    public static class TestClass2 {}
}

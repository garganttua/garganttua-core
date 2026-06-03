package com.garganttua.core.dsl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.garganttua.core.dsl.annotations.Scan;
import com.garganttua.core.reflection.IAnnotationScanner;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IReflection;

/**
 * Behaviour tests for {@link PackageScanHelper} focusing on the
 * {@link IAnnotationScanner}-based overload, null guards, multi-package
 * iteration, and partial-failure resilience — complementing the existing
 * {@code PackageScanHelperTest}.
 */
@DisplayName("PackageScanHelper Behaviour Tests")
class PackageScanHelperBehaviourTest {

    @SuppressWarnings("unchecked")
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

    /**
     * IReflection whose getClass(Scan.class) returns a usable IClass and whose
     * getClassesWithAnnotation is never called (the scanner overload is used).
     */
    private static IReflection reflectionForConstruction() {
        InvocationHandler handler = (proxy, method, args) -> switch (method.getName()) {
            case "getClass" -> {
                if (args != null && args.length == 1 && args[0] instanceof Class<?> clazz) {
                    yield mockIClass(clazz);
                }
                yield null;
            }
            default -> throw new UnsupportedOperationException(method.getName());
        };
        return (IReflection) Proxy.newProxyInstance(
                IReflection.class.getClassLoader(),
                new Class[]{IReflection.class},
                handler);
    }

    /**
     * Scanner returning a fixed list per package, recording every package queried.
     * A {@code null} response list triggers a thrown exception for that package.
     */
    private static IAnnotationScanner recordingScanner(
            java.util.Map<String, List<IClass<?>>> perPackage, List<String> queried) {
        InvocationHandler handler = (proxy, method, args) -> {
            if ("getClassesWithAnnotation".equals(method.getName())
                    && args != null && args.length == 2 && args[0] instanceof String pkg) {
                queried.add(pkg);
                List<IClass<?>> result = perPackage.get(pkg);
                if (result == null) {
                    throw new RuntimeException("scan failure for " + pkg);
                }
                return result;
            }
            throw new UnsupportedOperationException(method.getName());
        };
        return (IAnnotationScanner) Proxy.newProxyInstance(
                IAnnotationScanner.class.getClassLoader(),
                new Class[]{IAnnotationScanner.class},
                handler);
    }

    // ---- construction guard ----------------------------------------------

    @Test
    @DisplayName("Constructor rejects null IReflection")
    void constructorRejectsNull() {
        assertThrows(NullPointerException.class, () -> new PackageScanHelper(null));
    }

    // ---- scanner overload null guards ------------------------------------

    @Test
    @DisplayName("Scanner overload rejects a null scanner")
    void scannerOverloadRejectsNullScanner() {
        PackageScanHelper helper = new PackageScanHelper(reflectionForConstruction());
        assertThrows(NullPointerException.class,
                () -> helper.scanAndAddPackages(null, new MockPackageableBuilder(), new String[]{"com.x"}));
    }

    @Test
    @DisplayName("Scanner overload does nothing for null builder / null / empty packages")
    void scannerOverloadNoopGuards() throws DslException {
        PackageScanHelper helper = new PackageScanHelper(reflectionForConstruction());
        IAnnotationScanner scanner = recordingScanner(java.util.Map.of(), new ArrayList<>());

        // null builder
        helper.scanAndAddPackages(scanner, null, new String[]{"com.x"});

        MockPackageableBuilder b1 = new MockPackageableBuilder();
        helper.scanAndAddPackages(scanner, b1, null);
        assertTrue(b1.packages.isEmpty());

        MockPackageableBuilder b2 = new MockPackageableBuilder();
        helper.scanAndAddPackages(scanner, b2, new String[0]);
        assertTrue(b2.packages.isEmpty());
    }

    @Test
    @DisplayName("Scanner overload skips a non-packageable builder without invoking the scanner")
    void scannerOverloadSkipsNonPackageable() throws DslException {
        PackageScanHelper helper = new PackageScanHelper(reflectionForConstruction());
        List<String> queried = new ArrayList<>();
        IAnnotationScanner scanner = recordingScanner(java.util.Map.of("com.x", List.of()), queried);
        MockNonPackageableBuilder builder = new MockNonPackageableBuilder();

        helper.scanAndAddPackages(scanner, builder, new String[]{"com.x"});

        assertTrue(queried.isEmpty(), "scanner must not be queried for a non-packageable builder");
    }

    // ---- scanner overload happy path -------------------------------------

    @Test
    @DisplayName("Scanner overload collects @Scan packages across multiple base packages")
    void scannerOverloadCollectsAcrossPackages() throws DslException {
        PackageScanHelper helper = new PackageScanHelper(reflectionForConstruction());
        List<String> queried = new ArrayList<>();
        IAnnotationScanner scanner = recordingScanner(java.util.Map.of(
                "pkg.one", List.of(mockIClass(ScannedOne.class)),
                "pkg.two", List.of(mockIClass(ScannedTwo.class))), queried);
        MockPackageableBuilder builder = new MockPackageableBuilder();

        helper.scanAndAddPackages(scanner, builder, new String[]{"pkg.one", "pkg.two"});

        assertEquals(Set.of("pkg.one", "pkg.two"), new HashSet<>(queried));
        assertTrue(builder.packages.contains("scanned.from.one"));
        assertTrue(builder.packages.contains("scanned.from.two"));
        assertEquals(2, builder.packages.size());
    }

    @Test
    @DisplayName("Scanner overload continues past a failing package and still applies the good ones")
    void scannerOverloadResilientToPartialFailure() throws DslException {
        PackageScanHelper helper = new PackageScanHelper(reflectionForConstruction());
        List<String> queried = new ArrayList<>();
        java.util.Map<String, List<IClass<?>>> perPackage = new java.util.HashMap<>();
        perPackage.put("bad.pkg", null); // triggers exception
        perPackage.put("good.pkg", List.of(mockIClass(ScannedOne.class)));
        IAnnotationScanner scanner = recordingScanner(perPackage, queried);
        MockPackageableBuilder builder = new MockPackageableBuilder();

        helper.scanAndAddPackages(scanner, builder, new String[]{"bad.pkg", "good.pkg"});

        assertEquals(2, queried.size(), "both packages must be attempted");
        assertEquals(Set.of("scanned.from.one"), builder.packages,
                "the good package's @Scan must still be applied");
    }

    @Test
    @DisplayName("Scanner overload adds nothing when no classes carry @Scan")
    void scannerOverloadEmptyResult() throws DslException {
        PackageScanHelper helper = new PackageScanHelper(reflectionForConstruction());
        IAnnotationScanner scanner = recordingScanner(
                java.util.Map.of("pkg.empty", List.of()), new ArrayList<>());
        MockPackageableBuilder builder = new MockPackageableBuilder();

        helper.scanAndAddPackages(scanner, builder, new String[]{"pkg.empty"});

        assertTrue(builder.packages.isEmpty());
    }

    // ---- mocks / fixtures -------------------------------------------------

    static class MockPackageableBuilder implements IPackageableBuilder<MockPackageableBuilder, String> {
        Set<String> packages = new HashSet<>();

        @Override
        public MockPackageableBuilder withPackage(String packageName) {
            packages.add(packageName);
            return this;
        }

        @Override
        public MockPackageableBuilder withPackages(String[] packageNames) {
            for (String p : packageNames) {
                packages.add(p);
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
        @Override
        public String build() throws DslException {
            return "built";
        }
    }

    @Scan(scan = "scanned.from.one")
    public static class ScannedOne {}

    @Scan(scan = "scanned.from.two")
    public static class ScannedTwo {}
}

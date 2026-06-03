package com.garganttua.core.dsl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.garganttua.core.reflection.IReflection;
import com.garganttua.core.utils.OrderedMap;

/**
 * Behaviour tests for the non-dependency builder base classes:
 * {@link AbstractLinkedBuilder}, {@link AbstractAutomaticLinkedBuilder},
 * {@link AbstractAutomaticBuilder} (caching, auto-detect ordering, rebuild +
 * merge), and {@link OrderedMapBuilder}.
 *
 * <p>All tests use concrete in-test subclasses so the abstract template
 * behaviour can be exercised without depending on higher-layer modules.</p>
 */
@DisplayName("DSL abstract builder bases behaviour")
class AbstractBuildersBehaviourTest {

    // ====================================================================
    // AbstractLinkedBuilder
    // ====================================================================

    static final class LinkedBuilder extends AbstractLinkedBuilder<String, Integer> {
        LinkedBuilder(String link) {
            super(link);
        }

        @Override
        public Integer build() {
            return 42;
        }
    }

    @Test
    @DisplayName("AbstractLinkedBuilder.up() returns the constructor link")
    void linkedUpReturnsLink() {
        LinkedBuilder b = new LinkedBuilder("parent");
        assertEquals("parent", b.up());
    }

    @Test
    @DisplayName("AbstractLinkedBuilder constructor rejects null link with NPE and message")
    void linkedNullLinkRejected() {
        NullPointerException ex = assertThrows(NullPointerException.class, () -> new LinkedBuilder(null));
        assertEquals("Up cannot be null", ex.getMessage());
    }

    @Test
    @DisplayName("AbstractLinkedBuilder.setUp() re-parents the builder")
    void linkedSetUpReParents() {
        LinkedBuilder b = new LinkedBuilder("first");
        b.setUp("second");
        assertEquals("second", b.up());
    }

    @Test
    @DisplayName("AbstractLinkedBuilder.setUp(null) throws NPE and keeps old link")
    void linkedSetUpNullKeepsOldLink() {
        LinkedBuilder b = new LinkedBuilder("kept");
        assertThrows(NullPointerException.class, () -> b.setUp(null));
        assertEquals("kept", b.up(), "link must be unchanged after a rejected null setUp");
    }

    // ====================================================================
    // AbstractAutomaticLinkedBuilder
    // ====================================================================

    static final class AutoLinkedBuilder
            extends AbstractAutomaticLinkedBuilder<AutoLinkedBuilder, String, Integer> {
        int doBuildCalls = 0;
        int doAutoDetectCalls = 0;

        AutoLinkedBuilder(String link) {
            super(link);
        }

        @Override
        protected Integer doBuild() {
            this.doBuildCalls++;
            return 99;
        }

        @Override
        protected void doAutoDetection() {
            this.doAutoDetectCalls++;
        }
    }

    @Test
    @DisplayName("AbstractAutomaticLinkedBuilder defaults autoDetect to false")
    void autoLinkedDefaultsAutoDetectFalse() {
        AutoLinkedBuilder b = new AutoLinkedBuilder("p");
        assertFalse(b.isAutoDetected());
    }

    @Test
    @DisplayName("AbstractAutomaticLinkedBuilder.up()/setUp() chain returns self")
    void autoLinkedUpAndSetUp() throws DslException {
        AutoLinkedBuilder b = new AutoLinkedBuilder("p1");
        assertEquals("p1", b.up());
        AutoLinkedBuilder ret = b.setUp("p2");
        assertSame(b, ret, "setUp must return this for chaining");
        assertEquals("p2", b.up());
    }

    @Test
    @DisplayName("AbstractAutomaticLinkedBuilder constructor rejects null link")
    void autoLinkedNullLinkRejected() {
        assertThrows(NullPointerException.class, () -> new AutoLinkedBuilder(null));
    }

    @Test
    @DisplayName("AbstractAutomaticLinkedBuilder.setUp(null) throws and keeps old link")
    void autoLinkedSetUpNull() {
        AutoLinkedBuilder b = new AutoLinkedBuilder("orig");
        assertThrows(NullPointerException.class, () -> b.setUp(null));
        assertEquals("orig", b.up());
    }

    @Test
    @DisplayName("AbstractAutomaticLinkedBuilder build() caches and skips auto-detect when disabled")
    void autoLinkedBuildCachesNoAutoDetect() throws DslException {
        AutoLinkedBuilder b = new AutoLinkedBuilder("p");
        assertEquals(99, b.build());
        assertEquals(99, b.build());
        assertEquals(1, b.doBuildCalls, "doBuild must run exactly once thanks to caching");
        assertEquals(0, b.doAutoDetectCalls, "auto-detect disabled means doAutoDetection never runs");
    }

    // ====================================================================
    // AbstractAutomaticBuilder — caching / auto-detect / invalidate / rebuild
    // ====================================================================

    static class AutoBuilder extends AbstractAutomaticBuilder<AutoBuilder, List<String>> {
        final List<String> order = new ArrayList<>();
        boolean failBuild = false;

        @Override
        protected List<String> doBuild() throws DslException {
            order.add("build");
            if (failBuild) {
                throw new DslException("boom");
            }
            return new ArrayList<>(List.of("built"));
        }

        @Override
        protected void doAutoDetection() {
            order.add("autodetect");
        }
    }

    @Test
    @DisplayName("autoDetect(true) sets isAutoDetected and runs detection before build")
    void autoDetectEnabledRunsBeforeBuild() throws DslException {
        AutoBuilder b = new AutoBuilder();
        assertSame(b, b.autoDetect(true));
        assertTrue(b.isAutoDetected());
        b.build();
        assertEquals(List.of("autodetect", "build"), b.order,
                "auto-detection must precede doBuild");
    }

    @Test
    @DisplayName("build() caches the result across calls (doBuild runs once)")
    void buildCaches() throws DslException {
        AutoBuilder b = new AutoBuilder();
        List<String> first = b.build();
        List<String> second = b.build();
        assertSame(first, second, "cached instance must be returned");
        assertEquals(1, b.order.stream().filter("build"::equals).count());
    }

    @Test
    @DisplayName("build() failure propagates DslException and does not cache")
    void buildFailurePropagates() {
        AutoBuilder b = new AutoBuilder();
        b.failBuild = true;
        DslException ex = assertThrows(DslException.class, b::build);
        assertEquals("boom", ex.getMessage());
    }

    @Test
    @DisplayName("invalidate() flips isInvalidated and returns this")
    void invalidateFlagsBuilder() {
        AutoBuilder b = new AutoBuilder();
        assertFalse(b.isInvalidated());
        assertSame(b, b.invalidate());
        assertTrue(b.isInvalidated());
    }

    @Test
    @DisplayName("rebuild() clears the cache, re-runs doBuild and resets the invalidation flag")
    void rebuildClearsCacheAndFlag() throws DslException {
        AutoBuilder b = new AutoBuilder();
        b.build();
        b.invalidate();
        assertTrue(b.isInvalidated());
        b.rebuild();
        assertFalse(b.isInvalidated(), "rebuild must reset the invalidation flag");
        assertEquals(2, b.order.stream().filter("build"::equals).count(),
                "doBuild must run again on rebuild");
    }

    @Test
    @DisplayName("rebuild() invokes doMerge with previous and current built instances")
    void rebuildInvokesMerge() throws DslException {
        AtomicInteger mergeCalls = new AtomicInteger();
        List<Object> mergeArgs = new ArrayList<>();
        AutoBuilder b = new AutoBuilder() {
            @Override
            protected void doMerge(List<String> previous, List<String> current) {
                mergeCalls.incrementAndGet();
                mergeArgs.add(previous);
                mergeArgs.add(current);
            }
        };
        List<String> first = b.build();
        List<String> rebuilt = b.rebuild();

        assertEquals(1, mergeCalls.get(), "merge fires once when a previous build exists");
        assertSame(first, mergeArgs.get(0), "previous arg must be the first built instance");
        assertSame(rebuilt, mergeArgs.get(1), "current arg must be the rebuilt instance");
    }

    @Test
    @DisplayName("rebuild() with no previous build does NOT invoke doMerge")
    void rebuildNoPreviousNoMerge() throws DslException {
        AtomicInteger mergeCalls = new AtomicInteger();
        AutoBuilder b = new AutoBuilder() {
            @Override
            protected void doMerge(List<String> previous, List<String> current) {
                mergeCalls.incrementAndGet();
            }
        };
        b.rebuild();
        assertEquals(0, mergeCalls.get(), "no previous instance means merge must not fire");
    }

    @Test
    @DisplayName("rebuild() re-runs auto-detection when enabled")
    void rebuildRerunsAutoDetection() throws DslException {
        AutoBuilder b = new AutoBuilder();
        b.autoDetect(true);
        b.build();
        b.order.clear();
        b.rebuild();
        assertEquals(List.of("autodetect", "build"), b.order);
    }

    @Test
    @DisplayName("rebuild() failure propagates and leaves no cached instance")
    void rebuildFailurePropagates() throws DslException {
        AutoBuilder b = new AutoBuilder();
        b.build();
        b.failBuild = true;
        assertThrows(DslException.class, b::rebuild);
    }

    @Test
    @DisplayName("default getPackagesForScanning is empty and getReflection is null")
    void defaultScanningHooks() {
        AutoBuilder b = new AutoBuilder();
        assertEquals(0, b.getPackagesForScanning().length);
        assertNull(b.getReflection());
    }

    // A packageable automatic builder whose scanning hooks return null reflection
    // so the @Scan scan branch is skipped (reflect == null guard).
    static final class PackageableAutoBuilder
            extends AbstractAutomaticBuilder<PackageableAutoBuilder, String>
            implements IPackageableBuilder<PackageableAutoBuilder, String> {
        private final List<String> packages = new ArrayList<>();

        @Override
        protected String doBuild() {
            return "ok";
        }

        @Override
        protected void doAutoDetection() {
            // nothing
        }

        @Override
        public PackageableAutoBuilder withPackage(String packageName) {
            packages.add(packageName);
            return this;
        }

        @Override
        public PackageableAutoBuilder withPackages(String[] packageNames) {
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
        protected String[] getPackagesForScanning() {
            return getPackages();
        }

        @Override
        protected IReflection getReflection() {
            return null; // disables the scan branch
        }
    }

    @Test
    @DisplayName("packageable builder with null reflection skips @Scan scan but still builds")
    void packageableNullReflectionSkipsScan() throws DslException {
        PackageableAutoBuilder b = new PackageableAutoBuilder();
        b.withPackage("com.example");
        b.autoDetect(true);
        assertEquals("ok", b.build(), "build proceeds even when reflection is null");
    }

    // ====================================================================
    // OrderedMapBuilder
    // ====================================================================

    static final class ConstBuilder implements IBuilder<String> {
        private final String value;
        boolean fail = false;

        ConstBuilder(String value) {
            this.value = value;
        }

        @Override
        public String build() throws DslException {
            if (fail) {
                throw new DslException("value-build-failed");
            }
            return value;
        }
    }

    @Test
    @DisplayName("OrderedMapBuilder.build() preserves insertion order of keys")
    void orderedMapPreservesOrder() throws DslException {
        OrderedMapBuilder<String, ConstBuilder, String> omb = new OrderedMapBuilder<>();
        omb.put("first", new ConstBuilder("1"));
        omb.put("second", new ConstBuilder("2"));
        omb.put("third", new ConstBuilder("3"));

        OrderedMap<String, String> result = omb.build();
        assertEquals(List.of("first", "second", "third"), new ArrayList<>(result.keySet()));
        assertEquals("1", result.get("first"));
        assertEquals("2", result.get("second"));
        assertEquals("3", result.get("third"));
    }

    @Test
    @DisplayName("OrderedMapBuilder rejects a null value builder at put() time")
    void orderedMapPutNullRejected() {
        OrderedMapBuilder<String, ConstBuilder, String> omb = new OrderedMapBuilder<>();
        // The underlying OrderedMap refuses null values, so the documented
        // "skip null value builders" filter in build() is never reachable.
        assertThrows(NullPointerException.class, () -> omb.put("b", null));
    }

    @Test
    @DisplayName("OrderedMapBuilder.build() on empty map yields an empty OrderedMap")
    void orderedMapEmpty() throws DslException {
        OrderedMapBuilder<String, ConstBuilder, String> omb = new OrderedMapBuilder<>();
        OrderedMap<String, String> result = omb.build();
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("OrderedMapBuilder.build() propagates a value builder failure")
    void orderedMapPropagatesValueFailure() {
        OrderedMapBuilder<String, ConstBuilder, String> omb = new OrderedMapBuilder<>();
        ConstBuilder failing = new ConstBuilder("x");
        failing.fail = true;
        omb.put("bad", failing);

        assertThrows(RuntimeException.class, omb::build,
                "a failing value builder must surface as a (wrapped) exception");
    }
}

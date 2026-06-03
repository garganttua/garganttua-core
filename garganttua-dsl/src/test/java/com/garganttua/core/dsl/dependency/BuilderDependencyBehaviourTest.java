package com.garganttua.core.dsl.dependency;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.dsl.IBuilderObserver;
import com.garganttua.core.dsl.IObservableBuilder;
import com.garganttua.core.dsl.IPackageableBuilder;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IReflection;
import com.garganttua.core.reflection.runtime.RuntimeReflectionProvider;

/**
 * Behaviour-focused tests for {@link BuilderDependency}: the full
 * unresolved -> resolved state machine, deferred resolution, validation,
 * package synchronization, and firing-event idempotency memory.
 */
class BuilderDependencyBehaviourTest {

    private static final RuntimeReflectionProvider PROVIDER = new RuntimeReflectionProvider();

    @BeforeAll
    static void setUpReflection() {
        IReflection reflection = (IReflection) Proxy.newProxyInstance(
                IReflection.class.getClassLoader(),
                new Class<?>[]{IReflection.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getClass" -> PROVIDER.getClass((Class<?>) args[0]);
                    case "supports" -> PROVIDER.supports((Class<?>) args[0]);
                    case "forName" -> PROVIDER.forName((String) args[0]);
                    default -> throw new UnsupportedOperationException(method.getName());
                });
        IClass.setReflection(reflection);
    }

    @AfterAll
    static void tearDownReflection() {
        IClass.setReflection(null);
    }

    // ---- Test doubles -----------------------------------------------------

    /** Builder whose build() can be made to fail, succeed, or count calls. */
    static class DepBuilder implements IObservableBuilder<DepBuilder, String> {
        int buildCalls = 0;
        boolean failOnBuild = false;
        String result = "built-value";

        @Override
        public String build() throws DslException {
            buildCalls++;
            if (failOnBuild) {
                throw new DslException("not yet buildable");
            }
            return result;
        }

        @Override
        public DepBuilder observer(IBuilderObserver<DepBuilder, String> observer) {
            return this;
        }
    }

    /** A packageable upstream builder used to test synchronizePackagesFromContext. */
    static class PackageableDepBuilder
            implements IObservableBuilder<PackageableDepBuilder, String>,
                       IPackageableBuilder<PackageableDepBuilder, String> {
        private final Set<String> packages = new HashSet<>();

        @Override
        public PackageableDepBuilder withPackage(String packageName) {
            packages.add(packageName);
            return this;
        }

        @Override
        public PackageableDepBuilder withPackages(String[] packageNames) {
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

        @Override
        public PackageableDepBuilder observer(IBuilderObserver<PackageableDepBuilder, String> observer) {
            return this;
        }
    }

    /** An unrelated builder type — used to exercise the type-mismatch path. */
    static class OtherBuilder implements IObservableBuilder<OtherBuilder, Integer> {
        @Override
        public Integer build() throws DslException {
            return 1;
        }

        @Override
        public OtherBuilder observer(IBuilderObserver<OtherBuilder, Integer> observer) {
            return this;
        }
    }

    private static BuilderDependency<DepBuilder, String> newDep(DependencySpec spec) {
        return new BuilderDependency<>(IClass.getClass(DepBuilder.class), spec);
    }

    private static DependencySpec buildBuiltRequired() {
        return DependencySpec.require(IClass.getClass(DepBuilder.class));
    }

    // ---- Construction & null guards --------------------------------------

    @Test
    @DisplayName("Constructor rejects null class and null spec")
    void constructorRejectsNulls() {
        assertThrows(NullPointerException.class,
                () -> new BuilderDependency<>(null, buildBuiltRequired()));
        assertThrows(NullPointerException.class,
                () -> new BuilderDependency<>(IClass.getClass(DepBuilder.class), null));
    }

    @Test
    @DisplayName("Fresh dependency is empty, not ready, and exposes its class")
    void freshDependencyState() {
        BuilderDependency<DepBuilder, String> dep = newDep(buildBuiltRequired());
        assertTrue(dep.isEmpty());
        assertFalse(dep.isReady());
        assertFalse(dep.hasBuilder());
        assertFalse(dep.hasBuilt());
        assertEquals(IClass.getClass(DepBuilder.class), dep.getDependency());
    }

    // ---- handle() ---------------------------------------------------------

    @Test
    @DisplayName("handle(builder) of mismatching type is ignored, leaving dep empty")
    void handleTypeMismatchIgnored() {
        BuilderDependency<DepBuilder, String> dep = newDep(buildBuiltRequired());
        dep.handle(new OtherBuilder());
        assertFalse(dep.hasBuilder(), "mismatched builder must not be stored");
        assertTrue(dep.isEmpty());
    }

    @Test
    @DisplayName("handle(builder) stores reference but does not eagerly build")
    void handleBuilderDoesNotBuild() {
        BuilderDependency<DepBuilder, String> dep = newDep(buildBuiltRequired());
        DepBuilder upstream = new DepBuilder();
        dep.handle(upstream);
        assertTrue(dep.hasBuilder());
        assertEquals(0, upstream.buildCalls, "handle must defer building");
        assertSame(upstream, dep.builder());
    }

    @Test
    @DisplayName("handle(builtObject) records the built object and rejects null")
    void handleBuiltObject() {
        BuilderDependency<DepBuilder, String> dep = newDep(buildBuiltRequired());
        assertThrows(NullPointerException.class, () -> dep.handle((String) null));
        dep.handle("explicit-built");
        assertTrue(dep.hasBuilt());
    }

    @Test
    @DisplayName("A built object alone is NOT ready — readiness requires both builder and built")
    void builtAloneIsNotReady() {
        BuilderDependency<DepBuilder, String> dep = newDep(buildBuiltRequired());
        dep.handle("explicit-built");
        assertTrue(dep.hasBuilt());
        assertFalse(dep.hasBuilder());
        assertFalse(dep.isReady(), "isReady requires both a builder and a built object");
        // isEmpty is false because builtObject is set
        assertFalse(dep.isEmpty());
    }

    @Test
    @DisplayName("Both builder and built object present makes the dependency ready")
    void builderAndBuiltMakesReady() {
        BuilderDependency<DepBuilder, String> dep = newDep(buildBuiltRequired());
        dep.handle(new DepBuilder());
        dep.handle("explicit-built");
        assertTrue(dep.isReady());
        assertEquals("explicit-built", dep.get());
    }

    // ---- tryResolve (via isReady/get) ------------------------------------

    @Test
    @DisplayName("isReady lazily builds the upstream when only the builder was provided")
    void lazyResolveOnIsReady() {
        BuilderDependency<DepBuilder, String> dep = newDep(buildBuiltRequired());
        DepBuilder upstream = new DepBuilder();
        dep.handle(upstream);
        assertTrue(dep.isReady(), "isReady should trigger lazy build");
        assertEquals(1, upstream.buildCalls);
        assertEquals("built-value", dep.get());
        // second call must not rebuild (builtObject already cached)
        dep.isReady();
        assertEquals(1, upstream.buildCalls);
    }

    @Test
    @DisplayName("Lazy resolution swallows DslException and stays not-ready")
    void lazyResolveSwallowsDslException() {
        BuilderDependency<DepBuilder, String> dep = newDep(buildBuiltRequired());
        DepBuilder upstream = new DepBuilder();
        upstream.failOnBuild = true;
        dep.handle(upstream);
        assertFalse(dep.isReady(), "build failure must leave dependency not ready");
        assertTrue(upstream.buildCalls > 0, "build was attempted");
        assertFalse(dep.hasBuilt());
    }

    // ---- get() / builder() error paths -----------------------------------

    @Test
    @DisplayName("get() throws IllegalStateException when not ready")
    void getThrowsWhenNotReady() {
        BuilderDependency<DepBuilder, String> dep = newDep(buildBuiltRequired());
        IllegalStateException ex = assertThrows(IllegalStateException.class, dep::get);
        assertTrue(ex.getMessage().contains(DepBuilder.class.getName()));
    }

    @Test
    @DisplayName("builder() throws IllegalStateException when no builder provided")
    void builderThrowsWhenAbsent() {
        BuilderDependency<DepBuilder, String> dep = newDep(buildBuiltRequired());
        IllegalStateException ex = assertThrows(IllegalStateException.class, dep::builder);
        assertTrue(ex.getMessage().contains("Builder not yet provided"));
    }

    // ---- ifReady family ---------------------------------------------------

    @Test
    @DisplayName("ifReady runs the consumer only once resolved, with the built value")
    void ifReadyConsumer() {
        BuilderDependency<DepBuilder, String> dep = newDep(buildBuiltRequired());
        AtomicReference<String> seen = new AtomicReference<>();
        dep.ifReady(seen::set);
        assertEquals(null, seen.get(), "consumer must not run when empty");

        // Provide a builder so lazy resolution yields a built value -> ready.
        dep.handle(new DepBuilder());
        dep.ifReady(seen::set);
        assertEquals("built-value", seen.get());
    }

    @Test
    @DisplayName("ifReadyOrElse runs the fallback when not ready and consumer when ready")
    void ifReadyOrElse() {
        BuilderDependency<DepBuilder, String> dep = newDep(buildBuiltRequired());
        AtomicBoolean fallbackRan = new AtomicBoolean(false);
        AtomicReference<String> consumed = new AtomicReference<>();

        dep.ifReadyOrElse(consumed::set, () -> fallbackRan.set(true));
        assertTrue(fallbackRan.get());
        assertEquals(null, consumed.get());

        dep.handle(new DepBuilder());
        fallbackRan.set(false);
        dep.ifReadyOrElse(consumed::set, () -> fallbackRan.set(true));
        assertFalse(fallbackRan.get());
        assertEquals("built-value", consumed.get());
    }

    @Test
    @DisplayName("ifReadyOrElseThrow throws IllegalStateException when not ready")
    void ifReadyOrElseThrowDefault() {
        BuilderDependency<DepBuilder, String> dep = newDep(buildBuiltRequired());
        assertThrows(IllegalStateException.class, () -> dep.ifReadyOrElseThrow(v -> {}));

        dep.handle(new DepBuilder());
        AtomicReference<String> consumed = new AtomicReference<>();
        dep.ifReadyOrElseThrow(consumed::set);
        assertEquals("built-value", consumed.get());
    }

    @Test
    @DisplayName("ifReadyOrElseThrow uses the custom exception supplier when not ready")
    void ifReadyOrElseThrowCustom() {
        BuilderDependency<DepBuilder, String> dep = newDep(buildBuiltRequired());
        assertThrows(IllegalArgumentException.class,
                () -> dep.ifReadyOrElseThrow(v -> {}, () -> new IllegalArgumentException("boom")));
    }

    // ---- requireNotEmpty --------------------------------------------------

    @Test
    @DisplayName("requireNotEmpty throws when empty and passes once a builder is present")
    void requireNotEmpty() {
        BuilderDependency<DepBuilder, String> dep = newDep(buildBuiltRequired());
        assertThrows(IllegalStateException.class, dep::requireNotEmpty);
        dep.handle(new DepBuilder());
        // no longer empty -> must not throw
        dep.requireNotEmpty();
    }

    // ---- stage / kind / requirement accessors -----------------------------

    @Test
    @DisplayName("Stage/kind/requirement accessors mirror the spec exactly")
    void accessorsMirrorSpec() {
        DependencySpec spec = DependencySpec.configure(IClass.getClass(DepBuilder.class));
        BuilderDependency<DepBuilder, String> dep = newDep(spec);

        assertEquals(DependencyStage.CONFIGURATION, dep.stage());
        assertEquals(DependencyKind.BUILDER, dep.kind());
        assertSame(spec, dep.spec());
        assertSame(spec, dep.getSpec());
        assertTrue(dep.isConfigurationStage());
        assertFalse(dep.isAutoDetectStage());
        assertFalse(dep.isBuildStage());
        assertTrue(dep.isBuilderKind());
        assertFalse(dep.isBuiltKind());
        assertTrue(dep.isOptional());
        assertFalse(dep.isRequired());
    }

    @Test
    @DisplayName("BUILD+BUILT required spec reports build stage and built kind")
    void buildBuiltRequiredAccessors() {
        BuilderDependency<DepBuilder, String> dep = newDep(buildBuiltRequired());
        assertTrue(dep.isBuildStage());
        assertTrue(dep.isBuiltKind());
        assertTrue(dep.isRequired());
        assertTrue(dep.isNeededForBuild());
        assertFalse(dep.isNeededForAutoDetect());
        assertTrue(dep.isRequiredForBuild());
        assertFalse(dep.isRequiredForAutoDetect());
        assertFalse(dep.isOptionalForBuild());
    }

    // ---- validateUseDependency -------------------------------------------

    @Test
    @DisplayName("validateUseDependency passes when never provided (optional absent)")
    void validateUsePassesWhenAbsent() throws DslException {
        BuilderDependency<DepBuilder, String> dep =
                newDep(DependencySpec.use(IClass.getClass(DepBuilder.class)));
        dep.validateUseDependency();
    }

    @Test
    @DisplayName("validateUseDependency throws when a builder was provided but cannot build")
    void validateUseThrowsWhenProvidedNotBuilt() {
        BuilderDependency<DepBuilder, String> dep =
                newDep(DependencySpec.use(IClass.getClass(DepBuilder.class)));
        DepBuilder upstream = new DepBuilder();
        upstream.failOnBuild = true;
        dep.handle(upstream);
        DslException ex = assertThrows(DslException.class, dep::validateUseDependency);
        assertTrue(ex.getMessage().contains("must be built"));
    }

    @Test
    @DisplayName("validateUseDependency passes when provided builder builds successfully")
    void validateUsePassesWhenBuildable() throws DslException {
        BuilderDependency<DepBuilder, String> dep =
                newDep(DependencySpec.use(IClass.getClass(DepBuilder.class)));
        dep.handle(new DepBuilder());
        dep.validateUseDependency();
        assertTrue(dep.isReady());
    }

    // ---- validateRequiredDependency --------------------------------------

    @Test
    @DisplayName("validateRequiredDependency throws 'not provided' when empty, with stage failure")
    void validateRequiredEmpty() {
        BuilderDependency<DepBuilder, String> dep = newDep(buildBuiltRequired());
        DslException ex = assertThrows(DslException.class,
                () -> dep.validateRequiredDependency("BUILD"));
        assertTrue(ex.getMessage().contains("was not provided"));
    }

    @Test
    @DisplayName("validateRequiredDependency throws 'not built' when builder provided but unbuildable")
    void validateRequiredProvidedNotBuilt() {
        BuilderDependency<DepBuilder, String> dep = newDep(buildBuiltRequired());
        DepBuilder upstream = new DepBuilder();
        upstream.failOnBuild = true;
        dep.handle(upstream);
        DslException ex = assertThrows(DslException.class,
                () -> dep.validateRequiredDependency("BUILD"));
        assertTrue(ex.getMessage().contains("provided but not built"));
    }

    @Test
    @DisplayName("validateRequiredDependency passes when builder builds successfully")
    void validateRequiredOk() throws DslException {
        BuilderDependency<DepBuilder, String> dep = newDep(buildBuiltRequired());
        dep.handle(new DepBuilder());
        dep.validateRequiredDependency("BUILD");
    }

    // ---- firing-event idempotency memory ----------------------------------

    @Test
    @DisplayName("tryMarkFired returns true once then false; hasFired tracks it")
    void firingMemory() {
        BuilderDependency<DepBuilder, String> dep = newDep(buildBuiltRequired());
        assertFalse(dep.hasFired(BuilderDependency.FiringEvent.PRE_BUILD));
        assertTrue(dep.tryMarkFired(BuilderDependency.FiringEvent.PRE_BUILD));
        assertTrue(dep.hasFired(BuilderDependency.FiringEvent.PRE_BUILD));
        assertFalse(dep.tryMarkFired(BuilderDependency.FiringEvent.PRE_BUILD),
                "second mark of same event returns false");
        assertThrows(NullPointerException.class, () -> dep.tryMarkFired(null));
    }

    @Test
    @DisplayName("resetFiringMemory(false) clears everything including CONFIGURATION")
    void resetFiringMemoryFull() {
        BuilderDependency<DepBuilder, String> dep = newDep(buildBuiltRequired());
        dep.tryMarkFired(BuilderDependency.FiringEvent.CONFIGURATION);
        dep.tryMarkFired(BuilderDependency.FiringEvent.PRE_BUILD);
        dep.resetFiringMemory(false);
        assertFalse(dep.hasFired(BuilderDependency.FiringEvent.CONFIGURATION));
        assertFalse(dep.hasFired(BuilderDependency.FiringEvent.PRE_BUILD));
    }

    @Test
    @DisplayName("resetFiringMemory(true) keeps CONFIGURATION but clears the per-build events")
    void resetFiringMemoryKeepConfiguration() {
        BuilderDependency<DepBuilder, String> dep = newDep(buildBuiltRequired());
        dep.tryMarkFired(BuilderDependency.FiringEvent.CONFIGURATION);
        dep.tryMarkFired(BuilderDependency.FiringEvent.PRE_BUILD);
        dep.tryMarkFired(BuilderDependency.FiringEvent.POST_BUILD);
        dep.resetFiringMemory(true);
        assertTrue(dep.hasFired(BuilderDependency.FiringEvent.CONFIGURATION),
                "CONFIGURATION must survive a keep-config reset");
        assertFalse(dep.hasFired(BuilderDependency.FiringEvent.PRE_BUILD));
        assertFalse(dep.hasFired(BuilderDependency.FiringEvent.POST_BUILD));
    }

    @Test
    @DisplayName("resetFiringMemory(true) with no prior CONFIGURATION leaves CONFIGURATION unfired")
    void resetFiringMemoryKeepConfigurationWhenNeverConfigured() {
        BuilderDependency<DepBuilder, String> dep = newDep(buildBuiltRequired());
        dep.tryMarkFired(BuilderDependency.FiringEvent.PRE_BUILD);
        dep.resetFiringMemory(true);
        assertFalse(dep.hasFired(BuilderDependency.FiringEvent.CONFIGURATION));
        assertFalse(dep.hasFired(BuilderDependency.FiringEvent.PRE_BUILD));
    }

    // ---- synchronizePackagesFromContext -----------------------------------

    @Test
    @DisplayName("synchronizePackagesFromContext reads packages off a packageable upstream builder")
    void syncPackagesFromPackageableBuilder() {
        BuilderDependency<PackageableDepBuilder, String> dep =
                new BuilderDependency<>(IClass.getClass(PackageableDepBuilder.class),
                        DependencySpec.use(IClass.getClass(PackageableDepBuilder.class)));
        PackageableDepBuilder upstream = new PackageableDepBuilder();
        upstream.withPackage("com.foo").withPackage("com.bar");
        dep.handle(upstream);

        AtomicReference<Set<String>> captured = new AtomicReference<>();
        dep.synchronizePackagesFromContext(captured::set);
        assertEquals(Set.of("com.foo", "com.bar"), captured.get());
    }

    @Test
    @DisplayName("synchronizePackagesFromContext yields the empty local set for a non-packageable builder")
    void syncPackagesFallsBackToEmptyLocal() {
        BuilderDependency<DepBuilder, String> dep = newDep(buildBuiltRequired());
        dep.handle(new DepBuilder());
        AtomicReference<Set<String>> captured = new AtomicReference<>();
        dep.synchronizePackagesFromContext(captured::set);
        assertTrue(captured.get().isEmpty());
    }
}

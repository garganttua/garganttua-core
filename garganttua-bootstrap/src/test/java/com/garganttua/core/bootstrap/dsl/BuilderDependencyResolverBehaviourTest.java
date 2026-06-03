package com.garganttua.core.bootstrap.dsl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.dsl.IBuilder;
import com.garganttua.core.dsl.IBuilderObserver;
import com.garganttua.core.dsl.IObservableBuilder;
import com.garganttua.core.dsl.dependency.IDependentBuilder;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IReflection;
import com.garganttua.core.reflection.dsl.ReflectionBuilder;
import com.garganttua.core.reflection.runtime.RuntimeReflectionProvider;

/**
 * Behaviour tests for {@link BuilderDependencyResolver}: Kahn topological sort
 * (with cycle detection) and the require()/use() wiring done by resolveDependencies().
 */
@DisplayName("BuilderDependencyResolver behaviour")
class BuilderDependencyResolverBehaviourTest {

    @BeforeAll
    static void setUpReflection() throws Exception {
        IReflection reflection = ReflectionBuilder.builder()
                .withProvider(new RuntimeReflectionProvider())
                .build();
        IClass.setReflection(reflection);
    }

    @AfterAll
    static void tearDownReflection() {
        IClass.setReflection(null);
    }

    // ---- test doubles -------------------------------------------------------

    /** Plain builder with no dependencies and no observability. */
    static class Plain implements IBuilder<String> {
        final String name;
        Plain(String name) { this.name = name; }
        @Override public String build() { return name; }
        @Override public String toString() { return "Plain(" + name + ")"; }
    }

    /** Observable builder — can satisfy other builders' deps. */
    static class Observable implements IObservableBuilder<Observable, String> {
        final String name;
        Observable(String name) { this.name = name; }
        @Override public String build() { return name; }
        @Override public Observable observer(IBuilderObserver<Observable, String> o) { return this; }
        @Override public String toString() { return "Observable(" + name + ")"; }
    }

    /** Second observable subtype, used to test class-based lookup specificity. */
    static class OtherObservable implements IObservableBuilder<OtherObservable, String> {
        final String name;
        OtherObservable(String name) { this.name = name; }
        @Override public String build() { return name; }
        @Override public OtherObservable observer(IBuilderObserver<OtherObservable, String> o) { return this; }
        @Override public String toString() { return "OtherObservable(" + name + ")"; }
    }

    /** Dependent builder recording everything provided to it. */
    static class Dependent implements IDependentBuilder<Dependent, String> {
        final String name;
        final Set<IClass<? extends IObservableBuilder<?, ?>>> required = new HashSet<>();
        final Set<IClass<? extends IObservableBuilder<?, ?>>> optional = new HashSet<>();
        final List<IObservableBuilder<?, ?>> provided = new ArrayList<>();

        Dependent(String name) { this.name = name; }

        Dependent requires(Class<? extends IObservableBuilder<?, ?>> c) {
            required.add(IClass.getClass(c));
            return this;
        }
        Dependent uses(Class<? extends IObservableBuilder<?, ?>> c) {
            optional.add(IClass.getClass(c));
            return this;
        }

        @Override public String build() { return name; }
        @Override public Dependent provide(IObservableBuilder<?, ?> dep) { provided.add(dep); return this; }
        @Override public Set<IClass<? extends IObservableBuilder<?, ?>>> require() { return required; }
        @Override public Set<IClass<? extends IObservableBuilder<?, ?>>> use() { return optional; }
        @Override public String toString() { return "Dependent(" + name + ")"; }
    }

    /** Both observable AND dependent — for chains. */
    static class DependentObservable
            implements IObservableBuilder<DependentObservable, String>,
                       IDependentBuilder<DependentObservable, String> {
        final String name;
        final Set<IClass<? extends IObservableBuilder<?, ?>>> required = new HashSet<>();
        DependentObservable(String name) { this.name = name; }
        DependentObservable requires(Class<? extends IObservableBuilder<?, ?>> c) {
            required.add(IClass.getClass(c));
            return this;
        }
        @Override public String build() { return name; }
        @Override public DependentObservable observer(IBuilderObserver<DependentObservable, String> o) { return this; }
        @Override public DependentObservable provide(IObservableBuilder<?, ?> dep) { return this; }
        @Override public Set<IClass<? extends IObservableBuilder<?, ?>>> require() { return required; }
        @Override public Set<IClass<? extends IObservableBuilder<?, ?>>> use() { return new HashSet<>(); }
        @Override public String toString() { return "DependentObservable(" + name + ")"; }
    }

    private static BuilderDependencyResolver resolver(List<IBuilder<?>> builders) {
        return new BuilderDependencyResolver(builders, new ArrayList<>());
    }

    // ---- topological sort ---------------------------------------------------

    @Test
    @DisplayName("independent builders are all returned, order preserved")
    void independentBuildersKeepInsertionOrder() throws DslException {
        Plain a = new Plain("a");
        Plain b = new Plain("b");
        Plain c = new Plain("c");
        List<IBuilder<?>> result = resolver(List.of(a, b, c)).sortBuildersByDependencies();

        assertEquals(List.of(a, b, c), result);
    }

    @Test
    @DisplayName("dependency is sorted before the builder that requires it")
    void dependencySortsBeforeDependent() throws DslException {
        Observable obs = new Observable("obs");
        Dependent dep = new Dependent("dep").requires(Observable.class);
        // Register dependent FIRST to prove the sort, not insertion, drives order.
        List<IBuilder<?>> result = resolver(List.of(dep, obs)).sortBuildersByDependencies();

        assertEquals(2, result.size());
        assertTrue(result.indexOf(obs) < result.indexOf(dep),
                "obs must come before dep, got " + result);
    }

    @Test
    @DisplayName("a three-level chain is sorted root -> middle -> leaf")
    void threeLevelChainIsOrdered() throws DslException {
        Observable root = new Observable("root");
        DependentObservable middle = new DependentObservable("middle").requires(Observable.class);
        Dependent leaf = new Dependent("leaf").requires(DependentObservable.class);

        // Reverse insertion order to stress the topological sort.
        List<IBuilder<?>> result = resolver(List.of(leaf, middle, root)).sortBuildersByDependencies();

        assertEquals(3, result.size());
        assertTrue(result.indexOf(root) < result.indexOf(middle), "root before middle: " + result);
        assertTrue(result.indexOf(middle) < result.indexOf(leaf), "middle before leaf: " + result);
    }

    @Test
    @DisplayName("optional use() dependency also constrains the sort order")
    void optionalDependencyConstrainsOrder() throws DslException {
        Observable obs = new Observable("obs");
        Dependent dep = new Dependent("dep").uses(Observable.class);
        List<IBuilder<?>> result = resolver(List.of(dep, obs)).sortBuildersByDependencies();

        assertTrue(result.indexOf(obs) < result.indexOf(dep), "obs before dep: " + result);
    }

    @Test
    @DisplayName("a required dependency whose class is absent does not block the sort")
    void missingDependencyClassIsIgnoredDuringSort() throws DslException {
        // Requires OtherObservable, but only Observable is present.
        Dependent dep = new Dependent("dep").requires(OtherObservable.class);
        Observable obs = new Observable("obs");
        List<IBuilder<?>> result = resolver(List.of(dep, obs)).sortBuildersByDependencies();

        // No edge was created (the class isn't present), so both are roots.
        assertEquals(2, result.size());
        assertTrue(result.contains(dep) && result.contains(obs));
    }

    @Test
    @DisplayName("empty builder list yields empty sorted list")
    void emptyListSortsToEmpty() throws DslException {
        List<IBuilder<?>> result = resolver(new ArrayList<>()).sortBuildersByDependencies();
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("a direct two-builder cycle is detected and reported")
    void directCycleIsDetected() {
        // a requires b, b requires a -> neither has in-degree 0 -> cycle.
        DependentObservable a = new DependentObservable("a");
        DependentObservable b = new DependentObservable("b");
        a.requires(b.getClass());
        b.requires(a.getClass());

        BuilderDependencyResolver r = resolver(List.of(a, b));
        DslException ex = assertThrows(DslException.class, r::sortBuildersByDependencies);
        assertTrue(ex.getMessage().contains("Circular dependency detected"),
                "message should flag the cycle: " + ex.getMessage());
        // Both unprocessed builders should be named in the message.
        assertTrue(ex.getMessage().contains("DependentObservable"),
                "message should name the offending builders: " + ex.getMessage());
    }

    @Test
    @DisplayName("self-dependency is detected as a cycle")
    void selfDependencyIsDetected() {
        DependentObservable a = new DependentObservable("a");
        a.requires(a.getClass());

        BuilderDependencyResolver r = resolver(List.of(a));
        DslException ex = assertThrows(DslException.class, r::sortBuildersByDependencies);
        assertTrue(ex.getMessage().contains("Circular dependency detected"));
    }

    // ---- dependency wiring (resolveDependencies) ----------------------------

    @Test
    @DisplayName("required dependency is provided exactly once to the dependent")
    void requiredDependencyIsProvided() throws DslException {
        Observable obs = new Observable("obs");
        Dependent dep = new Dependent("dep").requires(Observable.class);
        resolver(List.of(obs, dep)).resolveDependencies();

        assertEquals(1, dep.provided.size());
        assertSame(obs, dep.provided.get(0));
    }

    @Test
    @DisplayName("optional dependency is provided when present")
    void optionalDependencyProvidedWhenPresent() throws DslException {
        Observable obs = new Observable("obs");
        Dependent dep = new Dependent("dep").uses(Observable.class);
        resolver(List.of(obs, dep)).resolveDependencies();

        assertEquals(1, dep.provided.size());
        assertSame(obs, dep.provided.get(0));
    }

    @Test
    @DisplayName("optional dependency that is absent is silently skipped (no provide call)")
    void optionalDependencyAbsentIsSkipped() throws DslException {
        Dependent dep = new Dependent("dep").uses(Observable.class);
        // No observable registered at all.
        resolver(List.of(dep)).resolveDependencies();

        assertTrue(dep.provided.isEmpty(), "no provide call expected for absent optional dep");
    }

    @Test
    @DisplayName("missing REQUIRED dependency throws DslException naming the dep and builder")
    void missingRequiredDependencyThrows() {
        Dependent dep = new Dependent("dep").requires(Observable.class);
        BuilderDependencyResolver r = resolver(List.of(dep));

        DslException ex = assertThrows(DslException.class, r::resolveDependencies);
        assertTrue(ex.getMessage().contains("Required dependency not found"),
                "message: " + ex.getMessage());
        assertTrue(ex.getMessage().contains("Observable"),
                "should name the missing dependency type: " + ex.getMessage());
        assertTrue(ex.getMessage().contains("Dependent"),
                "should name the requesting builder: " + ex.getMessage());
    }

    @Test
    @DisplayName("provided (not managed) observable builders also satisfy required deps")
    void providedBuildersSatisfyRequiredDeps() throws DslException {
        Observable provided = new Observable("provided");
        Dependent dep = new Dependent("dep").requires(Observable.class);

        // dep is a managed builder; the observable comes only from the provided list.
        List<IBuilder<?>> managed = new ArrayList<>(List.of(dep));
        List<IObservableBuilder<?, ?>> providedList = new ArrayList<>(List.of(provided));
        new BuilderDependencyResolver(managed, providedList).resolveDependencies();

        assertEquals(1, dep.provided.size());
        assertSame(provided, dep.provided.get(0));
    }

    @Test
    @DisplayName("a managed observable is not duplicated when also present in provided list")
    void managedObservableNotDuplicatedFromProvidedList() throws DslException {
        Observable obs = new Observable("obs");
        Dependent dep = new Dependent("dep").requires(Observable.class);

        List<IBuilder<?>> managed = new ArrayList<>(List.of(obs, dep));
        // Same instance also in provided list -> collectObservableBuilders must dedupe.
        List<IObservableBuilder<?, ?>> providedList = new ArrayList<>(List.of(obs));
        new BuilderDependencyResolver(managed, providedList).resolveDependencies();

        // findBuilderByClass returns the first match; provide called exactly once.
        assertEquals(1, dep.provided.size());
        assertSame(obs, dep.provided.get(0));
    }

    @Test
    @DisplayName("a builder requiring two distinct observables receives both")
    void multipleRequiredDependenciesAllProvided() throws DslException {
        Observable obs = new Observable("obs");
        OtherObservable other = new OtherObservable("other");
        Dependent dep = new Dependent("dep")
                .requires(Observable.class)
                .requires(OtherObservable.class);

        resolver(List.of(obs, other, dep)).resolveDependencies();

        assertEquals(2, dep.provided.size());
        assertTrue(dep.provided.contains(obs));
        assertTrue(dep.provided.contains(other));
    }

    @Test
    @DisplayName("non-dependent builders are ignored by resolveDependencies (no error)")
    void nonDependentBuildersIgnored() throws DslException {
        Plain plain = new Plain("plain");
        Observable obs = new Observable("obs");
        // Should simply do nothing harmful.
        resolver(List.of(plain, obs)).resolveDependencies();
        // Nothing to assert beyond no exception; reaching here means success.
        assertTrue(true);
    }
}

package com.garganttua.core.dsl.dependency;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.util.Set;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.dsl.IBuilderObserver;
import com.garganttua.core.dsl.IObservableBuilder;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IReflection;
import com.garganttua.core.reflection.runtime.RuntimeReflectionProvider;

@DisplayName("@DependsOn annotation discovery")
class DependsOnAnnotationTest {

    private static final RuntimeReflectionProvider PROVIDER = new RuntimeReflectionProvider();

    @BeforeAll
    static void wireReflection() {
        IReflection reflection = (IReflection) Proxy.newProxyInstance(
                IReflection.class.getClassLoader(),
                new Class<?>[]{ IReflection.class },
                (proxy, method, args) -> switch (method.getName()) {
                    case "getClass" -> PROVIDER.getClass((Class<?>) args[0]);
                    case "supports" -> PROVIDER.supports((Class<?>) args[0]);
                    case "forName" -> PROVIDER.forName((String) args[0]);
                    default -> throw new UnsupportedOperationException(method.getName());
                });
        IClass.setReflection(reflection);
    }

    @AfterAll
    static void resetReflection() {
        IClass.setReflection(null);
    }

    @Test
    @DisplayName("Single @DependsOn with defaults expands to BUILD + BUILT + OPTIONAL")
    void singleAnnotationDefault() {
        Set<DependencySpec> specs = DependencySpec.fromAnnotations(WithDefaults.class);
        assertEquals(1, specs.size());
        DependencySpec only = specs.iterator().next();
        assertEquals(DependencyStage.BUILD, only.stage());
        assertEquals(DependencyKind.BUILT, only.kind());
        assertEquals(DependencyRequirement.OPTIONAL, only.requirement());
        assertEquals(UpstreamBuilder.class.getName(), only.dependencyBuilderClass().getName());
    }

    @Test
    @DisplayName("Repeated @DependsOn produces multiple DependencySpec entries")
    void repeatableAnnotations() {
        Set<DependencySpec> specs = DependencySpec.fromAnnotations(WithRepeated.class);
        assertEquals(2, specs.size());
        assertTrue(specs.stream().anyMatch(DependencySpec::isConfiguration));
        assertTrue(specs.stream().anyMatch(DependencySpec::isBuild));
    }

    @Test
    @DisplayName("Annotation-derived specs merge with constructor-passed set")
    void annotationsMergeIntoConstructorSet() {
        WithAnnotatedDep dep = new WithAnnotatedDep();
        assertEquals(1, (dep.support.use().size() + dep.support.require().size()));
    }

    @Test
    @DisplayName("No annotations + empty ctor set → empty support")
    void noAnnotationsNoCtor() {
        WithNothing dep = new WithNothing();
        assertEquals(0, (dep.support.use().size() + dep.support.require().size()));
    }

    // ---- Fixtures --------------------------------------------------------

    static class UpstreamBuilder implements IObservableBuilder<UpstreamBuilder, String> {
        @Override public String build() throws DslException { return "u"; }
        @Override public UpstreamBuilder observer(IBuilderObserver<UpstreamBuilder, String> o) { return this; }
    }

    @DependsOn(target = UpstreamBuilder.class)
    static class WithDefaults {}

    @DependsOn(target = UpstreamBuilder.class, stage = DependencyStage.CONFIGURATION,
               kind = DependencyKind.BUILDER)
    @DependsOn(target = UpstreamBuilder.class)
    static class WithRepeated {}

    @DependsOn(target = UpstreamBuilder.class)
    static class WithAnnotatedDep
            extends AbstractAutomaticDependentBuilder<WithAnnotatedDep, String>
            implements IObservableBuilder<WithAnnotatedDep, String> {
        WithAnnotatedDep() { super(Set.of()); }
        @Override public WithAnnotatedDep observer(IBuilderObserver<WithAnnotatedDep, String> o) { return this; }
        @Override protected String doBuild() { return "x"; }
        @Override protected void doAutoDetection() {}
        @Override protected void doAutoDetectionWithDependency(Object d) {}
        @Override protected void doPreBuildWithDependency(Object d) {}
        @Override protected void doPostBuildWithDependency(Object d) {}
    }

    static class WithNothing
            extends AbstractAutomaticDependentBuilder<WithNothing, String>
            implements IObservableBuilder<WithNothing, String> {
        WithNothing() { super(Set.of()); }
        @Override public WithNothing observer(IBuilderObserver<WithNothing, String> o) { return this; }
        @Override protected String doBuild() { return "x"; }
        @Override protected void doAutoDetection() {}
        @Override protected void doAutoDetectionWithDependency(Object d) {}
        @Override protected void doPreBuildWithDependency(Object d) {}
        @Override protected void doPostBuildWithDependency(Object d) {}
    }
}

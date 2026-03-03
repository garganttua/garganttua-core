package com.garganttua.core.reflection.dsl;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.reflection.IAnnotationScanner;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IMethod;
import com.garganttua.core.reflection.IReflection;
import com.garganttua.core.reflection.runtime.RuntimeReflectionProvider;

class ReflectionBuilderTest {

    @AfterEach
    void cleanup() {
        IClass.clearThreadReflection();
        IClass.setReflection(null);
    }

    // ========================================================================
    // 1. Building with a single provider
    // ========================================================================

    @Test
    void buildWithSingleProvider_getClassWorks() throws DslException {
        IReflection reflection = ReflectionBuilder.builder()
                .withProvider(new RuntimeReflectionProvider())
                .build();

        assertNotNull(reflection);

        IClass<String> strClass = reflection.getClass(String.class);
        assertNotNull(strClass);
        assertEquals("java.lang.String", strClass.getName());
    }

    @Test
    void buildWithSingleProvider_forNameWorks() throws Exception {
        IReflection reflection = ReflectionBuilder.builder()
                .withProvider(new RuntimeReflectionProvider())
                .build();

        IClass<?> resolved = reflection.forName("java.util.ArrayList");
        assertNotNull(resolved);
        assertEquals("java.util.ArrayList", resolved.getName());
    }

    @Test
    void buildWithSingleProvider_forNameThrowsForUnknownClass() throws DslException {
        IReflection reflection = ReflectionBuilder.builder()
                .withProvider(new RuntimeReflectionProvider())
                .build();

        assertThrows(ClassNotFoundException.class,
                () -> reflection.forName("com.nonexistent.FakeClass"));
    }

    // ========================================================================
    // 2. Building with provider + scanner
    // ========================================================================

    @Test
    void buildWithProviderAndScanner_bothWork() throws DslException {
        List<String> scannerCalls = new ArrayList<>();
        IAnnotationScanner stubScanner = new StubAnnotationScanner(scannerCalls);

        IReflection reflection = ReflectionBuilder.builder()
                .withProvider(new RuntimeReflectionProvider())
                .withScanner(stubScanner)
                .build();

        // Provider still works
        IClass<Integer> intClass = reflection.getClass(Integer.class);
        assertNotNull(intClass);
        assertEquals("java.lang.Integer", intClass.getName());

        // Scanner delegates correctly (returns empty list from stub)
        IClass.setReflection(reflection);
        IClass<? extends Annotation> annotationType = IClass.getClass(Deprecated.class);
        List<IClass<?>> result = reflection.getClassesWithAnnotation(annotationType);
        assertNotNull(result);
        assertTrue(result.isEmpty());
        assertEquals(1, scannerCalls.size());
        assertEquals("getClassesWithAnnotation", scannerCalls.get(0));
    }

    // ========================================================================
    // 3. Observer pattern
    // ========================================================================

    @Test
    void observer_calledOnBuild() throws DslException {
        AtomicReference<IReflection> observed = new AtomicReference<>();

        ReflectionBuilder.builder()
                .withProvider(new RuntimeReflectionProvider())
                .observer(observed::set)
                .build();

        assertNotNull(observed.get(), "Observer should have been called with the built IReflection");
    }

    @Test
    void observer_addedAfterBuild_calledImmediately() throws DslException {
        AtomicReference<IReflection> observed = new AtomicReference<>();

        IReflectionBuilder builder = ReflectionBuilder.builder()
                .withProvider(new RuntimeReflectionProvider());

        IReflection reflection = builder.build();

        // Add observer after build -- should be called immediately with the cached instance
        builder.observer(observed::set);

        assertNotNull(observed.get(), "Observer added after build should be called immediately");
        assertSame(reflection, observed.get());
    }

    @Test
    void multipleObservers_allCalled() throws DslException {
        List<IReflection> observations = new ArrayList<>();

        ReflectionBuilder.builder()
                .withProvider(new RuntimeReflectionProvider())
                .observer(observations::add)
                .observer(observations::add)
                .build();

        assertEquals(2, observations.size(), "Both observers should have been called");
        assertSame(observations.get(0), observations.get(1),
                "Both observers should receive the same IReflection instance");
    }

    // ========================================================================
    // 4. doBuild() sets IClass.setReflection() automatically
    // ========================================================================

    @Test
    void buildSetsGlobalReflection() throws DslException {
        IReflection reflection = ReflectionBuilder.builder()
                .withProvider(new RuntimeReflectionProvider())
                .build();

        assertSame(reflection, IClass.getReflection(),
                "Builder should auto-set the global IReflection");
    }

    // ========================================================================
    // 5. ThreadLocal reflection takes precedence over global
    // ========================================================================

    @Test
    void threadLocalReflection_takesPrecedenceOverGlobal() throws DslException {
        IReflection global = ReflectionBuilder.builder()
                .withProvider(new RuntimeReflectionProvider())
                .build();

        IReflection threadLocal = ReflectionBuilder.builder()
                .withProvider(new RuntimeReflectionProvider())
                .build();

        IClass.setReflection(global);
        assertSame(global, IClass.getReflection());

        IClass.setThreadReflection(threadLocal);
        assertSame(threadLocal, IClass.getReflection(),
                "Thread-local reflection should take precedence over global");

        IClass.clearThreadReflection();
        assertSame(global, IClass.getReflection(),
                "After clearing thread-local, global should be returned again");
    }

    @Test
    void threadLocalReflection_worksWithoutGlobal() throws DslException {
        IReflection threadLocal = ReflectionBuilder.builder()
                .withProvider(new RuntimeReflectionProvider())
                .build();

        // No global set -- getReflection would throw without thread-local
        IClass.setThreadReflection(threadLocal);
        assertSame(threadLocal, IClass.getReflection());

        // Static factory methods should work through thread-local
        IClass<String> strClass = IClass.getClass(String.class);
        assertNotNull(strClass);
        assertEquals("java.lang.String", strClass.getName());
    }

    // ========================================================================
    // Stub scanner for testing scanner integration
    // ========================================================================

    private static class StubAnnotationScanner implements IAnnotationScanner {

        private final List<String> calls;

        StubAnnotationScanner(List<String> calls) {
            this.calls = calls;
        }

        @Override
        public List<IClass<?>> getClassesWithAnnotation(IClass<? extends Annotation> annotation) {
            calls.add("getClassesWithAnnotation");
            return List.of();
        }

        @Override
        public List<IClass<?>> getClassesWithAnnotation(String packageName, IClass<? extends Annotation> annotation) {
            calls.add("getClassesWithAnnotation");
            return List.of();
        }

        @Override
        public List<IMethod> getMethodsWithAnnotation(IClass<? extends Annotation> annotation) {
            calls.add("getMethodsWithAnnotation");
            return List.of();
        }

        @Override
        public List<IMethod> getMethodsWithAnnotation(String packageName, IClass<? extends Annotation> annotation) {
            calls.add("getMethodsWithAnnotation");
            return List.of();
        }

    }
}

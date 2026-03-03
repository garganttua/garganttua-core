package com.garganttua.core.reflection.constructors;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.garganttua.core.reflection.IReflectionProvider;
import com.garganttua.core.reflection.ReflectionException;
import com.garganttua.core.reflection.runtime.RuntimeClass;
import com.garganttua.core.reflection.runtime.RuntimeReflectionProvider;

public class ConstructorResolverTest {

    private static final IReflectionProvider PROVIDER = new RuntimeReflectionProvider();

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.CONSTRUCTOR)
    public @interface MyInject {}

    public static class DefaultCtorClass {
        public DefaultCtorClass() {}
    }

    public static class MultiCtorClass {
        public final String name;
        public final int value;

        public MultiCtorClass() {
            this("default", 0);
        }

        public MultiCtorClass(String name) {
            this(name, 0);
        }

        public MultiCtorClass(String name, int value) {
            this.name = name;
            this.value = value;
        }
    }

    public static class AnnotatedCtorClass {
        public AnnotatedCtorClass() {}

        @MyInject
        public AnnotatedCtorClass(String name) {}
    }

    public static class NoDefaultCtorClass {
        public NoDefaultCtorClass(String required) {}
    }

    // --- defaultConstructor ---

    @Test
    void testDefaultConstructor() throws ReflectionException {
        ResolvedConstructor<DefaultCtorClass> resolved = ConstructorResolver.defaultConstructor(
                RuntimeClass.of(DefaultCtorClass.class), PROVIDER);

        assertNotNull(resolved);
        assertEquals(0, resolved.parameterCount());
    }

    @Test
    void testDefaultConstructorNotFound() {
        assertThrows(ReflectionException.class, () -> {
            ConstructorResolver.defaultConstructor(RuntimeClass.of(NoDefaultCtorClass.class), PROVIDER);
        });
    }

    // --- constructorByParameterTypes ---

    @Test
    void testConstructorByParameterTypesNoArgs() throws ReflectionException {
        ResolvedConstructor<MultiCtorClass> resolved = ConstructorResolver.constructorByParameterTypes(
                RuntimeClass.of(MultiCtorClass.class), PROVIDER);

        assertNotNull(resolved);
        assertEquals(0, resolved.parameterCount());
    }

    @Test
    void testConstructorByParameterTypesOneArg() throws ReflectionException {
        ResolvedConstructor<MultiCtorClass> resolved = ConstructorResolver.constructorByParameterTypes(
                RuntimeClass.of(MultiCtorClass.class), PROVIDER,
                RuntimeClass.of(String.class));

        assertNotNull(resolved);
        assertEquals(1, resolved.parameterCount());
    }

    @Test
    void testConstructorByParameterTypesTwoArgs() throws ReflectionException {
        ResolvedConstructor<MultiCtorClass> resolved = ConstructorResolver.constructorByParameterTypes(
                RuntimeClass.of(MultiCtorClass.class), PROVIDER,
                RuntimeClass.of(String.class), RuntimeClass.of(int.class));

        assertNotNull(resolved);
        assertEquals(2, resolved.parameterCount());
    }

    @Test
    void testConstructorByParameterTypesNotFound() {
        assertThrows(ReflectionException.class, () -> {
            ConstructorResolver.constructorByParameterTypes(
                    RuntimeClass.of(MultiCtorClass.class), PROVIDER,
                    RuntimeClass.of(double.class));
        });
    }

    // --- constructorByAnnotation ---

    @Test
    void testConstructorByAnnotation() throws ReflectionException {
        ResolvedConstructor<AnnotatedCtorClass> resolved = ConstructorResolver.constructorByAnnotation(
                RuntimeClass.of(AnnotatedCtorClass.class), PROVIDER,
                RuntimeClass.of(MyInject.class));

        assertNotNull(resolved);
        assertEquals(1, resolved.parameterCount());
    }

    @Test
    void testConstructorByAnnotationNotFound() {
        assertThrows(ReflectionException.class, () -> {
            ConstructorResolver.constructorByAnnotation(
                    RuntimeClass.of(DefaultCtorClass.class), PROVIDER,
                    RuntimeClass.of(MyInject.class));
        });
    }

    // --- allConstructors ---

    @Test
    void testAllConstructors() throws ReflectionException {
        List<ResolvedConstructor<MultiCtorClass>> all = ConstructorResolver.allConstructors(
                RuntimeClass.of(MultiCtorClass.class), PROVIDER);

        assertEquals(3, all.size());
    }

    @Test
    void testAllConstructorsSingleCtor() throws ReflectionException {
        List<ResolvedConstructor<DefaultCtorClass>> all = ConstructorResolver.allConstructors(
                RuntimeClass.of(DefaultCtorClass.class), PROVIDER);

        assertEquals(1, all.size());
    }

    // --- null checks ---

    @Test
    void testNullOwnerType() {
        assertThrows(NullPointerException.class, () -> {
            ConstructorResolver.defaultConstructor(null, PROVIDER);
        });
    }

    @Test
    void testNullProvider() {
        assertThrows(NullPointerException.class, () -> {
            ConstructorResolver.defaultConstructor(RuntimeClass.of(DefaultCtorClass.class), null);
        });
    }
}

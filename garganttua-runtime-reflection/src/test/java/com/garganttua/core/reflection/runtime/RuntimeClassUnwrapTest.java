package com.garganttua.core.reflection.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;

import org.junit.jupiter.api.Test;

import com.garganttua.core.reflection.IClass;

/**
 * Regression coverage for hybrid AOT/runtime mixing: RuntimeClass.unwrapClass
 * must accept any IClass whose getType() yields a raw Class<?>, not only
 * RuntimeClass<?>. Otherwise AOTClass (priority 20) leaking into a runtime
 * code path (priority 10) blows up with "Cannot unwrap non-RuntimeClass IClass".
 */
class RuntimeClassUnwrapTest {

    @Test
    void unwrapClass_returns_inner_class_for_RuntimeClass() {
        IClass<?> rc = RuntimeClass.ofUnchecked(String.class);
        assertSame(String.class, RuntimeClass.unwrapClass(rc));
    }

    @Test
    void unwrapClass_returns_class_for_foreign_IClass_with_Class_getType() {
        IClass<?> foreign = fakeIClassWithType(Integer.class);
        assertSame(Integer.class, RuntimeClass.unwrapClass(foreign));
    }

    @Test
    void unwrapClass_null_returns_null() {
        assertNull(RuntimeClass.unwrapClass(null));
    }

    @Test
    void unwrapClass_throws_when_getType_is_not_a_Class() {
        Type notAClass = new WildcardType() {
            @Override public Type[] getUpperBounds() { return new Type[0]; }
            @Override public Type[] getLowerBounds() { return new Type[0]; }
            @Override public String getTypeName() { return "?"; }
        };
        IClass<?> foreign = fakeIClassWithType(notAClass);
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> RuntimeClass.unwrapClass(foreign));
        assertEquals(true, ex.getMessage().contains("non-Class Type"));
    }

    @Test
    void unwrapAnnotationClass_works_for_foreign_IClass() {
        @SuppressWarnings("unchecked")
        IClass<java.lang.annotation.Retention> foreign =
                (IClass<java.lang.annotation.Retention>) fakeIClassWithType(java.lang.annotation.Retention.class);
        assertSame(java.lang.annotation.Retention.class, RuntimeClass.unwrapAnnotationClass(foreign));
    }

    private static IClass<?> fakeIClassWithType(Type type) {
        InvocationHandler handler = (proxy, method, args) -> {
            if ("getType".equals(method.getName()) && method.getParameterCount() == 0) {
                return type;
            }
            if ("toString".equals(method.getName())) return "fakeIClass(" + type + ")";
            if ("hashCode".equals(method.getName())) return System.identityHashCode(proxy);
            if ("equals".equals(method.getName())) return proxy == args[0];
            throw new UnsupportedOperationException(
                    "fakeIClass does not implement " + method.getName());
        };
        Object proxy = Proxy.newProxyInstance(
                RuntimeClassUnwrapTest.class.getClassLoader(),
                new Class<?>[] { IClass.class },
                handler);
        return (IClass<?>) proxy;
    }
}

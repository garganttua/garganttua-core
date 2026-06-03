package com.garganttua.core.aot.reflection;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;

import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IReflection;

/**
 * Test-only helper that installs a minimal {@link IReflection} facade backed by
 * {@link AOTReflectionProvider}.
 *
 * <p>The full {@code IReflection} interface declares ~52 methods, but the AOT
 * descriptors only ever call {@code getClass(Class)}, {@code getClass(Object)},
 * {@code forName(String)} and {@code forName(String, boolean, ClassLoader)} on
 * the global facade (e.g. {@code AOTClass.getSuperclass()} resolves via
 * {@code IClass.forName}). Everything else is routed to a dynamic proxy that
 * throws {@link UnsupportedOperationException} so accidental reliance on
 * unimplemented behaviour surfaces loudly instead of silently passing.</p>
 */
final class TestReflectionSupport {

    private TestReflectionSupport() {
    }

    /**
     * Installs the facade globally on {@link IClass}. Idempotent and safe to
     * call from every test's {@code @BeforeEach}.
     */
    static void installReflection() {
        IClass.setReflection(facade());
    }

    static IReflection facade() {
        AOTReflectionProvider provider = new AOTReflectionProvider();
        InvocationHandler handler = (proxy, method, args) -> {
            String name = method.getName();
            switch (name) {
                case "getClass": {
                    if (args != null && args.length == 1 && args[0] instanceof Class<?> c) {
                        return provider.getClass(c);
                    }
                    if (args != null && args.length == 1 && args[0] != null) {
                        return provider.getClass(args[0].getClass());
                    }
                    break;
                }
                case "forName": {
                    if (args != null && args.length == 1) {
                        return provider.forName((String) args[0]);
                    }
                    if (args != null && args.length == 3) {
                        return provider.forName((String) args[0],
                                (Boolean) args[1], (ClassLoader) args[2]);
                    }
                    break;
                }
                case "supports":
                    return provider.supports((Class<?>) args[0]);
                case "toString":
                    return "TestReflectionFacade";
                case "hashCode":
                    return System.identityHashCode(proxy);
                case "equals":
                    return proxy == args[0];
                default:
                    break;
            }
            throw new UnsupportedOperationException(
                    "TestReflectionSupport facade does not implement " + name);
        };
        return (IReflection) Proxy.newProxyInstance(
                TestReflectionSupport.class.getClassLoader(),
                new Class<?>[] { IReflection.class },
                handler);
    }
}

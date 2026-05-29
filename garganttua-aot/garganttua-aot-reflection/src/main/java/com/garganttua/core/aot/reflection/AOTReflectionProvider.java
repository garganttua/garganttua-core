package com.garganttua.core.aot.reflection;

import com.garganttua.core.aot.commons.AOTRegistry;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IReflectionProvider;

import jakarta.annotation.Priority;

/**
 * AOT implementation of {@link IReflectionProvider}.
 *
 * <p>Resolves classes from the {@link AOTRegistry} singleton. Only supports
 * classes that have been registered by AOT-generated code.</p>
 */
@Priority(20)
public class AOTReflectionProvider implements IReflectionProvider {

    static {
        // Seed AOTRegistry with descriptors for the framework's public
        // infrastructure interfaces — Bootstrap's static init resolves
        // them via IClass.getClass(...) before user code runs, and the
        // consumer-side annotation processor can't see types from JAR
        // dependencies. Without this seed an AOT-only app crashes at
        // `new Bootstrap()`.
        CoreInfrastructureSeed.bootstrap();
    }

    @Override
    public <T> IClass<T> getClass(Class<T> clazz) {
        return AOTRegistry.getInstance().<T>get(clazz.getName())
                .orElseThrow(() -> new IllegalArgumentException(
                        "No AOT descriptor registered for: " + clazz.getName()));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> IClass<T> forName(String className) throws ClassNotFoundException {
        return (IClass<T>) AOTRegistry.getInstance().get(className)
                .orElseThrow(() -> new ClassNotFoundException(
                        "No AOT descriptor registered for: " + className));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> IClass<T> forName(String className, boolean initialize, ClassLoader loader)
            throws ClassNotFoundException {
        return (IClass<T>) AOTRegistry.getInstance().get(className)
                .orElseThrow(() -> new ClassNotFoundException(
                        "No AOT descriptor registered for: " + className));
    }

    @Override
    public boolean supports(Class<?> type) {
        return AOTRegistry.getInstance().contains(type.getName());
    }
}

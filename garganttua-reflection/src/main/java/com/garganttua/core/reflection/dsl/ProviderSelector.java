package com.garganttua.core.reflection.dsl;

import java.util.List;

import com.garganttua.core.diagnostic.Diagnostics;
import com.garganttua.core.diagnostic.IDiagnostic;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IReflectionProvider;

class ProviderSelector implements IReflectionProvider {
    private static final IDiagnostic log = Diagnostics.of(ProviderSelector.class);

    private final List<IReflectionProvider> providers;

    ProviderSelector(List<IReflectionProvider> providers) {
        this.providers = providers;
    }

    IReflectionProvider select(Class<?> type) {
        for (IReflectionProvider provider : providers) {
            if (provider.supports(type)) {
                return provider;
            }
        }
        // No provider claims ownership via supports(). Last resort: hand the
        // type to the highest-priority provider and let its getClass() decide.
        // The AOT provider uses this path to engage its fallback synthesis
        // (type-identity descriptor from the class literal). The runtime
        // provider's supports() returns true universally so this branch only
        // fires when AOT is the sole provider (pure-AOT / native-image).
        if (!providers.isEmpty()) {
            return providers.get(0);
        }
        throw new UnsupportedOperationException(
                "No IReflectionProvider registered. Add a starter (garganttua-starter-aot,"
                + " -runtime, -hybrid, or -native) to your classpath. Failed to resolve: "
                + type);
    }

    @Override
    public <T> IClass<T> getClass(Class<T> clazz) {
        return select(clazz).getClass(clazz);
    }

    @Override
    public <T> IClass<T> forName(String className) throws ClassNotFoundException {
        for (IReflectionProvider provider : providers) {
            try {
                return provider.forName(className);
            } catch (ClassNotFoundException e) {
                log.trace("Provider {} could not find class {}", provider.getClass().getName(), className);
            }
        }
        throw new ClassNotFoundException(className);
    }

    @Override
    public <T> IClass<T> forName(String className, boolean initialize, ClassLoader loader) throws ClassNotFoundException {
        for (IReflectionProvider provider : providers) {
            try {
                return provider.forName(className, initialize, loader);
            } catch (ClassNotFoundException e) {
                log.trace("Provider {} could not find class {}", provider.getClass().getName(), className);
            }
        }
        throw new ClassNotFoundException(className);
    }

    @Override
    public boolean supports(Class<?> type) {
        for (IReflectionProvider provider : providers) {
            if (provider.supports(type)) {
                return true;
            }
        }
        return false;
    }
}

package com.garganttua.core.reflection.dsl;

import java.util.List;

import com.garganttua.core.observability.Logger;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IReflectionProvider;

/**
 * Selects the effective {@link IReflectionProvider} among the registered ones.
 *
 * <p>
 * For a single registered provider it short-circuits to that provider. Otherwise
 * it returns the first provider whose {@link IReflectionProvider#supports(Class)}
 * accepts the type, falling back to the highest-priority provider when none
 * claims ownership. {@code forName} probes providers in order until one resolves
 * the class.
 * </p>
 */
class ProviderSelector implements IReflectionProvider {
    private static final Logger log = Logger.getLogger(ProviderSelector.class);

    private final List<IReflectionProvider> providers;

    ProviderSelector(List<IReflectionProvider> providers) {
        this.providers = providers;
    }

    IReflectionProvider select(Class<?> type) {
        // Single-provider fast path (pure-AOT, pure-runtime, native-image):
        // skip the supports() check entirely — there's nowhere else to go.
        // Whichever provider is registered owns every getClass() call by
        // construction, and supports() can be non-trivial (AOT's checks
        // AOTRegistry containment + isIntrinsic per call).
        int size = providers.size();
        if (size == 1) {
            return providers.get(0);
        }
        for (int i = 0; i < size; i++) {
            IReflectionProvider provider = providers.get(i);
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

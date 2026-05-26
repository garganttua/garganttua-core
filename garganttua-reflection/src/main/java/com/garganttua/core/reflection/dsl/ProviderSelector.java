package com.garganttua.core.reflection.dsl;

import java.util.List;

import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IReflectionProvider;

import lombok.extern.slf4j.Slf4j;

@Slf4j
class ProviderSelector implements IReflectionProvider {

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
        List<String> tried = providers.stream()
                .map(p -> p.getClass().getSimpleName())
                .toList();
        throw new UnsupportedOperationException(
                "No IReflectionProvider supports type: " + type
                + ". Providers tried: " + tried
                + ". Hint: if you only see an AOT provider, ensure 'garganttua-runtime-reflection' is on the"
                + " runtime classpath (it is a 'runtime' scope transitive of 'garganttua-bootstrap'), or"
                + " pre-register " + type.getName() + " in your AOTRegistry.");
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
                log.atTrace().log("Provider {} could not find class {}", provider.getClass().getName(), className);
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
                log.atTrace().log("Provider {} could not find class {}", provider.getClass().getName(), className);
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

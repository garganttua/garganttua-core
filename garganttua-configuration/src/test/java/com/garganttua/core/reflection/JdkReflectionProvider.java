package com.garganttua.core.reflection;

public class JdkReflectionProvider implements IReflectionProvider {

    @Override
    public <T> IClass<T> getClass(Class<T> clazz) {
        return JdkClass.of(clazz);
    }

    @Override
    public <T> IClass<T> forName(String className) throws ClassNotFoundException {
        return JdkClass.ofUnchecked(Class.forName(className));
    }

    @Override
    public <T> IClass<T> forName(String name, boolean initialize, ClassLoader loader) throws ClassNotFoundException {
        return JdkClass.ofUnchecked(Class.forName(name, initialize, loader));
    }

    @Override
    public boolean supports(Class<?> type) {
        return true;
    }
}

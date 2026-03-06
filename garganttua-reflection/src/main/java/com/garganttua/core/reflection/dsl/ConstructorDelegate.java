package com.garganttua.core.reflection.dsl;

import java.util.Optional;

import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IConstructor;
import com.garganttua.core.reflection.IMethodReturn;
import com.garganttua.core.reflection.IReflectionProvider;
import com.garganttua.core.reflection.ReflectionException;
import com.garganttua.core.reflection.TypeUtils;
import com.garganttua.core.reflection.constructors.ConstructorInvoker;
import com.garganttua.core.reflection.constructors.ResolvedConstructor;

import lombok.extern.slf4j.Slf4j;

@Slf4j
class ConstructorDelegate {

    private final IReflectionProvider provider;

    ConstructorDelegate(IReflectionProvider provider) {
        this.provider = provider;
    }

    Optional<IConstructor<?>> findConstructor(IClass<?> clazz) {
        log.atTrace().log("Finding no-param constructor for class: {}", clazz.getName());
        try {
            IConstructor<?> ctor = clazz.getDeclaredConstructor();
            log.atDebug().log("Found no-param constructor for class: {}", clazz.getName());
            return Optional.of(ctor);
        } catch (NoSuchMethodException | SecurityException e) {
            log.atDebug().log("No no-param constructor found for class: {}", clazz.getName());
            return Optional.empty();
        }
    }

    Optional<IConstructor<?>> findConstructor(IClass<?> clazz, IClass<?>... parameterTypes) {
        log.atTrace().log("Finding constructor for class: {} with {} parameters", clazz.getName(), parameterTypes.length);
        for (IConstructor<?> ctor : clazz.getDeclaredConstructors()) {
            IClass<?>[] pts = ctor.getParameterTypes();
            if (pts.length != parameterTypes.length) {
                continue;
            }

            boolean ok = true;
            for (int i = 0; i < pts.length; i++) {
                IClass<?> formal = pts[i];
                IClass<?> actual = parameterTypes[i];

                if (actual == null) {
                    if (formal.isPrimitive()) {
                        ok = false;
                        break;
                    }
                } else {
                    if (!TypeUtils.isAssignable(formal, actual)) {
                        ok = false;
                        break;
                    }
                }
            }
            if (ok) {
                log.atDebug().log("Found matching constructor for class: {}", clazz.getName());
                return Optional.of(ctor);
            }
        }
        log.atDebug().log("No matching constructor found for class: {}", clazz.getName());
        return Optional.empty();
    }

    <T> T newInstance(IClass<T> clazz) throws ReflectionException {
        return newInstance(clazz, false);
    }

    <T> T newInstance(IClass<T> clazz, boolean force) throws ReflectionException {
        log.atTrace().log("Instantiating new object of class: {} with no params, force={}", clazz.getName(), force);
        Optional<IConstructor<?>> optCtor = findConstructor(clazz);
        if (optCtor.isEmpty()) {
            log.atError().log("Class {} does not have constructor with no params", clazz.getName());
            throw new ReflectionException("Class " + clazz.getSimpleName() + " does not have constructor with no params");
        }
        return invokeConstructor(clazz, (IConstructor<T>) optCtor.get(), force);
    }

    <T> T newInstance(IClass<T> clazz, Object... args) throws ReflectionException {
        return newInstance(clazz, false, args);
    }

    <T> T newInstance(IClass<T> clazz, boolean force, Object... args) throws ReflectionException {
        log.atTrace().log("Instantiating new object of class: {} with {} params, force={}", clazz.getName(),
                args != null ? args.length : 0, force);
        if (args == null || args.length == 0) {
            return newInstance(clazz, force);
        }

        IClass<?>[] paramTypes = new IClass<?>[args.length];
        for (int i = 0; i < args.length; i++) {
            paramTypes[i] = provider.getClass(args[i].getClass());
        }

        Optional<IConstructor<?>> optCtor = findConstructor(clazz, paramTypes);
        if (optCtor.isEmpty()) {
            log.atError().log("Class {} does not have constructor with provided params", clazz.getName());
            throw new ReflectionException(
                    "Class " + clazz.getSimpleName() + " does not have constructor with params " + args);
        }
        return invokeConstructor(clazz, (IConstructor<T>) optCtor.get(), force, args);
    }

    private <T> T invokeConstructor(IClass<T> clazz, IConstructor<T> ctor, boolean force, Object... args)
            throws ReflectionException {
        var resolved = new ResolvedConstructor<>(ctor);
        var invoker = new ConstructorInvoker<>(resolved, force);
        IMethodReturn<T> result = invoker.newInstance(args);
        if (result.hasException()) {
            Throwable ex = result.getException();
            log.atError().log("Error instantiating object of class: {}", clazz.getName(), ex);
            throw new ReflectionException("Error creating new instance of type " + clazz.getSimpleName(), ex);
        }
        log.atDebug().log("Successfully instantiated new object of class: {}", clazz.getName());
        return result.single();
    }

}

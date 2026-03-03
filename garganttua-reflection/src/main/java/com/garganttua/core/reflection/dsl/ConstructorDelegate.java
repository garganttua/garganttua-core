package com.garganttua.core.reflection.dsl;

import java.lang.reflect.InvocationTargetException;
import java.util.Optional;

import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IConstructor;
import com.garganttua.core.reflection.IReflectionProvider;
import com.garganttua.core.reflection.ReflectionException;
import com.garganttua.core.reflection.TypeUtils;

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
        log.atTrace().log("Instantiating new object of class: {} with no params", clazz.getName());
        Optional<IConstructor<?>> optCtor = findConstructor(clazz);
        if (optCtor.isPresent()) {
            IConstructor<?> ctor = optCtor.get();
            ctor.setAccessible(true);
            try {
                T result = (T) ctor.newInstance();
                log.atDebug().log("Successfully instantiated new object of class: {}", clazz.getName());
                return result;
            } catch (InstantiationException | IllegalAccessException | IllegalArgumentException
                    | InvocationTargetException e) {
                log.atError().log("Error instantiating object of class: {}", clazz.getName(), e);
                throw new ReflectionException(e);
            }
        }
        log.atError().log("Class {} does not have constructor with no params", clazz.getName());
        throw new ReflectionException("Class " + clazz.getSimpleName() + " does not have constructor with no params");
    }

    <T> T newInstance(IClass<T> clazz, Object... args) throws ReflectionException {
        log.atTrace().log("Instantiating new object of class: {} with {} params", clazz.getName(),
                args != null ? args.length : 0);
        if (args == null || args.length == 0) {
            return newInstance(clazz);
        }

        IClass<?>[] paramTypes = new IClass<?>[args.length];
        for (int i = 0; i < args.length; i++) {
            paramTypes[i] = provider.getClass(args[i].getClass());
        }

        Optional<IConstructor<?>> optCtor = findConstructor(clazz, paramTypes);
        if (optCtor.isPresent()) {
            IConstructor<?> ctor = optCtor.get();
            ctor.setAccessible(true);
            try {
                T result = (T) ctor.newInstance(args);
                log.atDebug().log("Successfully instantiated new object of class: {}", clazz.getName());
                return result;
            } catch (InstantiationException | IllegalAccessException | IllegalArgumentException
                    | InvocationTargetException e) {
                log.atError().log("Error instantiating object of class: {}", clazz.getName(), e);
                throw new ReflectionException(e);
            }
        }

        log.atError().log("Class {} does not have constructor with provided params", clazz.getName());
        throw new ReflectionException(
                "Class " + clazz.getSimpleName() + " does not have constructor with params " + args);
    }

}

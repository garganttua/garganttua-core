package com.garganttua.core.reflection.dsl;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IMethod;
import com.garganttua.core.reflection.IMethodReturn;
import com.garganttua.core.reflection.IReflectionProvider;
import com.garganttua.core.reflection.ObjectAddress;
import com.garganttua.core.reflection.ReflectionException;
import com.garganttua.core.reflection.methods.MethodInvoker;
import com.garganttua.core.reflection.methods.MethodResolver;
import com.garganttua.core.reflection.methods.ResolvedMethod;

import lombok.extern.slf4j.Slf4j;

@Slf4j
class MethodDelegate {

    private final IReflectionProvider provider;

    MethodDelegate(IReflectionProvider provider) {
        this.provider = provider;
    }

    Optional<IMethod> findMethod(IClass<?> clazz, String methodName) {
        log.atTrace().log("Finding method {} in class: {}", methodName, clazz.getName());
        for (IMethod m : clazz.getDeclaredMethods()) {
            if (m.getName().equals(methodName)) {
                return Optional.of(m);
            }
        }
        if (clazz.getSuperclass() != null) {
            return findMethod(clazz.getSuperclass(), methodName);
        }
        return Optional.empty();
    }

    List<IMethod> findMethods(IClass<?> clazz, String methodName) {
        log.atTrace().log("Finding all methods named {} in class: {}", methodName, clazz.getName());
        List<IMethod> methods = new ArrayList<>();
        HashSet<String> seenSignatures = new HashSet<>();

        for (IMethod m : clazz.getDeclaredMethods()) {
            if (m.getName().equals(methodName)) {
                String sig = buildMethodSignature(m);
                if (seenSignatures.add(sig)) {
                    methods.add(m);
                }
            }
        }

        if (clazz.getSuperclass() != null) {
            for (IMethod m : findMethods(clazz.getSuperclass(), methodName)) {
                String sig = buildMethodSignature(m);
                if (seenSignatures.add(sig)) {
                    methods.add(m);
                }
            }
        }

        return methods;
    }

    Optional<IMethod> findMethodAnnotatedWith(IClass<?> clazz, IClass<? extends Annotation> annotation) {
        log.atTrace().log("Finding method annotated with {} in class: {}", annotation.getName(), clazz.getName());
        for (IMethod m : clazz.getDeclaredMethods()) {
            if (m.isAnnotationPresent(annotation)) {
                return Optional.of(m);
            }
        }
        if (clazz.getSuperclass() != null) {
            return findMethodAnnotatedWith(clazz.getSuperclass(), annotation);
        }
        return Optional.empty();
    }

    Optional<IMethod> resolveMethod(IClass<?> ownerType, String methodName) throws ReflectionException {
        try {
            ResolvedMethod resolved = MethodResolver.methodByName(ownerType, provider, methodName);
            return Optional.of(resolved);
        } catch (ReflectionException e) {
            return Optional.empty();
        }
    }

    Optional<IMethod> resolveMethod(IClass<?> ownerType, String methodName, IClass<?> returnType,
            IClass<?>... parameterTypes) throws ReflectionException {
        try {
            ResolvedMethod resolved = MethodResolver.methodByName(ownerType, provider, methodName, returnType, parameterTypes);
            return Optional.of(resolved);
        } catch (ReflectionException e) {
            return Optional.empty();
        }
    }

    Optional<IMethod> resolveMethod(IClass<?> ownerType, ObjectAddress methodAddress) throws ReflectionException {
        try {
            ResolvedMethod resolved = MethodResolver.methodByAddress(ownerType, provider, methodAddress);
            return Optional.of(resolved);
        } catch (ReflectionException e) {
            return Optional.empty();
        }
    }

    Optional<IMethod> resolveMethod(IClass<?> ownerType, ObjectAddress methodAddress, IClass<?> returnType,
            IClass<?>... parameterTypes) throws ReflectionException {
        try {
            ResolvedMethod resolved = MethodResolver.methodByAddress(ownerType, provider, methodAddress, returnType, parameterTypes);
            return Optional.of(resolved);
        } catch (ReflectionException e) {
            return Optional.empty();
        }
    }

    <R> R invokeMethod(Object object, IMethod method, IClass<R> returnType, Object... args) throws ReflectionException {
        return invokeMethod(object, method, returnType, false, args);
    }

    @SuppressWarnings("unchecked")
    <R> R invokeMethod(Object object, IMethod method, IClass<R> returnType, boolean force, Object... args)
            throws ReflectionException {
        IClass<?> ownerType = provider.getClass(object.getClass());
        ResolvedMethod resolved = MethodResolver.methodByName(ownerType, provider, method.getName(), returnType,
                method.getParameterTypes());
        var invoker = new MethodInvoker<>(resolved, force);
        IMethodReturn<R> result = (IMethodReturn<R>) invoker.invoke(object, args);
        if (result.hasException()) {
            throw new ReflectionException(
                    "Cannot invoke method " + method.getName() + " of object " + object.getClass().getName(),
                    result.getException());
        }
        return result.single();
    }

    <R> R invokeMethod(Object object, String methodName, IClass<R> returnType, Object... args)
            throws ReflectionException {
        return invokeMethod(object, methodName, returnType, false, args);
    }

    @SuppressWarnings("unchecked")
    <R> R invokeMethod(Object object, String methodName, IClass<R> returnType, boolean force, Object... args)
            throws ReflectionException {
        IClass<?> ownerType = provider.getClass(object.getClass());
        IClass<?>[] paramTypes = new IClass<?>[args != null ? args.length : 0];
        if (args != null) {
            for (int i = 0; i < args.length; i++) {
                paramTypes[i] = args[i] != null ? provider.getClass(args[i].getClass()) : IClass.getClass(Object.class);
            }
        }
        ResolvedMethod resolved = MethodResolver.methodByName(ownerType, provider, methodName, returnType, paramTypes);
        var invoker = new MethodInvoker<>(resolved, force);
        IMethodReturn<R> result = (IMethodReturn<R>) invoker.invoke(object, args);
        if (result.hasException()) {
            throw new ReflectionException(
                    "Cannot invoke method " + methodName + " of object " + object.getClass().getName(),
                    result.getException());
        }
        return result.single();
    }

    <R> IMethodReturn<R> invokeDeep(Object object, ObjectAddress address, IClass<R> returnType,
            IClass<?>[] paramTypes, Object... args)
            throws ReflectionException {
        return invokeDeep(object, address, returnType, false, paramTypes, args);
    }

    @SuppressWarnings("unchecked")
    <R> IMethodReturn<R> invokeDeep(Object object, ObjectAddress address, IClass<R> returnType, boolean force,
            IClass<?>[] paramTypes, Object... args)
            throws ReflectionException {
        IClass<?> ownerType = provider.getClass(object.getClass());
        ResolvedMethod resolvedMethod = MethodResolver.methodByAddress(ownerType, provider, address, returnType, paramTypes);
        return (IMethodReturn<R>) new MethodInvoker<>(resolvedMethod, force).invoke(object, args);
    }

    private static String buildMethodSignature(IMethod method) {
        StringBuilder sig = new StringBuilder(method.getName());
        sig.append("(");
        IClass<?>[] paramTypes = method.getParameterTypes();
        for (int i = 0; i < paramTypes.length; i++) {
            if (i > 0) sig.append(",");
            sig.append(paramTypes[i].getName());
        }
        sig.append(")");
        return sig.toString();
    }
}

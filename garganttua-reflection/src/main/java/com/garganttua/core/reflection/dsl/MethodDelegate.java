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
        method.setAccessible(true);
        try {
            return (R) method.invoke(object, args);
        } catch (Exception e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            throw new ReflectionException(
                    "Cannot invoke method " + method.getName() + " of object " + object.getClass().getName(),
                    cause);
        }
    }

    <R> R invokeMethod(Object object, String methodName, IClass<R> returnType, Object... args)
            throws ReflectionException {
        IClass<?> objectClass = provider.getClass(object.getClass());
        Optional<IMethod> optMethod = findMethod(objectClass, methodName);
        if (optMethod.isEmpty()) {
            throw new ReflectionException(
                    "Method " + methodName + " not found in class " + object.getClass().getName());
        }
        return invokeMethod(object, optMethod.get(), returnType, args);
    }

    <R> IMethodReturn<R> invokeDeep(Object object, ObjectAddress address, IClass<R> returnType,
            IClass<?>[] paramTypes, Object... args)
            throws ReflectionException {
        IClass<?> ownerType = provider.getClass(object.getClass());
        ResolvedMethod resolvedMethod = MethodResolver.methodByAddress(ownerType, provider, address, returnType, paramTypes);
        return (IMethodReturn<R>) new MethodInvoker<>(resolvedMethod).invoke(object, args);
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

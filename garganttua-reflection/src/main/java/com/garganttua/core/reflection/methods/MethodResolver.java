package com.garganttua.core.reflection.methods;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import com.garganttua.core.reflection.IObjectQuery;
import com.garganttua.core.reflection.ObjectAddress;
import com.garganttua.core.reflection.ReflectionException;
import com.garganttua.core.reflection.query.ObjectQueryFactory;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MethodResolver {

        public static ResolvedMethod methodByName(Class<?> ownerType, String methodName, Class<?> returnType,
                        Class<?>... parameterTypes) throws ReflectionException {
                log.atTrace().log("[methodByName] Start: methodName={}, ownerType={}", methodName, ownerType);

                IObjectQuery<?> query = ObjectQueryFactory.objectQuery(ownerType);

                List<ResolvedMethod> methods = query.addresses(methodName).stream()
                                .flatMap(a -> query.findAll(a).stream().map(m -> new ResolvedMethod(a, m)).toList()
                                                .stream())
                                .toList();

                List<ResolvedMethod> found = methods.stream()
                                .filter(m -> m.matches(ownerType, returnType, parameterTypes)).toList();

                if (found.size() > 1) {
                        log.atError().log(
                                        "[methodByName] Multiple methods found matching signature for method {} in ownertype {}",
                                        methodName, ownerType.getName());
                        throw new ReflectionException("Multiple overloads of method " + methodName + " in ownertype "
                                        + ownerType.getName() + " match the specified signature (returnType="
                                        + returnType + ", parameterTypes=" + Arrays.toString(parameterTypes) + ")");
                }
                if (found.isEmpty()) {
                        log.atError().log(
                                        "[methodByName] No method found matching signature for method {} in ownertype {}",
                                        methodName, ownerType.getName());
                        throw new ReflectionException("No overload of method " + methodName + " in ownertype "
                                        + ownerType.getName() + " matches the specified signature (returnType="
                                        + returnType + ", parameterTypes=" + Arrays.toString(parameterTypes) + ")");
                }
                return found.get(0);

        }

        public static ResolvedMethod methodByName(Class<?> ownerType, String methodName)
                        throws ReflectionException {
                IObjectQuery<?> query = ObjectQueryFactory.objectQuery(ownerType);

                List<ResolvedMethod> found = query.addresses(methodName).stream()
                                .flatMap(a -> query.findAll(a).stream().map(m -> new ResolvedMethod(a, m)).toList()
                                                .stream())
                                .toList();

                if (found.size() > 1) {
                        log.atError().log(
                                        "[methodByName] Multiple methods found matching signature for method {} in ownertype {}",
                                        methodName, ownerType.getName());
                        throw new ReflectionException("Multiple overloads of method " + methodName + " in ownertype "
                                        + ownerType.getName());
                }
                if (found.isEmpty()) {
                        log.atError().log(
                                        "[methodByName] No method found matching signature for method {} in ownertype {}",
                                        methodName, ownerType.getName());
                        throw new ReflectionException(
                                        "No overload of method " + methodName + " in ownertype " + ownerType.getName());
                }
                return found.get(0);
        }

        public static ResolvedMethod methodByMethod(Class<?> ownerType, Method method) throws ReflectionException {
                return methodByName(ownerType, method.getName(), method.getReturnType(),
                                method.getParameterTypes());
        }

        public static ResolvedMethod methodByAddress(Class<?> ownerType, ObjectAddress methodAddress)
                        throws ReflectionException {
                log.atTrace().log("[methodByAddress] Start: methodAddress={}, ownerType={}", methodAddress,
                                ownerType);
                return MethodResolver.methodByName(ownerType, methodAddress.getLastElement());
        }

        public static ResolvedMethod methodByAddress(Class<?> ownerType, ObjectAddress methodAddress,
                        Class<?> returnType, Class<?>... parameterTypes)
                        throws ReflectionException {
                return MethodResolver.methodByName(ownerType, methodAddress.getLastElement(), returnType,
                                parameterTypes);
        }

}
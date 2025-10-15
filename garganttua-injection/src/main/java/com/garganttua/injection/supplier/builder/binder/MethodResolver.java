package com.garganttua.injection.supplier.builder.binder;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import com.garganttua.injection.DiException;
import com.garganttua.reflection.GGObjectAddress;
import com.garganttua.reflection.GGReflectionException;
import com.garganttua.reflection.query.GGObjectQueryFactory;
import com.garganttua.reflection.query.IGGObjectQuery;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MethodResolver {

        public static GGObjectAddress methodByName(String methodName, IGGObjectQuery objectQuery, Class<?> entityClass)
                        throws DiException {
                log.atTrace().log("[methodByName] Start: methodName={}, entityClass={}", methodName, entityClass);
                return MethodResolver.methodByName(methodName, objectQuery, entityClass, null);
        }

        public static GGObjectAddress methodByMethod(Method method, Class<?> entityClass) throws DiException {
                log.atTrace().log("[methodByMethod] Start: method={}, entityClass={}", method, entityClass);
                return MethodResolver.methodByMethod(method, entityClass, null);
        }

        public static GGObjectAddress methodByAddress(GGObjectAddress methodAddress, IGGObjectQuery objectQuery,
                        Class<?> entityClass) throws DiException {
                log.atTrace().log("[methodByAddress] Start: methodAddress={}, entityClass={}", methodAddress,
                                entityClass);
                return MethodResolver.methodByAddress(methodAddress, objectQuery, entityClass, null);
        }

        public static GGObjectAddress methodByName(String methodName, IGGObjectQuery objectQuery, Class<?> entityClass,
                        Class<?> returnType, Class<?>... parameterTypes) throws DiException {
                log.atDebug().log(
                                "[methodByName] Resolving: methodName={}, returnType={}, parameterTypes={}, entityClass={}",
                                methodName, returnType, Arrays.toString(parameterTypes), entityClass);

                Objects.requireNonNull(methodName, "Method name cannot be null");
                Objects.requireNonNull(objectQuery, "Object query cannot be null");
                Objects.requireNonNull(entityClass, "Entity class cannot be null");

                try {
                        GGObjectAddress address = objectQuery.address(methodName);
                        log.atTrace().log("[methodByName] Resolved GGObjectAddress={} for methodName={}", address,
                                        methodName);

                        if (address == null) {
                                log.atWarn().log("[methodByName] Method {} not found in entity {}", methodName,
                                                entityClass.getName());
                                throw new DiException(
                                                "Method " + methodName + " not found in entity "
                                                                + entityClass.getName());
                        }

                        return MethodResolver.methodByAddress(address, objectQuery, entityClass, returnType,
                                        parameterTypes);
                } catch (GGReflectionException e) {
                        log.atError().log("[methodByName] Reflection error resolving method {} in entity {}",
                                        methodName,
                                        entityClass.getName(), e);
                        throw new DiException(
                                        "Method " + methodName + " not found in entity " + entityClass.getName(), e);
                }
        }

        public static GGObjectAddress methodByMethod(Method method, Class<?> entityClass, Class<?> returnType,
                        Class<?>... parameterTypes) throws DiException {
                log.atDebug().log(
                                "[methodByMethod] Resolving: method={}, returnType={}, parameterTypes={}, entityClass={}",
                                method, returnType, Arrays.toString(parameterTypes), entityClass);

                Objects.requireNonNull(method, "Method cannot be null");
                Objects.requireNonNull(entityClass, "Entity class cannot be null");

                String methodName = method.getName();

                try {
                        IGGObjectQuery query = GGObjectQueryFactory.objectQuery(entityClass);
                        GGObjectAddress address = methodByName(methodName, query, entityClass, returnType,
                                        parameterTypes);

                        List<Object> struct = query.find(address);
                        log.atTrace().log("[methodByMethod] Object query returned structure: {}", struct);

                        Object leaf = struct.getLast();
                        log.atTrace().log("[methodByMethod] Leaf object resolved: {}", leaf);

                        if (!(leaf instanceof Method)) {
                                log.atError().log("[methodByMethod] Leaf object {} is not a Method", leaf);
                                throw new DiException(
                                                "Method " + methodName + " in entity " + entityClass.getName()
                                                                + " does not match the provided Method object");
                        }

                        Method methodFound = (Method) leaf;

                        if (!methodFound.equals(method)) {
                                log.atError().log(
                                                "[methodByMethod] Method {} in entity {} does not match the provided Method object",
                                                methodName, entityClass.getName());
                                throw new DiException(
                                                "Method " + methodName + " in entity " + entityClass.getName()
                                                                + " does not match the provided Method object");
                        }

                        log.atInfo().log("[methodByMethod] Successfully resolved method {} in entity {}",
                                        method.getName(),
                                        entityClass.getName());
                        return address;

                } catch (SecurityException | GGReflectionException e) {
                        log.atError().log("[methodByMethod] Error resolving method {} in entity {}", method.getName(),
                                        entityClass.getName(), e);
                        throw new DiException(e.getMessage(), e);
                }
        }

        public static GGObjectAddress methodByAddress(GGObjectAddress methodAddress, IGGObjectQuery objectQuery,
                        Class<?> entityClass, Class<?> returnType, Class<?>... parameterTypes) throws DiException {
                log.atDebug().log(
                                "[methodByAddress] Resolving: methodAddress={}, returnType={}, parameterTypes={}, entityClass={}",
                                methodAddress, returnType, Arrays.toString(parameterTypes), entityClass);

                Objects.requireNonNull(methodAddress, "Method address cannot be null");
                Objects.requireNonNull(objectQuery, "Object query cannot be null");
                Objects.requireNonNull(entityClass, "Entity class cannot be null");

                try {
                        List<Object> struct = objectQuery.find(methodAddress);
                        log.atTrace().log("[methodByAddress] Object query returned structure: {}", struct);

                        Object leaf = struct.getLast();
                        log.atTrace().log("[methodByAddress] Leaf object resolved: {}", leaf);

                        if (!(leaf instanceof Method)) {
                                log.atWarn().log("[methodByAddress] Leaf object {} is not a Method", leaf);
                                throw new DiException(
                                                "Method " + methodAddress + " not found in entity "
                                                                + entityClass.getName());
                        }

                        Method method = (Method) leaf;
                        validateSignature(method, returnType, parameterTypes, entityClass);

                        log.atInfo().log("[methodByAddress] Successfully resolved method {} in entity {}",
                                        method.getName(),
                                        entityClass.getName());
                        return methodAddress;

                } catch (GGReflectionException e) {
                        log.atError().log("[methodByAddress] Reflection error resolving method {} in entity {}",
                                        methodAddress,
                                        entityClass.getName(), e);
                        throw new DiException(e.getMessage(), e);
                }
        }

        private static void validateSignature(Method method, Class<?> returnType, Class<?>[] parameterTypes,
                        Class<?> entityClass) throws DiException {
                if (returnType != null && !returnType.isAssignableFrom(method.getReturnType())) {
                        log.atWarn().log(
                                        "[validateSignature] Method {} in entity {} has return type {} but expected {}",
                                        method.getName(), entityClass.getName(), method.getReturnType(), returnType);
                        throw new DiException(
                                        "Method " + method.getName() + " in entity " + entityClass.getName()
                                                        + " does not return type " + returnType.getName());
                }

                if (parameterTypes != null && parameterTypes.length > 0) {
                        Class<?>[] actualParams = method.getParameterTypes();
                        if (actualParams.length != parameterTypes.length) {
                                log.atWarn().log(
                                                "[validateSignature] Method {} in entity {} has {} parameters but expected {}",
                                                method.getName(), entityClass.getName(), actualParams.length,
                                                parameterTypes.length);
                                throw new DiException(
                                                "Method " + method.getName() + " in entity " + entityClass.getName()
                                                                + " has " + actualParams.length
                                                                + " parameters but expected " + parameterTypes.length);
                        }
                        for (int i = 0; i < actualParams.length; i++) {
                                if (!actualParams[i].isAssignableFrom(parameterTypes[i])) {
                                        log.atWarn().log(
                                                        "[validateSignature] Parameter {} of method {} in entity {} has type {} but expected {}",
                                                        i, method.getName(), entityClass.getName(), actualParams[i],
                                                        parameterTypes[i]);
                                        throw new DiException(
                                                        "Parameter " + i + " of method " + method.getName()
                                                                        + " in entity "
                                                                        + entityClass.getName() + " has type "
                                                                        + actualParams[i].getName()
                                                                        + " but expected "
                                                                        + parameterTypes[i].getName());
                                }
                        }
                }
        }
}
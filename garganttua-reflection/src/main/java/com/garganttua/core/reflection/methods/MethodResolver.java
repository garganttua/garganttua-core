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

        public static ObjectAddress methodByName(String methodName, IObjectQuery<?> objectQuery, Class<?> entityClass)
                        throws ReflectionException {
                log.atTrace().log("[methodByName] Start: methodName={}, entityClass={}", methodName, entityClass);
                return MethodResolver.methodByName(methodName, objectQuery, entityClass, null);
        }

        public static ObjectAddress methodByMethod(Method method, Class<?> entityClass) throws ReflectionException {
                log.atTrace().log("[methodByMethod] Start: method={}, entityClass={}", method, entityClass);
                return MethodResolver.methodByMethod(method, entityClass, null);
        }

        public static ObjectAddress methodByAddress(ObjectAddress methodAddress, IObjectQuery<?> objectQuery,
                        Class<?> entityClass) throws ReflectionException {
                log.atTrace().log("[methodByAddress] Start: methodAddress={}, entityClass={}", methodAddress,
                                entityClass);
                return MethodResolver.methodByAddress(methodAddress, objectQuery, entityClass, null);
        }

        public static ObjectAddress methodByName(String methodName, IObjectQuery<?> objectQuery, Class<?> entityClass,
                        Class<?> returnType, Class<?>... parameterTypes) throws ReflectionException {
                log.atDebug().log(
                                "[methodByName] Resolving: methodName={}, returnType={}, parameterTypes={}, entityClass={}",
                                methodName, returnType, Arrays.toString(parameterTypes), entityClass);

                Objects.requireNonNull(methodName, "Method name cannot be null");
                Objects.requireNonNull(objectQuery, "Object query cannot be null");
                Objects.requireNonNull(entityClass, "Entity class cannot be null");

                try {
                        // Get the address for this method name
                        ObjectAddress address = objectQuery.address(methodName);
                        log.atTrace().log("[methodByName] Resolved ObjectAddress={} for methodName={}", address,
                                        methodName);

                        if (address == null) {
                                log.atWarn().log("[methodByName] Method {} not found in entity {}", methodName,
                                                entityClass.getName());
                                throw new ReflectionException(
                                                "Method " + methodName + " not found in entity "
                                                                + entityClass.getName());
                        }

                        return MethodResolver.methodByAddress(address, objectQuery, entityClass, returnType,
                                        parameterTypes);
                } catch (ReflectionException e) {
                        log.atError().log("[methodByName] Reflection error resolving method {} in entity {}",
                                        methodName,
                                        entityClass.getName(), e);
                        throw new ReflectionException(
                                        "Method " + methodName + " not found in entity " + entityClass.getName(), e);
                }
        }

        public static ObjectAddress methodByMethod(Method method, Class<?> entityClass, Class<?> returnType,
                        Class<?>... parameterTypes) throws ReflectionException {
                log.atDebug().log(
                                "[methodByMethod] Resolving: method={}, returnType={}, parameterTypes={}, entityClass={}",
                                method, returnType, Arrays.toString(parameterTypes), entityClass);

                Objects.requireNonNull(method, "Method cannot be null");
                Objects.requireNonNull(entityClass, "Entity class cannot be null");

                String methodName = method.getName();

                try {
                        IObjectQuery<?> query = ObjectQueryFactory.objectQuery(entityClass);

                        // Get the address for this method name
                        ObjectAddress address = query.address(methodName);
                        log.atTrace().log("[methodByMethod] Resolved ObjectAddress={} for methodName={}",
                                        address, methodName);

                        if (address == null) {
                                log.atWarn().log("[methodByMethod] Method {} not found in entity {}", methodName,
                                                entityClass.getName());
                                throw new ReflectionException(
                                                "Method " + methodName + " not found in entity "
                                                                + entityClass.getName());
                        }

                        // Use findAll to get all method overloads with this name
                        List<List<Object>> struct = query.findAll(address);
                        log.atTrace().log("[methodByMethod] Object query returned {} element(s)", struct.size());

                        if (struct.isEmpty()) {
                                log.atWarn().log("[methodByMethod] No methods found for {}", methodName);
                                throw new ReflectionException(
                                                "Method " + methodName + " not found in entity "
                                                                + entityClass.getName());
                        }

                        // Find the exact matching method among all overloads
                        for (List<Object> obj : struct) {
                                if (obj.get(0) instanceof Method candidate) {
                                        if (candidate.equals(method)) {
                                                log.atInfo().log(
                                                                "[methodByMethod] Found exact match for method {} in entity {}",
                                                                method.getName(), entityClass.getName());
                                                return address;
                                        }
                                }
                        }

                        // Method not found
                        log.atError().log(
                                        "[methodByMethod] Method {} in entity {} does not match the provided Method object",
                                        methodName, entityClass.getName());
                        throw new ReflectionException(
                                        "Method " + methodName + " in entity " + entityClass.getName()
                                                        + " does not match the provided Method object");

                } catch (SecurityException | ReflectionException e) {
                        log.atError().log("[methodByMethod] Error resolving method {} in entity {}", method.getName(),
                                        entityClass.getName(), e);
                        throw new ReflectionException(e.getMessage(), e);
                }
        }

        public static ObjectAddress methodByAddress(ObjectAddress methodAddress, IObjectQuery<?> objectQuery,
                        Class<?> entityClass, Class<?> returnType, Class<?>... parameterTypes)
                        throws ReflectionException {
                log.atDebug().log(
                                "[methodByAddress] Resolving: methodAddress={}, returnType={}, parameterTypes={}, entityClass={}",
                                methodAddress, returnType, Arrays.toString(parameterTypes), entityClass);

                Objects.requireNonNull(methodAddress, "Method address cannot be null");
                Objects.requireNonNull(objectQuery, "Object query cannot be null");
                Objects.requireNonNull(entityClass, "Entity class cannot be null");

                try {
                        // Use findAll to get all possible methods (including overloads)
                        List<List<Object>> methodPaths = objectQuery.findAll(methodAddress);
                        log.atTrace().log("[methodByAddress] Object query returned methodPaths with {} element(s)",
                                        methodPaths.size());

                        if (methodPaths.isEmpty()) {
                                log.atWarn().log("[methodByAddress] No methods found for address {}", methodAddress);
                                throw new ReflectionException(
                                                "Method " + methodAddress + " not found in entity "
                                                                + entityClass.getName());
                        }

                        // If we have multiple methods (overloads), find the best match
                        Method method;
                        if (methodPaths.size() > 1) {
                                log.atDebug().log("[methodByAddress] Found {} overloaded methods, selecting best match",
                                                methodPaths.size());
                                method = (Method) selectBestMatch(methodPaths, returnType, parameterTypes, entityClass).getLast();
                        } else {
                                // Single method or no signature specified
                                Object leaf = methodPaths.get(0).getLast();
                                log.atTrace().log("[methodByAddress] Leaf object resolved: {}", leaf);

                                if (!(leaf instanceof Method)) {
                                        log.atWarn().log("[methodByAddress] Leaf object {} is not a Method", leaf);
                                        throw new ReflectionException(
                                                        "Method " + methodAddress + " not found in entity "
                                                                        + entityClass.getName());
                                }
                                method = (Method) leaf;

                                // Only validate signature if specific criteria were provided
                                // Note: parameterTypes.length == 0 means we want a method with NO parameters
                                if (returnType != null || parameterTypes != null) {
                                        validateSignature(method, returnType, parameterTypes, entityClass);
                                }
                        }

                        log.atInfo().log("[methodByAddress] Successfully resolved method {} in entity {}",
                                        method.getName(),
                                        entityClass.getName());
                        return methodAddress;

                } catch (ReflectionException e) {
                        log.atError().log("[methodByAddress] Reflection error resolving method {} in entity {}",
                                        methodAddress,
                                        entityClass.getName(), e);
                        throw new ReflectionException(e.getMessage(), e);
                }
        }

        /**
         * Selects the best matching method from a list of overloaded methods based on
         * return type and parameters.
         *
         * @param methods        list of method objects
         * @param returnType     expected return type (may be null)
         * @param parameterTypes expected parameter types (may be null or empty)
         * @param entityClass    the entity class for error messages
         * @return the best matching method
         * @throws ReflectionException if no matching method is found
         */
        public static List<Object> selectBestMatch(List<List<Object>> methodPaths, Class<?> returnType,
                        Class<?>[] parameterTypes,
                        Class<?> ownerType) throws ReflectionException {
                log.atTrace().log("[selectBestMatch] Selecting from {} methodPaths", methodPaths.size());

                for (List<Object> methodPath : methodPaths) {
                        Object obj = methodPath.getLast();
                        if (!(obj instanceof Method)) {
                                continue;
                        }

                        Method method = (Method) obj;
                        try {
                                validateSignature(method, returnType, parameterTypes, ownerType);
                                log.atDebug().log("[selectBestMatch] Found matching method: {}", method);
                                return methodPath;
                        } catch (ReflectionException e) {
                                // This method doesn't match, try next one
                                log.atTrace().log("[selectBestMatch] Method {} doesn't match: {}", method,
                                                e.getMessage());
                                continue;
                        }
                }

                // No matching method found
                String methodName = methodPaths.isEmpty() ? "unknown"
                                : (methodPaths.get(0).getLast() instanceof Method
                                                ? ((Method) methodPaths.get(0).getLast()).getName()
                                                : "unknown");

                log.atError().log("[selectBestMatch] No matching method found for signature in entity {}",
                                ownerType.getName());
                throw new ReflectionException(
                                "No overload of method " + methodName + " in entity " + ownerType.getName()
                                                + " matches the specified signature (returnType=" + returnType
                                                + ", parameterTypes=" + Arrays.toString(parameterTypes) + ")");
        }

        private static void validateSignature(Method method, Class<?> returnType, Class<?>[] parameterTypes,
                        Class<?> ownerType) throws ReflectionException {
                if (returnType != null && !method.getReturnType().isAssignableFrom(returnType)) {
                        log.atWarn().log(
                                        "[validateSignature] Method {} in entity {} has return type {} but expected {}",
                                        method.getName(), ownerType.getName(), method.getReturnType(), returnType);
                        throw new ReflectionException(
                                        "Method " + method.getName() + " in entity " + ownerType.getName()
                                                        + " does not return type " + returnType.getName()+ " but "+ method.getReturnType().getName());
                }

                if( (parameterTypes == null || parameterTypes.length == 0) && method.getParameterCount() != 0 ) {
                        log.atWarn().log(
                                        "[validateSignature] Method {} in entity {} has {} parameters but expected none",
                                        method.getName(), ownerType.getName(), method.getParameterCount());
                        throw new ReflectionException(
                                        "Method " + method.getName() + " in entity " + ownerType.getName()
                                                        + " has " + method.getParameterCount()
                                                        + " parameters but expected none");
                }

                if (parameterTypes != null) {
                        Class<?>[] actualParams = method.getParameterTypes();
                        if (actualParams.length != parameterTypes.length) {
                                log.atWarn().log(
                                                "[validateSignature] Method {} in entity {} has {} parameters but expected {}",
                                                method.getName(), ownerType.getName(), actualParams.length,
                                                parameterTypes.length);
                                throw new ReflectionException(
                                                "Method " + method.getName() + " in entity " + ownerType.getName()
                                                                + " has " + actualParams.length
                                                                + " parameters but expected " + parameterTypes.length);
                        }
                        // Only check individual parameter types if there are parameters
                        for (int i = 0; i < actualParams.length; i++) {
                                if (!actualParams[i].isAssignableFrom(parameterTypes[i])) {
                                        log.atWarn().log(
                                                        "[validateSignature] Parameter {} of method {} in entity {} has type {} but expected {}",
                                                        i, method.getName(), ownerType.getName(), actualParams[i],
                                                        parameterTypes[i]);
                                        throw new ReflectionException(
                                                        "Parameter " + i + " of method " + method.getName()
                                                                        + " in entity "
                                                                        + ownerType.getName() + " has type "
                                                                        + actualParams[i].getName()
                                                                        + " but expected "
                                                                        + parameterTypes[i].getName());
                                }
                        }
                }
        }
}
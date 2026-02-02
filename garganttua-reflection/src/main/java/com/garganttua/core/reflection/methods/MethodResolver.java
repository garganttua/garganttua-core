package com.garganttua.core.reflection.methods;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

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
                                .filter(m -> m.matches(ownerType, returnType, parameterTypes)).distinct().toList();

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
                                .distinct().toList();

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

        /**
         * Selects the best matching method path from a list of candidates based on return type and parameter types.
         *
         * @param methodPaths the list of method paths (each path is a List of Objects ending with a Method)
         * @param returnType the expected return type (can be null to skip return type check)
         * @param parameterTypes the expected parameter types
         * @param ownerType the declaring class type (unused, kept for API compatibility)
         * @return the best matching method path
         * @throws ReflectionException if no matching method is found or multiple matches exist
         */
        @SuppressWarnings("java:S1172") // ownerType kept for API compatibility
        public static List<Object> selectBestMatch(List<List<Object>> methodPaths, Class<?> returnType,
                        Class<?>[] parameterTypes, Class<?> ownerType) throws ReflectionException {

                List<List<Object>> matches = methodPaths.stream()
                                .filter(path -> matchesSignature(path, returnType, parameterTypes))
                                .toList();

                if (matches.isEmpty()) {
                        throw new ReflectionException("No matching method found with returnType=" + returnType
                                        + ", parameterTypes=" + Arrays.toString(parameterTypes));
                }
                if (matches.size() > 1) {
                        throw new ReflectionException("Multiple methods match the signature");
                }
                return matches.get(0);
        }

        private static boolean matchesSignature(List<Object> path, Class<?> returnType, Class<?>[] parameterTypes) {
                if (path.isEmpty()) return false;
                Object last = path.getLast();
                if (!(last instanceof Method method)) return false;

                if (!matchesReturnType(method, returnType)) return false;
                return matchesParameterTypes(method, parameterTypes);
        }

        private static boolean matchesReturnType(Method method, Class<?> returnType) {
                if (returnType == null) return true;
                return method.getReturnType().isAssignableFrom(returnType)
                                || returnType.isAssignableFrom(method.getReturnType());
        }

        private static boolean matchesParameterTypes(Method method, Class<?>[] parameterTypes) {
                Class<?>[] actualParams = method.getParameterTypes();
                if (parameterTypes == null || parameterTypes.length == 0) {
                        return actualParams.length == 0;
                }
                if (actualParams.length != parameterTypes.length) {
                        return false;
                }
                for (int i = 0; i < actualParams.length; i++) {
                        if (!actualParams[i].isAssignableFrom(parameterTypes[i])) {
                                return false;
                        }
                }
                return true;
        }

}
package com.garganttua.core.reflection.methods;

import java.util.Arrays;
import java.util.List;

import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IMethod;
import com.garganttua.core.reflection.IObjectQuery;
import com.garganttua.core.reflection.IReflectionProvider;
import com.garganttua.core.reflection.ObjectAddress;
import com.garganttua.core.reflection.ReflectionException;
import com.garganttua.core.reflection.query.ObjectQueryFactory;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MethodResolver {

        // ========================================================================
        // Provider-based API (preferred)
        // ========================================================================

        public static ResolvedMethod methodByName(IClass<?> ownerType, IReflectionProvider provider,
                        String methodName, IClass<?> returnType,
                        IClass<?>... parameterTypes) throws ReflectionException {
                log.atTrace().log("[methodByName] Start: methodName={}, ownerType={}", methodName, ownerType);

                IObjectQuery<?> query = ObjectQueryFactory.objectQuery(ownerType, provider);

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

        public static ResolvedMethod methodByName(IClass<?> ownerType, IReflectionProvider provider,
                        String methodName)
                        throws ReflectionException {
                IObjectQuery<?> query = ObjectQueryFactory.objectQuery(ownerType, provider);

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

        public static ResolvedMethod methodByMethod(IClass<?> ownerType, IReflectionProvider provider,
                        IMethod method) throws ReflectionException {
                return methodByName(ownerType, provider, method.getName(), method.getReturnType(),
                                method.getParameterTypes());
        }

        public static ResolvedMethod methodByAddress(IClass<?> ownerType, IReflectionProvider provider,
                        ObjectAddress methodAddress)
                        throws ReflectionException {
                log.atTrace().log("[methodByAddress] Start: methodAddress={}, ownerType={}", methodAddress,
                                ownerType);
                return MethodResolver.methodByName(ownerType, provider, methodAddress.getLastElement());
        }

        public static ResolvedMethod methodByAddress(IClass<?> ownerType, IReflectionProvider provider,
                        ObjectAddress methodAddress,
                        IClass<?> returnType, IClass<?>... parameterTypes)
                        throws ReflectionException {
                return MethodResolver.methodByName(ownerType, provider, methodAddress.getLastElement(), returnType,
                                parameterTypes);
        }

        // ========================================================================
        // Signature matching utilities
        // ========================================================================

        /**
         * Selects the best matching method path from a list of candidates based on
         * return type and parameter types.
         */
        @SuppressWarnings("java:S1172") // ownerType kept for API compatibility
        @Deprecated(since = "2.0.0-ALPHA01", forRemoval = true)
        public static List<Object> selectBestMatch(List<List<Object>> methodPaths, IClass<?> returnType,
                        IClass<?>[] parameterTypes, IClass<?> ownerType) throws ReflectionException {

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

        private static boolean matchesSignature(List<Object> path, IClass<?> returnType, IClass<?>[] parameterTypes) {
                if (path.isEmpty())
                        return false;
                Object last = path.getLast();
                if (!(last instanceof IMethod method))
                        return false;

                if (!matchesReturnType(method, returnType))
                        return false;
                return matchesParameterTypes(method, parameterTypes);
        }

        private static boolean matchesReturnType(IMethod method, IClass<?> returnType) {
                if (returnType == null)
                        return true;
                return method.getReturnType().isAssignableFrom(returnType)
                                || returnType.isAssignableFrom(method.getReturnType());
        }

        private static boolean matchesParameterTypes(IMethod method, IClass<?>[] parameterTypes) {
                IClass<?>[] actualParams = method.getParameterTypes();
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

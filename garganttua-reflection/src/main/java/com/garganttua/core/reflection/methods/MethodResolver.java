package com.garganttua.core.reflection.methods;

import java.util.Arrays;
import java.util.List;

import com.garganttua.core.observability.Logger;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IMethod;
import com.garganttua.core.reflection.IObjectQuery;
import com.garganttua.core.reflection.IReflectionProvider;
import com.garganttua.core.reflection.ObjectAddress;
import com.garganttua.core.reflection.ReflectionException;
import com.garganttua.core.reflection.query.ObjectQueryFactory;

/**
 * Resolves a single {@link ResolvedMethod} on a class from a name, an existing
 * {@link IMethod}, or an {@link ObjectAddress}, applying optional return-type and
 * parameter-type signature constraints.
 */
public class MethodResolver {
    private static final Logger log = Logger.getLogger(MethodResolver.class);

        // ========================================================================
        // Provider-based API (preferred)
        // ========================================================================

        /**
         * Resolves the single method matching the given name and signature constraints.
         *
         * @param ownerType      the class to search
         * @param provider       the reflection provider used to query members
         * @param methodName      the method name
         * @param returnType      the expected return type, or {@code null} for no constraint
         * @param parameterTypes  the expected parameter types; empty or {@code null} means no
         *                        constraint on parameters
         * @return the single matching resolved method
         * @throws ReflectionException if no method or more than one method matches
         */
        public static ResolvedMethod methodByName(IClass<?> ownerType, IReflectionProvider provider,
                        String methodName, IClass<?> returnType,
                        IClass<?>... parameterTypes) throws ReflectionException {
                log.trace("[methodByName] Start: methodName={}, ownerType={}", methodName, ownerType);

                IObjectQuery<?> query = ObjectQueryFactory.objectQuery(ownerType, provider);

                List<ResolvedMethod> methods = query.addresses(methodName).stream()
                                .flatMap(a -> query.findAll(a).stream().map(m -> new ResolvedMethod(a, m)).toList()
                                                .stream())
                                .toList();

                // Empty varargs (no parameter types specified) = no constraint on parameters.
                // Pass null to matches() to signal wildcard. Non-empty array = exact match.
                IClass<?>[] effectiveParams = (parameterTypes == null || parameterTypes.length == 0)
                                ? null : parameterTypes;

                List<ResolvedMethod> found = methods.stream()
                                .filter(m -> m.matches(ownerType, returnType, effectiveParams)).distinct().toList();

                if (found.size() > 1) {
                        log.error(
                                        "[methodByName] Multiple methods found matching signature for method {} in ownertype {}",
                                        methodName, ownerType.getName());
                        throw new ReflectionException("Multiple overloads of method " + methodName + " in ownertype "
                                        + ownerType.getName() + " match the specified signature (returnType="
                                        + returnType + ", parameterTypes=" + Arrays.toString(parameterTypes) + ")");
                }
                if (found.isEmpty()) {
                        log.error(
                                        "[methodByName] No method found matching signature for method {} in ownertype {}",
                                        methodName, ownerType.getName());
                        throw new ReflectionException("No overload of method " + methodName + " in ownertype "
                                        + ownerType.getName() + " matches the specified signature (returnType="
                                        + returnType + ", parameterTypes=" + Arrays.toString(parameterTypes) + ")");
                }
                return found.get(0);

        }

        /**
         * Resolves the single method with the given name, ignoring signature.
         *
         * @param ownerType   the class to search
         * @param provider    the reflection provider used to query members
         * @param methodName  the method name
         * @return the single matching resolved method
         * @throws ReflectionException if no method or more than one method with that name exists
         */
        public static ResolvedMethod methodByName(IClass<?> ownerType, IReflectionProvider provider,
                        String methodName)
                        throws ReflectionException {
                IObjectQuery<?> query = ObjectQueryFactory.objectQuery(ownerType, provider);

                List<ResolvedMethod> found = query.addresses(methodName).stream()
                                .flatMap(a -> query.findAll(a).stream().map(m -> new ResolvedMethod(a, m)).toList()
                                                .stream())
                                .distinct().toList();

                if (found.size() > 1) {
                        log.error(
                                        "[methodByName] Multiple methods found matching signature for method {} in ownertype {}",
                                        methodName, ownerType.getName());
                        throw new ReflectionException("Multiple overloads of method " + methodName + " in ownertype "
                                        + ownerType.getName());
                }
                if (found.isEmpty()) {
                        log.error(
                                        "[methodByName] No method found matching signature for method {} in ownertype {}",
                                        methodName, ownerType.getName());
                        throw new ReflectionException(
                                        "No overload of method " + methodName + " in ownertype " + ownerType.getName());
                }
                return found.get(0);
        }

        /**
         * Resolves the method on {@code ownerType} that exactly matches the signature of
         * the supplied {@code method} (name, return type and parameter types).
         *
         * @param ownerType  the class to search
         * @param provider   the reflection provider used to query members
         * @param method     the method whose exact signature is matched
         * @return the single matching resolved method
         * @throws ReflectionException if no method or more than one method matches the exact signature
         */
        public static ResolvedMethod methodByMethod(IClass<?> ownerType, IReflectionProvider provider,
                        IMethod method) throws ReflectionException {
                log.trace("[methodByMethod] Start: method={}, ownerType={}", method.getName(), ownerType);

                IObjectQuery<?> query = ObjectQueryFactory.objectQuery(ownerType, provider);

                List<ResolvedMethod> methods = query.addresses(method.getName()).stream()
                                .flatMap(a -> query.findAll(a).stream().map(m -> new ResolvedMethod(a, m)).toList()
                                                .stream())
                                .toList();

                // Exact match: pass actual parameter types (including empty array for 0-param methods)
                List<ResolvedMethod> found = methods.stream()
                                .filter(m -> m.matches(ownerType, method.getReturnType(), method.getParameterTypes()))
                                .distinct().toList();

                if (found.size() > 1) {
                        throw new ReflectionException("Multiple overloads of method " + method.getName()
                                        + " in ownertype " + ownerType.getName() + " match the exact signature");
                }
                if (found.isEmpty()) {
                        throw new ReflectionException("No overload of method " + method.getName()
                                        + " in ownertype " + ownerType.getName() + " matches the exact signature");
                }
                return found.get(0);
        }

        /**
         * Resolves the method named by the last element of {@code methodAddress}, ignoring signature.
         *
         * @param ownerType      the class to search
         * @param provider       the reflection provider used to query members
         * @param methodAddress  the address whose last element is the method name
         * @return the single matching resolved method
         * @throws ReflectionException if no method or more than one method with that name exists
         */
        public static ResolvedMethod methodByAddress(IClass<?> ownerType, IReflectionProvider provider,
                        ObjectAddress methodAddress)
                        throws ReflectionException {
                log.trace("[methodByAddress] Start: methodAddress={}, ownerType={}", methodAddress,
                                ownerType);
                return MethodResolver.methodByName(ownerType, provider, methodAddress.getLastElement());
        }

        /**
         * Resolves the method named by the last element of {@code methodAddress} matching the
         * given signature constraints.
         *
         * @param ownerType      the class to search
         * @param provider       the reflection provider used to query members
         * @param methodAddress  the address whose last element is the method name
         * @param returnType     the expected return type, or {@code null} for no constraint
         * @param parameterTypes the expected parameter types; empty or {@code null} means no constraint
         * @return the single matching resolved method
         * @throws ReflectionException if no method or more than one method matches
         */
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

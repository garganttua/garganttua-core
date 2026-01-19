package com.garganttua.core.reflection.methods;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.garganttua.core.reflection.IMethodReturn;
import com.garganttua.core.reflection.ObjectAddress;
import com.garganttua.core.reflection.query.ObjectQueryFactory;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Methods {

        public static boolean isStatic(Method method) {
                return Modifier.isStatic(method.getModifiers());
        }

        public static String prettyColored(Method m) {
                log.atTrace().log("Creating pretty colored representation for method: {}", m);
                return "\u001B[36m" + m.getDeclaringClass().getSimpleName() + "\u001B[0m"
                                + "."
                                + "\u001B[32m" + m.getName() + "\u001B[0m"
                                + "("
                                + Arrays.stream(m.getParameterTypes())
                                                .map(c -> "\u001B[33m" + c.getSimpleName() + "\u001B[0m")
                                                .collect(Collectors.joining(", "))
                                + ")";
        }

        public static String pretty(Method method) {
                log.atTrace().log("Creating pretty representation for method: {}", method);

                String pretty = method.getDeclaringClass().getSimpleName()
                                + "."
                                + method.getName()
                                + "("
                                + Arrays.stream(method.getParameterTypes())
                                                .map(Class::getSimpleName)
                                                .collect(Collectors.joining(", "))
                                + ")";

                return pretty;
        }

        /**
         * Creates a single-value MethodReturn.
         *
         * @param <R>   the type of the value
         * @param value the single value
         * @return a MethodReturn containing the single value
         */
        static <R> IMethodReturn<R> singleMethodReturn(R value) {
                return new SingleMethodReturn<>(value);
        }

        /**
         * Creates a single-value MethodReturn with explicit type.
         *
         * @param <R>   the type of the value
         * @param value the single value
         * @param type  the runtime type of the value
         * @return a MethodReturn containing the single value
         */
        static <R> IMethodReturn<R> singleMethodReturn(R value, Class<R> type) {
                return new SingleMethodReturn<>(value, type);
        }

        /**
         * Creates a multiple-value MethodReturn.
         *
         * @param <R>    the type of the values
         * @param values the list of values
         * @return a MethodReturn containing multiple values
         */
        static <R> IMethodReturn<R> multipleMethodReturn(List<R> values) {
                return new MultipleMethodReturn<>(values);
        }

        /**
         * Creates a multiple-value MethodReturn with explicit type.
         *
         * @param <R>    the type of the values
         * @param values the list of values
         * @param type   the runtime type of the values
         * @return a MethodReturn containing multiple values
         */
        static <R> IMethodReturn<R> multipleMethodReturn(List<R> values, Class<R> type) {
                return new MultipleMethodReturn<>(values, type);
        }

}

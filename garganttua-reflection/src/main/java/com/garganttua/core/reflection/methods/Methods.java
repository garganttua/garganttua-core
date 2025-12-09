package com.garganttua.core.reflection.methods;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.stream.Collectors;

import com.garganttua.core.reflection.ObjectAddress;
import com.garganttua.core.reflection.query.ObjectQueryFactory;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Methods {

        public static boolean isStatic(Class<?> methodOwner, ObjectAddress methodAddress) {
                Method method = getMethod(methodOwner, methodAddress);
                return Modifier.isStatic(method.getModifiers());
        }

        private static Method getMethod(Class<?> methodOwner, ObjectAddress methodAddress) {
                Method method = (Method) ObjectQueryFactory.objectQuery(methodOwner).find(methodAddress).getLast();
                return method;
        }

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

}

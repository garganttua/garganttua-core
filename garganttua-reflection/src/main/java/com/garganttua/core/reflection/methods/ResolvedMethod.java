package com.garganttua.core.reflection.methods;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.List;

import com.garganttua.core.reflection.ObjectAddress;

public record ResolvedMethod(ObjectAddress address, List<Object> methodPath) {

    public Method method() {
        return (Method) methodPath.getLast();
    }

    boolean matches(Method other) {
        return this.method().equals(other);
    }

    boolean matches(Class<?> ownerType, Class<?> returnType, Class<?>[] parameterTypes) {
        if( !method().getDeclaringClass().isAssignableFrom(ownerType) )
            return false;

        if (returnType != null && !method().getReturnType().isAssignableFrom(returnType)) {
            return false;
        }

        if ((parameterTypes == null || parameterTypes.length == 0) && method().getParameterCount() != 0) {
            return false;
        }

        if (parameterTypes != null) {
            Class<?>[] actualParams = method().getParameterTypes();
            if (actualParams.length != parameterTypes.length) {
                return false;
            }
            for (int i = 0; i < actualParams.length; i++) {
                if (!actualParams[i].isAssignableFrom(parameterTypes[i])) {
                    return false;
                }
            }
        }
        return true;
    }

    public Class<?> returnType() {
        return this.method().getReturnType();
    }

    public Class<?> ownerType() {
        return this.method().getDeclaringClass();
    }

    public boolean isStatic(){
        return Methods.isStatic(this.method());
    }

    public String name() {
        return this.method().getName();
    }

    public Parameter[] parameters() {
        return this.method().getParameters();
    }

    public Class<?>[] parameterTypes() {
        return this.method().getParameterTypes();
    }

}

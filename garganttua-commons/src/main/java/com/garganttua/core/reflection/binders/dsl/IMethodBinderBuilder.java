package com.garganttua.core.reflection.binders.dsl;

import java.lang.reflect.Method;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.reflection.ObjectAddress;
import com.garganttua.core.reflection.binders.IMethodBinder;

public interface IMethodBinderBuilder<ExecutionReturn, Builder extends IMethodBinderBuilder<ExecutionReturn, ?, ?, ?>, Link, Built extends IMethodBinder<ExecutionReturn>>
                extends IExecutableBinderBuilder<ExecutionReturn, Builder, Link, Built> {

        Builder method() throws DslException;

        Builder method(Method method) throws DslException;

        Builder method(ObjectAddress methodAddress) throws DslException;

        Builder withReturn(Class<ExecutionReturn> returnedType) throws DslException;

        Builder method(String methodName) throws DslException;

        Builder method(Method method,
                        Class<ExecutionReturn> returnType, Class<?>... parameterTypes) throws DslException;

        Builder method(ObjectAddress methodAddress,
                        Class<ExecutionReturn> returnType, Class<?>... parameterTypes) throws DslException;

        Builder method(String methodName,
                        Class<ExecutionReturn> returnType, Class<?>... parameterTypes) throws DslException;
}

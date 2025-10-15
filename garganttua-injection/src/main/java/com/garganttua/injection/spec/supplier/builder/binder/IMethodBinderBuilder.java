package com.garganttua.injection.spec.supplier.builder.binder;

import java.lang.reflect.Method;

import com.garganttua.dsl.DslException;
import com.garganttua.injection.spec.supplier.binder.IMethodBinder;
import com.garganttua.reflection.GGObjectAddress;

public interface IMethodBinderBuilder<ExecutionReturn, Builder extends IMethodBinderBuilder<ExecutionReturn, ?, ?, ?>, Link, Built extends IMethodBinder<ExecutionReturn>>
                extends IExecutableBinderBuilder<ExecutionReturn, Builder, Link, Built> {

        Builder method() throws DslException;

        Builder method(Method method) throws DslException;

        Builder method(GGObjectAddress methodAddress) throws DslException;

        Builder withReturn(Class<ExecutionReturn> returnedType) throws DslException;

        Builder method(String methodName) throws DslException;

        Builder method(Method method,
                        Class<ExecutionReturn> returnType, Class<?>... parameterTypes) throws DslException;

        Builder method(GGObjectAddress methodAddress,
                        Class<ExecutionReturn> returnType, Class<?>... parameterTypes) throws DslException;

        Builder method(String methodName,
                        Class<ExecutionReturn> returnType, Class<?>... parameterTypes) throws DslException;
}

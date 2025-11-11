package com.garganttua.core.runtime;

import java.util.Map;

import com.garganttua.core.reflection.binders.IMethodBinder;

public interface IRuntimeStep {

    <T> IMethodBinder<T> getBinder(Class<T> clazz);

    Map<Class<?>, IMethodBinder<?>> getBinders();

    String getStepName();

}

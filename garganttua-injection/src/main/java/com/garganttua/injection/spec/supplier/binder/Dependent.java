package com.garganttua.injection.spec.supplier.binder;

import java.util.Set;

public interface Dependent {

    Set<Class<?>> getDependencies();
}

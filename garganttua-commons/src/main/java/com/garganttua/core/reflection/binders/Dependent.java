package com.garganttua.core.reflection.binders;

import java.util.Set;

public interface Dependent {

    Set<Class<?>> getDependencies();
}

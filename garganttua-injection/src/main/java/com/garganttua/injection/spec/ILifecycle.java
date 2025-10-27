package com.garganttua.injection.spec;

import com.garganttua.injection.DiException;

public interface ILifecycle {

    ILifecycle onStart() throws DiException;

    ILifecycle onStop() throws DiException;

    ILifecycle onFlush() throws DiException;

    ILifecycle onInit() throws DiException;

    ILifecycle onReload() throws DiException;

}

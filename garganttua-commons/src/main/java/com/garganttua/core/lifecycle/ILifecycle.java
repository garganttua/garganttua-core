package com.garganttua.core.lifecycle;

public interface ILifecycle {

    ILifecycle onStart() throws LifecycleException;

    ILifecycle onStop() throws LifecycleException;

    ILifecycle onFlush() throws LifecycleException;

    ILifecycle onInit() throws LifecycleException;

    ILifecycle onReload() throws LifecycleException;

}

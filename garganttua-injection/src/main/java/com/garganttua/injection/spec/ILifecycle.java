package com.garganttua.injection.spec;

import com.garganttua.injection.DiException;

public interface ILifecycle {

    void onStart() throws DiException;

    void onStop() throws DiException;

    void onFlush() throws DiException;

    void onInit() throws DiException;

    void onReload() throws DiException;

}

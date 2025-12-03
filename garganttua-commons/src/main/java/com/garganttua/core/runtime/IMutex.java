package com.garganttua.core.runtime;

public interface IMutex {

    @FunctionalInterface
    public interface ThrowingFunction<R> {
        R execute() throws MutexException;
    }

    <R> R tryAcquire(String mutexName, ThrowingFunction<R> function) throws MutexException;

    <R> R acquire(String mutexName, ThrowingFunction<R> function) throws MutexException;

}

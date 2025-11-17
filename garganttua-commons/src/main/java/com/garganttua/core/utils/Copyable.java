package com.garganttua.core.utils;

public interface Copyable<T> {

    T copy() throws CopyException;

}

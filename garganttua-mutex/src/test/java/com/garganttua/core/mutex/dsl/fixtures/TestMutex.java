package com.garganttua.core.mutex.dsl.fixtures;

import com.garganttua.core.mutex.IMutex;
import com.garganttua.core.mutex.MutexException;
import com.garganttua.core.mutex.MutexStrategy;

/**
 * Test mutex implementation for auto-detection testing.
 *
 * <p>
 * This is a minimal implementation used solely for testing the auto-detection
 * mechanism of MutexManagerBuilder.
 * </p>
 */
public class TestMutex implements IMutex {

    private final String name;

    public TestMutex(String name) {
        this.name = name;
    }

    @Override
    public <R> R acquire(ThrowingFunction<R> function) throws MutexException {
        // Simple implementation for testing
        try {
            return function.execute();
        } catch (MutexException e) {
            throw e;
        } catch (Exception e) {
            throw new MutexException("Unexpected exception", e);
        }
    }

    @Override
    public <R> R acquire(ThrowingFunction<R> function, MutexStrategy strategy) throws MutexException {
        // Simple implementation for testing
        return acquire(function);
    }

    public String getName() {
        return name;
    }

}

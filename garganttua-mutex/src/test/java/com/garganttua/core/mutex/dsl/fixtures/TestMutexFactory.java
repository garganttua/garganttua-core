package com.garganttua.core.mutex.dsl.fixtures;

import java.util.Objects;

import com.garganttua.core.mutex.IMutex;
import com.garganttua.core.mutex.IMutexFactory;
import com.garganttua.core.mutex.MutexException;
import com.garganttua.core.mutex.MutexStrategy;
import com.garganttua.core.mutex.annotations.MutexFactory;

import lombok.extern.slf4j.Slf4j;

/**
 * Test mutex factory annotated with @MutexFactory for auto-detection testing.
 *
 * <p>
 * This factory is used to verify that MutexManagerBuilder can automatically
 * discover and register factories annotated with @MutexFactory when scanning
 * the classpath.
 * </p>
 */
@Slf4j
@MutexFactory(type = TestMutex.class)
public class TestMutexFactory implements IMutexFactory {

    @Override
    public IMutex createMutex(String name) throws MutexException {
        validateName(name);
        log.atDebug().log("Creating TestMutex: {}", name);
        return new TestMutex(name);
    }

    @Override
    public IMutex createMutex(String name, MutexStrategy defaultStrategy) throws MutexException {
        validateName(name);
        Objects.requireNonNull(defaultStrategy, "Default strategy cannot be null");
        log.atDebug().log("Creating TestMutex with strategy: {}", name);
        // For testing purposes, we ignore the strategy
        return createMutex(name);
    }

    private void validateName(String name) throws MutexException {
        if (name == null || name.trim().isEmpty()) {
            throw new MutexException("Mutex name cannot be null or empty");
        }
    }

}

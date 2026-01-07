package com.garganttua.core.mutex;

/**
 * Test mutex implementation B for MutexName testing.
 */
public class TestMutexB implements IMutex {

    private final String name;

    public TestMutexB(String name) {
        this.name = name;
    }

    @Override
    public <R> R acquire(ThrowingFunction<R> function) throws MutexException {
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
        return acquire(function);
    }

    public String getName() {
        return name;
    }
}

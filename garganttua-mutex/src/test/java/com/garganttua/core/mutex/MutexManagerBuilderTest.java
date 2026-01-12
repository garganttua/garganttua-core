package com.garganttua.core.mutex;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.mutex.dsl.IMutexManagerBuilder;
import com.garganttua.core.mutex.dsl.MutexManagerBuilder;

class MutexManagerBuilderTest {

    @Test
    void testBuilderWithNoFactories() throws DslException {
        IMutexManager manager = MutexManagerBuilder.builder()
                .build();

        assertNotNull(manager, "Manager should not be null");
    }

    @Test
    void testBuilderWithSingleFactory() throws DslException {
        IMutexFactory factory = new InterruptibleLeaseMutexFactory();

        IMutexManager manager = MutexManagerBuilder.builder()
                .withFactory(InterruptibleLeaseMutex.class, factory)
                .build();

        assertNotNull(manager, "Manager should not be null");
    }

    @Test
    void testBuilderWithMultipleFactories() throws DslException {
        IMutexFactory factory1 = new InterruptibleLeaseMutexFactory();

        IMutexManager manager = MutexManagerBuilder.builder()
                .withFactory(InterruptibleLeaseMutex.class, factory1)
                .build();

        assertNotNull(manager, "Manager should not be null");
    }

    @Test
    void testBuilderWithNullFactoryThrows() {
        assertThrows(NullPointerException.class, () -> {
            MutexManagerBuilder.builder()
                    .withFactory(InterruptibleLeaseMutex.class, null);
        }, "Should throw when factory is null");
    }

    @Test
    void testBuilderWithNullTypeThrows() {
        IMutexFactory factory = new InterruptibleLeaseMutexFactory();

        assertThrows(NullPointerException.class, () -> {
            MutexManagerBuilder.builder()
                    .withFactory(null, factory);
        }, "Should throw when mutex type is null");
    }

    @Test
    void testBuilderReturnsThis() {
        IMutexManagerBuilder builder = MutexManagerBuilder.builder();
        IMutexFactory factory = new InterruptibleLeaseMutexFactory();

        IMutexManagerBuilder result = builder.withFactory(InterruptibleLeaseMutex.class, factory);

        assertSame(builder, result, "Builder should return itself for method chaining");
    }

    @Test
    void testMutexCreationUsesRegisteredFactory() throws DslException, MutexException {
        IMutexFactory factory = new InterruptibleLeaseMutexFactory();

        IMutexManager manager = MutexManagerBuilder.builder()
                .withFactory(InterruptibleLeaseMutex.class, factory)
                .build();

        MutexName name = MutexName.fromString("com.garganttua.core.mutex.InterruptibleLeaseMutex::test");
        IMutex mutex = manager.mutex(name);

        assertNotNull(mutex, "Mutex should be created");
        assertInstanceOf(InterruptibleLeaseMutex.class, mutex, "Should create InterruptibleLeaseMutex");
    }

    @Test
    void testMutexCreationWithNoFactoryUsesDefault() throws DslException, MutexException {
        IMutexManager manager = MutexManagerBuilder.builder()
                .build();

        MutexName name = MutexName.fromString("com.garganttua.core.mutex.InterruptibleLeaseMutex::test");
        IMutex mutex = manager.mutex(name);

        assertNotNull(mutex, "Mutex should be created with default factory");
        assertInstanceOf(InterruptibleLeaseMutex.class, mutex, "Should fallback to InterruptibleLeaseMutex");
    }

    @Test
    void testBuilderCachesBuiltInstance() throws DslException {
        IMutexManagerBuilder builder = MutexManagerBuilder.builder();

        IMutexManager first = builder.build();
        IMutexManager second = builder.build();

        assertSame(first, second, "Builder should return cached instance");
    }

    @Test
    void testAutoDetectDoesNotThrow() {
        assertDoesNotThrow(() -> {
            MutexManagerBuilder.builder()
                    .autoDetect(true)
                    .build();
        }, "Auto-detect should not throw when no context is available");
    }

    @Test
    void testStaticBuilderMethods() {
        assertNotNull(MutexManagerBuilder.builder(), "builder() should create instance");
    }

}

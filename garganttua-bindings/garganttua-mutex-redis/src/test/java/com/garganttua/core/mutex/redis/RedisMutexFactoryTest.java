package com.garganttua.core.mutex.redis;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.TimeUnit;

import org.github.siahsang.redutils.common.RedUtilsConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.garganttua.core.mutex.IMutex;
import com.garganttua.core.mutex.IMutexFactory;
import com.garganttua.core.mutex.MutexException;
import com.garganttua.core.mutex.MutexStrategy;

class RedisMutexFactoryTest {

    private IMutexFactory factory;

    @BeforeEach
    void setUp() {
        factory = new RedisMutexFactory();
    }

    @Test
    void testCreateMutexWithValidName() throws MutexException {
        IMutex mutex = factory.createMutex("test-mutex");

        assertNotNull(mutex);
        assertTrue(mutex instanceof RedisMutex);
    }

    @Test
    void testCreateMutexWithQualifiedName() throws MutexException {
        IMutex mutex = factory.createMutex("distributed::user-lock");

        assertNotNull(mutex);
        assertTrue(mutex instanceof RedisMutex);
    }

    @Test
    void testCreateMutexReturnsNewInstanceEachTime() throws MutexException {
        IMutex mutex1 = factory.createMutex("test");
        IMutex mutex2 = factory.createMutex("test");

        assertNotNull(mutex1);
        assertNotNull(mutex2);
        assertNotSame(mutex1, mutex2, "Factory should create new instances each time");
    }

    @Test
    void testCreateMutexRejectsNullName() {
        assertThrows(NullPointerException.class, () -> {
            factory.createMutex(null);
        });
    }

    @Test
    void testCreateMutexRejectsEmptyName() {
        assertThrows(IllegalArgumentException.class, () -> {
            factory.createMutex("");
        });
    }

    @Test
    void testCreateMutexRejectsWhitespaceOnlyName() {
        assertThrows(IllegalArgumentException.class, () -> {
            factory.createMutex("   ");
        });
    }

    @Test
    void testCreateMutexTrimsWhitespace() throws MutexException {
        IMutex mutex = factory.createMutex("  test-mutex  ");

        assertNotNull(mutex);
        assertTrue(mutex instanceof RedisMutex);
    }

    @Test
    void testCreateMutexWithStrategy() throws MutexException {
        MutexStrategy strategy = new MutexStrategy(
            5, TimeUnit.SECONDS,
            3, 100, TimeUnit.MILLISECONDS,
            10, TimeUnit.SECONDS
        );

        IMutex mutex = factory.createMutex("test-mutex", strategy);

        assertNotNull(mutex);
        assertTrue(mutex instanceof RedisMutex);
    }

    @Test
    void testCreateMutexWithStrategyRejectsNullName() {
        MutexStrategy strategy = new MutexStrategy(
            5, TimeUnit.SECONDS,
            3, 100, TimeUnit.MILLISECONDS,
            10, TimeUnit.SECONDS
        );

        assertThrows(NullPointerException.class, () -> {
            factory.createMutex(null, strategy);
        });
    }

    @Test
    void testCreateMutexWithStrategyRejectsNullStrategy() {
        assertThrows(NullPointerException.class, () -> {
            factory.createMutex("test-mutex", null);
        });
    }

    @Test
    void testCreateMutexWithStrategyRejectsEmptyName() {
        MutexStrategy strategy = new MutexStrategy(
            5, TimeUnit.SECONDS,
            3, 100, TimeUnit.MILLISECONDS,
            10, TimeUnit.SECONDS
        );

        assertThrows(IllegalArgumentException.class, () -> {
            factory.createMutex("", strategy);
        });
    }

    @Test
    void testFactoryWithCustomRedisConfig() {
        RedUtilsConfig config = new RedUtilsConfig.RedUtilsConfigBuilder()
            .hostAddress("localhost")
            .port(6379)
            .replicaCount(1)
            .leaseTimeMillis(30000)
            .build();

        IMutexFactory customFactory = new RedisMutexFactory(config);
        assertNotNull(customFactory);
    }

    @Test
    void testFactoryWithCustomConfigCreatesValidMutex() throws MutexException {
        RedUtilsConfig config = new RedUtilsConfig.RedUtilsConfigBuilder()
            .hostAddress("localhost")
            .port(6379)
            .replicaCount(1)
            .leaseTimeMillis(30000)
            .build();

        IMutexFactory customFactory = new RedisMutexFactory(config);
        IMutex mutex = customFactory.createMutex("test-mutex");

        assertNotNull(mutex);
        assertTrue(mutex instanceof RedisMutex);
    }

    @Test
    void testFactoryRejectsNullConfig() {
        assertThrows(NullPointerException.class, () -> {
            new RedisMutexFactory(null);
        });
    }

    @Test
    void testMultipleMutexesWithDifferentNames() throws MutexException {
        IMutex mutex1 = factory.createMutex("mutex-1");
        IMutex mutex2 = factory.createMutex("mutex-2");
        IMutex mutex3 = factory.createMutex("mutex-3");

        assertNotNull(mutex1);
        assertNotNull(mutex2);
        assertNotNull(mutex3);
        assertNotSame(mutex1, mutex2);
        assertNotSame(mutex2, mutex3);
        assertNotSame(mutex1, mutex3);
    }

}

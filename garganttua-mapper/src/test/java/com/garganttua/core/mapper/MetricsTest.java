package com.garganttua.core.mapper;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IReflection;
import com.garganttua.core.reflection.dsl.ReflectionBuilder;
import com.garganttua.core.reflection.runtime.RuntimeReflectionProvider;

import lombok.Data;

class MetricsTest {

	private static IReflection reflection;

	@BeforeAll
	static void setUp() throws Exception {
		reflection = ReflectionBuilder.builder()
				.withProvider(new RuntimeReflectionProvider())
				.build();
		IClass.setReflection(reflection);
	}

	@AfterAll
	static void tearDown() {
		IClass.setReflection(null);
	}

	@Data
	static class MetricSource {
		private String name;
	}

	@Data
	static class MetricDest {
		private String name;
	}

	@Test
	void testMetricsCounters() throws MapperException {
		Mapper mapper = new Mapper(reflection);
		MapperMetrics metrics = mapper.getMetrics();

		assertEquals(0, metrics.getTotalMappings());

		MetricSource source = new MetricSource();
		source.setName("test");

		mapper.map(source, reflection.getClass(MetricDest.class));
		assertEquals(1, metrics.getTotalMappings());
		assertEquals(0, metrics.getFailedMappings());
		assertTrue(metrics.getTotalMappingTimeNanos() > 0);
		assertTrue(metrics.getTotalRulesExecuted() > 0);

		mapper.map(source, reflection.getClass(MetricDest.class));
		assertEquals(2, metrics.getTotalMappings());
	}

	@Test
	void testMetricsReset() throws MapperException {
		Mapper mapper = new Mapper(reflection);
		MapperMetrics metrics = mapper.getMetrics();

		MetricSource source = new MetricSource();
		source.setName("test");

		mapper.map(source, reflection.getClass(MetricDest.class));
		assertTrue(metrics.getTotalMappings() > 0);

		metrics.reset();
		assertEquals(0, metrics.getTotalMappings());
		assertEquals(0, metrics.getFailedMappings());
	}

	@Test
	void testListener() throws MapperException {
		Mapper mapper = new Mapper(reflection);

		AtomicInteger beforeCount = new AtomicInteger();
		AtomicInteger afterCount = new AtomicInteger();
		AtomicInteger errorCount = new AtomicInteger();

		mapper.addListener(new IMappingListener() {
			@Override
			public void onBeforeMapping(Object source, IClass<?> destClass) {
				beforeCount.incrementAndGet();
			}

			@Override
			public void onAfterMapping(Object source, Object dest, long durationNanos) {
				afterCount.incrementAndGet();
			}

			@Override
			public void onMappingError(Object source, IClass<?> destClass, Exception error) {
				errorCount.incrementAndGet();
			}
		});

		MetricSource source = new MetricSource();
		source.setName("test");

		mapper.map(source, reflection.getClass(MetricDest.class));

		assertEquals(1, beforeCount.get());
		assertEquals(1, afterCount.get());
		assertEquals(0, errorCount.get());
	}

	@Test
	void testListenerOnError() {
		Mapper mapper = new Mapper(reflection);
		mapper.configure(MapperConfigurationItem.AUTO_CONVENTION_MAPPING, false);

		AtomicInteger errorCount = new AtomicInteger();
		mapper.addListener(new IMappingListener() {
			@Override
			public void onBeforeMapping(Object source, IClass<?> destClass) {}

			@Override
			public void onAfterMapping(Object source, Object dest, long durationNanos) {}

			@Override
			public void onMappingError(Object source, IClass<?> destClass, Exception error) {
				errorCount.incrementAndGet();
			}
		});

		MetricSource source = new MetricSource();
		source.setName("test");

		// No annotations and convention off → should error
		try {
			mapper.map(source, reflection.getClass(MetricDest.class));
		} catch (MapperException e) {
			// expected
		}

		assertEquals(1, errorCount.get());
		assertEquals(1, mapper.getMetrics().getFailedMappings());
	}
}

package com.garganttua.core.reflection.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;

import org.junit.jupiter.api.Test;

import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IConstructor;

/**
 * Behavioural coverage for {@link RuntimeConstructor} — instantiation,
 * parameter reflection, exception propagation, caching and unwrap path.
 */
class RuntimeConstructorBehaviourTest {

	static class Bean {
		final String name;
		final int count;

		public Bean(String name, int count) {
			this.name = name;
			this.count = count;
		}
	}

	static class Throwing {
		public Throwing() {
			throw new IllegalStateException("ctor-fail");
		}
	}

	static class Varargs {
		final int total;

		public Varargs(int... values) {
			int t = 0;
			for (int v : values) t += v;
			this.total = t;
		}
	}

	@Test
	void of_caches_per_underlying_constructor() throws Exception {
		Constructor<Bean> c = Bean.class.getConstructor(String.class, int.class);
		assertSame(RuntimeConstructor.of(c), RuntimeConstructor.of(c));
		assertSame(RuntimeConstructor.of(c), RuntimeConstructor.ofUnchecked(c));
	}

	@Test
	void parameter_reflection_matches_jdk() throws Exception {
		IConstructor<Bean> c = RuntimeConstructor.of(Bean.class.getConstructor(String.class, int.class));
		assertEquals(2, c.getParameterCount());
		IClass<?>[] types = c.getParameterTypes();
		assertEquals(String.class.getName(), types[0].getName());
		assertEquals(int.class.getName(), types[1].getName());
		assertEquals(Bean.class.getName(), c.getDeclaringClass().getName());
	}

	@Test
	void newInstance_constructs_object() throws Exception {
		IConstructor<Bean> c = RuntimeConstructor.of(Bean.class.getConstructor(String.class, int.class));
		Bean bean = c.newInstance("widget", 3);
		assertEquals("widget", bean.name);
		assertEquals(3, bean.count);
	}

	@Test
	void newInstance_wraps_constructor_exception() throws Exception {
		IConstructor<Throwing> c = RuntimeConstructor.of(Throwing.class.getConstructor());
		InvocationTargetException ex = assertThrows(InvocationTargetException.class, c::newInstance);
		assertTrue(ex.getCause() instanceof IllegalStateException);
		assertEquals("ctor-fail", ex.getCause().getMessage());
	}

	@Test
	void newInstance_with_wrong_args_throws_IllegalArgument() throws Exception {
		IConstructor<Bean> c = RuntimeConstructor.of(Bean.class.getConstructor(String.class, int.class));
		assertThrows(IllegalArgumentException.class, () -> c.newInstance(1, 2));
	}

	@Test
	void isVarArgs_detects_varargs_constructor() throws Exception {
		IConstructor<Varargs> va = RuntimeConstructor.of(Varargs.class.getConstructor(int[].class));
		assertTrue(va.isVarArgs());
		Varargs v = va.newInstance((Object) new int[] { 4, 5, 6 });
		assertEquals(15, v.total);

		IConstructor<Bean> bean = RuntimeConstructor.of(Bean.class.getConstructor(String.class, int.class));
		assertFalse(bean.isVarArgs());
	}

	@Test
	void unwrap_static_returns_constructor_and_throws_for_foreign() throws Exception {
		Constructor<Bean> c = Bean.class.getConstructor(String.class, int.class);
		assertEquals(c, RuntimeConstructor.unwrap(RuntimeConstructor.of(c)));

		@SuppressWarnings("unchecked")
		IConstructor<Object> foreign = (IConstructor<Object>) Proxy.newProxyInstance(
				getClass().getClassLoader(),
				new Class<?>[] { IConstructor.class },
				(p, m, a) -> {
					if ("toString".equals(m.getName())) return "foreign";
					throw new UnsupportedOperationException(m.getName());
				});
		assertThrows(IllegalArgumentException.class, () -> RuntimeConstructor.unwrap(foreign));
	}

	@Test
	void equals_and_hashCode_track_underlying_constructor() throws Exception {
		Constructor<Bean> c = Bean.class.getConstructor(String.class, int.class);
		assertEquals(RuntimeConstructor.of(c), RuntimeConstructor.of(c));
		assertEquals(c.hashCode(), RuntimeConstructor.of(c).hashCode());
		assertFalse(RuntimeConstructor.of(c).equals("x"));
	}
}

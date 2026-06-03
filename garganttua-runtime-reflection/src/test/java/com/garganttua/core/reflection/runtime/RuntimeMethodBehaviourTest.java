package com.garganttua.core.reflection.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import org.junit.jupiter.api.Test;

import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IMethod;
import com.garganttua.core.reflection.IParameter;

/**
 * Behavioural coverage for {@link RuntimeMethod} — signature reflection,
 * invocation (including exception unwrapping), varargs/default detection,
 * caching and the unwrap exception path.
 */
class RuntimeMethodBehaviourTest {

	@SuppressWarnings("unused")
	static class Target {
		public int add(int a, int b) {
			return a + b;
		}

		public String greet(String name) {
			return "hi " + name;
		}

		public void boom() {
			throw new IllegalStateException("kaboom");
		}

		public int sum(int... values) {
			int s = 0;
			for (int v : values) s += v;
			return s;
		}

		private String hidden() {
			return "secret";
		}

		public static String stat() {
			return "static-value";
		}
	}

	interface WithDefault {
		default int answer() {
			return 42;
		}

		int plain();
	}

	private static IMethod method(String name, Class<?>... params) throws Exception {
		return RuntimeMethod.of(Target.class.getDeclaredMethod(name, params));
	}

	@Test
	void of_caches_per_underlying_method() throws Exception {
		Method m = Target.class.getDeclaredMethod("add", int.class, int.class);
		assertSame(RuntimeMethod.of(m), RuntimeMethod.of(m));
	}

	@Test
	void signature_reflection_matches_jdk() throws Exception {
		IMethod m = method("add", int.class, int.class);
		assertEquals("add", m.getName());
		assertEquals(2, m.getParameterCount());
		assertEquals(int.class.getName(), m.getReturnType().getName());
		IClass<?>[] paramTypes = m.getParameterTypes();
		assertEquals(int.class.getName(), paramTypes[0].getName());
		assertEquals(int.class.getName(), paramTypes[1].getName());
		assertEquals(Target.class.getName(), m.getDeclaringClass().getName());
	}

	@Test
	void getParameters_wraps_each_parameter() throws Exception {
		IParameter[] params = method("greet", String.class).getParameters();
		assertEquals(1, params.length);
		assertEquals(String.class.getName(), params[0].getType().getName());
	}

	@Test
	void invoke_returns_computed_result() throws Exception {
		assertEquals(7, method("add", int.class, int.class).invoke(new Target(), 3, 4));
		assertEquals("hi bob", method("greet", String.class).invoke(new Target(), "bob"));
	}

	@Test
	void invoke_static_method_ignores_target() throws Exception {
		assertEquals("static-value", method("stat").invoke(null));
	}

	@Test
	void invoke_wraps_thrown_exception_in_InvocationTargetException() throws Exception {
		IMethod m = method("boom");
		InvocationTargetException ex = assertThrows(InvocationTargetException.class,
				() -> m.invoke(new Target()));
		assertTrue(ex.getCause() instanceof IllegalStateException);
		assertEquals("kaboom", ex.getCause().getMessage());
	}

	@Test
	void invoke_with_wrong_arg_type_throws_IllegalArgument() throws Exception {
		IMethod m = method("add", int.class, int.class);
		assertThrows(IllegalArgumentException.class, () -> m.invoke(new Target(), "x", "y"));
	}

	@Test
	void invoke_private_method_requires_setAccessible() throws Exception {
		IMethod m = method("hidden");
		assertThrows(IllegalAccessException.class, () -> m.invoke(new Target()));
		m.setAccessible(true);
		assertEquals("secret", m.invoke(new Target()));
	}

	@Test
	void isVarArgs_detects_varargs_method() throws Exception {
		assertTrue(method("sum", int[].class).isVarArgs());
		assertFalse(method("add", int.class, int.class).isVarArgs());
	}

	@Test
	void varargs_invocation_sums_values() throws Exception {
		IMethod m = method("sum", int[].class);
		assertEquals(6, m.invoke(new Target(), (Object) new int[] { 1, 2, 3 }));
	}

	@Test
	void isDefault_detects_default_interface_method() throws Exception {
		IMethod def = RuntimeMethod.of(WithDefault.class.getDeclaredMethod("answer"));
		IMethod plain = RuntimeMethod.of(WithDefault.class.getDeclaredMethod("plain"));
		assertTrue(def.isDefault());
		assertFalse(plain.isDefault());
	}

	@Test
	void getExceptionTypes_lists_declared_throws() throws Exception {
		Method m = String.class.getMethod("getBytes", String.class);
		IClass<?>[] thrown = RuntimeMethod.of(m).getExceptionTypes();
		assertEquals(1, thrown.length);
		assertEquals(java.io.UnsupportedEncodingException.class.getName(), thrown[0].getName());
	}

	@Test
	void unwrap_static_returns_method_and_throws_for_foreign() throws Exception {
		Method m = Target.class.getDeclaredMethod("add", int.class, int.class);
		assertEquals(m, RuntimeMethod.unwrap(RuntimeMethod.of(m)));

		IMethod foreign = (IMethod) Proxy.newProxyInstance(
				getClass().getClassLoader(),
				new Class<?>[] { IMethod.class },
				(p, mm, a) -> {
					if ("toString".equals(mm.getName())) return "foreign";
					throw new UnsupportedOperationException(mm.getName());
				});
		assertThrows(IllegalArgumentException.class, () -> RuntimeMethod.unwrap(foreign));
	}

	@Test
	void equals_and_hashCode_track_underlying_method() throws Exception {
		Method m = Target.class.getDeclaredMethod("add", int.class, int.class);
		assertEquals(RuntimeMethod.of(m), RuntimeMethod.of(m));
		assertEquals(m.hashCode(), RuntimeMethod.of(m).hashCode());
		assertFalse(RuntimeMethod.of(m).equals("x"));
	}
}

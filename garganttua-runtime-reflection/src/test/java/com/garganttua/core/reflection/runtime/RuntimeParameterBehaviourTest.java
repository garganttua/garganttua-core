package com.garganttua.core.reflection.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.garganttua.core.reflection.IParameter;

/**
 * Behavioural coverage for {@link RuntimeParameter} — type/varargs reflection,
 * generic type, annotation lookup and equality.
 */
class RuntimeParameterBehaviourTest {

	@Retention(RetentionPolicy.RUNTIME)
	@interface Ann {
		String value();
	}

	@SuppressWarnings("unused")
	static class Holder {
		void scalar(String text, int count) {
		}

		void varargs(int first, String... rest) {
		}

		void annotated(@Ann("p") String x) {
		}

		void generic(List<String> items) {
		}
	}

	private static Parameter[] paramsOf(String name, Class<?>... types) throws Exception {
		Method m = Holder.class.getDeclaredMethod(name, types);
		return m.getParameters();
	}

	@Test
	void getType_matches_declared_type() throws Exception {
		Parameter[] p = paramsOf("scalar", String.class, int.class);
		assertEquals(String.class.getName(), RuntimeParameter.of(p[0]).getType().getName());
		assertEquals(int.class.getName(), RuntimeParameter.of(p[1]).getType().getName());
	}

	@Test
	void isVarArgs_true_only_for_the_vararg_parameter() throws Exception {
		Parameter[] p = paramsOf("varargs", int.class, String[].class);
		assertFalse(RuntimeParameter.of(p[0]).isVarArgs());
		assertTrue(RuntimeParameter.of(p[1]).isVarArgs());
		assertEquals(String[].class.getName(), RuntimeParameter.of(p[1]).getType().getName());
	}

	@Test
	void getParameterizedType_carries_generics() throws Exception {
		Parameter[] p = paramsOf("generic", List.class);
		assertEquals("java.util.List<java.lang.String>",
				RuntimeParameter.of(p[0]).getParameterizedType().getTypeName());
	}

	@Test
	void annotation_lookup_returns_value_and_null_when_absent() throws Exception {
		Parameter[] p = paramsOf("annotated", String.class);
		IParameter ip = RuntimeParameter.of(p[0]);
		Ann a = ip.getAnnotation(RuntimeClass.of(Ann.class));
		assertEquals("p", a.value());
		assertTrue(ip.isAnnotationPresent(RuntimeClass.of(Ann.class)));

		IParameter scalar = RuntimeParameter.of(paramsOf("scalar", String.class, int.class)[0]);
		assertNull(scalar.getAnnotation(RuntimeClass.of(Ann.class)));
		assertFalse(scalar.isAnnotationPresent(RuntimeClass.of(Ann.class)));
	}

	@Test
	void getDeclaredAnnotations_returns_present_annotations() throws Exception {
		Parameter[] p = paramsOf("annotated", String.class);
		assertEquals(1, RuntimeParameter.of(p[0]).getDeclaredAnnotations().length);
		assertEquals(0, RuntimeParameter.of(paramsOf("scalar", String.class, int.class)[0])
				.getDeclaredAnnotations().length);
	}

	@Test
	void equals_and_hashCode_track_underlying_parameter() throws Exception {
		Parameter p = paramsOf("scalar", String.class, int.class)[0];
		assertEquals(RuntimeParameter.of(p), RuntimeParameter.of(p));
		assertEquals(p.hashCode(), RuntimeParameter.of(p).hashCode());
		assertFalse(RuntimeParameter.of(p).equals("x"));
	}

	@Test
	void unwrap_returns_the_wrapped_parameter() throws Exception {
		Parameter p = paramsOf("scalar", String.class, int.class)[0];
		assertEquals(p, RuntimeParameter.of(p).unwrap());
	}
}

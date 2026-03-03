package com.garganttua.core.mapper.rules;

import java.util.Optional;
import java.util.function.Function;

import com.garganttua.core.reflection.IClass;

/**
 * Provides implicit type conversions for the mapper.
 * Supports String ↔ primitives/wrappers, String ↔ enum, and Optional unwrapping.
 */
public final class ImplicitConversions {

	private ImplicitConversions() {
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static Optional<Function<Object, Object>> findConversion(IClass<?> source, IClass<?> dest) {
		String srcName = source.getName();
		String dstName = dest.getName();

		// String -> enum
		if (srcName.equals("java.lang.String") && dest.isEnum()) {
			return Optional.of(val -> {
				if (val == null) return null;
				return Enum.valueOf((Class<Enum>) dest.getType(), (String) val);
			});
		}

		// enum -> String
		if (source.isEnum() && dstName.equals("java.lang.String")) {
			return Optional.of(val -> val == null ? null : ((Enum<?>) val).name());
		}

		// String -> Integer/int
		if (srcName.equals("java.lang.String") && (dstName.equals("java.lang.Integer") || dstName.equals("int"))) {
			return Optional.of(val -> val == null ? null : Integer.parseInt((String) val));
		}
		// Integer/int -> String
		if ((srcName.equals("java.lang.Integer") || srcName.equals("int")) && dstName.equals("java.lang.String")) {
			return Optional.of(val -> val == null ? null : val.toString());
		}

		// String -> Long/long
		if (srcName.equals("java.lang.String") && (dstName.equals("java.lang.Long") || dstName.equals("long"))) {
			return Optional.of(val -> val == null ? null : Long.parseLong((String) val));
		}
		// Long/long -> String
		if ((srcName.equals("java.lang.Long") || srcName.equals("long")) && dstName.equals("java.lang.String")) {
			return Optional.of(val -> val == null ? null : val.toString());
		}

		// String -> Double/double
		if (srcName.equals("java.lang.String") && (dstName.equals("java.lang.Double") || dstName.equals("double"))) {
			return Optional.of(val -> val == null ? null : Double.parseDouble((String) val));
		}
		// Double/double -> String
		if ((srcName.equals("java.lang.Double") || srcName.equals("double")) && dstName.equals("java.lang.String")) {
			return Optional.of(val -> val == null ? null : val.toString());
		}

		// String -> Float/float
		if (srcName.equals("java.lang.String") && (dstName.equals("java.lang.Float") || dstName.equals("float"))) {
			return Optional.of(val -> val == null ? null : Float.parseFloat((String) val));
		}
		// Float/float -> String
		if ((srcName.equals("java.lang.Float") || srcName.equals("float")) && dstName.equals("java.lang.String")) {
			return Optional.of(val -> val == null ? null : val.toString());
		}

		// String -> Boolean/boolean
		if (srcName.equals("java.lang.String") && (dstName.equals("java.lang.Boolean") || dstName.equals("boolean"))) {
			return Optional.of(val -> val == null ? null : Boolean.parseBoolean((String) val));
		}
		// Boolean/boolean -> String
		if ((srcName.equals("java.lang.Boolean") || srcName.equals("boolean")) && dstName.equals("java.lang.String")) {
			return Optional.of(val -> val == null ? null : val.toString());
		}

		// Optional<T> -> T (unwrap)
		if (srcName.equals("java.util.Optional")) {
			return Optional.of(val -> {
				if (val == null) return null;
				return ((java.util.Optional<?>) val).orElse(null);
			});
		}

		// T -> Optional<T> (wrap)
		if (dstName.equals("java.util.Optional")) {
			return Optional.of(val -> java.util.Optional.ofNullable(val));
		}

		return Optional.empty();
	}
}

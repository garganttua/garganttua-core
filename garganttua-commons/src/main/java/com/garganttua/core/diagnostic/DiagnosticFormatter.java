package com.garganttua.core.diagnostic;

/**
 * Internal helpers shared by {@link IDiagnostic} implementations.
 *
 * <p>The {@code {}}-placeholder format is SLF4J-compatible: each {@code {}} in
 * the format string is consumed by the next non-{@link Throwable} argument.
 * If the LAST argument is a {@link Throwable}, it is not used to substitute a
 * placeholder — it is treated as the cause and (typically) printed after the
 * formatted message.
 *
 * @since 2.0.0-ALPHA02
 */
public final class DiagnosticFormatter {

    private DiagnosticFormatter() {
    }

    /**
     * Split args into (substitution arguments, optional trailing throwable).
     * The throwable is detected as the LAST element being an instance of
     * {@link Throwable} — matching SLF4J's convention.
     */
    public static FormatArgs split(Object[] args) {
        if (args == null || args.length == 0) {
            return new FormatArgs(EMPTY, null);
        }
        Object last = args[args.length - 1];
        if (last instanceof Throwable t) {
            Object[] head = new Object[args.length - 1];
            System.arraycopy(args, 0, head, 0, head.length);
            return new FormatArgs(head, t);
        }
        return new FormatArgs(args, null);
    }

    /**
     * Substitute {@code {}} placeholders in {@code format} with successive
     * elements of {@code args}. Excess placeholders are left as literal
     * {@code {}}; excess args are silently ignored — same behavior as SLF4J.
     */
    public static String format(String format, Object[] args) {
        if (format == null) {
            return "null";
        }
        if (args == null || args.length == 0) {
            return format;
        }
        StringBuilder out = new StringBuilder(format.length() + args.length * 8);
        int argIdx = 0;
        int i = 0;
        final int n = format.length();
        while (i < n) {
            char c = format.charAt(i);
            if (c == '{' && i + 1 < n && format.charAt(i + 1) == '}' && argIdx < args.length) {
                out.append(stringOf(args[argIdx++]));
                i += 2;
            } else {
                out.append(c);
                i++;
            }
        }
        return out.toString();
    }

    private static String stringOf(Object o) {
        if (o == null) {
            return "null";
        }
        if (o.getClass().isArray()) {
            // Best-effort array rendering (mirrors SLF4J's Arrays.toString fallback).
            if (o instanceof Object[] arr) {
                return java.util.Arrays.deepToString(arr);
            }
            if (o instanceof int[] arr) return java.util.Arrays.toString(arr);
            if (o instanceof long[] arr) return java.util.Arrays.toString(arr);
            if (o instanceof double[] arr) return java.util.Arrays.toString(arr);
            if (o instanceof float[] arr) return java.util.Arrays.toString(arr);
            if (o instanceof boolean[] arr) return java.util.Arrays.toString(arr);
            if (o instanceof byte[] arr) return java.util.Arrays.toString(arr);
            if (o instanceof short[] arr) return java.util.Arrays.toString(arr);
            if (o instanceof char[] arr) return java.util.Arrays.toString(arr);
        }
        return o.toString();
    }

    private static final Object[] EMPTY = new Object[0];

    /**
     * Decomposed argument list: the placeholder-substitution args and the
     * optional trailing throwable.
     */
    public record FormatArgs(Object[] args, Throwable throwable) {
    }
}

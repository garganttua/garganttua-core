package com.garganttua.core.condition;

class ComparisonHelper {

    /**
     * Optional-aware operand normalisation shared by every value-inspecting
     * primitive condition (equals, notEquals, ordering comparisons). A present
     * Optional yields its value; an empty Optional yields {@code null} so it
     * compares as "no value"; non-Optional operands pass through unchanged.
     *
     * <p>Suppliers fed by request-arg style lookups return
     * {@code Optional.ofNullable(...)}, so without this the conditions would
     * compare the Optional wrapper itself ("Type mismatch: Optional VS String")
     * and silently return false. Mirrors the fix applied to
     * {@link NotNullCondition} / {@link NullCondition}.</p>
     */
    static Object unwrapOptional(Object o) {
        return o instanceof java.util.Optional<?> opt ? opt.orElse(null) : o;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    static int compare(Object a, Object b) {
        // If both are numbers or can be parsed as numbers, compare numerically
        Number na = toNumber(a);
        Number nb = toNumber(b);
        if (na != null && nb != null) {
            return Double.compare(na.doubleValue(), nb.doubleValue());
        }
        // Fall back to Comparable
        if (a instanceof Comparable ca && a.getClass().equals(b.getClass())) {
            return ca.compareTo(b);
        }
        // Compare string representations
        return a.toString().compareTo(b.toString());
    }

    private static Number toNumber(Object obj) {
        if (obj instanceof Number n) {
            return n;
        }
        if (obj instanceof String s) {
            try {
                return Long.parseLong(s);
            } catch (NumberFormatException e1) {
                try {
                    return Double.parseDouble(s);
                } catch (NumberFormatException e2) {
                    return null;
                }
            }
        }
        return null;
    }

    private ComparisonHelper() {
    }
}

package com.garganttua.core.workflow.generator;

/**
 * Stateless helpers that render individual Garganttua Script constructs as strings.
 *
 * <p>Used to assemble fragments of generated workflow scripts; all values destined
 * for string literals are escaped via the internal {@code escape}/{@code formatValue} routines.
 */
public final class ScriptTemplate {

    private ScriptTemplate() {
    }

    /** {@return an {@code include("path")} statement for the given (escaped) script path} */
    public static String include(String path) {
        return "include(\"" + escape(path) + "\")";
    }

    /** {@return the given content wrapped in a {@code (...)} statement group} */
    public static String inline(String content) {
        return "(" + content + ")";
    }

    /** {@return a {@code @name = expression} assignment (no execution)} */
    public static String variableAssignment(String name, String expression) {
        return "@" + name + " = " + expression;
    }

    /** {@return a {@code @varName <- expression} assignment that executes the expression} */
    public static String resultCapture(String varName, String expression) {
        return "@" + varName + " <- " + expression;
    }

    /** {@return {@code expression ! catchExpression}, attaching an immediate catch clause} */
    public static String catchClause(String expression, String catchExpression) {
        return expression + " ! " + catchExpression;
    }

    /** {@return a conditional pipe {@code @varName operator value | action}} */
    public static String condition(String varName, String operator, Object value, String action) {
        return "@" + varName + " " + operator + " " + formatValue(value) + " | " + action;
    }

    /** {@return a {@code # text} comment line} */
    public static String comment(String text) {
        return "# " + text;
    }

    /** {@return a {@code # Stage: stageName} comment line} */
    public static String stageComment(String stageName) {
        return "# Stage: " + stageName;
    }

    private static String escape(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private static String formatValue(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof String) {
            return "\"" + escape((String) value) + "\"";
        }
        return value.toString();
    }
}

package com.garganttua.core.workflow.generator;

public final class ScriptTemplate {

    private ScriptTemplate() {
    }

    public static String include(String path) {
        return "include(\"" + escape(path) + "\")";
    }

    public static String inline(String content) {
        return "(" + content + ")";
    }

    public static String variableAssignment(String name, String expression) {
        return "@" + name + " = " + expression;
    }

    public static String resultCapture(String varName, String expression) {
        return "@" + varName + " <- " + expression;
    }

    public static String catchClause(String expression, String catchExpression) {
        return expression + " ! " + catchExpression;
    }

    public static String condition(String varName, String operator, Object value, String action) {
        return "@" + varName + " " + operator + " " + formatValue(value) + " | " + action;
    }

    public static String comment(String text) {
        return "# " + text;
    }

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

package com.garganttua.core.runtime;

public record RuntimeExceptionRecord(String runtimeName, String stageName, String stepName, Throwable exception, Integer code, Boolean hasAborted) {

    public boolean matches(RuntimeExceptionRecord pattern) {
        if (pattern == null) return false;

        if (pattern.runtimeName != null && !pattern.runtimeName.equals(this.runtimeName))
            return false;

        if (pattern.stageName != null && !pattern.stageName.equals(this.stageName))
            return false;

        if (pattern.stepName != null && !pattern.stepName.equals(this.stepName))
            return false;

        if (pattern.exception != null) {
            if (this.exception == null) return false;

            Class<?> expected = pattern.exception.getClass();
            Class<?> actual = this.exception.getClass();

            if (!expected.isAssignableFrom(actual))
                return false;
        }

        if (pattern.code != null && !pattern.code.equals(this.code))
            return false;

        if (pattern.hasAborted != null && !pattern.hasAborted.equals(this.hasAborted))
            return false;

        return true;
    }

}

package com.garganttua.core.runtime;

public record RuntimeExceptionRecord(String runtimeName, String stageName, String stepName, Class<? extends Throwable> exceptionType, Throwable exception, Integer code, Boolean hasAborted, String executableReference) {

    public boolean matches(RuntimeExceptionRecord pattern) {
        if (pattern == null) return false;

        if (pattern.runtimeName != null && !pattern.runtimeName.equals(this.runtimeName))
            return false;

        if (pattern.stageName != null && !pattern.stageName.equals(this.stageName))
            return false;

        if (pattern.stepName != null && !pattern.stepName.equals(this.stepName))
            return false;

        if (pattern.exceptionType != null) {
            if (this.exceptionType == null) return false;

            Class<?> expected = pattern.exceptionType;
            Class<?> actual = this.exceptionType;

            if (!expected.isAssignableFrom(actual))
                return false;
        }

        if (pattern.code != null && !pattern.code.equals(this.code))
            return false;

        if (pattern.hasAborted != null && !pattern.hasAborted.equals(this.hasAborted))
            return false;

        return true;
    }

    public boolean matches(IRuntimeStepOnException onException) {
        return this.matches(new RuntimeExceptionRecord(onException.runtimeName(), onException.fromStage(), onException.fromStep(), onException.exception(), null, null, true, null));
    }

    public String exceptionMessage() {
        return this.exception.getMessage();
    }

}

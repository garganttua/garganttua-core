package com.garganttua.core.runtime;

public record RuntimeStepOperationPosition(Position position,
        Class<?> element) {

    public static RuntimeStepOperationPosition after(Class<?>  element) {
        return new RuntimeStepOperationPosition(Position.AFTER, element);
    }

    public static RuntimeStepOperationPosition before(Class<?>  element) {
        return new RuntimeStepOperationPosition(Position.BEFORE, element);
    }

}
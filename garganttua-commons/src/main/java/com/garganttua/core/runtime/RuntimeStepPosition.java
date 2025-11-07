package com.garganttua.core.runtime;


public record RuntimeStepPosition (Position position,
        String elementName) {

    public static RuntimeStepPosition after(String elementName) {
        return new RuntimeStepPosition(Position.AFTER, elementName);
    }

    public static RuntimeStepPosition before(String elementName) {
        return new RuntimeStepPosition(Position.BEFORE, elementName);
    }

}
package com.garganttua.core.runtime;

public record RuntimeStagePosition(Position position,
        String elementName) {

    public static RuntimeStagePosition after(String elementName) {
        return new RuntimeStagePosition(Position.AFTER, elementName);
    }

    public static RuntimeStagePosition before(String elementName) {
        return new RuntimeStagePosition(Position.BEFORE, elementName);
    }

}

package com.garganttua.core.configuration.populator;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;

public class PopulationContext {

    @Getter
    private final boolean strict;
    private final List<String> warnings = new ArrayList<>();
    private final List<String> errors = new ArrayList<>();
    @Getter
    private String currentPath = "";

    public PopulationContext(boolean strict) {
        this.strict = strict;
    }

    public void pushPath(String key) {
        this.currentPath = this.currentPath.isEmpty() ? key : this.currentPath + "." + key;
    }

    public void popPath() {
        int dot = this.currentPath.lastIndexOf('.');
        this.currentPath = dot > 0 ? this.currentPath.substring(0, dot) : "";
    }

    public void addWarning(String message) {
        this.warnings.add(this.currentPath + ": " + message);
    }

    public void addError(String message) {
        this.errors.add(this.currentPath + ": " + message);
    }

    public List<String> getWarnings() {
        return List.copyOf(this.warnings);
    }

    public List<String> getErrors() {
        return List.copyOf(this.errors);
    }

    public boolean hasErrors() {
        return !this.errors.isEmpty();
    }
}

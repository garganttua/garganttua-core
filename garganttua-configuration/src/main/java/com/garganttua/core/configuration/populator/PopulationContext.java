package com.garganttua.core.configuration.populator;

import java.util.ArrayList;
import java.util.List;

/**
 * Mutable state carried through a builder population pass: the current dot-path within the
 * configuration tree and the accumulated warnings and errors.
 */
public class PopulationContext {

    private final boolean strict;
    private final List<String> warnings = new ArrayList<>();
    private final List<String> errors = new ArrayList<>();
    private String currentPath = "";

    /**
     * Creates a population context.
     *
     * @param strict whether unknown keys are treated as errors rather than warnings
     */
    public PopulationContext(boolean strict) {
        this.strict = strict;
    }

    /**
     * Returns the current dot-path being populated.
     *
     * @return the current path, empty at the root
     */
    public String getCurrentPath() {
        return this.currentPath;
    }

    /**
     * Descends into a child key, appending it to the current path.
     *
     * @param key the child key to enter
     */
    public void pushPath(String key) {
        this.currentPath = this.currentPath.isEmpty() ? key : this.currentPath + "." + key;
    }

    /**
     * Ascends to the parent path, removing the last path segment.
     */
    public void popPath() {
        int dot = this.currentPath.lastIndexOf('.');
        this.currentPath = dot > 0 ? this.currentPath.substring(0, dot) : "";
    }

    /**
     * Records a warning prefixed with the current path.
     *
     * @param message the warning message
     */
    public void addWarning(String message) {
        this.warnings.add(this.currentPath + ": " + message);
    }

    /**
     * Records an error prefixed with the current path.
     *
     * @param message the error message
     */
    public void addError(String message) {
        this.errors.add(this.currentPath + ": " + message);
    }

    /**
     * Returns an immutable snapshot of the accumulated warnings.
     *
     * @return the warnings collected so far
     */
    public List<String> getWarnings() {
        return List.copyOf(this.warnings);
    }

    /**
     * Returns an immutable snapshot of the accumulated errors.
     *
     * @return the errors collected so far
     */
    public List<String> getErrors() {
        return List.copyOf(this.errors);
    }

    /**
     * Indicates whether any error has been recorded.
     *
     * @return {@code true} if at least one error was added
     */
    public boolean hasErrors() {
        return !this.errors.isEmpty();
    }
}

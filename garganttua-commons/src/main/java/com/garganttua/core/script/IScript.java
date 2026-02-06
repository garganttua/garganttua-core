package com.garganttua.core.script;

import java.io.File;
import java.io.InputStream;
import java.util.Optional;

public interface IScript {

    void load(String script) throws ScriptException;

    void load(File file) throws ScriptException;

    void load(InputStream inputStream) throws ScriptException;

    void compile() throws ScriptException;

    int execute(Object... args) throws ScriptException;

    <T> Optional<T> getVariable(String name, Class<T> type);

    /**
     * Sets an initial variable value before script execution.
     * Variables set this way will be available to the script via @varName references.
     *
     * @param name the variable name
     * @param value the variable value
     */
    void setVariable(String name, Object value);

    /**
     * Returns the output value set by the script via @output variable.
     * This is analogous to the runtime's output mechanism.
     *
     * @return an Optional containing the output if set, otherwise empty
     */
    Optional<Object> getOutput();

    /**
     * Returns the last exception that occurred during script execution, if any.
     * When an exception occurs during execution, it is captured here instead of
     * being rethrown. The execute() method will return an error code in this case.
     *
     * @return an Optional containing the exception if one occurred, otherwise empty
     */
    Optional<Throwable> getLastException();

    /**
     * Returns the message of the last exception that occurred during execution, if any.
     *
     * @return an Optional containing the exception message if one occurred, otherwise empty
     */
    Optional<String> getLastExceptionMessage();

    /**
     * Indicates whether the last execution was aborted due to an exception.
     *
     * @return true if the last execution was aborted, false otherwise
     */
    boolean hasAborted();
}

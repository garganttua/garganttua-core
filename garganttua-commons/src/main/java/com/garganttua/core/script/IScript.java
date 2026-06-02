package com.garganttua.core.script;

import java.io.File;
import java.io.InputStream;
import java.util.Optional;

import com.garganttua.core.reflection.IClass;

/**
 * A single-use, mutable script handle following the load &rarr; compile &rarr;
 * execute lifecycle.
 *
 * <p>
 * An {@code IScript} carries per-execution state (variables, output, last
 * exception) and is therefore <b>not</b> thread-safe. Obtain one from
 * {@link IScriptingEnvironment#newScript()}. For a thread-safe, reusable
 * handle compiled once and executed concurrently, use
 * {@link ICompiledScript} via {@link IScriptingEnvironment#precompile}.
 * </p>
 *
 * @see ICompiledScript
 * @see IScriptingEnvironment
 */
public interface IScript {

    /**
     * Loads script source from a string.
     *
     * @param script the {@code .gs} source code
     * @throws ScriptException if the source cannot be read or parsed
     */
    void load(String script) throws ScriptException;

    /**
     * Loads script source from a file.
     *
     * @param file the {@code .gs} file to read
     * @throws ScriptException if the file cannot be read or parsed
     */
    void load(File file) throws ScriptException;

    /**
     * Loads script source from an input stream.
     *
     * @param inputStream the stream to read the source from
     * @throws ScriptException if the stream cannot be read or parsed
     */
    void load(InputStream inputStream) throws ScriptException;

    /**
     * Compiles the previously loaded source into an executable runtime.
     *
     * @throws ScriptException if compilation fails
     */
    void compile() throws ScriptException;

    /**
     * Executes the compiled script.
     *
     * @param args positional script arguments (mapped to {@code @0}, {@code @1}, &hellip;)
     * @return the script exit code; {@code 0} on success, non-zero on failure
     * @throws ScriptException if execution fails outside the script's own error handling
     */
    int execute(Object... args) throws ScriptException;

    /**
     * Retrieves a named script variable, coerced to the requested type.
     *
     * @param <T>  the expected variable type
     * @param name the variable name
     * @param type the class of the expected type
     * @return an {@link Optional} holding the value, or empty if unset
     */
    <T> Optional<T> getVariable(String name, IClass<T> type);

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

package com.garganttua.core.script.functions;

import java.io.File;
import java.io.InputStream;

import com.garganttua.core.classloader.ClassLoaderException;
import com.garganttua.core.classloader.IClassLoaderManager;
import com.garganttua.core.observability.Logger;
import com.garganttua.core.expression.ExpressionException;
import com.garganttua.core.expression.annotations.Expression;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.script.IScript;
import com.garganttua.core.script.ScriptException;
import com.garganttua.core.script.context.ScriptContext;
import com.garganttua.core.script.context.ScriptExecutionContext;

import jakarta.annotation.Nullable;

import com.garganttua.core.reflection.annotations.Reflected;

/**
 * Core built-in script expression functions: console output ({@code print}/{@code eprint}),
 * type casting, {@link String#format} string formatting, and script composition
 * ({@code include}, {@code call}, {@code execute_script}, {@code script_variable}).
 */
@Reflected(queryAllDeclaredMethods = true)
public class ScriptFunctions {
    private static final Logger log = Logger.getLogger(ScriptFunctions.class);

    private ScriptFunctions() {
    }

    /**
     * Prints a single value to standard output.
     *
     * @param value the value to print
     * @return the printed string (allows chaining, e.g., time(print("hello")))
     */
    @Expression(name = "print", description = "Prints a single value to standard output")
    public static String print(@Nullable Object value) {
        String str = value == null ? "null" : value.toString();
        System.out.println(str);
        return str;
    }

    /**
     * Prints String and int to standard output.
     *
     * @param value1 the string value
     * @param value2 the int value
     * @return the printed string
     */
    @Expression(name = "print", description = "Prints String and int to standard output")
    public static String print(@Nullable String value1, int value2) {
        String s1 = value1 == null ? "null" : value1;
        String result = s1 + value2;
        System.out.println(result);
        return result;
    }

    /**
     * Prints a value to standard output with newline.
     *
     * @param value the value to print
     * @return the printed string
     */
    @Expression(name = "println", description = "Prints a value to standard output with newline")
    public static String println(@Nullable Object value) {
        return print(value);
    }

    // ========== Cast Function ==========

    /**
     * Casts a value to the given type, returning {@code null} when the value is {@code null}.
     *
     * @param <T>   the target type
     * @param type  the target type wrapper
     * @param value the value to cast
     * @return the value cast to {@code T}, or {@code null}
     * @throws ExpressionException if {@code type} is {@code null} or the value is not assignable
     */
    @Expression(name = "cast", description = "Casts a value to the specified type")
    public static <T> T cast(@Nullable IClass<T> type, @Nullable Object value) {
        log.debug("cast({}, {})", type, value);
        if (type == null) {
            throw new ExpressionException("cast: type cannot be null");
        }
        if (value == null) {
            return null;
        }
        if (type.isInstance(value)) {
            return type.cast(value);
        }
        throw new ExpressionException("cast: cannot cast " + value.getClass().getName() + " to " + type.getName());
    }

    // ========== Standard Error Output Functions ==========

    /**
     * Prints a value to standard error.
     *
     * @param value the value to print
     * @return the printed string
     */
    @Expression(name = "eprint", description = "Prints a value to standard error")
    public static String eprint(@Nullable Object value) {
        String str = value == null ? "null" : value.toString();
        System.err.println(str);
        return str;
    }

    /**
     * Prints a value to standard error with newline (alias for eprint).
     *
     * @param value the value to print
     * @return the printed string
     */
    @Expression(name = "eprintln", description = "Prints a value to standard error with newline")
    public static String eprintln(@Nullable Object value) {
        return eprint(value);
    }

    // ========== Format Functions ==========

    /**
     * Formats a string using {@link String#format} with one argument.
     *
     * <p>Usage in script:</p>
     * <pre>{@code
     * msg <- format("Hello %s", @name)
     * msg <- format("Count: %d", @count)
     * }</pre>
     *
     * @param pattern the format pattern (see {@link java.util.Formatter})
     * @param arg1 the first argument
     * @return the formatted string
     */
    @Expression(name = "format", description = "Formats a string using String.format with 1 argument")
    public static String format(@Nullable Object pattern, @Nullable Object arg1) {
        if (pattern == null) {
            return "null";
        }
        return String.format(pattern.toString(), arg1);
    }

    /**
     * Formats a string using {@link String#format} with two arguments.
     *
     * @param pattern the format pattern
     * @param arg1 the first argument
     * @param arg2 the second argument
     * @return the formatted string
     */
    @Expression(name = "format", description = "Formats a string using String.format with 2 arguments")
    public static String format(@Nullable Object pattern, @Nullable Object arg1, @Nullable Object arg2) {
        if (pattern == null) {
            return "null";
        }
        return String.format(pattern.toString(), arg1, arg2);
    }

    /**
     * Formats a string using {@link String#format} with three arguments.
     *
     * @param pattern the format pattern
     * @param arg1 the first argument
     * @param arg2 the second argument
     * @param arg3 the third argument
     * @return the formatted string
     */
    @Expression(name = "format", description = "Formats a string using String.format with 3 arguments")
    public static String format(@Nullable Object pattern, @Nullable Object arg1, @Nullable Object arg2,
            @Nullable Object arg3) {
        if (pattern == null) {
            return "null";
        }
        return String.format(pattern.toString(), arg1, arg2, arg3);
    }

    // ========== Include/Call Functions ==========

    private static final String CLASSPATH_PREFIX = "classpath:";

    /**
     * Includes a {@code .jar} plugin or compiles a {@code .gs} script for later invocation.
     * A {@code classpath:} prefix resolves the path as a classpath resource (scripts only).
     *
     * @param path the file path or {@code classpath:} resource of the {@code .jar} or {@code .gs}
     * @return the loaded JAR path, or the registered name of the included script
     * @throws ExpressionException if the path is blank, no script context is active,
     *                             the file type is unsupported, or loading fails
     */
    @Expression(name = "include", description = "Includes a JAR or a script file (.gs). Supports classpath: prefix for classpath resources.")
    public static String include(@Nullable String path) {
        log.debug("include({})", path);
        if (path == null || path.isBlank()) {
            throw new ExpressionException("include: path cannot be null or blank");
        }

        ScriptContext ctx = ScriptExecutionContext.get();
        if (ctx == null) {
            throw new ExpressionException("include: no script execution context available");
        }

        boolean isClasspath = path.startsWith(CLASSPATH_PREFIX);
        String resolvedPath = isClasspath ? path.substring(CLASSPATH_PREFIX.length()) : path;

        if (resolvedPath.endsWith(".jar")) {
            if (isClasspath) {
                throw new ExpressionException("include: classpath: prefix is not supported for JAR files: " + path);
            }
            includeJar(ctx, resolvedPath);
            return resolvedPath;
        } else if (resolvedPath.endsWith(".gs")) {
            if (isClasspath) {
                return includeClasspathScript(ctx, resolvedPath);
            }
            return includeScript(ctx, resolvedPath);
        } else {
            throw new ExpressionException("include: unsupported file type: " + path
                    + ". Expected .jar or .gs");
        }
    }

    /**
     * Executes a previously {@code include}d script by its registered name.
     *
     * @param name the registered script name
     * @return the script's exit code
     * @throws ExpressionException if the name is blank, no script context is active,
     *                             or no script is registered under {@code name}
     */
    @Expression(name = "call", description = "Calls an included script by name")
    public static int call(@Nullable String name) {
        log.debug("call({})", name);
        if (name == null || name.isBlank()) {
            throw new ExpressionException("call: script name cannot be null or blank");
        }

        ScriptContext ctx = ScriptExecutionContext.get();
        if (ctx == null) {
            throw new ExpressionException("call: no script execution context available");
        }

        IScript script = ctx.getIncludedScript(name);
        if (script == null) {
            throw new ExpressionException("call: script not found: " + name
                    + ". Did you call include() first?");
        }

        return script.execute();
    }


    private static void includeJar(ScriptContext ctx, String path) {
        try {
            IClassLoaderManager mgr = ctx.getClassLoaderManager();
            if (mgr == null) {
                log.warn("No IClassLoaderManager configured for ScriptContext, "
                        + "cannot hot-load JAR via include(): {}", path);
                return;
            }
            mgr.loadJar(java.nio.file.Path.of(path));
            log.debug("JAR loaded via IClassLoaderManager: {}", path);
        } catch (ClassLoaderException e) {
            throw new ExpressionException("include: failed to load JAR: " + path + " - " + e.getMessage());
        } catch (Exception e) {
            throw new ExpressionException("include: failed to load JAR: " + path + " - " + e.getMessage());
        }
    }

    private static String includeScript(ScriptContext ctx, String path) {
        try {
            File scriptFile = new File(path);
            if (!scriptFile.exists()) {
                throw new ExpressionException("include: script file not found: " + path);
            }

            ScriptContext subScript = ctx.createChildScript();
            subScript.load(scriptFile);
            subScript.compile();

            String name = scriptFile.getName().replaceFirst("\\.gs$", "");
            ctx.registerIncludedScript(name, subScript);

            log.debug("Script included as '{}' from {}", name, path);
            return name;
        } catch (ScriptException e) {
            throw new ExpressionException("include: failed to load script: " + path + " - " + e.getMessage());
        }
    }

    private static String includeClasspathScript(ScriptContext ctx, String resource) {
        try {
            InputStream is = resolveClasspathResource(resource);

            try (is) {
                ScriptContext subScript = ctx.createChildScript();
                subScript.load(is);
                subScript.compile();

                String name = resource.contains("/")
                        ? resource.substring(resource.lastIndexOf('/') + 1)
                        : resource;
                name = name.replaceFirst("\\.gs$", "");
                ctx.registerIncludedScript(name, subScript);

                log.debug("Script included as '{}' from classpath:{}", name, resource);
                return name;
            }
        } catch (ExpressionException e) {
            throw e;
        } catch (Exception e) {
            throw new ExpressionException("include: failed to load classpath script: " + resource + " - " + e.getMessage());
        }
    }

    private static InputStream resolveClasspathResource(String resource) {
        InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(resource);
        if (is == null) {
            is = ScriptFunctions.class.getClassLoader().getResourceAsStream(resource);
        }
        if (is == null) {
            throw new ExpressionException("include: classpath resource not found: " + resource);
        }
        return is;
    }

    // ========== Execute Script Functions ==========

    private static int executeScriptImpl(Object name, Object... args) {
        log.debug("execute_script({}, {} args)", name, args != null ? args.length : 0);
        if (name == null) {
            throw new ExpressionException("execute_script: script name cannot be null");
        }

        ScriptContext ctx = ScriptExecutionContext.get();
        if (ctx == null) {
            throw new ExpressionException("execute_script: no script execution context available");
        }

        String scriptName = name.toString();
        IScript script = ctx.getIncludedScript(scriptName);
        if (script == null) {
            throw new ExpressionException("execute_script: script not found: " + scriptName
                    + ". Did you call include() first?");
        }

        return script.execute(args != null ? args : new Object[0]);
    }

    /**
     * Executes a previously included script with no arguments.
     *
     * @param name the registered script name
     * @return the script's exit code
     * @throws ExpressionException if the name is {@code null}, no script context is active,
     *                             or no script is registered under {@code name}
     */
    @Expression(name = "execute_script", description = "Executes an included script with no arguments")
    public static int executeScript(@Nullable Object name) {
        return executeScriptImpl(name);
    }

    /**
     * Executes a previously included script with one positional argument.
     *
     * @param name the registered script name
     * @param arg0 the first positional argument (accessible as {@code @0})
     * @return the script's exit code
     */
    @Expression(name = "execute_script", description = "Executes an included script with 1 argument")
    public static int executeScript(@Nullable Object name, @Nullable Object arg0) {
        return executeScriptImpl(name, arg0);
    }

    /**
     * Executes a previously included script with 2 positional arguments (accessible as {@code @0}..{@code @1}).
     *
     * @param name the registered script name
     * @param arg0 positional argument 0
     * @param arg1 positional argument 1
     * @return the script's exit code
     */
    @Expression(name = "execute_script", description = "Executes an included script with 2 arguments")
    public static int executeScript(@Nullable Object name, @Nullable Object arg0, @Nullable Object arg1) {
        return executeScriptImpl(name, arg0, arg1);
    }

    /**
     * Executes a previously included script with 3 positional arguments (accessible as {@code @0}..{@code @2}).
     *
     * @param name the registered script name
     * @param arg0 positional argument 0
     * @param arg1 positional argument 1
     * @param arg2 positional argument 2
     * @return the script's exit code
     */
    @Expression(name = "execute_script", description = "Executes an included script with 3 arguments")
    public static int executeScript(@Nullable Object name, @Nullable Object arg0, @Nullable Object arg1,
            @Nullable Object arg2) {
        return executeScriptImpl(name, arg0, arg1, arg2);
    }

    /**
     * Executes a previously included script with 4 positional arguments (accessible as {@code @0}..{@code @3}).
     *
     * @param name the registered script name
     * @param arg0 positional argument 0
     * @param arg1 positional argument 1
     * @param arg2 positional argument 2
     * @param arg3 positional argument 3
     * @return the script's exit code
     */
    @Expression(name = "execute_script", description = "Executes an included script with 4 arguments")
    public static int executeScript(@Nullable Object name, @Nullable Object arg0, @Nullable Object arg1,
            @Nullable Object arg2, @Nullable Object arg3) {
        return executeScriptImpl(name, arg0, arg1, arg2, arg3);
    }

    /**
     * Executes a previously included script with 5 positional arguments (accessible as {@code @0}..{@code @4}).
     *
     * @param name the registered script name
     * @param arg0 positional argument 0
     * @param arg1 positional argument 1
     * @param arg2 positional argument 2
     * @param arg3 positional argument 3
     * @param arg4 positional argument 4
     * @return the script's exit code
     */
    @Expression(name = "execute_script", description = "Executes an included script with 5 arguments")
    public static int executeScript(@Nullable Object name, @Nullable Object arg0, @Nullable Object arg1,
            @Nullable Object arg2, @Nullable Object arg3, @Nullable Object arg4) {
        return executeScriptImpl(name, arg0, arg1, arg2, arg3, arg4);
    }

    /**
     * Executes a previously included script with 6 positional arguments (accessible as {@code @0}..{@code @5}).
     *
     * @param name the registered script name
     * @param arg0 positional argument 0
     * @param arg1 positional argument 1
     * @param arg2 positional argument 2
     * @param arg3 positional argument 3
     * @param arg4 positional argument 4
     * @param arg5 positional argument 5
     * @return the script's exit code
     */
    @Expression(name = "execute_script", description = "Executes an included script with 6 arguments")
    public static int executeScript(@Nullable Object name, @Nullable Object arg0, @Nullable Object arg1,
            @Nullable Object arg2, @Nullable Object arg3, @Nullable Object arg4, @Nullable Object arg5) {
        return executeScriptImpl(name, arg0, arg1, arg2, arg3, arg4, arg5);
    }

    /**
     * Executes a previously included script with 7 positional arguments (accessible as {@code @0}..{@code @6}).
     *
     * @param name the registered script name
     * @param arg0 positional argument 0
     * @param arg1 positional argument 1
     * @param arg2 positional argument 2
     * @param arg3 positional argument 3
     * @param arg4 positional argument 4
     * @param arg5 positional argument 5
     * @param arg6 positional argument 6
     * @return the script's exit code
     */
    @Expression(name = "execute_script", description = "Executes an included script with 7 arguments")
    public static int executeScript(@Nullable Object name, @Nullable Object arg0, @Nullable Object arg1,
            @Nullable Object arg2, @Nullable Object arg3, @Nullable Object arg4, @Nullable Object arg5,
            @Nullable Object arg6) {
        return executeScriptImpl(name, arg0, arg1, arg2, arg3, arg4, arg5, arg6);
    }

    /**
     * Executes a previously included script with 8 positional arguments (accessible as {@code @0}..{@code @7}).
     *
     * @param name the registered script name
     * @param arg0 positional argument 0
     * @param arg1 positional argument 1
     * @param arg2 positional argument 2
     * @param arg3 positional argument 3
     * @param arg4 positional argument 4
     * @param arg5 positional argument 5
     * @param arg6 positional argument 6
     * @param arg7 positional argument 7
     * @return the script's exit code
     */
    @Expression(name = "execute_script", description = "Executes an included script with 8 arguments")
    public static int executeScript(@Nullable Object name, @Nullable Object arg0, @Nullable Object arg1,
            @Nullable Object arg2, @Nullable Object arg3, @Nullable Object arg4, @Nullable Object arg5,
            @Nullable Object arg6, @Nullable Object arg7) {
        return executeScriptImpl(name, arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7);
    }

    /**
     * Executes a previously included script with 9 positional arguments (accessible as {@code @0}..{@code @8}).
     *
     * @param name the registered script name
     * @param arg0 positional argument 0
     * @param arg1 positional argument 1
     * @param arg2 positional argument 2
     * @param arg3 positional argument 3
     * @param arg4 positional argument 4
     * @param arg5 positional argument 5
     * @param arg6 positional argument 6
     * @param arg7 positional argument 7
     * @param arg8 positional argument 8
     * @return the script's exit code
     */
    @Expression(name = "execute_script", description = "Executes an included script with 9 arguments")
    public static int executeScript(@Nullable Object name, @Nullable Object arg0, @Nullable Object arg1,
            @Nullable Object arg2, @Nullable Object arg3, @Nullable Object arg4, @Nullable Object arg5,
            @Nullable Object arg6, @Nullable Object arg7, @Nullable Object arg8) {
        return executeScriptImpl(name, arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8);
    }

    /**
     * Executes a previously included script with 10 positional arguments (accessible as {@code @0}..{@code @9}).
     *
     * @param name the registered script name
     * @param arg0 positional argument 0
     * @param arg1 positional argument 1
     * @param arg2 positional argument 2
     * @param arg3 positional argument 3
     * @param arg4 positional argument 4
     * @param arg5 positional argument 5
     * @param arg6 positional argument 6
     * @param arg7 positional argument 7
     * @param arg8 positional argument 8
     * @param arg9 positional argument 9
     * @return the script's exit code
     */
    @Expression(name = "execute_script", description = "Executes an included script with 10 arguments")
    public static int executeScript(@Nullable Object name, @Nullable Object arg0, @Nullable Object arg1,
            @Nullable Object arg2, @Nullable Object arg3, @Nullable Object arg4, @Nullable Object arg5,
            @Nullable Object arg6, @Nullable Object arg7, @Nullable Object arg8, @Nullable Object arg9) {
        return executeScriptImpl(name, arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9);
    }

    // ========== Script Variable Function ==========

    /**
     * Retrieves a variable from an included script after it has been executed.
     *
     * @param scriptName the registered script name
     * @param varName    the variable name to read
     * @return the variable value, or {@code null} if unset
     * @throws ExpressionException if either argument is blank/null, no script context is active,
     *                             or no script is registered under {@code scriptName}
     */
    @Expression(name = "script_variable", description = "Retrieves a variable from an included script after execution")
    public static Object scriptVariable(@Nullable Object scriptName, @Nullable String varName) {
        log.debug("script_variable({}, {})", scriptName, varName);
        if (scriptName == null) {
            throw new ExpressionException("script_variable: script name cannot be null");
        }
        if (varName == null || varName.isBlank()) {
            throw new ExpressionException("script_variable: variable name cannot be null or blank");
        }

        ScriptContext ctx = ScriptExecutionContext.get();
        if (ctx == null) {
            throw new ExpressionException("script_variable: no script execution context available");
        }

        String name = scriptName.toString();
        IScript script = ctx.getIncludedScript(name);
        if (script == null) {
            throw new ExpressionException("script_variable: script not found: " + name
                    + ". Did you call include() and execute_script() first?");
        }

        return script.getVariable(varName, IClass.getClass(Object.class)).orElse(null);
    }
}

package com.garganttua.core.script.functions;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.garganttua.core.bootstrap.dsl.IBoostrap;
import com.garganttua.core.mutex.IMutex;
import com.garganttua.core.mutex.MutexException;
import com.garganttua.core.mutex.MutexStrategy;
import com.garganttua.core.supply.ISupplier;
import com.garganttua.core.dsl.DslException;
import com.garganttua.core.expression.ExpressionException;
import com.garganttua.core.expression.annotations.Expression;
import com.garganttua.core.script.IScript;
import com.garganttua.core.script.ScriptException;
import com.garganttua.core.script.context.ScriptContext;
import com.garganttua.core.script.context.ScriptExecutionContext;
import com.garganttua.core.script.loader.JarManifestReader;

import jakarta.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ScriptFunctions {

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

    @Expression(name = "include", description = "Includes a JAR or a script file (.gs)")
    public static String include(@Nullable String path) {
        log.atDebug().log("include({})", path);
        if (path == null || path.isBlank()) {
            throw new ExpressionException("include: path cannot be null or blank");
        }

        ScriptContext ctx = ScriptExecutionContext.get();
        if (ctx == null) {
            throw new ExpressionException("include: no script execution context available");
        }

        if (path.endsWith(".jar")) {
            includeJar(ctx, path);
            return path;
        } else if (path.endsWith(".gs")) {
            return includeScript(ctx, path);
        } else {
            throw new ExpressionException("include: unsupported file type: " + path
                    + ". Expected .jar or .gs");
        }
    }

    @Expression(name = "call", description = "Calls an included script by name")
    public static int call(@Nullable String name) {
        log.atDebug().log("call({})", name);
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

    // ========== Time Measurement Functions ==========

    /**
     * Measures the execution time of an expression.
     *
     * <p>The expression is passed lazily as an ISupplier, meaning it is NOT evaluated
     * before being passed to this function. The execution time is measured when the
     * supplier is invoked.</p>
     *
     * <p>Usage examples in script:</p>
     * <pre>
     * // Measure time of any expression (expression is passed lazily)
     * elapsed <- time(print("hello"))
     * elapsed <- time(someExpensiveOperation())
     *
     * // Measure time of a stored expression
     * expr = someExpensiveOperation()
     * elapsed <- time(@expr)
     * print(concatenate("Operation took ", string(elapsed), "ms"))
     * </pre>
     *
     * @param expression the expression to measure (passed lazily as ISupplier)
     * @return the elapsed time in milliseconds
     */
    @Expression(name = "time", description = "Measures execution time of an expression in milliseconds")
    public static long time(@Nullable ISupplier<?> expression) {
        log.atDebug().log("time(ISupplier)");

        if (expression == null) {
            return 0L;
        }

        long startTime = System.currentTimeMillis();

        try {
            expression.supply();
        } catch (Exception e) {
            // Still return elapsed time even on failure
            long elapsed = System.currentTimeMillis() - startTime;
            log.atDebug().log("time: expression failed after {}ms: {}", elapsed, e.getMessage());
            throw new ExpressionException("time: expression execution failed: " + e.getMessage());
        }

        long elapsed = System.currentTimeMillis() - startTime;
        log.atDebug().log("time: execution completed in {}ms", elapsed);
        return elapsed;
    }

    /**
     * Measures the execution time of an expression and returns both time and result.
     *
     * <p>Returns an array where [0] is the elapsed time in milliseconds and [1] is the result.</p>
     *
     * <p>The expression is passed lazily as an ISupplier, meaning it is NOT evaluated
     * before being passed to this function.</p>
     *
     * <p>Usage examples in script:</p>
     * <pre>
     * // Measure and get result
     * result <- timeWithResult(someOperation())
     * // result[0] = elapsed time, result[1] = operation result
     *
     * // Or with stored expression
     * expr = someOperation()
     * result <- timeWithResult(@expr)
     * </pre>
     *
     * @param expression the expression to measure (passed lazily as ISupplier)
     * @return array of [elapsedMs, result]
     */
    @Expression(name = "timeWithResult", description = "Measures execution time and returns [timeMs, result]")
    public static Object[] timeWithResult(@Nullable ISupplier<?> expression) {
        log.atDebug().log("timeWithResult(ISupplier)");

        if (expression == null) {
            return new Object[] { 0L, null };
        }

        long startTime = System.currentTimeMillis();
        Object result = null;

        try {
            result = expression.supply().orElse(null);
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - startTime;
            throw new ExpressionException("timeWithResult: expression execution failed after " + elapsed + "ms: " + e.getMessage());
        }

        long elapsed = System.currentTimeMillis() - startTime;
        log.atDebug().log("timeWithResult: execution completed in {}ms", elapsed);
        return new Object[] { elapsed, result };
    }

    // ========== Time Unit Functions ==========

    @Expression(name = "milliseconds", description = "Returns the value as milliseconds (identity function)")
    public static long milliseconds(long value) {
        return value;
    }

    @Expression(name = "milliseconds", description = "Returns the value as milliseconds (identity function)")
    public static long milliseconds(int value) {
        return value;
    }

    @Expression(name = "seconds", description = "Converts seconds to milliseconds")
    public static long seconds(long value) {
        return TimeUnit.SECONDS.toMillis(value);
    }

    @Expression(name = "seconds", description = "Converts seconds to milliseconds")
    public static long seconds(int value) {
        return TimeUnit.SECONDS.toMillis(value);
    }

    @Expression(name = "minutes", description = "Converts minutes to milliseconds")
    public static long minutes(long value) {
        return TimeUnit.MINUTES.toMillis(value);
    }

    @Expression(name = "minutes", description = "Converts minutes to milliseconds")
    public static long minutes(int value) {
        return TimeUnit.MINUTES.toMillis(value);
    }

    @Expression(name = "hours", description = "Converts hours to milliseconds")
    public static long hours(long value) {
        return TimeUnit.HOURS.toMillis(value);
    }

    @Expression(name = "hours", description = "Converts hours to milliseconds")
    public static long hours(int value) {
        return TimeUnit.HOURS.toMillis(value);
    }

    // ========== Retry Functions ==========

    /**
     * Retries the execution of a supplier until it succeeds or max attempts are reached.
     *
     * <p>The expression is passed lazily as an ISupplier and will be re-evaluated
     * on each retry attempt.</p>
     *
     * <p>Usage examples in script:</p>
     * <pre>
     * // Re-evaluates on each attempt:
     * result &lt;- retry(3, seconds(10), riskyOperation())
     *
     * // With stored expression (also re-evaluates):
     * expr = riskyOperation()
     * result &lt;- retry(3, seconds(10), @expr)
     * </pre>
     *
     * @param maxAttempts maximum number of attempts (must be >= 1)
     * @param delayMs delay between attempts in milliseconds
     * @param expression the expression to execute (passed lazily as ISupplier)
     * @return the result of the successful execution
     * @throws ExpressionException if all attempts fail
     */
    @Expression(name = "retry", description = "Retries a supplier expression with delay between attempts")
    public static Object retry(int maxAttempts, long delayMs, @Nullable ISupplier<?> expression) {
        log.atDebug().log("retry({}, {}ms, ISupplier)", maxAttempts, delayMs);

        if (maxAttempts < 1) {
            throw new ExpressionException("retry: maxAttempts must be >= 1, got: " + maxAttempts);
        }
        if (delayMs < 0) {
            throw new ExpressionException("retry: delay cannot be negative, got: " + delayMs);
        }

        // If expression is null, return null immediately
        if (expression == null) {
            return null;
        }

        Throwable lastException = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                log.atTrace().log("retry: attempt {}/{}", attempt, maxAttempts);
                Object result = expression.supply().orElse(null);
                log.atDebug().log("retry: succeeded on attempt {}", attempt);
                return result;
            } catch (Exception e) {
                lastException = e;
                log.atDebug().log("retry: attempt {}/{} failed: {}", attempt, maxAttempts, e.getMessage());

                if (attempt < maxAttempts && delayMs > 0) {
                    try {
                        log.atTrace().log("retry: waiting {}ms before next attempt", delayMs);
                        Thread.sleep(delayMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new ExpressionException("retry: interrupted while waiting: " + ie.getMessage());
                    }
                }
            }
        }

        String msg = "retry: all " + maxAttempts + " attempts failed";
        if (lastException != null) {
            msg += ": " + lastException.getMessage();
        }
        throw new ExpressionException(msg);
    }

    /**
     * Retries with exponential backoff.
     *
     * <p>The delay doubles after each failed attempt, starting from initialDelayMs.</p>
     *
     * <p>The expression is passed lazily as an ISupplier and will be re-evaluated
     * on each retry attempt.</p>
     *
     * @param maxAttempts maximum number of attempts (must be >= 1)
     * @param initialDelayMs initial delay in milliseconds (doubles after each failure)
     * @param maxDelayMs maximum delay cap in milliseconds
     * @param expression the expression to execute (passed lazily as ISupplier)
     * @return the result of the successful execution
     * @throws ExpressionException if all attempts fail
     */
    @Expression(name = "retryWithBackoff", description = "Retries with exponential backoff (delay doubles after each failure)")
    public static Object retryWithBackoff(int maxAttempts, long initialDelayMs, long maxDelayMs,
            @Nullable ISupplier<?> expression) {
        log.atDebug().log("retryWithBackoff({}, {}ms initial, {}ms max, ISupplier)",
                maxAttempts, initialDelayMs, maxDelayMs);

        if (maxAttempts < 1) {
            throw new ExpressionException("retryWithBackoff: maxAttempts must be >= 1, got: " + maxAttempts);
        }
        if (initialDelayMs < 0) {
            throw new ExpressionException("retryWithBackoff: initialDelay cannot be negative, got: " + initialDelayMs);
        }
        if (maxDelayMs < initialDelayMs) {
            throw new ExpressionException("retryWithBackoff: maxDelay must be >= initialDelay");
        }

        // If expression is null, return null immediately
        if (expression == null) {
            return null;
        }

        Throwable lastException = null;
        long currentDelay = initialDelayMs;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                log.atTrace().log("retryWithBackoff: attempt {}/{}", attempt, maxAttempts);
                Object result = expression.supply().orElse(null);
                log.atDebug().log("retryWithBackoff: succeeded on attempt {}", attempt);
                return result;
            } catch (Exception e) {
                lastException = e;
                log.atDebug().log("retryWithBackoff: attempt {}/{} failed: {}", attempt, maxAttempts, e.getMessage());

                if (attempt < maxAttempts && currentDelay > 0) {
                    try {
                        log.atTrace().log("retryWithBackoff: waiting {}ms before next attempt", currentDelay);
                        Thread.sleep(currentDelay);
                        currentDelay = Math.min(currentDelay * 2, maxDelayMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new ExpressionException("retryWithBackoff: interrupted while waiting: " + ie.getMessage());
                    }
                }
            }
        }

        String msg = "retryWithBackoff: all " + maxAttempts + " attempts failed";
        if (lastException != null) {
            msg += ": " + lastException.getMessage();
        }
        throw new ExpressionException(msg);
    }

    // ========== Synchronized (Mutex) Functions ==========

    /**
     * Executes an expression within a synchronized mutex lock.
     *
     * <p>This function provides distributed or local mutex synchronization
     * for critical sections. The mutex implementation (IMutex) determines
     * whether the lock is local (JVM) or distributed (e.g., Redis).</p>
     *
     * <p>The expression is passed lazily and executed only after the lock is acquired.</p>
     *
     * <p>Usage examples in script:</p>
     * <pre>
     * // Acquire mode - waits for lock with timeout
     * result &lt;- synchronized("my-mutex", $redisMutex, "acquire", seconds(30), myExpression())
     *
     * // TryAcquire mode - immediate, fails if lock not available
     * result &lt;- synchronized("my-mutex", $localMutex, "tryAcquire", milliseconds(0), myExpression())
     *
     * // With bean lookup for mutex
     * result &lt;- synchronized("order-lock", bean("myRedisMutex"), "acquire", seconds(10), processOrder())
     * </pre>
     *
     * @param mutexName the name/identifier for the mutex lock
     * @param mutex the IMutex implementation to use (local or distributed)
     * @param mode acquisition mode: "acquire" (wait with timeout) or "tryAcquire" (immediate)
     * @param timeoutMs timeout in milliseconds (use seconds(), milliseconds(), etc.)
     * @param expression the expression to execute within the lock (passed lazily as ISupplier)
     * @return the result of the expression execution
     * @throws ExpressionException if lock acquisition fails or expression execution fails
     */
    @Expression(name = "synchronized", description = "Executes expression within a mutex lock")
    public static Object synchronizedExec(
            @Nullable String mutexName,
            @Nullable IMutex mutex,
            @Nullable String mode,
            long timeoutMs,
            @Nullable ISupplier<?> expression) {

        log.atDebug().log("synchronized('{}', mutex, '{}', {}ms, ISupplier)",
                mutexName, mode, timeoutMs);

        // Validate parameters
        if (mutexName == null || mutexName.isBlank()) {
            throw new ExpressionException("synchronized: mutexName cannot be null or blank");
        }
        if (mutex == null) {
            throw new ExpressionException("synchronized: mutex cannot be null. " +
                    "Provide an IMutex instance (e.g., via bean() function)");
        }
        if (mode == null || mode.isBlank()) {
            throw new ExpressionException("synchronized: mode cannot be null. " +
                    "Use 'acquire' (wait with timeout) or 'tryAcquire' (immediate)");
        }
        if (expression == null) {
            return null;
        }

        // Determine acquisition strategy based on mode
        MutexStrategy strategy = createStrategy(mode, timeoutMs);

        try {
            return mutex.acquire(() -> {
                try {
                    return expression.supply().orElse(null);
                } catch (Exception e) {
                    throw new MutexException("Expression execution failed: " + e.getMessage(), e);
                }
            }, strategy);
        } catch (MutexException e) {
            throw new ExpressionException("synchronized: failed to acquire mutex '" + mutexName + "': " + e.getMessage());
        }
    }

    /**
     * Simplified synchronized execution with default acquire mode and no timeout (wait forever).
     *
     * <p>The expression is passed lazily and executed only after the lock is acquired.</p>
     *
     * <p>Usage in script:</p>
     * <pre>
     * result &lt;- sync("my-mutex", $mutex, myExpression())
     * </pre>
     *
     * @param mutexName the name/identifier for the mutex lock
     * @param mutex the IMutex implementation to use
     * @param expression the expression to execute within the lock (passed lazily as ISupplier)
     * @return the result of the expression execution
     */
    @Expression(name = "sync", description = "Simplified synchronized execution (waits forever for lock)")
    public static Object sync(
            @Nullable String mutexName,
            @Nullable IMutex mutex,
            @Nullable ISupplier<?> expression) {

        log.atDebug().log("sync('{}', mutex, ISupplier)", mutexName);

        if (mutexName == null || mutexName.isBlank()) {
            throw new ExpressionException("sync: mutexName cannot be null or blank");
        }
        if (mutex == null) {
            throw new ExpressionException("sync: mutex cannot be null");
        }
        if (expression == null) {
            return null;
        }

        try {
            return mutex.acquire(() -> {
                try {
                    return expression.supply().orElse(null);
                } catch (Exception e) {
                    throw new MutexException("Expression execution failed: " + e.getMessage(), e);
                }
            });
        } catch (MutexException e) {
            throw new ExpressionException("sync: failed to acquire mutex '" + mutexName + "': " + e.getMessage());
        }
    }

    /**
     * Creates a MutexStrategy based on the acquisition mode and timeout.
     *
     * @param mode "acquire" (with timeout) or "tryAcquire" (immediate)
     * @param timeoutMs timeout in milliseconds
     * @return the MutexStrategy
     */
    private static MutexStrategy createStrategy(String mode, long timeoutMs) {
        int waitTime;
        TimeUnit waitTimeUnit = TimeUnit.MILLISECONDS;

        switch (mode.toLowerCase()) {
            case "acquire":
                // acquire mode: use timeout, or -1 if timeout is 0 (wait forever)
                waitTime = timeoutMs > 0 ? (int) timeoutMs : -1;
                break;
            case "tryacquire":
                // tryAcquire mode: immediate attempt, no waiting
                waitTime = 0;
                break;
            default:
                throw new ExpressionException("synchronized: unknown mode '" + mode + "'. " +
                        "Use 'acquire' or 'tryAcquire'");
        }

        // Create strategy with:
        // - waitTime/waitTimeUnit for timeout behavior
        // - No retries (0)
        // - No lease time enforcement (-1)
        return new MutexStrategy(
                waitTime,
                waitTimeUnit,
                0,                          // retries
                0,                          // retryInterval
                TimeUnit.MILLISECONDS,      // retryIntervalUnit
                -1,                         // leaseTime (-1 = no limit)
                TimeUnit.MILLISECONDS       // leaseTimeUnit
        );
    }

    private static void includeJar(ScriptContext ctx, String path) {
        try {
            File jarFile = new File(path);
            if (!jarFile.exists()) {
                throw new ExpressionException("include: JAR file not found: " + path);
            }

            URL jarUrl = jarFile.toURI().toURL();
            URLClassLoader classLoader = new URLClassLoader(
                    new URL[] { jarUrl },
                    Thread.currentThread().getContextClassLoader());
            Thread.currentThread().setContextClassLoader(classLoader);

            // Read packages from JAR manifest
            List<String> packages = JarManifestReader.getPackages(jarUrl);
            if (packages.isEmpty()) {
                log.atWarn().log("No Garganttua-Packages attribute in JAR manifest: {}", path);
                log.atDebug().log("JAR loaded onto classpath but no packages to scan: {}", path);
                return;
            }

            // Get bootstrap and add packages
            IBoostrap bootstrap = ctx.getBootstrap();
            if (bootstrap == null) {
                log.atWarn().log("No bootstrap configured for ScriptContext, cannot rebuild after JAR include: {}", path);
                log.atDebug().log("JAR loaded onto classpath, packages declared: {} (rebuild skipped)", packages);
                return;
            }

            // Add packages and rebuild
            for (String pkg : packages) {
                bootstrap.withPackage(pkg);
                log.atDebug().log("Added package to bootstrap: {}", pkg);
            }

            try {
                bootstrap.rebuild();
                log.atDebug().log("JAR loaded with {} packages, components rebuilt: {}", packages.size(), path);
            } catch (DslException e) {
                log.atError().log("Failed to rebuild after loading JAR: {}", path, e);
                throw new ExpressionException("include: failed to rebuild after loading JAR: " + path + " - " + e.getMessage());
            }
        } catch (ExpressionException e) {
            throw e;
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

            log.atDebug().log("Script included as '{}' from {}", name, path);
            return name;
        } catch (ScriptException e) {
            throw new ExpressionException("include: failed to load script: " + path + " - " + e.getMessage());
        }
    }

    // ========== Execute Script Functions ==========

    private static int executeScriptImpl(Object name, Object... args) {
        log.atDebug().log("execute_script({}, {} args)", name, args != null ? args.length : 0);
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

    @Expression(name = "execute_script", description = "Executes an included script with no arguments")
    public static int executeScript(@Nullable Object name) {
        return executeScriptImpl(name);
    }

    @Expression(name = "execute_script", description = "Executes an included script with 1 argument")
    public static int executeScript(@Nullable Object name, @Nullable Object arg0) {
        return executeScriptImpl(name, arg0);
    }

    @Expression(name = "execute_script", description = "Executes an included script with 2 arguments")
    public static int executeScript(@Nullable Object name, @Nullable Object arg0, @Nullable Object arg1) {
        return executeScriptImpl(name, arg0, arg1);
    }

    @Expression(name = "execute_script", description = "Executes an included script with 3 arguments")
    public static int executeScript(@Nullable Object name, @Nullable Object arg0, @Nullable Object arg1,
            @Nullable Object arg2) {
        return executeScriptImpl(name, arg0, arg1, arg2);
    }

    @Expression(name = "execute_script", description = "Executes an included script with 4 arguments")
    public static int executeScript(@Nullable Object name, @Nullable Object arg0, @Nullable Object arg1,
            @Nullable Object arg2, @Nullable Object arg3) {
        return executeScriptImpl(name, arg0, arg1, arg2, arg3);
    }

    @Expression(name = "execute_script", description = "Executes an included script with 5 arguments")
    public static int executeScript(@Nullable Object name, @Nullable Object arg0, @Nullable Object arg1,
            @Nullable Object arg2, @Nullable Object arg3, @Nullable Object arg4) {
        return executeScriptImpl(name, arg0, arg1, arg2, arg3, arg4);
    }

    @Expression(name = "execute_script", description = "Executes an included script with 6 arguments")
    public static int executeScript(@Nullable Object name, @Nullable Object arg0, @Nullable Object arg1,
            @Nullable Object arg2, @Nullable Object arg3, @Nullable Object arg4, @Nullable Object arg5) {
        return executeScriptImpl(name, arg0, arg1, arg2, arg3, arg4, arg5);
    }

    @Expression(name = "execute_script", description = "Executes an included script with 7 arguments")
    public static int executeScript(@Nullable Object name, @Nullable Object arg0, @Nullable Object arg1,
            @Nullable Object arg2, @Nullable Object arg3, @Nullable Object arg4, @Nullable Object arg5,
            @Nullable Object arg6) {
        return executeScriptImpl(name, arg0, arg1, arg2, arg3, arg4, arg5, arg6);
    }

    @Expression(name = "execute_script", description = "Executes an included script with 8 arguments")
    public static int executeScript(@Nullable Object name, @Nullable Object arg0, @Nullable Object arg1,
            @Nullable Object arg2, @Nullable Object arg3, @Nullable Object arg4, @Nullable Object arg5,
            @Nullable Object arg6, @Nullable Object arg7) {
        return executeScriptImpl(name, arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7);
    }

    @Expression(name = "execute_script", description = "Executes an included script with 9 arguments")
    public static int executeScript(@Nullable Object name, @Nullable Object arg0, @Nullable Object arg1,
            @Nullable Object arg2, @Nullable Object arg3, @Nullable Object arg4, @Nullable Object arg5,
            @Nullable Object arg6, @Nullable Object arg7, @Nullable Object arg8) {
        return executeScriptImpl(name, arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8);
    }

    @Expression(name = "execute_script", description = "Executes an included script with 10 arguments")
    public static int executeScript(@Nullable Object name, @Nullable Object arg0, @Nullable Object arg1,
            @Nullable Object arg2, @Nullable Object arg3, @Nullable Object arg4, @Nullable Object arg5,
            @Nullable Object arg6, @Nullable Object arg7, @Nullable Object arg8, @Nullable Object arg9) {
        return executeScriptImpl(name, arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9);
    }

    // ========== Script Variable Function ==========

    @Expression(name = "script_variable", description = "Retrieves a variable from an included script after execution")
    public static Object scriptVariable(@Nullable Object scriptName, @Nullable String varName) {
        log.atDebug().log("script_variable({}, {})", scriptName, varName);
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

        return script.getVariable(varName, Object.class).orElse(null);
    }
}

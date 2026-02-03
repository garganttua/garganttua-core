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

    @Expression(name = "print", description = "Prints a single value to standard output")
    public static void print(@Nullable Object value) {
        System.out.println(value == null ? "null" : value.toString());
    }

    @Expression(name = "print", description = "Prints String and int to standard output")
    public static void print(@Nullable String value1, int value2) {
        String s1 = value1 == null ? "null" : value1;
        System.out.println(s1 + value2);
    }

    @Expression(name = "println", description = "Prints a value to standard output with newline")
    public static void println(@Nullable Object value) {
        print(value);
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
        } else if (path.endsWith(".gs")) {
            includeScript(ctx, path);
        } else {
            throw new ExpressionException("include: unsupported file type: " + path
                    + ". Expected .jar or .gs");
        }

        return path;
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
     * <p>If the expression parameter is an ISupplier (from an expression stored via '=' assignment),
     * it will be re-evaluated on each retry attempt. If it's a regular value (already evaluated),
     * it will be returned immediately.</p>
     *
     * <p>Usage examples in script:</p>
     * <pre>
     * // With supplier (re-evaluates on each attempt):
     * expr = riskyOperation()
     * result &lt;- retry(3, seconds(10), $expr)
     *
     * // With direct expression (evaluated once, then returned):
     * result &lt;- retry(3, seconds(10), "already-evaluated")
     * </pre>
     *
     * @param maxAttempts maximum number of attempts (must be >= 1)
     * @param delayMs delay between attempts in milliseconds
     * @param expression the expression/supplier to execute, or an already-evaluated value
     * @return the result of the successful execution
     * @throws ExpressionException if all attempts fail
     */
    @Expression(name = "retry", description = "Retries a supplier expression with delay between attempts")
    public static Object retry(int maxAttempts, long delayMs, @Nullable Object expression) {
        log.atDebug().log("retry({}, {}ms, {})", maxAttempts, delayMs,
                expression == null ? "null" : expression.getClass().getSimpleName());

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

        // If expression is not a supplier, it's already evaluated - return it
        if (!(expression instanceof ISupplier<?> supplier)) {
            log.atDebug().log("retry: expression is not a supplier, returning value directly");
            return expression;
        }

        Throwable lastException = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                log.atTrace().log("retry: attempt {}/{}", attempt, maxAttempts);
                Object result = supplier.supply().orElse(null);
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
     * <p>If the expression parameter is an ISupplier, it will be re-evaluated on each retry.
     * If it's a regular value (already evaluated), it will be returned immediately.</p>
     *
     * @param maxAttempts maximum number of attempts (must be >= 1)
     * @param initialDelayMs initial delay in milliseconds (doubles after each failure)
     * @param maxDelayMs maximum delay cap in milliseconds
     * @param expression the expression/supplier to execute, or an already-evaluated value
     * @return the result of the successful execution
     * @throws ExpressionException if all attempts fail
     */
    @Expression(name = "retryWithBackoff", description = "Retries with exponential backoff (delay doubles after each failure)")
    public static Object retryWithBackoff(int maxAttempts, long initialDelayMs, long maxDelayMs,
            @Nullable Object expression) {
        log.atDebug().log("retryWithBackoff({}, {}ms initial, {}ms max, {})",
                maxAttempts, initialDelayMs, maxDelayMs,
                expression == null ? "null" : expression.getClass().getSimpleName());

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

        // If expression is not a supplier, it's already evaluated - return it
        if (!(expression instanceof ISupplier<?> supplier)) {
            log.atDebug().log("retryWithBackoff: expression is not a supplier, returning value directly");
            return expression;
        }

        Throwable lastException = null;
        long currentDelay = initialDelayMs;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                log.atTrace().log("retryWithBackoff: attempt {}/{}", attempt, maxAttempts);
                Object result = supplier.supply().orElse(null);
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
     * @param expression the expression to execute within the lock
     * @return the result of the expression execution
     * @throws ExpressionException if lock acquisition fails or expression execution fails
     */
    @Expression(name = "synchronized", description = "Executes expression within a mutex lock")
    public static Object synchronizedExec(
            @Nullable String mutexName,
            @Nullable IMutex mutex,
            @Nullable String mode,
            long timeoutMs,
            @Nullable Object expression) {

        log.atDebug().log("synchronized('{}', mutex, '{}', {}ms, expression)",
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
            // If expression is a supplier, wrap it in ThrowingFunction
            if (expression instanceof ISupplier<?> supplier) {
                return mutex.acquire(() -> {
                    try {
                        return supplier.supply().orElse(null);
                    } catch (Exception e) {
                        throw new MutexException("Expression execution failed: " + e.getMessage(), e);
                    }
                }, strategy);
            } else {
                // Already evaluated value - just return it within the lock
                return mutex.acquire(() -> expression, strategy);
            }
        } catch (MutexException e) {
            throw new ExpressionException("synchronized: failed to acquire mutex '" + mutexName + "': " + e.getMessage());
        }
    }

    /**
     * Simplified synchronized execution with default acquire mode and no timeout (wait forever).
     *
     * <p>Usage in script:</p>
     * <pre>
     * result &lt;- sync("my-mutex", $mutex, myExpression())
     * </pre>
     *
     * @param mutexName the name/identifier for the mutex lock
     * @param mutex the IMutex implementation to use
     * @param expression the expression to execute within the lock
     * @return the result of the expression execution
     */
    @Expression(name = "sync", description = "Simplified synchronized execution (waits forever for lock)")
    public static Object sync(
            @Nullable String mutexName,
            @Nullable IMutex mutex,
            @Nullable Object expression) {

        log.atDebug().log("sync('{}', mutex, expression)", mutexName);

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
            if (expression instanceof ISupplier<?> supplier) {
                return mutex.acquire(() -> {
                    try {
                        return supplier.supply().orElse(null);
                    } catch (Exception e) {
                        throw new MutexException("Expression execution failed: " + e.getMessage(), e);
                    }
                });
            } else {
                return mutex.acquire(() -> expression);
            }
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

    private static void includeScript(ScriptContext ctx, String path) {
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
        } catch (ScriptException e) {
            throw new ExpressionException("include: failed to load script: " + path + " - " + e.getMessage());
        }
    }
}

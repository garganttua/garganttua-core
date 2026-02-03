package com.garganttua.core.script.functions;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;

import com.garganttua.core.bootstrap.dsl.IBoostrap;
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

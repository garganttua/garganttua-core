package com.garganttua.core.script;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;

import com.garganttua.core.expression.context.IExpressionContext;
import com.garganttua.core.expression.dsl.ExpressionContextBuilder;
import com.garganttua.core.injection.IInjectionContext;
import com.garganttua.core.injection.context.InjectionContext;
import com.garganttua.core.injection.context.dsl.IInjectionContextBuilder;
import com.garganttua.core.reflection.utils.ObjectReflectionHelper;
import com.garganttua.core.reflections.ReflectionsAnnotationScanner;
import com.garganttua.core.script.context.ScriptContext;

public class Main {

    private static final String VERSION = "2.0.0-ALPHA01";
    private static final String SHEBANG_PREFIX = "#!";

    public static void main(String[] args) {
        if (args.length == 0) {
            printUsage();
            System.exit(1);
        }

        String firstArg = args[0];

        if ("--help".equals(firstArg) || "-h".equals(firstArg)) {
            printUsage();
            System.exit(0);
        }

        if ("--version".equals(firstArg) || "-v".equals(firstArg)) {
            System.out.println("garganttua-script " + VERSION);
            System.exit(0);
        }

        File scriptFile = new File(firstArg);
        if (!scriptFile.exists()) {
            System.err.println("Error: Script file not found: " + firstArg);
            System.exit(1);
        }

        String[] scriptArgs = args.length > 1 ? Arrays.copyOfRange(args, 1, args.length) : new String[0];

        try {
            int exitCode = executeScript(scriptFile, scriptArgs);
            System.exit(exitCode);
        } catch (ScriptException e) {
            System.err.println("Script error: " + e.getMessage());
            if (e.getCause() != null) {
                System.err.println("Caused by: " + e.getCause().getMessage());
            }
            System.exit(1);
        } catch (Exception e) {
            System.err.println("Unexpected error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static int executeScript(File scriptFile, String[] args) throws ScriptException, IOException {
        ObjectReflectionHelper.setAnnotationScanner(new ReflectionsAnnotationScanner());

        // Build injection context
        IInjectionContextBuilder injectionContextBuilder = InjectionContext.builder()
                .autoDetect(true)
                .withPackage("com.garganttua.core.runtime");

        // Build expression context with dependency on injection context
        ExpressionContextBuilder expressionContextBuilder = ExpressionContextBuilder.builder();
        expressionContextBuilder
                .withPackage("com.garganttua")
                .autoDetect(true)
                .provide(injectionContextBuilder);

        // Build injection context first
        IInjectionContext injectionContext = injectionContextBuilder.build();

        // Initialize lifecycle BEFORE building expression context
        injectionContext.onInit().onStart();

        // Now build expression context
        IExpressionContext expressionContext = expressionContextBuilder.build();

        ScriptContext script = new ScriptContext(expressionContext, injectionContext);

        String scriptContent = readScriptFile(scriptFile);
        script.load(scriptContent);
        script.compile();

        return script.execute((Object[]) args);
    }

    private static String readScriptFile(File file) throws IOException {
        String content = Files.readString(file.toPath());
        return stripShebang(content);
    }

    private static String stripShebang(String content) {
        if (content.startsWith(SHEBANG_PREFIX)) {
            int newlineIndex = content.indexOf('\n');
            if (newlineIndex >= 0) {
                return content.substring(newlineIndex + 1);
            }
            return "";
        }
        return content;
    }

    private static void printUsage() {
        System.out.println("Garganttua Script Engine " + VERSION);
        System.out.println();
        System.out.println("Usage: garganttua-script <script.gs> [args...]");
        System.out.println("       garganttua-script --help");
        System.out.println("       garganttua-script --version");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  -h, --help      Show this help message");
        System.out.println("  -v, --version   Show version information");
        System.out.println();
        System.out.println("The script file can start with a shebang line:");
        System.out.println("  #!/usr/bin/env garganttua-script");
        System.out.println();
        System.out.println("Example:");
        System.out.println("  garganttua-script myscript.gs arg1 arg2");
    }
}

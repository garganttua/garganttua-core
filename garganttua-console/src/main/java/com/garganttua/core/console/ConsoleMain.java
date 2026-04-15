package com.garganttua.core.console;

/**
 * Entry point for the Garganttua Script Console fat JAR.
 *
 * <p>Launches the interactive REPL console. Use:
 * {@code java -jar garganttua-console-*-executable.jar}</p>
 */
public class ConsoleMain {

    public static void main(String[] args) {
        boolean useAOT = false;
        for (String arg : args) {
            if ("--aot".equals(arg)) {
                useAOT = true;
            }
        }
        ScriptConsole console = new ScriptConsole(useAOT);
        console.start();
    }
}

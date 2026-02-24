package com.garganttua.core.console;

/**
 * Entry point for the Garganttua Script Console fat JAR.
 *
 * <p>Launches the interactive REPL console. Use:
 * {@code java -jar garganttua-console-*-executable.jar}</p>
 */
public class ConsoleMain {

    public static void main(String[] args) {
        ScriptConsole console = new ScriptConsole();
        console.start();
    }
}

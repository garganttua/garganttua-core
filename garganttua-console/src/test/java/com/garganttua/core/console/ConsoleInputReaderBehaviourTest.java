package com.garganttua.core.console;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.StringReader;

import org.junit.jupiter.api.Test;

/**
 * Behaviour tests for the {@link ConsoleInputReader} multi-line state machine.
 * Drives the fallback (non-JLine) reader path with a {@link StringReader} so the
 * continuation logic (explicit {@code ..} / {@code \\} markers and unclosed
 * bracket detection, string-awareness, escape handling) is exercised deterministically.
 */
class ConsoleInputReaderBehaviourTest {

    private ConsoleInputReader reader(String input) {
        BufferedReader fallback = new BufferedReader(new StringReader(input));
        PrintStream out = new PrintStream(new ByteArrayOutputStream());
        // useJLine=false, lineReader=null -> fallback path; colorsEnabled=false -> plain prompts.
        return new ConsoleInputReader(false, null, fallback, out, false);
    }

    @Test
    void singleCompleteLine_returnedAsIs() throws IOException {
        assertEquals("foo()", reader("foo()\n").readStatement());
    }

    @Test
    void emptyFirstLine_returnsEmptyString() throws IOException {
        // A blank first line short-circuits to "".
        assertEquals("", reader("\nfoo()\n").readStatement());
    }

    @Test
    void whitespaceOnlyFirstLine_returnsEmptyString() throws IOException {
        assertEquals("", reader("   \n").readStatement());
    }

    @Test
    void endOfStreamOnFirstLine_returnsNull() throws IOException {
        assertNull(reader("").readStatement());
    }

    @Test
    void doubleDotContinuation_joinsLinesWithNewline_andStripsMarker() throws IOException {
        // "a .." continues; marker and trailing whitespace stripped, newline inserted before next line.
        String result = reader("a ..\nb\n").readStatement();
        assertEquals("a\nb", result);
    }

    @Test
    void backslashContinuation_joinsLinesWithNewline_andStripsMarker() throws IOException {
        String result = reader("a \\\nb\n").readStatement();
        assertEquals("a\nb", result);
    }

    @Test
    void chainedDoubleDotContinuations_joinAllThreeLines() throws IOException {
        String result = reader("a ..\nb ..\nc\n").readStatement();
        assertEquals("a\nb\nc", result);
    }

    @Test
    void unclosedParen_triggersContinuationUntilClosed() throws IOException {
        // First line has an open paren -> needs continuation; closing paren on next line ends it.
        String result = reader("foo(\n)\n").readStatement();
        assertEquals("foo(\n)", result);
    }

    @Test
    void balancedParensOnOneLine_noContinuation() throws IOException {
        assertEquals("foo(bar)", reader("foo(bar)\nnext\n").readStatement());
    }

    @Test
    void unclosedBracket_triggersContinuation() throws IOException {
        String result = reader("[1,\n2]\n").readStatement();
        assertEquals("[1,\n2]", result);
    }

    @Test
    void unclosedBrace_triggersContinuation() throws IOException {
        String result = reader("{a\n}\n").readStatement();
        assertEquals("{a\n}", result);
    }

    @Test
    void parenInsideStringLiteral_doesNotTriggerContinuation() throws IOException {
        // The '(' is inside a double-quoted string, so brackets are balanced.
        assertEquals("print(\"(\")", reader("print(\"(\")\n").readStatement());
    }

    @Test
    void unterminatedStringLiteral_triggersContinuation() throws IOException {
        // Opening quote never closed on first line -> inString true -> continuation.
        // Closing it on the next line ends the statement.
        String result = reader("\"abc\n\"\n").readStatement();
        assertEquals("\"abc\n\"", result);
    }

    @Test
    void escapedQuoteInsideString_keepsStringOpen() throws IOException {
        // \" does not close the string; the real closing quote does.
        // Line: "a\"b"  -> balanced, no continuation.
        String input = "\"a\\\"b\"\n";
        assertEquals("\"a\\\"b\"", reader(input).readStatement());
    }

    @Test
    void explicitContinuationTakesPrecedence_evenWhenBracketsBalanced() throws IOException {
        // "foo() .." is balanced but the .. forces another line read.
        String result = reader("foo() ..\nbar()\n").readStatement();
        assertEquals("foo()\nbar()", result);
    }

    @Test
    void continuationThenEndOfStream_returnsAccumulatedStatement() throws IOException {
        // After the open paren we expect more input, but the stream ends -> return what we have.
        String result = reader("foo(\n").readStatement();
        assertEquals("foo(", result);
    }

    @Test
    void extraClosingParen_doesNotForceContinuation() throws IOException {
        // parens count goes negative (not > 0), so no continuation is requested.
        assertEquals("foo))", reader("foo))\nnext\n").readStatement());
    }
}

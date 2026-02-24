package com.garganttua.core.console;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jline.reader.Candidate;
import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;

import com.garganttua.core.expression.context.IExpressionContext;

/**
 * JLine completer for the Garganttua Script REPL.
 *
 * <p>Provides tab-completion for:</p>
 * <ul>
 *   <li>Expression function names (with signature description)</li>
 *   <li>Session variables (prefixed with {@code @})</li>
 *   <li>Language keywords ({@code true}, {@code false}, {@code null}, primitive types)</li>
 * </ul>
 *
 * <p>This completer uses volatile references so it can be constructed before
 * the expression context is available, then configured later.</p>
 */
public class ScriptCompleter implements Completer {

    private static final List<String> KEYWORDS = List.of(
            "true", "false", "null",
            "boolean", "int", "long", "float", "double", "short", "byte", "char",
            "Class"
    );

    private volatile IExpressionContext expressionContext;
    private volatile Map<String, Object> sessionVariables;

    public void setExpressionContext(IExpressionContext expressionContext) {
        this.expressionContext = expressionContext;
    }

    public void setSessionVariables(Map<String, Object> sessionVariables) {
        this.sessionVariables = sessionVariables;
    }

    @Override
    public void complete(LineReader reader, ParsedLine line, List<Candidate> candidates) {
        String word = line.word();
        if (word == null) {
            word = "";
        }

        if (word.startsWith("@")) {
            completeVariables(word, candidates);
        } else {
            completeFunctions(word, candidates);
            completeKeywords(word, candidates);
        }
    }

    private void completeVariables(String prefix, List<Candidate> candidates) {
        Map<String, Object> vars = this.sessionVariables;
        if (vars == null) {
            return;
        }

        String namePrefix = prefix.substring(1); // strip leading @
        for (Map.Entry<String, Object> entry : vars.entrySet()) {
            String name = entry.getKey();
            if (name.startsWith(namePrefix)) {
                String typeName = entry.getValue() != null
                        ? entry.getValue().getClass().getSimpleName()
                        : "null";
                candidates.add(new Candidate(
                        "@" + name,     // value
                        "@" + name,     // display
                        "Variables",    // group
                        typeName,       // description
                        null,           // suffix
                        null,           // key
                        true            // complete (add space after)
                ));
            }
        }
    }

    private void completeFunctions(String prefix, List<Candidate> candidates) {
        IExpressionContext ctx = this.expressionContext;
        if (ctx == null) {
            return;
        }

        Set<String> keys = ctx.getFactoryKeys();

        // Deduplicate function names (multiple overloads share the same name)
        Set<String> seen = new LinkedHashSet<>();
        for (String key : keys) {
            int parenIdx = key.indexOf('(');
            if (parenIdx > 0) {
                String name = key.substring(0, parenIdx);
                if (name.startsWith(prefix) && seen.add(name)) {
                    candidates.add(new Candidate(
                            name + "(",     // value — includes opening paren
                            name,           // display
                            "Functions",    // group
                            null,           // description
                            null,           // suffix
                            null,           // key
                            false           // complete = false (no space after paren)
                    ));
                }
            }
        }
    }

    private void completeKeywords(String prefix, List<Candidate> candidates) {
        for (String keyword : KEYWORDS) {
            if (keyword.startsWith(prefix)) {
                candidates.add(new Candidate(
                        keyword,        // value
                        keyword,        // display
                        "Keywords",     // group
                        null,           // description
                        null,           // suffix
                        null,           // key
                        true            // complete
                ));
            }
        }
    }
}

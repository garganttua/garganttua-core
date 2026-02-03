package com.garganttua.core.script.context;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;

import com.garganttua.core.script.ScriptException;

public class ScriptErrorListener extends BaseErrorListener {

    @Override
    public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol,
                            int line, int charPositionInLine, String msg, RecognitionException e) {
        throw new ScriptException("Syntax error at line " + line + ":" + charPositionInLine + " " + msg);
    }
}

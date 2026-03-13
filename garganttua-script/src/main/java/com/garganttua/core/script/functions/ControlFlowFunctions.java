package com.garganttua.core.script.functions;

import com.garganttua.core.expression.annotations.Expression;
import com.garganttua.core.script.nodes.StatementBlock;

import jakarta.annotation.Nullable;

/**
 * Control flow expression functions for the script language.
 *
 * <p>Provides the {@code if(condition, thenBlock [, elseBlock])} function which
 * supports lazy block evaluation. When a {@link StatementBlock} is passed as an
 * argument (via the {@code (...)} syntax), it is only executed if the corresponding
 * branch is taken. Non-block arguments are returned as-is.
 *
 * <p>This function is used by the workflow {@code ScriptGenerator} for conditional
 * script execution, replacing the previous {@code noop() + pipe} pattern.
 */
public class ControlFlowFunctions {

    private ControlFlowFunctions() {
    }

    @Expression(name = "if", description = "Conditional execution: if(condition, thenBlock)")
    public static Object ifExpr(boolean condition, @Nullable Object thenBlock) {
        if (condition) {
            if (thenBlock instanceof StatementBlock block) {
                return block.execute();
            }
            return thenBlock;
        }
        return null;
    }

    @Expression(name = "if", description = "Conditional execution with else: if(condition, thenBlock, elseBlock)")
    public static Object ifExpr(boolean condition, @Nullable Object thenBlock, @Nullable Object elseBlock) {
        if (condition) {
            if (thenBlock instanceof StatementBlock block) {
                return block.execute();
            }
            return thenBlock;
        } else {
            if (elseBlock instanceof StatementBlock block) {
                return block.execute();
            }
            return elseBlock;
        }
    }
}

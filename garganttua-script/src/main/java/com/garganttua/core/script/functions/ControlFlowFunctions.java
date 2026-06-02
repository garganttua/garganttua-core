package com.garganttua.core.script.functions;

import com.garganttua.core.expression.annotations.Expression;
import com.garganttua.core.script.nodes.StatementBlock;

import jakarta.annotation.Nullable;

import com.garganttua.core.reflection.annotations.Reflected;
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
@Reflected(queryAllDeclaredMethods = true)
public class ControlFlowFunctions {

    private ControlFlowFunctions() {
    }

    /**
     * Evaluates {@code thenBlock} only when {@code condition} is true.
     *
     * @param condition the branch condition
     * @param thenBlock the value or {@link StatementBlock} to run when {@code condition} holds
     * @return the block's result (or the value itself) when taken, otherwise {@code null}
     */
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

    /**
     * Evaluates {@code thenBlock} when {@code condition} is true, otherwise {@code elseBlock}.
     * Only the taken branch is executed.
     *
     * @param condition the branch condition
     * @param thenBlock the value or {@link StatementBlock} to run when {@code condition} holds
     * @param elseBlock the value or {@link StatementBlock} to run otherwise
     * @return the result of the taken branch (block result or the value itself)
     */
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

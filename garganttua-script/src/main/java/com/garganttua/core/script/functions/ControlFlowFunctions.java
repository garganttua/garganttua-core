package com.garganttua.core.script.functions;

import com.garganttua.core.expression.annotations.Expression;
import com.garganttua.core.script.nodes.StatementBlock;

import jakarta.annotation.Nullable;

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

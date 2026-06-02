package com.garganttua.core.script.aot;

import com.garganttua.core.aot.commons.IAOTInfrastructureSeed;
import com.garganttua.core.aot.commons.IAOTSeedContext;
import com.garganttua.core.script.nodes.CatchClause;
import com.garganttua.core.script.nodes.CatchClauseHandler;
import com.garganttua.core.script.nodes.FunctionDefNode;
import com.garganttua.core.script.nodes.PipeClause;
import com.garganttua.core.script.nodes.ScriptFunction;
import com.garganttua.core.script.nodes.StatementBlock;
import com.garganttua.core.script.nodes.StatementGroupNode;
import com.garganttua.core.script.nodes.StatementNode;

/**
 * Pre-registers the script module's AST node classes into the AOT registry on
 * cold start. Discovered via
 * {@code META-INF/services/com.garganttua.core.aot.commons.IAOTInfrastructureSeed}.
 *
 * <p>The script/expression engine resolves a node's own runtime type lazily via
 * {@code Class.forName(...)} during evaluation (e.g. a {@link StatementBlock}
 * backing an {@code if(cond, (...))} guard). In a native image (closed world)
 * that throws {@code ClassNotFoundException} unless the class is registered for
 * reflection. Companion to {@code RuntimeInfrastructureSeed}; this covers the
 * {@code com.garganttua.core.script.nodes} surface.</p>
 *
 * @since 2.0.0-ALPHA02
 */
public class ScriptInfrastructureSeed implements IAOTInfrastructureSeed {

    /**
     * Registers each {@code com.garganttua.core.script.nodes} AST class with the
     * AOT registry so they remain reflectively resolvable in a native image.
     *
     * @param ctx the seed context that records classes for reflection
     */
    @Override
    public void seed(IAOTSeedContext ctx) {
        ctx.registerClass(StatementBlock.class);
        ctx.registerClass(StatementGroupNode.class);
        ctx.registerClass(StatementNode.class);
        ctx.registerClass(ScriptFunction.class);
        ctx.registerClass(FunctionDefNode.class);
        ctx.registerClass(PipeClause.class);
        ctx.registerClass(CatchClause.class);
        ctx.registerClass(CatchClauseHandler.class);
    }
}

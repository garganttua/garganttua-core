package com.garganttua.core.script;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.garganttua.core.expression.ExpressionException;
import com.garganttua.core.expression.IExpression;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.script.ScriptException;
import com.garganttua.core.script.nodes.CatchClause;
import com.garganttua.core.script.nodes.FunctionDefNode;
import com.garganttua.core.script.nodes.IScriptNode;
import com.garganttua.core.script.nodes.PipeClause;
import com.garganttua.core.script.nodes.StatementGroupNode;
import com.garganttua.core.script.nodes.StatementNode;
import com.garganttua.core.supply.ISupplier;
import com.garganttua.core.supply.SupplyException;

/**
 * Behaviour tests for the script AST nodes: {@link StatementNode} execution and
 * root-cause unwrapping, defaulting of clause lists, the
 * {@link UnsupportedOperationException} contract on group/function-def nodes, and
 * {@link PipeClause} defaults.
 */
class ScriptAstNodeBehaviourTest {

    /** An expression whose supplier returns a fixed value. */
    private static IExpression<Object, ISupplier<Object>> exprOf(Object value) {
        return new IExpression<>() {
            @Override
            public ISupplier<Object> evaluate() throws ExpressionException {
                return new ISupplier<>() {
                    @Override
                    public Optional<Object> supply() throws SupplyException {
                        return Optional.ofNullable(value);
                    }
                    @Override
                    public Type getSuppliedType() { return Object.class; }
                    @Override
                    public IClass<Object> getSuppliedClass() { return IClass.getClass(Object.class); }
                };
            }
            @Override
            public Type getSuppliedType() { return Object.class; }
            @Override
            public IClass<Object> getSuppliedClass() { return IClass.getClass(Object.class); }
            @Override
            public boolean isContextual() { return false; }
        };
    }

    /** An expression whose supplier throws the given throwable. */
    private static IExpression<Object, ISupplier<Object>> throwingExpr(RuntimeException toThrow) {
        return new IExpression<>() {
            @Override
            public ISupplier<Object> evaluate() throws ExpressionException {
                return new ISupplier<>() {
                    @Override
                    public Optional<Object> supply() throws SupplyException {
                        throw toThrow;
                    }
                    @Override
                    public Type getSuppliedType() { return Object.class; }
                    @Override
                    public IClass<Object> getSuppliedClass() { return IClass.getClass(Object.class); }
                };
            }
            @Override
            public Type getSuppliedType() { return Object.class; }
            @Override
            public IClass<Object> getSuppliedClass() { return IClass.getClass(Object.class); }
            @Override
            public boolean isContextual() { return false; }
        };
    }

    // ---- StatementNode ----

    @Test
    void statementNodeExecuteReturnsSupplierValue() throws Exception {
        StatementNode n = new StatementNode(exprOf("result"), "v", false, 200, null, null, null);
        assertEquals("result", n.execute());
        assertEquals("v", n.variableName());
        assertFalse(n.assignExpression());
        assertEquals(200, n.code());
    }

    @Test
    void statementNodeExecuteReturnsNullForEmptyOptional() throws Exception {
        StatementNode n = new StatementNode(exprOf(null), null, false, null, null, null, null);
        assertNull(n.execute());
    }

    @Test
    void statementNodeNullClauseListsDefaultToEmpty() {
        StatementNode n = new StatementNode(exprOf("x"), null, true, null, null, null, null);
        assertTrue(n.assignExpression());
        assertTrue(n.catchClauses().isEmpty());
        assertTrue(n.downstreamCatchClauses().isEmpty());
        assertTrue(n.pipeClauses().isEmpty());
        assertEquals(0, n.line());
        assertNull(n.sourceText());
    }

    @Test
    void statementNodePreservesClausesAndSourceLocation() {
        CatchClause cc = new CatchClause(List.of("X"), null);
        StatementNode n = new StatementNode(exprOf("x"), null, false, null,
                List.of(cc), List.of(cc), List.of(), 17, "foo()");
        assertEquals(1, n.catchClauses().size());
        assertEquals(1, n.downstreamCatchClauses().size());
        assertEquals(17, n.line());
        assertEquals("foo()", n.sourceText());
    }

    @Test
    void statementNodeWrapsFailureAndUnwrapsRootCause() {
        // supply throws a RuntimeException wrapping an IllegalStateException("root")
        RuntimeException wrapped = new RuntimeException("outer", new IllegalStateException("root"));
        StatementNode n = new StatementNode(throwingExpr(wrapped), null, false, null, null, null, null);
        ScriptException ex = assertThrows(ScriptException.class, n::execute);
        assertEquals("root", ex.getMessage(), "ScriptException should carry the root cause message");
    }

    @Test
    void statementNodeUsesFallbackMessageWhenRootHasNoMessage() {
        StatementNode n = new StatementNode(throwingExpr(new RuntimeException((String) null)),
                null, false, null, null, null, null);
        ScriptException ex = assertThrows(ScriptException.class, n::execute);
        assertEquals("Expression execution failed", ex.getMessage());
    }

    // ---- StatementGroupNode ----

    @Test
    void groupNodeExecuteThrowsUnsupported() {
        StatementGroupNode g = new StatementGroupNode(List.of(), "out", 60, null, null, null);
        assertThrows(UnsupportedOperationException.class, g::execute);
    }

    @Test
    void groupNodeContract() {
        IScriptNode inner = new StatementNode(exprOf("a"), null, false, null, null, null, null);
        StatementGroupNode g = new StatementGroupNode(List.of(inner), "out", 60, null, null, null, 3, "(...)");
        assertEquals("out", g.variableName());
        assertEquals(60, g.code());
        assertFalse(g.assignExpression(), "groups always assign result, not expression");
        assertNull(g.expression());
        assertTrue(g.isGroup());
        assertEquals(1, g.statements().size());
        assertEquals(3, g.line());
        assertEquals("(...)", g.sourceText());
        assertTrue(g.catchClauses().isEmpty());
        assertTrue(g.pipeClauses().isEmpty());
    }

    @Test
    void groupNodeNullStatementsBecomesEmptyList() {
        StatementGroupNode g = new StatementGroupNode(null, null, null, null, null, null);
        assertTrue(g.statements().isEmpty());
    }

    // ---- FunctionDefNode ----

    @Test
    void functionDefNodeExecuteThrowsUnsupported() {
        FunctionDefNode f = new FunctionDefNode("fn", List.of("a", "b"), "block$1", 5, "fn = ...");
        assertThrows(UnsupportedOperationException.class, f::execute);
    }

    @Test
    void functionDefNodeContract() {
        FunctionDefNode f = new FunctionDefNode("myFn", List.of("x", "y"), "block$2", 9, "src");
        assertEquals("myFn", f.variableName());
        assertEquals(List.of("x", "y"), f.parameterNames());
        assertEquals("block$2", f.bodyBlockName());
        assertFalse(f.assignExpression());
        assertNull(f.code());
        assertNull(f.expression());
        assertEquals(9, f.line());
        assertEquals("src", f.sourceText());
        assertTrue(f.catchClauses().isEmpty());
        assertTrue(f.downstreamCatchClauses().isEmpty());
        assertTrue(f.pipeClauses().isEmpty());
    }

    @Test
    void functionDefNodeParameterListIsImmutable() {
        FunctionDefNode f = new FunctionDefNode("fn", List.of("a"), "b", 0, "s");
        assertThrows(UnsupportedOperationException.class, () -> f.parameterNames().add("z"));
    }

    // ---- PipeClause ----

    @Test
    void pipeClauseDefaultBranchHasNullCondition() {
        PipeClause pc = new PipeClause(null, null);
        assertTrue(pc.isDefault());
        assertNull(pc.condition());
        assertNull(pc.code());
    }

    @Test
    void pipeClauseWithConditionIsNotDefault() {
        @SuppressWarnings("unchecked")
        IExpression<Object, ISupplier<Object>> cond = exprOf(Boolean.TRUE);
        PipeClause pc = new PipeClause(cond, null, 418);
        assertFalse(pc.isDefault());
        assertEquals(418, pc.code());
        assertSame(cond, pc.condition());
    }
}

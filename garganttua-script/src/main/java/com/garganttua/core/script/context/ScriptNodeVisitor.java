package com.garganttua.core.script.context;

import java.util.ArrayList;
import java.util.List;

import com.garganttua.core.expression.IExpression;
import com.garganttua.core.expression.context.IExpressionContext;
import com.garganttua.core.script.antlr4.ScriptBaseVisitor;
import com.garganttua.core.script.antlr4.ScriptParser;
import com.garganttua.core.script.nodes.CatchClause;
import com.garganttua.core.script.nodes.IScriptNode;
import com.garganttua.core.script.nodes.PipeClause;
import com.garganttua.core.script.nodes.StatementNode;
import com.garganttua.core.supply.ISupplier;

public class ScriptNodeVisitor extends ScriptBaseVisitor<Object> {

    private final IExpressionContext expressionContext;
    private final List<IScriptNode> statements = new ArrayList<>();

    public ScriptNodeVisitor(IExpressionContext expressionContext) {
        this.expressionContext = expressionContext;
    }

    public List<IScriptNode> getStatements() {
        return this.statements;
    }

    @Override
    public Object visitScript(ScriptParser.ScriptContext ctx) {
        for (ScriptParser.StatementContext stmt : ctx.statement()) {
            visit(stmt);
        }
        return null;
    }

    @Override
    public Object visitResultAssignStatement(ScriptParser.ResultAssignStatementContext ctx) {
        List<CatchClause> catchClauses = buildCatchClauses(ctx.catchClause());
        List<CatchClause> downstreamCatchClauses = buildCatchClauses(ctx.downstreamCatchClause());
        List<PipeClause> pipeClauses = buildPipeClauses(ctx.pipeClause());
        return buildStatement(ctx.IDENTIFIER() != null ? ctx.IDENTIFIER().getText() : null,
                false,
                ctx.expression().getText(),
                ctx.INT_LITERAL() != null ? Integer.parseInt(ctx.INT_LITERAL().getText()) : null,
                catchClauses, downstreamCatchClauses, pipeClauses);
    }

    @Override
    public Object visitExpressionAssignStatement(ScriptParser.ExpressionAssignStatementContext ctx) {
        List<CatchClause> catchClauses = buildCatchClauses(ctx.catchClause());
        List<CatchClause> downstreamCatchClauses = buildCatchClauses(ctx.downstreamCatchClause());
        List<PipeClause> pipeClauses = buildPipeClauses(ctx.pipeClause());
        return buildStatement(ctx.IDENTIFIER() != null ? ctx.IDENTIFIER().getText() : null,
                true,
                ctx.expression().getText(),
                ctx.INT_LITERAL() != null ? Integer.parseInt(ctx.INT_LITERAL().getText()) : null,
                catchClauses, downstreamCatchClauses, pipeClauses);
    }

    private List<CatchClause> buildCatchClauses(List<? extends org.antlr.v4.runtime.ParserRuleContext> clauses) {
        if (clauses == null || clauses.isEmpty()) {
            return List.of();
        }
        List<CatchClause> result = new ArrayList<>();
        for (org.antlr.v4.runtime.ParserRuleContext clause : clauses) {
            ScriptParser.ExceptionListContext exList;
            ScriptParser.CatchHandlerContext handler;
            if (clause instanceof ScriptParser.CatchClauseContext cc) {
                exList = cc.exceptionList();
                handler = cc.catchHandler();
            } else if (clause instanceof ScriptParser.DownstreamCatchClauseContext dc) {
                exList = dc.exceptionList();
                handler = dc.catchHandler();
            } else {
                continue;
            }
            List<String> exceptionTypes = new ArrayList<>();
            if (exList != null) {
                for (ScriptParser.ExceptionTypeContext et : exList.exceptionType()) {
                    exceptionTypes.add(et.getText().replace(".Class", ""));
                }
            }
            IScriptNode handlerNode = buildHandlerNode(handler);
            result.add(new CatchClause(exceptionTypes, handlerNode));
        }
        return result;
    }

    private List<PipeClause> buildPipeClauses(List<ScriptParser.PipeClauseContext> clauses) {
        if (clauses == null || clauses.isEmpty()) {
            return List.of();
        }
        List<PipeClause> result = new ArrayList<>();
        for (ScriptParser.PipeClauseContext clause : clauses) {
            IExpression<?, ? extends ISupplier<?>> condition = null;
            if (clause.expression() != null) {
                condition = this.expressionContext.expression(clause.expression().getText());
            }
            IScriptNode handlerNode = buildPipeHandlerNode(clause.pipeHandler());
            result.add(new PipeClause(condition, handlerNode));
        }
        return result;
    }

    private IScriptNode buildHandlerNode(ScriptParser.CatchHandlerContext handler) {
        if (handler instanceof ScriptParser.ResultAssignHandlerContext ctx) {
            String varName = ctx.IDENTIFIER() != null ? ctx.IDENTIFIER().getText() : null;
            String exprText = ctx.expression().getText();
            Integer code = ctx.INT_LITERAL() != null ? Integer.parseInt(ctx.INT_LITERAL().getText()) : null;
            IExpression<?, ? extends ISupplier<?>> expression = this.expressionContext.expression(exprText);
            return new StatementNode(expression, varName, false, code, List.of(), List.of(), List.of());
        } else if (handler instanceof ScriptParser.ExpressionAssignHandlerContext ctx) {
            String varName = ctx.IDENTIFIER() != null ? ctx.IDENTIFIER().getText() : null;
            String exprText = ctx.expression().getText();
            Integer code = ctx.INT_LITERAL() != null ? Integer.parseInt(ctx.INT_LITERAL().getText()) : null;
            IExpression<?, ? extends ISupplier<?>> expression = this.expressionContext.expression(exprText);
            return new StatementNode(expression, varName, true, code, List.of(), List.of(), List.of());
        }
        throw new IllegalStateException("Unknown handler type: " + handler.getClass());
    }

    private IScriptNode buildPipeHandlerNode(ScriptParser.PipeHandlerContext handler) {
        if (handler instanceof ScriptParser.ResultAssignPipeHandlerContext ctx) {
            String varName = ctx.IDENTIFIER() != null ? ctx.IDENTIFIER().getText() : null;
            String exprText = ctx.expression().getText();
            Integer code = ctx.INT_LITERAL() != null ? Integer.parseInt(ctx.INT_LITERAL().getText()) : null;
            IExpression<?, ? extends ISupplier<?>> expression = this.expressionContext.expression(exprText);
            return new StatementNode(expression, varName, false, code, List.of(), List.of(), List.of());
        } else if (handler instanceof ScriptParser.ExpressionAssignPipeHandlerContext ctx) {
            String varName = ctx.IDENTIFIER() != null ? ctx.IDENTIFIER().getText() : null;
            String exprText = ctx.expression().getText();
            Integer code = ctx.INT_LITERAL() != null ? Integer.parseInt(ctx.INT_LITERAL().getText()) : null;
            IExpression<?, ? extends ISupplier<?>> expression = this.expressionContext.expression(exprText);
            return new StatementNode(expression, varName, true, code, List.of(), List.of(), List.of());
        }
        throw new IllegalStateException("Unknown pipe handler type: " + handler.getClass());
    }

    private Object buildStatement(String variableName, boolean assignExpression, String expressionText, Integer code,
                                  List<CatchClause> catchClauses, List<CatchClause> downstreamCatchClauses, List<PipeClause> pipeClauses) {
        IExpression<?, ? extends ISupplier<?>> expression = this.expressionContext.expression(expressionText);
        this.statements.add(new StatementNode(expression, variableName, assignExpression, code, catchClauses, downstreamCatchClauses, pipeClauses));
        return null;
    }
}

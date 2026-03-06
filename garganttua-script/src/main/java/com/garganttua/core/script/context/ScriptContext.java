package com.garganttua.core.script.context;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;

import com.garganttua.core.bootstrap.dsl.IBoostrap;
import com.garganttua.core.expression.context.IExpressionContext;
import com.garganttua.core.injection.IInjectionContext;
import com.garganttua.core.runtime.IRuntime;
import com.garganttua.core.runtime.IRuntimeResult;
import com.garganttua.core.runtime.IRuntimeStep;
import com.garganttua.core.runtime.Runtime;
import com.garganttua.core.script.IScript;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.script.ScriptException;
import com.garganttua.core.script.antlr4.ScriptLexer;
import com.garganttua.core.script.antlr4.ScriptParser;
import com.garganttua.core.script.nodes.IScriptNode;
import com.garganttua.core.script.nodes.StatementBlock;
import com.garganttua.core.supply.ISupplier;

public class ScriptContext implements IScript {

    private final IExpressionContext expressionContext;
    private final IInjectionContext injectionContext;
    private final IBoostrap bootstrap;
    private volatile String scriptSource;
    private volatile IRuntime<Object[], Object> runtime;
    private volatile Map<String, Object> lastVariables = Map.of();
    private volatile Object lastOutput = null;
    private volatile Throwable lastException = null;
    private volatile boolean aborted = false;
    private final Map<String, Object> initialVariables = Collections.synchronizedMap(new HashMap<>());
    private final Map<String, IScript> includedScripts = new ConcurrentHashMap<>();

    /**
     * Creates a new ScriptContext with expression and injection contexts.
     *
     * @param expressionContext the expression context for evaluating expressions
     * @param injectionContext the injection context for bean resolution
     * @deprecated Use {@link #ScriptContext(IExpressionContext, IInjectionContext, IBoostrap)} instead
     */
    @Deprecated
    public ScriptContext(IExpressionContext expressionContext, IInjectionContext injectionContext) {
        this(expressionContext, injectionContext, null);
    }

    /**
     * Creates a new ScriptContext with expression, injection contexts, and bootstrap.
     *
     * @param expressionContext the expression context for evaluating expressions
     * @param injectionContext the injection context for bean resolution
     * @param bootstrap the bootstrap for rebuilding components after JAR loading (may be null)
     */
    public ScriptContext(IExpressionContext expressionContext, IInjectionContext injectionContext, IBoostrap bootstrap) {
        this.expressionContext = expressionContext;
        this.injectionContext = injectionContext;
        this.bootstrap = bootstrap;
    }

    @Override
    public void load(String script) throws ScriptException {
        if (script == null || script.isBlank()) {
            throw new ScriptException("Script source cannot be null or blank");
        }
        this.scriptSource = script;
        this.runtime = null;
    }

    @Override
    public void load(File file) throws ScriptException {
        if (file == null || !file.exists()) {
            throw new ScriptException("Script file does not exist: " + file);
        }
        try {
            this.load(Files.readString(file.toPath()));
        } catch (IOException e) {
            throw new ScriptException("Failed to read script file: " + file, e);
        }
    }

    @Override
    public void load(InputStream inputStream) throws ScriptException {
        if (inputStream == null) {
            throw new ScriptException("InputStream cannot be null");
        }
        try {
            this.load(new String(inputStream.readAllBytes()));
        } catch (IOException e) {
            throw new ScriptException("Failed to read script from InputStream", e);
        }
    }

    @Override
    public void compile() throws ScriptException {
        if (this.scriptSource == null) {
            throw new ScriptException("No script loaded. Call load() before compile()");
        }

        // Register variable types before parsing so expressions can resolve method calls
        for (Map.Entry<String, Object> entry : this.initialVariables.entrySet()) {
            if (entry.getValue() != null) {
                this.expressionContext.registerVariableType(entry.getKey(), IClass.getClass(entry.getValue().getClass()));
            }
        }

        // Pre-process block expressions before ANTLR4 parsing
        BlockExpressionPreprocessor preprocessor = new BlockExpressionPreprocessor();
        String processedSource = preprocessor.preprocess(this.scriptSource);
        Map<String, String> blockSources = preprocessor.getBlockSources();

        // Compile each block into a StatementBlock
        Map<String, StatementBlock> compiledBlocks = new LinkedHashMap<>();
        for (Map.Entry<String, String> blockEntry : blockSources.entrySet()) {
            List<IScriptNode> blockStatements = parseStatements(blockEntry.getValue());
            compiledBlocks.put(blockEntry.getKey(), new StatementBlock(blockStatements));
        }

        List<IScriptNode> statements = parseStatements(processedSource);
        if (statements.isEmpty()) {
            throw new ScriptException("Failed to compile script: no statements found");
        }

        Map<String, IRuntimeStep<?, Object[], Object>> steps = new LinkedHashMap<>();
        for (int i = 0; i < statements.size(); i++) {
            steps.put("step-" + i, new ScriptRuntimeStep("step-" + i, statements.get(i)));
        }

        // Convert initial variables and compiled blocks to suppliers
        Map<String, ISupplier<?>> variableSuppliers = new HashMap<>();
        for (Map.Entry<String, StatementBlock> blockEntry : compiledBlocks.entrySet()) {
            StatementBlock block = blockEntry.getValue();
            variableSuppliers.put(blockEntry.getKey(), new ISupplier<StatementBlock>() {
                @Override
                public Optional<StatementBlock> supply() {
                    return Optional.of(block);
                }

                @Override
                public java.lang.reflect.Type getSuppliedType() {
                    return StatementBlock.class;
                }

                @Override
                public IClass<StatementBlock> getSuppliedClass() {
                    return IClass.getClass(StatementBlock.class);
                }
            });
        }
        for (Map.Entry<String, Object> entry : this.initialVariables.entrySet()) {
            Object value = entry.getValue();
            variableSuppliers.put(entry.getKey(), new ISupplier<Object>() {
                @Override
                public Optional<Object> supply() {
                    return Optional.ofNullable(value);
                }

                @Override
                public java.lang.reflect.Type getSuppliedType() {
                    return value != null ? value.getClass() : Object.class;
                }

                @Override
                public IClass<Object> getSuppliedClass() {
                    return IClass.getClass(Object.class);
                }
            });
        }

        this.runtime = new Runtime<>(
                "script",
                steps,
                this.injectionContext,
                Object[].class,
                Object.class,
                variableSuppliers
        );
    }

    private List<IScriptNode> parseStatements(String source) {
        ScriptLexer lexer = new ScriptLexer(CharStreams.fromString(source));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        ScriptParser parser = new ScriptParser(tokens);
        parser.removeErrorListeners();
        parser.addErrorListener(new ScriptErrorListener());
        ScriptParser.ScriptContext tree = parser.script();
        ScriptNodeVisitor visitor = new ScriptNodeVisitor(this.expressionContext);
        visitor.visit(tree);
        return visitor.getStatements();
    }

    @Override
    public int execute(Object... args) throws ScriptException {
        if (this.runtime == null) {
            throw new ScriptException("No script compiled. Call compile() before execute()");
        }

        // Reset exception state
        this.lastException = null;
        this.aborted = false;

        ScriptContext previous = ScriptExecutionContext.get();
        ScriptExecutionContext.set(this);
        try {
            Optional<IRuntimeResult<Object[], Object>> result = this.runtime.execute(args);
            if (result.isPresent()) {
                IRuntimeResult<Object[], Object> r = result.get();
                this.lastVariables = r.variables() != null ? r.variables() : Map.of();
                this.lastOutput = r.output();

                // Check if the runtime aborted due to an exception
                if (r.hasAborted()) {
                    this.aborted = true;
                    r.getAbortingException().ifPresent(exRecord -> {
                        this.lastException = exRecord.exception();
                    });
                }

                return r.code() != null ? r.code() : IRuntime.GENERIC_RUNTIME_SUCCESS_CODE;
            }
            this.lastVariables = Map.of();
            this.lastOutput = null;
            return IRuntime.GENERIC_RUNTIME_SUCCESS_CODE;
        } catch (com.garganttua.core.runtime.RuntimeException e) {
            // Capture the exception instead of rethrowing
            this.lastException = e;
            this.aborted = true;
            this.lastVariables = Map.of();
            this.lastOutput = null;
            return IRuntime.GENERIC_RUNTIME_ERROR_CODE;
        } finally {
            if (previous != null) {
                ScriptExecutionContext.set(previous);
            } else {
                ScriptExecutionContext.clear();
            }
        }
    }

    @Override
    public <T> Optional<T> getVariable(String name, IClass<T> type) {
        Object val = this.lastVariables.get(name);
        if (val != null && type.isInstance(val)) {
            return Optional.of(type.cast(val));
        }
        return Optional.empty();
    }

    @Override
    public void setVariable(String name, Object value) {
        this.initialVariables.put(name, value);
    }

    @Override
    public Optional<Object> getOutput() {
        return Optional.ofNullable(this.lastOutput);
    }

    @Override
    public Optional<Throwable> getLastException() {
        return Optional.ofNullable(this.lastException);
    }

    @Override
    public Optional<String> getLastExceptionMessage() {
        return Optional.ofNullable(this.lastException)
                .map(Throwable::getMessage);
    }

    @Override
    public boolean hasAborted() {
        return this.aborted;
    }

    public ScriptContext createChildScript() {
        return new ScriptContext(this.expressionContext, this.injectionContext, this.bootstrap);
    }

    public void registerIncludedScript(String name, IScript script) {
        this.includedScripts.put(name, script);
    }

    public IScript getIncludedScript(String name) {
        return this.includedScripts.get(name);
    }

    /**
     * Returns the expression context used by this script.
     *
     * @return the expression context
     */
    public IExpressionContext getExpressionContext() {
        return this.expressionContext;
    }

    /**
     * Returns the injection context used by this script.
     *
     * @return the injection context
     */
    public IInjectionContext getInjectionContext() {
        return this.injectionContext;
    }

    /**
     * Returns the bootstrap used by this script for rebuilding components.
     *
     * @return the bootstrap, or null if not configured
     */
    public IBoostrap getBootstrap() {
        return this.bootstrap;
    }
}

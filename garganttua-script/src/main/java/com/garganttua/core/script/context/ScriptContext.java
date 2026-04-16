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

import com.garganttua.core.CoreException;
import com.garganttua.core.bootstrap.dsl.IBoostrap;
import com.garganttua.core.expression.context.IExpressionContext;
import com.garganttua.core.runtime.IRuntime;
import com.garganttua.core.runtime.IRuntimeResult;
import com.garganttua.core.runtime.IRuntimeStep;
import com.garganttua.core.runtime.dsl.IRuntimeBuilder;
import com.garganttua.core.runtime.dsl.IRuntimesBuilder;
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
    private final IRuntimesBuilder runtimesBuilder;
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
     * Creates a new ScriptContext with expression context, runtimes builder, and bootstrap.
     *
     * @param expressionContext the expression context for evaluating expressions
     * @param runtimesBuilder the runtimes builder for runtime construction (provided by the caller)
     * @param bootstrap the bootstrap for rebuilding components after JAR loading (may be null)
     */
    public ScriptContext(IExpressionContext expressionContext, IRuntimesBuilder runtimesBuilder, IBoostrap bootstrap) {
        this.expressionContext = expressionContext;
        this.runtimesBuilder = runtimesBuilder;
        this.bootstrap = bootstrap;
        this.expressionContext.enableDynamicFunctions();
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

        ScriptStepFactory stepFactory = new ScriptStepFactory();
        Map<String, IRuntimeStep<?, Object[], Object>> steps = stepFactory.compile(statements);

        // Declare the script runtime — the caller provides the RuntimesBuilder
        @SuppressWarnings("unchecked")
        IClass<Object[]> inputType = (IClass<Object[]>) (IClass<?>) IClass.getClass(Object[].class);
        IClass<Object> outputType = IClass.getClass(Object.class);

        IRuntimeBuilder<Object[], Object> runtimeBuilder = this.runtimesBuilder
                .runtime("script", inputType, outputType);

        // Add pre-compiled steps
        for (Map.Entry<String, IRuntimeStep<?, Object[], Object>> entry : steps.entrySet()) {
            runtimeBuilder.step(entry.getKey(), entry.getValue());
        }

        // Add compiled blocks as variables
        for (Map.Entry<String, StatementBlock> blockEntry : compiledBlocks.entrySet()) {
            StatementBlock block = blockEntry.getValue();
            runtimeBuilder.variable(blockEntry.getKey(), block);
        }

        // Add initial variables
        for (Map.Entry<String, Object> entry : this.initialVariables.entrySet()) {
            runtimeBuilder.variable(entry.getKey(), entry.getValue());
        }

        Map<String, IRuntime<?, ?>> runtimes = this.runtimesBuilder.build();
        @SuppressWarnings("unchecked")
        IRuntime<Object[], Object> scriptRuntime = (IRuntime<Object[], Object>) runtimes.get("script");
        this.runtime = scriptRuntime;
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
        } catch (CoreException e) {
            // Capture RuntimeException and ScriptException (both extend CoreException)
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
        if (this.lastException == null) {
            return Optional.empty();
        }
        // Return root cause message
        Throwable root = this.lastException;
        while (root.getCause() != null) {
            root = root.getCause();
        }
        return Optional.ofNullable(root.getMessage());
    }

    @Override
    public boolean hasAborted() {
        return this.aborted;
    }

    public Map<String, Object> getAllVariables() {
        return Map.copyOf(this.lastVariables);
    }

    public ScriptContext createChildScript() {
        return new ScriptContext(this.expressionContext, this.runtimesBuilder, this.bootstrap);
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
     * Returns the runtimes builder used by this script.
     *
     * @return the runtimes builder
     */
    public IRuntimesBuilder getRuntimesBuilder() {
        return this.runtimesBuilder;
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

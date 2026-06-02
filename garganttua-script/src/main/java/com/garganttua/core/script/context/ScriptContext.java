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
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;

import com.garganttua.core.CoreException;
import com.garganttua.core.classloader.IClassLoaderManager;
import com.garganttua.core.expression.context.IExpressionContext;
import com.garganttua.core.observability.IObservable;
import com.garganttua.core.observability.IObserver;
import com.garganttua.core.observability.ObservabilityEmitter;
import com.garganttua.core.observability.ObservableEvent;
import com.garganttua.core.observability.ObservableRegistry;
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

/**
 * Mutable, single-threaded {@link IScript} implementation: loads {@code .gs}
 * source, compiles it into an immutable {@link IRuntime} via a fresh
 * {@link IRuntimesBuilder} per {@link #compile()}, and executes it while
 * tracking the last run's variables, output, exit code and aborting exception.
 *
 * <p>Observable as {@code scriptcontext:compile} and
 * {@code scriptcontext:execute} events. For concurrent reuse, freeze a compiled
 * instance into a thread-safe handle via {@link #toCompiled()}.
 */
public class ScriptContext implements IScript, IObservable {

    private final IExpressionContext expressionContext;
    private final Supplier<IRuntimesBuilder> runtimesBuilderFactory;
    private final IClassLoaderManager classLoaderManager;
    private volatile String scriptSource;
    private volatile IRuntime<Object[], Object> runtime;
    private volatile Map<String, Object> lastVariables = Map.of();
    private volatile Object lastOutput = null;
    private volatile Throwable lastException = null;
    private volatile boolean aborted = false;
    private final Map<String, Object> initialVariables = Collections.synchronizedMap(new HashMap<>());
    private final Map<String, IScript> includedScripts = new ConcurrentHashMap<>();
    private final ObservableRegistry observers = new ObservableRegistry();

    /**
     * Creates a new ScriptContext with expression context, runtimes builder
     * factory, and a class-loader manager.
     *
     * <p>The factory is called once per {@link #compile()} invocation,
     * producing a fresh {@code IRuntimesBuilder} for each script (including
     * child scripts from {@code include()}). This prevents builder state
     * sharing between independent script compilations.</p>
     *
     * <p>The {@link IClassLoaderManager} handles hot-loading JARs via the
     * {@code include("foo.jar")} script call. Pass {@code null} to disable JAR
     * hot-loading (the {@code include} call then logs a warning and skips).
     * The script layer no longer holds a reference to {@code Bootstrap} —
     * rebuild on JAR load is the manager's responsibility (via its registered
     * rebuild hooks).
     *
     * @param expressionContext      expression context for evaluating expressions
     * @param runtimesBuilderFactory factory creating a fresh IRuntimesBuilder per compilation
     * @param classLoaderManager     manager loading JARs at runtime, or {@code null}
     */
    public ScriptContext(IExpressionContext expressionContext,
                         Supplier<IRuntimesBuilder> runtimesBuilderFactory,
                         IClassLoaderManager classLoaderManager) {
        this.expressionContext = expressionContext;
        this.runtimesBuilderFactory = runtimesBuilderFactory;
        this.classLoaderManager = classLoaderManager;
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
    public void addObserver(IObserver<ObservableEvent> observer) {
        this.observers.addObserver(observer);
    }

    @Override
    public void removeObserver(IObserver<ObservableEvent> observer) {
        this.observers.removeObserver(observer);
    }

    @Override
    public void compile() throws ScriptException {
        if (this.scriptSource == null) {
            throw new ScriptException("No script loaded. Call load() before compile()");
        }

        try (ObservabilityEmitter.Scope scope = ObservabilityEmitter.open(this.observers, UUID.randomUUID())) {
            scope.fireStart("scriptcontext:compile");
            try {
                doCompile();
                scope.fireEnd("scriptcontext:compile");
            } catch (ScriptException e) {
                scope.fireError("scriptcontext:compile", e);
                throw e;
            } catch (RuntimeException e) {
                scope.fireError("scriptcontext:compile", e);
                throw e;
            }
        }
    }

    private void doCompile() throws ScriptException {
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

        // Create a fresh RuntimesBuilder for this compilation via the factory
        IRuntimesBuilder runtimesBuilder = this.runtimesBuilderFactory.get();

        @SuppressWarnings("unchecked")
        IClass<Object[]> inputType = (IClass<Object[]>) (IClass<?>) IClass.getClass(Object[].class);
        IClass<Object> outputType = IClass.getClass(Object.class);

        IRuntimeBuilder<Object[], Object> runtimeBuilder = runtimesBuilder
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

        Map<String, IRuntime<?, ?>> runtimes = runtimesBuilder.build();
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

        try (ObservabilityEmitter.Scope scope = ObservabilityEmitter.open(this.observers, UUID.randomUUID())) {
            scope.fireStart("scriptcontext:execute");
            int code;
            try {
                code = doExecute(args);
            } catch (ScriptException e) {
                scope.fireError("scriptcontext:execute", e);
                throw e;
            } catch (RuntimeException e) {
                scope.fireError("scriptcontext:execute", e);
                throw e;
            }
            scope.fireEnd("scriptcontext:execute", code);
            return code;
        }
    }

    private int doExecute(Object... args) throws ScriptException {
        return ScriptExecutionContext.callIn(this, () -> {
            try {
                Optional<IRuntimeResult<Object[], Object>> result = this.runtime.execute(args);
                if (result.isPresent()) {
                    IRuntimeResult<Object[], Object> r = result.get();
                    this.lastVariables = r.variables() != null ? r.variables() : Map.of();
                    this.lastOutput = r.output();

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
                this.lastException = e;
                this.aborted = true;
                this.lastVariables = Map.of();
                this.lastOutput = null;
                return IRuntime.GENERIC_RUNTIME_ERROR_CODE;
            }
        });
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

    /**
     * @return an immutable snapshot of all variables from the last execution
     */
    public Map<String, Object> getAllVariables() {
        return Map.copyOf(this.lastVariables);
    }

    /**
     * Creates an independent child script sharing this context's expression
     * context, runtimes-builder factory and class-loader manager.
     *
     * @return a fresh {@link ScriptContext}
     */
    public ScriptContext createChildScript() {
        return new ScriptContext(this.expressionContext, this.runtimesBuilderFactory, this.classLoaderManager);
    }

    /**
     * Registers a script under {@code name} so {@code include()} / {@code call()}
     * can later resolve it via {@link #getIncludedScript(String)}.
     *
     * @param name   logical name of the included script
     * @param script the compiled script to associate
     */
    public void registerIncludedScript(String name, IScript script) {
        this.includedScripts.put(name, script);
    }

    /**
     * @param name logical name of a previously included script
     * @return the registered script, or {@code null} if none is registered under {@code name}
     */
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
     * Returns the runtimes builder factory used by this script.
     *
     * @return the runtimes builder factory
     */
    public Supplier<IRuntimesBuilder> getRuntimesBuilderFactory() {
        return this.runtimesBuilderFactory;
    }

    /**
     * Returns the class-loader manager used by this script to hot-load JARs
     * (via {@code include("foo.jar")}).
     *
     * @return the manager, or {@code null} if JAR hot-loading is disabled
     */
    public IClassLoaderManager getClassLoaderManager() {
        return this.classLoaderManager;
    }

    /**
     * Freeze this {@link ScriptContext} into a thread-safe immutable handle
     * that can be {@code execute()}-d concurrently from multiple threads. The
     * returned {@link com.garganttua.core.script.ICompiledScript} wraps the
     * already-built {@link IRuntime} and never touches this context's mutable
     * last-* fields — every call produces its own
     * {@link com.garganttua.core.script.IScriptExecutionResult}.
     *
     * <p>Must be called AFTER {@link #compile()} (or its convenience wrappers
     * via {@code load() + compile()}).
     *
     * @throws com.garganttua.core.script.ScriptException if the script wasn't compiled yet
     */
    public com.garganttua.core.script.ICompiledScript toCompiled()
            throws com.garganttua.core.script.ScriptException {
        if (this.runtime == null) {
            throw new com.garganttua.core.script.ScriptException(
                    "Cannot freeze: script not compiled. Call compile() before toCompiled()");
        }
        if (this.scriptSource == null) {
            throw new com.garganttua.core.script.ScriptException(
                    "Cannot freeze: no source loaded");
        }
        return new CompiledScript(this.runtime, this.scriptSource, this);
    }
}

package com.garganttua.core.script;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.garganttua.core.expression.context.IExpressionContext;
import com.garganttua.core.expression.dsl.ExpressionContextBuilder;
import com.garganttua.core.injection.IInjectionContext;
import com.garganttua.core.injection.context.InjectionContext;
import com.garganttua.core.injection.context.dsl.IInjectionContextBuilder;
import com.garganttua.core.reflection.IReflectionProvider;
import com.garganttua.core.reflection.dsl.IReflectionBuilder;
import com.garganttua.core.reflection.dsl.ReflectionBuilder;
import com.garganttua.core.reflections.ReflectionsAnnotationScanner;
import com.garganttua.core.runtime.dsl.RuntimesBuilder;
import com.garganttua.core.script.context.ScriptContext;

/**
 * Acceptance criterion #1 of the "reentrant precompiled-script execution" bug
 * fiche (2026-06-09, garganttua-api security team):
 *
 * <p>An {@link ICompiledScript#execute(Object...)} call invoked from <em>inside</em>
 * another {@code execute()} of the <em>same</em> {@code ICompiledScript}, on the
 * same thread, must produce a correct result and must not corrupt the runtime
 * state (output / named variables) of the enclosing call.
 *
 * <p>This mirrors the api scenario where an {@code IContextualSupplier} resolved
 * mid-script triggers a nested {@code invoke()} on the same domain, hence the
 * same pre-compiled {@code IRuntime}.
 */
@DisplayName("ICompiledScript — reentrant execute() on the same compiled instance")
class CompiledScriptReentrancyTest {

    private static IReflectionBuilder reflectionBuilder;

    @TempDir
    Path tempDir;

    @SuppressWarnings("unchecked")
    @BeforeAll
    static void setup() throws Exception {
        Class<? extends IReflectionProvider> providerClass =
                (Class<? extends IReflectionProvider>) Class.forName(
                        "com.garganttua.core.reflection.runtime.RuntimeReflectionProvider");
        reflectionBuilder = ReflectionBuilder.builder()
                .withProvider(providerClass.getDeclaredConstructor().newInstance())
                .withScanner(new ReflectionsAnnotationScanner());
        reflectionBuilder.build();
    }

    /**
     * Seeded into the script as a variable. Its {@link #reenter()} method, when
     * first hit (depth 0), re-executes the captured compiled script with a
     * different input — exactly the way the api's PrincipalSupplier re-enters the
     * domain workflow. The depth guard stops infinite recursion.
     */
    public static final class Reenterer {
        final AtomicReference<ICompiledScript> holder = new AtomicReference<>();
        final AtomicInteger depth = new AtomicInteger(0);
        volatile IScriptExecutionResult innerResult;

        public String reenter() {
            if (depth.getAndIncrement() == 0) {
                try {
                    this.innerResult = holder.get().execute("INNER");
                } catch (ScriptException e) {
                    throw new RuntimeException(e);
                } finally {
                    depth.decrementAndGet();
                }
                return "reentered";
            }
            depth.decrementAndGet();
            return "inner-noop";
        }
    }

    private ICompiledScript compile(String source, Reenterer reenterer) throws Exception {
        IInjectionContextBuilder injectionContextBuilder = InjectionContext.builder()
                .provide(reflectionBuilder)
                .autoDetect(true)
                .withPackage("com.garganttua.core.runtime");

        ExpressionContextBuilder expressionContextBuilder = ExpressionContextBuilder.builder();
        expressionContextBuilder.withPackage("com.garganttua").autoDetect(true)
                .provide(injectionContextBuilder);

        IInjectionContext injectionContext = injectionContextBuilder.build();
        injectionContext.onInit().onStart();

        IExpressionContext expressionContext = expressionContextBuilder.build();

        ScriptContext ctx = new ScriptContext(expressionContext,
                () -> RuntimesBuilder.builder().provide(injectionContextBuilder), null);
        ctx.setVariable("reenterer", reenterer);
        ctx.load(source);
        ctx.compile();
        return ctx.toCompiled();
    }

    @Test
    @DisplayName("nested execute() does not clobber the enclosing call's output / variables")
    void reentrantExecute_doesNotCorruptEnclosingCall() throws Exception {
        // 1) set output + a named variable from the input
        // 2) trigger a nested execute() of THIS same compiled script (input "INNER")
        // 3) read the variable back — it must still hold the enclosing value
        String source =
                "output <- @0\n"
              + "marker <- @0\n"
              + "z <- :reenter(@reenterer)\n"
              + "after <- @marker\n";

        Reenterer reenterer = new Reenterer();
        ICompiledScript compiled = compile(source, reenterer);
        reenterer.holder.set(compiled);

        IScriptExecutionResult outer = compiled.execute("OUTER");

        assertFalse(outer.hasAborted(),
                "outer execution aborted: " + outer.exception().orElse(null));

        // The nested call really happened and saw the INNER input.
        assertEquals("INNER", reenterer.innerResult.output().orElse(null),
                "the nested execute() should have produced its own INNER output");

        // The enclosing call's output must be intact (not the nested INNER value).
        assertEquals("OUTER", outer.output().orElse(null),
                "enclosing output corrupted by reentrant execute()");

        // The enclosing call's named variables must be intact too.
        assertEquals("OUTER", outer.variables().get("marker"),
                "enclosing variable 'marker' corrupted by reentrant execute()");
        assertEquals("OUTER", outer.variables().get("after"),
                "variable read AFTER the nested call sees corrupted state");
    }

    /**
     * The api hits this through {@code include() + execute_script() + script_variable()}:
     * the workflow's {@code ScriptGenerator} renders every file-backed stage as an
     * included sub-script, runs it, then reads its outputs by name. The sub-scripts are
     * registered in the current {@link ScriptContext}'s {@code includedScripts} map.
     *
     * <p>Before the fix, all executions of one {@code ICompiledScript} shared a single
     * {@code captured} context: a nested {@code execute()} re-ran {@code include()},
     * <em>replacing</em> the enclosing call's sub-script instance in that shared map, so
     * the enclosing {@code script_variable()} read the nested run's value. This mirrors
     * the api authenticate→PrincipalSupplier→nested readAll path exactly.
     *
     * <p>The fix gives each {@code execute()} a fresh frame (its own {@code includedScripts}),
     * so the enclosing call keeps reading its own sub-script. Without the fix this asserts
     * {@code INNER} and fails.
     */
    @Test
    @DisplayName("nested execute() does not replace the enclosing call's included sub-scripts")
    void reentrantInclude_doesNotClobberEnclosingSubScript() throws Exception {
        File sub = tempDir.resolve("sub.gs").toFile();
        Files.writeString(sub.toPath(), "result <- @0\n");
        String subPath = sub.getAbsolutePath().replace("\\", "\\\\");

        // include the sub, run it with the enclosing input, re-enter (which re-includes &
        // re-runs the sub with INNER), then read the sub's 'result' back: it must still be
        // the enclosing call's value, not the nested call's.
        String source =
                "subName <- include(\"" + subPath + "\")\n"
              + "xcode <- execute_script(@subName, @0)\n"
              + "z <- :reenter(@reenterer)\n"
              + "recovered <- script_variable(@subName, \"result\")\n"
              + "output <- @recovered\n";

        Reenterer reenterer = new Reenterer();
        ICompiledScript compiled = compile(source, reenterer);
        reenterer.holder.set(compiled);

        IScriptExecutionResult outer = compiled.execute("OUTER");

        assertFalse(outer.hasAborted(),
                "outer execution aborted: " + outer.exception().orElse(null));

        // The nested call really happened and read its own INNER sub-script result.
        assertEquals("INNER", reenterer.innerResult.output().orElse(null),
                "the nested execute() should have recovered its own INNER sub-script value");

        // The enclosing call's sub-script result must NOT have been clobbered by the
        // nested call's re-include().
        assertEquals("OUTER", outer.output().orElse(null),
                "enclosing call read the nested run's sub-script value — included sub-script was clobbered");
        assertEquals("OUTER", outer.variables().get("recovered"),
                "script_variable() on the enclosing call returned the nested run's value");
    }
}

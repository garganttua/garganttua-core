package com.garganttua.core.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.garganttua.core.condition.ICondition;
import com.garganttua.core.execution.ExecutorException;
import com.garganttua.core.execution.IExecutor;
import com.garganttua.core.execution.IExecutorChain;
import com.garganttua.core.execution.IFallBackExecutor;
import com.garganttua.core.expression.IExpression;
import com.garganttua.core.injection.context.InjectionContext;
import com.garganttua.core.injection.context.dsl.IInjectionContextBuilder;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.dsl.IReflectionBuilder;
import com.garganttua.core.reflection.dsl.ReflectionBuilder;
import com.garganttua.core.reflection.runtime.RuntimeReflectionProvider;
import com.garganttua.core.reflections.ReflectionsAnnotationScanner;
import com.garganttua.core.supply.FixedSupplier;
import com.garganttua.core.supply.ISupplier;

/**
 * Behaviour tests for {@link RuntimeStepMethodBinder#execute(IRuntimeContext, IExecutorChain)}:
 * condition gating (skip vs run), success-code assignment, variable storage,
 * and pipe post-processing (matching/non-matching pipes, code/variable side-effects).
 */
class RuntimeStepMethodBinderExecuteBehaviourTest {

    private static IReflectionBuilder reflectionBuilder;

    @BeforeAll
    static void setup() throws Exception {
        reflectionBuilder = ReflectionBuilder.builder()
                .withProvider(new RuntimeReflectionProvider())
                .withScanner(new ReflectionsAnnotationScanner());
        reflectionBuilder.build();
    }

    @AfterAll
    static void tearDown() {
        IClass.setReflection(null);
    }

    private RuntimeContext<String, String> startedContext() {
        IInjectionContextBuilder builder = InjectionContext.builder()
                .provide(reflectionBuilder)
                .autoDetect(true)
                .withPackage("com.garganttua.core.runtime");
        RuntimeContext<String, String> ctx = new RuntimeContext<>(builder.build(), "in",
                String.class, Map.of(), UUID.randomUUID());
        ctx.onInit().onStart();
        return ctx;
    }

    /** Records whether the next executor in the chain was invoked. */
    private static final class RecordingChain implements IExecutorChain<IRuntimeContext<String, String>> {
        final AtomicInteger executeCount = new AtomicInteger();

        @Override
        public void execute(IRuntimeContext<String, String> request) {
            executeCount.incrementAndGet();
        }

        @Override
        public void executeFallBack(IRuntimeContext<String, String> request) {
        }

        @Override
        public void addExecutor(IExecutor<IRuntimeContext<String, String>> executor) {
        }

        @Override
        public void addExecutor(IExecutor<IRuntimeContext<String, String>> executor,
                IFallBackExecutor<IRuntimeContext<String, String>> fallBackExecutor) {
        }
    }

    /** Expression supplying a fixed String. */
    private static IExpression<String, ISupplier<String>> value(String v) {
        return new IExpression<>() {
            @Override
            public ISupplier<String> evaluate() {
                return new FixedSupplier<>(v, IClass.getClass(String.class));
            }

            @Override
            public IClass<String> getSuppliedClass() {
                return IClass.getClass(String.class);
            }

            @Override
            public Type getSuppliedType() {
                return String.class;
            }

            @Override
            public boolean isContextual() {
                return false;
            }
        };
    }

    @SuppressWarnings("unchecked")
    private static IExpression<Boolean, ISupplier<Boolean>> boolValue(boolean b) {
        return new IExpression<>() {
            @Override
            public ISupplier<Boolean> evaluate() {
                return new FixedSupplier<>(b, IClass.getClass(Boolean.class));
            }

            @Override
            public IClass<Boolean> getSuppliedClass() {
                return IClass.getClass(Boolean.class);
            }

            @Override
            public Type getSuppliedType() {
                return Boolean.class;
            }

            @Override
            public boolean isContextual() {
                return false;
            }
        };
    }

    /** Condition resolving to a fixed boolean. */
    private static ICondition condition(boolean b) {
        return () -> new FixedSupplier<>(b, IClass.getClass(Boolean.class));
    }

    private RuntimeStepMethodBinder<String, String, String> binder(
            IExpression<String, ISupplier<String>> expr, Optional<String> variable, int code,
            Optional<ICondition> condition, List<IRuntimeStepPipe> pipes) {
        return new RuntimeStepMethodBinder<>("rt", "step", expr, variable, false, code,
                Set.of(), pipes, condition, false, false, "ref");
    }

    // -------------------------------------------------------------------------
    // Condition gating
    // -------------------------------------------------------------------------

    @Test
    void execute_conditionFalse_skipsStepButAdvancesChain_andDoesNotStoreVariable() throws ExecutorException {
        RuntimeContext<String, String> ctx = startedContext();
        RecordingChain chain = new RecordingChain();
        var b = binder(value("computed"), Optional.of("v"), 0, Optional.of(condition(false)), List.of());

        b.execute(ctx, chain);

        assertEquals(1, chain.executeCount.get(), "skipped step still advances the chain");
        assertTrue(ctx.getVariable("v", IClass.getClass(String.class)).isEmpty(),
                "variable must not be written when condition is unmet");
    }

    @Test
    void execute_conditionTrue_runsStep_storesVariable_andAdvancesChain() throws ExecutorException {
        RuntimeContext<String, String> ctx = startedContext();
        RecordingChain chain = new RecordingChain();
        var b = binder(value("computed"), Optional.of("v"), 0, Optional.of(condition(true)), List.of());

        b.execute(ctx, chain);

        assertEquals(1, chain.executeCount.get());
        assertEquals(Optional.of("computed"), ctx.getVariable("v", IClass.getClass(String.class)));
    }

    @Test
    void execute_noCondition_runsByDefault() throws ExecutorException {
        RuntimeContext<String, String> ctx = startedContext();
        RecordingChain chain = new RecordingChain();
        var b = binder(value("hi"), Optional.of("out"), 0, Optional.empty(), List.of());

        b.execute(ctx, chain);

        assertEquals(Optional.of("hi"), ctx.getVariable("out", IClass.getClass(String.class)));
    }

    // -------------------------------------------------------------------------
    // Success code
    // -------------------------------------------------------------------------

    @Test
    void execute_nonZeroSuccessCode_setsCodeOnContext() throws ExecutorException {
        RuntimeContext<String, String> ctx = startedContext();
        var b = binder(value("x"), Optional.empty(), 207, Optional.empty(), List.of());

        b.execute(ctx, new RecordingChain());

        assertEquals(Optional.of(207), ctx.getCode());
    }

    @Test
    void execute_zeroSuccessCode_leavesDefaultCodeUnchanged() throws ExecutorException {
        RuntimeContext<String, String> ctx = startedContext();
        Optional<Integer> before = ctx.getCode();
        var b = binder(value("x"), Optional.empty(), 0, Optional.empty(), List.of());

        b.execute(ctx, new RecordingChain());

        assertEquals(before, ctx.getCode(), "code 0 must not overwrite the existing code");
    }

    // -------------------------------------------------------------------------
    // Pipes
    // -------------------------------------------------------------------------

    @Test
    void execute_matchingPipe_replacesResultAndSetsCodeAndVariable() throws ExecutorException {
        RuntimeContext<String, String> ctx = startedContext();
        IRuntimeStepPipe pipe = new RuntimeStepPipe(
                Optional.of(boolValue(true)), value("piped"), Optional.of(302), Optional.of("pv"));
        var b = binder(value("original"), Optional.of("stepVar"), 0, Optional.empty(), List.of(pipe));

        b.execute(ctx, new RecordingChain());

        // The piped value replaces the original, so the step variable receives "piped".
        assertEquals(Optional.of("piped"), ctx.getVariable("stepVar", IClass.getClass(String.class)));
        assertEquals(Optional.of("piped"), ctx.getVariable("pv", IClass.getClass(String.class)));
        assertEquals(Optional.of(302), ctx.getCode());
    }

    @Test
    void execute_nonMatchingPipe_keepsOriginalResult() throws ExecutorException {
        RuntimeContext<String, String> ctx = startedContext();
        IRuntimeStepPipe pipe = new RuntimeStepPipe(
                Optional.of(boolValue(false)), value("piped"), Optional.of(302), Optional.of("pv"));
        var b = binder(value("original"), Optional.of("stepVar"), 0, Optional.empty(), List.of(pipe));

        b.execute(ctx, new RecordingChain());

        assertEquals(Optional.of("original"), ctx.getVariable("stepVar", IClass.getClass(String.class)));
        assertTrue(ctx.getVariable("pv", IClass.getClass(String.class)).isEmpty(),
                "non-matching pipe must not set its variable");
    }

    @Test
    void execute_defaultPipe_withNoCondition_alwaysMatches() throws ExecutorException {
        RuntimeContext<String, String> ctx = startedContext();
        IRuntimeStepPipe pipe = new RuntimeStepPipe(
                Optional.empty(), value("always"), Optional.empty(), Optional.of("pv"));
        var b = binder(value("original"), Optional.empty(), 0, Optional.empty(), List.of(pipe));

        b.execute(ctx, new RecordingChain());

        assertEquals(Optional.of("always"), ctx.getVariable("pv", IClass.getClass(String.class)));
    }

    @Test
    void execute_firstMatchingPipeWins() throws ExecutorException {
        RuntimeContext<String, String> ctx = startedContext();
        IRuntimeStepPipe first = new RuntimeStepPipe(
                Optional.of(boolValue(true)), value("first"), Optional.empty(), Optional.of("p1"));
        IRuntimeStepPipe second = new RuntimeStepPipe(
                Optional.of(boolValue(true)), value("second"), Optional.empty(), Optional.of("p2"));
        var b = binder(value("orig"), Optional.empty(), 0, Optional.empty(), List.of(first, second));

        b.execute(ctx, new RecordingChain());

        assertEquals(Optional.of("first"), ctx.getVariable("p1", IClass.getClass(String.class)));
        assertTrue(ctx.getVariable("p2", IClass.getClass(String.class)).isEmpty(),
                "only the first matching pipe runs");
    }

    // -------------------------------------------------------------------------
    // Accessors sanity (output flag false here)
    // -------------------------------------------------------------------------

    @Test
    void binder_isNotOutput_whenConstructedWithFalse() {
        var b = binder(value("x"), Optional.empty(), 0, Optional.empty(), List.of());
        assertFalse(b.isOutput());
    }
}

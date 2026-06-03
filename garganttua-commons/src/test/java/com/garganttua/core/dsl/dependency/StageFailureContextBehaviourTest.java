package com.garganttua.core.dsl.dependency;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.garganttua.core.dsl.IObservableBuilder;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.JdkClass;

class StageFailureContextBehaviourTest {

    interface ITestBuilder extends IObservableBuilder<ITestBuilder, Object> {
    }

    static class Consumer {
    }

    @SuppressWarnings("unchecked")
    private static IClass<? extends IObservableBuilder<?, ?>> dep() {
        return (IClass<? extends IObservableBuilder<?, ?>>) (IClass<?>) JdkClass.of(ITestBuilder.class);
    }

    private static IClass<?> consumer() {
        return JdkClass.of(Consumer.class);
    }

    // --- validation ---

    @Test
    void rejectsNullMandatoryComponents() {
        assertThrows(NullPointerException.class,
                () -> new StageFailureContext(consumer(), null, DependencyStage.BUILD,
                        DependencyKind.BUILT, "not provided", null));
        assertThrows(NullPointerException.class,
                () -> new StageFailureContext(consumer(), dep(), null,
                        DependencyKind.BUILT, "not provided", null));
        assertThrows(NullPointerException.class,
                () -> new StageFailureContext(consumer(), dep(), DependencyStage.BUILD,
                        null, "not provided", null));
        assertThrows(NullPointerException.class,
                () -> new StageFailureContext(consumer(), dep(), DependencyStage.BUILD,
                        DependencyKind.BUILT, null, null));
    }

    @Test
    void allowsNullConsumerAndCause() {
        StageFailureContext ctx = new StageFailureContext(null, dep(), DependencyStage.BUILD,
                DependencyKind.BUILT, "not provided", null);
        assertTrue(ctx.causeOpt().isEmpty());
    }

    // --- describe() ---

    @Test
    void describeIncludesAllPresentFields() {
        StageFailureContext ctx = new StageFailureContext(consumer(), dep(),
                DependencyStage.AUTO_DETECT, DependencyKind.BUILDER, "not provided", null);
        String s = ctx.describe();
        assertTrue(s.contains("stage=AUTO_DETECT"), s);
        assertTrue(s.contains("kind=BUILDER"), s);
        assertTrue(s.contains("dep=ITestBuilder"), s);
        assertTrue(s.contains("consumer=Consumer"), s);
        assertTrue(s.contains("reason=not provided"), s);
        assertTrue(s.startsWith("[") && s.endsWith("]"), s);
        assertFalse(s.contains("cause="), s);
    }

    @Test
    void describeOmitsConsumerWhenNull() {
        StageFailureContext ctx = new StageFailureContext(null, dep(),
                DependencyStage.BUILD, DependencyKind.BUILT, "missing", null);
        String s = ctx.describe();
        assertFalse(s.contains("consumer="), s);
    }

    @Test
    void describeIncludesCauseWhenPresent() {
        Throwable cause = new IllegalStateException("boom");
        StageFailureContext ctx = new StageFailureContext(consumer(), dep(),
                DependencyStage.BUILD, DependencyKind.BUILT, "hook threw", cause);
        String s = ctx.describe();
        assertTrue(s.contains("cause=IllegalStateException: boom"), s);
    }

    // --- causeOpt ---

    @Test
    void causeOptWrapsCause() {
        Throwable cause = new RuntimeException("x");
        StageFailureContext ctx = new StageFailureContext(consumer(), dep(),
                DependencyStage.BUILD, DependencyKind.BUILT, "hook threw", cause);
        assertTrue(ctx.causeOpt().isPresent());
        assertSame(cause, ctx.causeOpt().get());
    }

    // --- record accessors ---

    @Test
    void recordAccessorsExposeComponents() {
        StageFailureContext ctx = new StageFailureContext(consumer(), dep(),
                DependencyStage.BUILD, DependencyKind.BUILT, "reason", null);
        assertEquals(DependencyStage.BUILD, ctx.stage());
        assertEquals(DependencyKind.BUILT, ctx.kind());
        assertEquals("reason", ctx.reason());
        assertEquals("Consumer", ctx.consumerClass().getSimpleName());
        assertEquals("ITestBuilder", ctx.depClass().getSimpleName());
    }
}

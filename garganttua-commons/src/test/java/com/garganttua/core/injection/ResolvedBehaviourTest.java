package com.garganttua.core.injection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.JdkClass;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

class ResolvedBehaviourTest {

    private static Resolved resolved(boolean nullable) {
        IClass<?> type = JdkClass.of(String.class);
        return new Resolved(true, type, null, nullable);
    }

    private static Resolved notResolved(boolean nullable) {
        IClass<?> type = JdkClass.of(String.class);
        return new Resolved(false, type, null, nullable);
    }

    // --- ifResolved ---

    @Test
    void ifResolvedRunsActionWhenResolved() {
        AtomicReference<Boolean> captured = new AtomicReference<>();
        resolved(true).ifResolved((supplier, nullable) -> captured.set(nullable));
        assertEquals(Boolean.TRUE, captured.get());
    }

    @Test
    void ifResolvedSkipsActionWhenNotResolved() {
        AtomicBoolean ran = new AtomicBoolean(false);
        notResolved(false).ifResolved((supplier, nullable) -> ran.set(true));
        assertFalse(ran.get());
    }

    // --- ifResolvedOrElse ---

    @Test
    void ifResolvedOrElseRunsResolvedBranch() {
        AtomicBoolean resolvedRan = new AtomicBoolean(false);
        AtomicBoolean elseRan = new AtomicBoolean(false);
        resolved(false).ifResolvedOrElse(
                (supplier, nullable) -> resolvedRan.set(true),
                nullable -> elseRan.set(true));
        assertTrue(resolvedRan.get());
        assertFalse(elseRan.get());
    }

    @Test
    void ifResolvedOrElseRunsElseBranchWithNullableFlag() {
        AtomicBoolean resolvedRan = new AtomicBoolean(false);
        AtomicReference<Boolean> elseNullable = new AtomicReference<>();
        notResolved(true).ifResolvedOrElse(
                (supplier, nullable) -> resolvedRan.set(true),
                nullable -> elseNullable.set(nullable));
        assertFalse(resolvedRan.get());
        assertEquals(Boolean.TRUE, elseNullable.get());
    }

    // --- record accessors ---

    @Test
    void accessorsExposeComponents() {
        Resolved r = resolved(true);
        assertTrue(r.resolved());
        assertTrue(r.nullable());
        assertEquals("java.lang.String", r.elementType().getName());
    }

    // --- isNullable(Annotation[]) static utility ---

    static class Holder {
        @Nullable
        Object nullableField;
        @Nonnull
        Object nonnullField;
        Object plainField;
    }

    private static Annotation[] annotationsOf(String field) throws NoSuchFieldException {
        Field f = Holder.class.getDeclaredField(field);
        return f.getAnnotations();
    }

    @Test
    void isNullableTrueWhenNullableAnnotationPresent() throws NoSuchFieldException {
        assertTrue(IInjectableElementResolver.isNullable(annotationsOf("nullableField")));
    }

    @Test
    void isNullableFalseWhenNonnullAnnotationPresent() throws NoSuchFieldException {
        assertFalse(IInjectableElementResolver.isNullable(annotationsOf("nonnullField")));
    }

    @Test
    void isNullableFalseForUnannotatedField() throws NoSuchFieldException {
        assertFalse(IInjectableElementResolver.isNullable(annotationsOf("plainField")));
    }

    @Test
    void isNullableFalseForEmptyAnnotations() {
        assertFalse(IInjectableElementResolver.isNullable(new Annotation[0]));
    }
}

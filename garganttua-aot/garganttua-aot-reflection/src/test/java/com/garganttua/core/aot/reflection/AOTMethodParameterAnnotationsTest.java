package com.garganttua.core.aot.reflection;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Modifier;

import org.junit.jupiter.api.Test;

import com.garganttua.core.reflection.IParameter;

/**
 * Regression: a generated {@link AOTMethod} descriptor stores parameter
 * types/names only (the source generator emits {@code new Annotation[0]}),
 * so RUNTIME-retained parameter annotations must be recovered from the live
 * {@link java.lang.reflect.Method}. Before the fix, {@code @Nullable} on a
 * parameter was invisible under AOT — which made the expression layer treat
 * the argument as non-nullable and abort on a null value (e.g. the workflow
 * {@code observe(…, Integer code)} timing marker).
 */
class AOTMethodParameterAnnotationsTest {

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.PARAMETER)
    @interface Marker {
    }

    @SuppressWarnings("unused")
    public static void target(@Marker String annotated, String plain) {
    }

    private static AOTMethod descriptorForTarget() {
        // Mirrors what the AOT source generator emits: types + names only,
        // empty method-level annotation array, no parameter annotations.
        return new AOTMethod(
                "target",
                AOTMethodParameterAnnotationsTest.class.getName(),
                "void",
                new String[] { "java.lang.String", "java.lang.String" },
                new String[] { "annotated", "plain" },
                Modifier.PUBLIC | Modifier.STATIC,
                new Annotation[0],
                false, false, false,
                new String[0]);
    }

    @Test
    void parameterAnnotationsAreRecoveredFromLiveMethod() {
        AOTMethod method = descriptorForTarget();

        Annotation[][] paramAnnotations = method.getParameterAnnotations();
        assertEquals(2, paramAnnotations.length);
        assertEquals(1, paramAnnotations[0].length, "@Marker must surface on the annotated parameter");
        assertEquals(Marker.class, paramAnnotations[0][0].annotationType());
        assertEquals(0, paramAnnotations[1].length, "plain parameter carries no annotations");
    }

    @Test
    void getParametersExposesParameterAnnotations() {
        AOTMethod method = descriptorForTarget();

        IParameter[] params = method.getParameters();
        assertEquals(2, params.length);
        // getAnnotations() avoids needing an installed IReflection; it is the
        // same data the expression layer reads (via isAnnotationPresent) to
        // mark a parameter @Nullable.
        Annotation[] annotated = params[0].getAnnotations();
        assertEquals(1, annotated.length, "annotated parameter must expose @Marker");
        assertEquals(Marker.class, annotated[0].annotationType());
        assertEquals(0, params[1].getAnnotations().length, "plain parameter carries no annotations");
    }
}

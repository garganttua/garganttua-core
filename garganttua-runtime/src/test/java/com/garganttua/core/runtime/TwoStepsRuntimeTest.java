package com.garganttua.core.runtime;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;

import org.junit.jupiter.api.BeforeAll;

import com.garganttua.core.injection.context.DiContext;
import com.garganttua.core.injection.context.dsl.IDiContextBuilder;
import com.garganttua.core.reflection.utils.ObjectReflectionHelper;
import com.garganttua.core.reflections.ReflectionsAnnotationScanner;
import com.garganttua.core.runtime.dsl.IRuntimesBuilder;
import com.garganttua.core.runtime.dsl.RuntimesBuilder;

public class TwoStepsRuntimeTest {

    @BeforeAll
    static void setup() {
        ObjectReflectionHelper.annotationScanner = new ReflectionsAnnotationScanner();
    }

    private IDiContextBuilder contextBuilder() {
        return DiContext.builder()
                .autoDetect(true)
                .withPackage("com.garganttua.core.runtime.annotations")
                .withPackage("com.garganttua.core.runtime.runtimes.twosteps");
    }

    private IRuntimesBuilder builder() {
        IDiContextBuilder ctx = contextBuilder();
        ctx.build().onInit().onStart();
        return RuntimesBuilder.builder().context(ctx);
    }

    private IRuntime<String, String> get(IRuntimesBuilder b) {
        Map<String, IRuntime<?, ?>> runtimes = b.build();
        assertEquals(1, runtimes.size());
        return cast(runtimes.get("runtime-1"));
    }

    @SuppressWarnings("unchecked")
    private <T> T cast(Object o) {
        return (T) o;
    }

}

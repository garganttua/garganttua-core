package com.garganttua.core.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.garganttua.api.core.runtime.executors.RuntimeExecutor;
import com.garganttua.api.spec.CoreExceptionCode;
import com.garganttua.core.runtime.RuntimeException;
import com.garganttua.core.runtime.dsl.RuntimeBuilder;
import com.garganttua.core.runtime.dsl.RuntimeProcessBuilder;

public class RuntimeTest {

    @Test
    public void exceptionThrownIfTriingToRegisterExecutorWithUnknownStep() throws RuntimeException {

        RuntimeProcessBuilder builder = new RuntimeProcessBuilder();
        builder.addStep("INITIALIZATION", "CONTEXT_INITIALIZATION", "INPUT_RAW_DATA")
                .addStep("INPUT_PROCESSING", "INPUT_DECODE", "INPUT_DECODED_DATA")
                .addStep("INPUT_PROCESSING", "INPUT_VALIDATE", "INPUT_VALIDATED_DATA")
                .addStep("OUTPUT_PROCESSING", "OUTPUT_SERIALIZE", "OUTPUT_SERIALIZED_DATA")
                .addStep("OUTPUT_PROCESSING", "OUTPUT_SEND", "OUTPUT_RAW_DATA");

        builder.print();

        RuntimeBuilder rb = new RuntimeBuilder(builder.build());

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> rb.executor("DECODE", RuntimeExecutor.class));
        assertEquals(CoreExceptionCode.RUNTIME_ERROR, exception.getCode());
        assertEquals("Cannot register executor [RuntimeExecutor] for non-existent step [DECODE]",
                exception.getMessage());

    }

    @Test
    public void test(){
        
    }
}

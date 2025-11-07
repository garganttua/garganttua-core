package com.garganttua.core.runtime;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

import com.garganttua.core.runtime.RuntimeProcess;
import com.garganttua.core.runtime.RuntimeStage;

public class RuntimeStageTest {

    @Test
    public void test(){
        RuntimeProcess process = RuntimeStage.getProcess();

        assertNotNull(process);
    }

}

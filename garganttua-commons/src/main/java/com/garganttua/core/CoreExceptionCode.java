package com.garganttua.core;

import lombok.Getter;

public enum CoreExceptionCode {

    UNKNOWN_ERROR(-1),
    SUPPLY_ERROR(100),
    RUNTIME_ERROR(200),
    REFLECTION_ERROR(300),
    MAPPER_ERROR(400),
    LIFECYCLE_ERROR(500),
    INJECTION_ERROR(600),
    EXECUTOR_ERROR(700),
    DSL_ERROR(800),
    CONDITION_ERROR(900), COPY_ERROR(000)

    ;

    @Getter
    private int code;

    CoreExceptionCode(int code) {
        this.code = code;
    }

}

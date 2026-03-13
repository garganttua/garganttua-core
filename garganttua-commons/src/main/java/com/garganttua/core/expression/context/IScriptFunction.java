package com.garganttua.core.expression.context;

import java.util.List;

public interface IScriptFunction {

    List<String> parameters();

    Object invoke(Object... args);
}

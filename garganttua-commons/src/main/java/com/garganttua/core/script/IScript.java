package com.garganttua.core.script;

import java.io.File;
import java.io.InputStream;
import java.util.Optional;

public interface IScript {

    void load(String script) throws ScriptException;

    void load(File file) throws ScriptException;

    void load(InputStream inputStream) throws ScriptException;

    void compile() throws ScriptException;

    int execute(Object... args) throws ScriptException;

    <T> Optional<T> getVariable(String name, Class<T> type);
}

package com.garganttua.core.query.dsl;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.garganttua.core.expression.dsl.IQueryMethodBinder;
import com.garganttua.core.reflection.ReflectionException;
import com.garganttua.core.reflection.methods.Methods;
import com.garganttua.core.supply.ISupplier;
import com.garganttua.core.supply.SupplyException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class QueryMethodBinder<S> implements IQueryMethodBinder<S>{

    private Method method;
    private Class<S> supplied;

    public QueryMethodBinder(Method method, Class<S> supplied)  {
        this.method = method;
        this.supplied = supplied;
    }

    @Override
    public String getExecutableReference() {
        log.atTrace().log("Getting executable reference for method {}", method);
        return Methods.prettyColored(this.method);
    }

    @Override
    public Optional<ISupplier<S>> execute() throws ReflectionException {
        throw new UnsupportedOperationException("Use executeQuery(List<Object> arguments) instead");
    }

    @Override
    public Optional<ISupplier<S>> execute(List<ISupplier<?>> arguments) throws ReflectionException {
        log.atTrace().log("Executing query method {} with {} arguments", method.getName(), arguments.size());

        return null;
    }

    @Override
    public Set<Class<?>> getDependencies() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getDependencies'");
    }

    @Override
    public String queryName() {
        return this.method.getName();
    }

    @Override
    public Type getSuppliedType() {
        return (Class<ISupplier<S>>) (Class<?>) ISupplier.class;
    }

    @Override
    public Optional<ISupplier<S>> supply() throws SupplyException {
        try {
            return this.execute();
        } catch (ReflectionException e) {
            throw new SupplyException(e);
        }
    }

}

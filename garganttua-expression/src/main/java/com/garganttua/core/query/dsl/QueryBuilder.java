package com.garganttua.core.query.dsl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import com.garganttua.core.dsl.AbstractAutomaticBuilder;
import com.garganttua.core.dsl.DslException;
import com.garganttua.core.expression.IQuery;
import com.garganttua.core.expression.dsl.IQueryBuilder;
import com.garganttua.core.expression.dsl.IQueryMethodBinder;
import com.garganttua.core.expression.dsl.IQueryMethodBinderBuilder;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class QueryBuilder extends AbstractAutomaticBuilder<IQueryBuilder, IQuery> implements IQueryBuilder {

    private final Set<String> packages = new HashSet<>();
    private final Collection<IQueryMethodBinderBuilder<?>> methodBinderBuilders = new ArrayList<>();

    @Override
    public IQueryBuilder withPackages(String[] packageNames) {
        log.atTrace().log("Adding {} packages to native configuration", packageNames.length);
        this.packages.addAll(Set.of(packageNames));
        log.atDebug().log("Added packages: {}", String.join(", ", packageNames));
        return this;
    }

    @Override
    public IQueryBuilder withPackage(String packageName) {
        log.atTrace().log("Adding package to native configuration: {}", packageName);
        this.packages.add(packageName);
        return this;
    }

    @Override
    public String[] getPackages() {
        return this.packages.toArray(new String[0]);
    }

    @Override
    public <S> IQueryMethodBinderBuilder<S> withQuery(Class<?> methodOwner, Class<S> supplied) {
        IQueryMethodBinderBuilder<S> builder = new QueryMethodBinderBuilder<>(this, methodOwner, supplied);
        this.methodBinderBuilders.add(builder);
        return builder;
    }

    @Override
    protected IQuery doBuild() throws DslException {
        Set<IQueryMethodBinder<?>> queries = this.methodBinderBuilders.stream().map(IQueryMethodBinderBuilder::build).collect(Collectors.toSet());

        return new Query(queries);
    }

    @Override
    protected void doAutoDetection() throws DslException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'doAutoDetection'");
    }

}

package com.garganttua.core.reflection.dsl;

import com.garganttua.core.dsl.IObservableBuilder;
import com.garganttua.core.reflection.IAnnotationScanner;
import com.garganttua.core.reflection.IReflection;
import com.garganttua.core.reflection.IReflectionProvider;

public interface IReflectionBuilder extends IObservableBuilder<IReflectionBuilder, IReflection> {

    IReflectionBuilder withProvider(IReflectionProvider provider);

    IReflectionBuilder withScanner(IAnnotationScanner scanner);

    //10 is the default priority, higher is more important, lower is less important
    IReflectionBuilder withProvider(IReflectionProvider provider, int priority);

    //10 is the default priority, higher is more important, lower is less important
    IReflectionBuilder withScanner(IAnnotationScanner scanner, int priority);

}

package com.garganttua.core.bootstrap.dsl;

import com.garganttua.core.dsl.IAutomaticBuilder;
import com.garganttua.core.dsl.IBuilder;
import com.garganttua.core.dsl.IPackageableBuilder;

public interface IBoostrap extends IAutomaticBuilder<IBoostrap, IBuiltRegistry>, IPackageableBuilder<IBoostrap, IBuiltRegistry> {

    IBoostrap withBuilder(IBuilder<?> builder);

}

package com.garganttua.core.bootstrap.dsl;

import com.garganttua.core.dsl.IAutomaticBuilder;
import com.garganttua.core.dsl.IBuilder;
import com.garganttua.core.dsl.IPackageableBuilder;

public interface IBoostrap extends IAutomaticBuilder<IBoostrap, Object>, IPackageableBuilder<IBoostrap, Object> {

    IBoostrap withBuilder(IBuilder<?> builder);

}

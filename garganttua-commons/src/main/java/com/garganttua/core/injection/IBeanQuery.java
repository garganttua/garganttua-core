package com.garganttua.core.injection;

import java.util.Optional;

public interface IBeanQuery<Bean> {

    Optional<Bean> execute() throws DiException;

}

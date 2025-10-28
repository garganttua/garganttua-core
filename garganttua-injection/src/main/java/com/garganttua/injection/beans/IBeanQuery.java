package com.garganttua.injection.beans;

import java.util.Optional;

import com.garganttua.injection.DiException;

public interface IBeanQuery<Bean> {

    Optional<Bean> execute() throws DiException;

}

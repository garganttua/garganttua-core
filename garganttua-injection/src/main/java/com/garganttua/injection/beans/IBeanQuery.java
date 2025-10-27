package com.garganttua.injection.beans;

import com.garganttua.injection.DiException;

public interface IBeanQuery<Bean> {

    Bean execute() throws DiException;

}

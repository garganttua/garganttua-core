package com.garganttua.di.impl.supplier;

import com.garganttua.injection.spec.beans.annotation.GGBean;
import com.garganttua.injection.spec.beans.annotation.GGBeanLoadingStrategy;

@GGBean(name = "dummy", strategy = GGBeanLoadingStrategy.newInstance)
public class DummyBean {

}

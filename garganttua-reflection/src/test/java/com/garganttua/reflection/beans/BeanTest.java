package com.garganttua.reflection.beans;

import com.garganttua.reflection.beans.annotation.GGBean;
import com.garganttua.reflection.beans.annotation.GGBeanLoadingStrategy;

@GGBean(name = "test", type = "test", strategy = GGBeanLoadingStrategy.singleton)
public class BeanTest {

}

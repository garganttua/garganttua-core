package com.garganttua.reflection.beans;

import com.garganttua.reflection.beans.annotation.GGBean;
import com.garganttua.reflection.beans.annotation.GGBeanLoadingStrategy;

@GGBean(strategy = GGBeanLoadingStrategy.newInstance)
public class BeanTestNI {

}

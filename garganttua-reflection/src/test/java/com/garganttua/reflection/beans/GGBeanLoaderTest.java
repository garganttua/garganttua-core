package com.garganttua.reflection.beans;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

public class GGBeanLoaderTest {
	
	@Test
	public void testBeanLoaderWithNameAndType() {
		
		IGGBeanLoader beanLoader = GGBeanLoaderFactory.getLoader(Arrays.asList("com"));
		assertNotNull(beanLoader);
		
		BeanTest b = beanLoader.getBean("gg", "beanName", "beanType", BeanTest.class);
		
		assertNotNull(b);
		
	}

}

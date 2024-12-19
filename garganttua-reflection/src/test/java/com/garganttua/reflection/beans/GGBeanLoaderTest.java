package com.garganttua.reflection.beans;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.garganttua.reflection.GGReflectionException;
import com.garganttua.reflection.utils.GGObjectReflectionHelper;
import com.garganttua.reflection.utils.ReflectionsAnnotationScanner;

public class GGBeanLoaderTest {
	
	@BeforeAll
	public static void setupAnnotationScanner() {
		GGObjectReflectionHelper.annotationScanner = new ReflectionsAnnotationScanner();
	}
	
	@Test
	public void testBeanLoaderWithName() throws GGReflectionException {
		
		IGGBeanLoader beanLoader = GGBeanLoaderFactory.getLoader(null, Arrays.asList("com"));
		assertNotNull(beanLoader);
		
		BeanTest b = (BeanTest) beanLoader.getBeanNamed("gg", "BeanTest");
		
		assertNotNull(b);
	}
	
	@Test
	public void testNamedBeanLoader() throws GGReflectionException {
		
		IGGBeanLoader beanLoader = GGBeanLoaderFactory.getLoader(null, Arrays.asList("com"));
		assertNotNull(beanLoader);
		
		NamedBeanTest b = (NamedBeanTest) beanLoader.getBeanNamed("gg", "test");
		
		assertNotNull(b);	
	}
	
	@Test
	public void testBeanLoaderWithType() throws GGReflectionException {
		
		IGGBeanLoader beanLoader = GGBeanLoaderFactory.getLoader(null, Arrays.asList("com"));
		assertNotNull(beanLoader);
		
		BeanTest b = beanLoader.getBeanOfType("gg", BeanTest.class);
		
		assertNotNull(b);
	}
	
	@Test
	public void testBeanLoaderWithInterfaceImplemented() throws GGReflectionException {
		
		IGGBeanLoader beanLoader = GGBeanLoaderFactory.getLoader(null, Arrays.asList("com"));
		assertNotNull(beanLoader);
		
		List<IBeanTest> b = beanLoader.getBeansImplementingInterface("gg", IBeanTest.class);
		
		assertNotNull(b);
		assertEquals(1, b.size());
	}
	
	@Test
	public void testBeanLoaderWithSingletonStrategy() throws GGReflectionException {
		
		IGGBeanLoader beanLoader = GGBeanLoaderFactory.getLoader(null, Arrays.asList("com"));
		assertNotNull(beanLoader);
		
		BeanTest b = (BeanTest) beanLoader.getBeanNamed("gg", "BeanTest");
		BeanTest b2 = (BeanTest) beanLoader.getBeanNamed("gg", "BeanTest");
		
		assertNotNull(b);
		assertNotNull(b2);
		
		assertEquals(b, b2);
	}
	
	@Test
	public void testBeanLoaderWithNewInstanceStrategy() throws GGReflectionException {
		
		IGGBeanLoader beanLoader = GGBeanLoaderFactory.getLoader(null, Arrays.asList("com"));
		assertNotNull(beanLoader);
		
		BeanTestNI b = (BeanTestNI) beanLoader.getBeanNamed("gg", "BeanTestNI");
		BeanTestNI b2 = (BeanTestNI) beanLoader.getBeanNamed("gg", "BeanTestNI");
		
		assertNotNull(b);
		assertNotNull(b2);
		
		assertNotEquals(b, b2);
	}
}

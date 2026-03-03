package com.garganttua.core.mapper;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.garganttua.core.mapper.annotations.FieldMappingRule;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IReflection;
import com.garganttua.core.reflection.dsl.ReflectionBuilder;
import com.garganttua.core.reflection.runtime.RuntimeReflectionProvider;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

public class ListMappingTest {

	private static IReflection reflection;

	@BeforeAll
	static void setUpReflection() throws Exception {
		reflection = ReflectionBuilder.builder()
				.withProvider(new RuntimeReflectionProvider())
				.build();
		IClass.setReflection(reflection);
	}

	@AfterAll
	static void tearDownReflection() {
		IClass.setReflection(null);
	}

	@NoArgsConstructor
	public static class Source {
		public List<SourceList> sourceList = new ArrayList<ListMappingTest.SourceList>();

	}
	
	@AllArgsConstructor
	@NoArgsConstructor
	public static class SourceList {
		public int sourceField;
	}
	
	@NoArgsConstructor
	public static class Dest {
		@FieldMappingRule(sourceFieldAddress = "sourceList")
		public List<DestList> destList = new ArrayList<ListMappingTest.DestList>(); 
	}
	
	@AllArgsConstructor
	@NoArgsConstructor
	public static class DestList {
		@FieldMappingRule(sourceFieldAddress = "sourceField")
		public int destField;
	}
	
	@Test
	public void test() throws MapperException {
		
		Source source = new Source();
		for( int i = 0; i < 10; i++)
			source.sourceList.add(new SourceList(i));
		
		Dest dest = new Mapper(reflection).configure(MapperConfigurationItem.FAIL_ON_ERROR, true).map(source, reflection.getClass(Dest.class));
		
		assertEquals(dest.destList.size(), 10);
		assertEquals(dest.destList.get(0).destField, 0);
	}

}

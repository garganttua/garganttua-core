package com.garganttua.objects.mapper;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.garganttua.core.mapper.MapperConfigurationItem;
import com.garganttua.core.mapper.MapperException;
import com.garganttua.core.mapper.annotations.FieldMappingRule;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

public class ListMappingTest {
	
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
		
		Dest dest = new Mapper().configure(MapperConfigurationItem.FAIL_ON_ERROR, true).map(source, Dest.class);
		
		assertEquals(dest.destList.size(), 10);
		assertEquals(dest.destList.get(0).destField, 0);
	}

}

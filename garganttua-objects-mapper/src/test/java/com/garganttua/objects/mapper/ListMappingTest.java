package com.garganttua.objects.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.garganttua.objects.mapper.annotations.GGFieldMappingRule;

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
		@GGFieldMappingRule(sourceFieldAddress = "sourceList")
		public List<DestList> destList = new ArrayList<ListMappingTest.DestList>(); 
	}
	
	@AllArgsConstructor
	@NoArgsConstructor
	public static class DestList {
		@GGFieldMappingRule(sourceFieldAddress = "sourceField")
		public int destField;
	}
	
	@Test
	public void test() throws GGMapperException {
		
		Source source = new Source();
		for( int i = 0; i < 10; i++)
			source.sourceList.add(new SourceList(i));
		
		Dest dest = new GGMapper().configure(GGMapperConfigurationItem.FAIL_ON_ERROR, true).map(source, Dest.class);
		
		assertEquals(dest.destList.size(), 10);
		assertEquals(dest.destList.get(0).destField, 0);
	}

}

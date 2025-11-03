package com.garganttua.reflection.fields;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

public class FieldsTest {
	
	static class TestObject {
		private String s;
		
		private ArrayList<String> sal;
		
		private List<Byte> bl;
		
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void testArrayListCreation() {
		ArrayList<String> list = new ArrayList<String>();
		list.add("hello");
		
		ArrayList<?> list2 = (ArrayList<?>) list;
		
		ArrayList<Integer> list3 = (ArrayList<Integer>) list2;
		
		list3.add(1);
		
		System.out.println(list3.get(0));
		System.out.println(list3.get(1));
		
		ArrayList<?> list4 = (ArrayList<?>) list3;
		
		ArrayList<String> list5 = (ArrayList<String>) list4;
		
		System.out.println(list5.get(0));
	}

	
}

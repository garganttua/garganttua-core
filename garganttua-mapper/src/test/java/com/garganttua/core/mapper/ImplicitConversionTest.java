package com.garganttua.core.mapper;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IReflection;
import com.garganttua.core.reflection.dsl.ReflectionBuilder;
import com.garganttua.core.reflection.runtime.RuntimeReflectionProvider;

class ImplicitConversionTest {

	private static IReflection reflection;

	@BeforeAll
	static void setUp() throws Exception {
		reflection = ReflectionBuilder.builder()
				.withProvider(new RuntimeReflectionProvider())
				.build();
		IClass.setReflection(reflection);
	}

	@AfterAll
	static void tearDown() {
		IClass.setReflection(null);
	}

	enum Color { RED, GREEN, BLUE }

	static class EnumSource {
		private String color;
	
	    public EnumSource() { }

	    public String getColor() { return this.color; }

	    public void setColor(String color) { this.color = color; }

	    @Override
	    public boolean equals(Object o) {
	        if (this == o) return true;
	        if (!(o instanceof EnumSource that)) return false;
	        return java.util.Objects.equals(this.color, that.color);
	    }

	    @Override
	    public int hashCode() {
	        return java.util.Objects.hash(this.color);
	    }

	    @Override
	    public String toString() {
	        return "EnumSource{" + "color=" + this.color + "}";
	    }
	}

	static class EnumDest {
		private Color color;
	
	    public EnumDest() { }

	    public Color getColor() { return this.color; }

	    public void setColor(Color color) { this.color = color; }

	    @Override
	    public boolean equals(Object o) {
	        if (this == o) return true;
	        if (!(o instanceof EnumDest that)) return false;
	        return java.util.Objects.equals(this.color, that.color);
	    }

	    @Override
	    public int hashCode() {
	        return java.util.Objects.hash(this.color);
	    }

	    @Override
	    public String toString() {
	        return "EnumDest{" + "color=" + this.color + "}";
	    }
	}

	@Test
	void testStringToEnum() throws MapperException {
		Mapper mapper = new Mapper(reflection);

		EnumSource source = new EnumSource();
		source.setColor("RED");

		EnumDest dest = mapper.map(source, reflection.getClass(EnumDest.class));
		assertNotNull(dest);
		assertEquals(Color.RED, dest.getColor());
	}

	static class EnumSource2 {
		private Color color;
	
	    public EnumSource2() { }

	    public Color getColor() { return this.color; }

	    public void setColor(Color color) { this.color = color; }

	    @Override
	    public boolean equals(Object o) {
	        if (this == o) return true;
	        if (!(o instanceof EnumSource2 that)) return false;
	        return java.util.Objects.equals(this.color, that.color);
	    }

	    @Override
	    public int hashCode() {
	        return java.util.Objects.hash(this.color);
	    }

	    @Override
	    public String toString() {
	        return "EnumSource2{" + "color=" + this.color + "}";
	    }
	}

	static class EnumDest2 {
		private String color;
	
	    public EnumDest2() { }

	    public String getColor() { return this.color; }

	    public void setColor(String color) { this.color = color; }

	    @Override
	    public boolean equals(Object o) {
	        if (this == o) return true;
	        if (!(o instanceof EnumDest2 that)) return false;
	        return java.util.Objects.equals(this.color, that.color);
	    }

	    @Override
	    public int hashCode() {
	        return java.util.Objects.hash(this.color);
	    }

	    @Override
	    public String toString() {
	        return "EnumDest2{" + "color=" + this.color + "}";
	    }
	}

	@Test
	void testEnumToString() throws MapperException {
		Mapper mapper = new Mapper(reflection);

		EnumSource2 source = new EnumSource2();
		source.setColor(Color.BLUE);

		EnumDest2 dest = mapper.map(source, reflection.getClass(EnumDest2.class));
		assertNotNull(dest);
		assertEquals("BLUE", dest.getColor());
	}

	static class IntSource {
		private String value;
	
	    public IntSource() { }

	    public String getValue() { return this.value; }

	    public void setValue(String value) { this.value = value; }

	    @Override
	    public boolean equals(Object o) {
	        if (this == o) return true;
	        if (!(o instanceof IntSource that)) return false;
	        return java.util.Objects.equals(this.value, that.value);
	    }

	    @Override
	    public int hashCode() {
	        return java.util.Objects.hash(this.value);
	    }

	    @Override
	    public String toString() {
	        return "IntSource{" + "value=" + this.value + "}";
	    }
	}

	static class IntDest {
		private int value;
	
	    public IntDest() { }

	    public int getValue() { return this.value; }

	    public void setValue(int value) { this.value = value; }

	    @Override
	    public boolean equals(Object o) {
	        if (this == o) return true;
	        if (!(o instanceof IntDest that)) return false;
	        return java.util.Objects.equals(this.value, that.value);
	    }

	    @Override
	    public int hashCode() {
	        return java.util.Objects.hash(this.value);
	    }

	    @Override
	    public String toString() {
	        return "IntDest{" + "value=" + this.value + "}";
	    }
	}

	@Test
	void testStringToInt() throws MapperException {
		Mapper mapper = new Mapper(reflection);

		IntSource source = new IntSource();
		source.setValue("42");

		IntDest dest = mapper.map(source, reflection.getClass(IntDest.class));
		assertNotNull(dest);
		assertEquals(42, dest.getValue());
	}

	static class BoolSource {
		private String active;
	
	    public BoolSource() { }

	    public String getActive() { return this.active; }

	    public void setActive(String active) { this.active = active; }

	    @Override
	    public boolean equals(Object o) {
	        if (this == o) return true;
	        if (!(o instanceof BoolSource that)) return false;
	        return java.util.Objects.equals(this.active, that.active);
	    }

	    @Override
	    public int hashCode() {
	        return java.util.Objects.hash(this.active);
	    }

	    @Override
	    public String toString() {
	        return "BoolSource{" + "active=" + this.active + "}";
	    }
	}

	static class BoolDest {
		private boolean active;
	
	    public BoolDest() { }

	    public boolean isActive() { return this.active; }

	    public void setActive(boolean active) { this.active = active; }

	    @Override
	    public boolean equals(Object o) {
	        if (this == o) return true;
	        if (!(o instanceof BoolDest that)) return false;
	        return java.util.Objects.equals(this.active, that.active);
	    }

	    @Override
	    public int hashCode() {
	        return java.util.Objects.hash(this.active);
	    }

	    @Override
	    public String toString() {
	        return "BoolDest{" + "active=" + this.active + "}";
	    }
	}

	@Test
	void testStringToBoolean() throws MapperException {
		Mapper mapper = new Mapper(reflection);

		BoolSource source = new BoolSource();
		source.setActive("true");

		BoolDest dest = mapper.map(source, reflection.getClass(BoolDest.class));
		assertNotNull(dest);
		assertTrue(dest.isActive());
	}

	static class IntToStrSource {
		private int count;
	
	    public IntToStrSource() { }

	    public int getCount() { return this.count; }

	    public void setCount(int count) { this.count = count; }

	    @Override
	    public boolean equals(Object o) {
	        if (this == o) return true;
	        if (!(o instanceof IntToStrSource that)) return false;
	        return java.util.Objects.equals(this.count, that.count);
	    }

	    @Override
	    public int hashCode() {
	        return java.util.Objects.hash(this.count);
	    }

	    @Override
	    public String toString() {
	        return "IntToStrSource{" + "count=" + this.count + "}";
	    }
	}

	static class IntToStrDest {
		private String count;
	
	    public IntToStrDest() { }

	    public String getCount() { return this.count; }

	    public void setCount(String count) { this.count = count; }

	    @Override
	    public boolean equals(Object o) {
	        if (this == o) return true;
	        if (!(o instanceof IntToStrDest that)) return false;
	        return java.util.Objects.equals(this.count, that.count);
	    }

	    @Override
	    public int hashCode() {
	        return java.util.Objects.hash(this.count);
	    }

	    @Override
	    public String toString() {
	        return "IntToStrDest{" + "count=" + this.count + "}";
	    }
	}

	@Test
	void testIntToString() throws MapperException {
		Mapper mapper = new Mapper(reflection);

		IntToStrSource source = new IntToStrSource();
		source.setCount(99);

		IntToStrDest dest = mapper.map(source, reflection.getClass(IntToStrDest.class));
		assertNotNull(dest);
		assertEquals("99", dest.getCount());
	}
}

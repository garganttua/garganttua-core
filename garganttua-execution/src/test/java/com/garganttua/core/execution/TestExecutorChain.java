package com.garganttua.core.execution;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class TestExecutorChain {

	@Test
	public void testSimpleAdder() throws ExecutorException {
		int integer = 0;
		ExecutorChain<Integer> executorChain = new ExecutorChain<>();
		
		executorChain.addExecutor((i, chain) -> {
			i = i+1;
			System.out.println(i);
			chain.execute(i);
		});
		executorChain.addExecutor((i, chain) -> {
			i++;
			System.out.println(i);
			chain.execute(i);
		});
		executorChain.addExecutor((i, chain) -> {
			i++;
			System.out.println(i);
			chain.execute(i);
		});
		executorChain.addExecutor((i, chain) -> {
			i++;
			System.out.println(i);
			chain.execute(i);
		});
		executorChain.addExecutor((i, chain) -> {
			assertEquals(4, i);
			chain.execute(i);
		});
		
		executorChain.execute(integer);
	}
	
	@Test
	public void testStringConcatenation() throws ExecutorException {
		StringBuilder stringBuilder = new StringBuilder();
		ExecutorChain<StringBuilder> executorChain = new ExecutorChain<>();
		
		executorChain.addExecutor((st, chain) -> {
			st.append("This ");
			chain.execute(st);
		});
		executorChain.addExecutor((st, chain) -> {
			st.append("is ");
			chain.execute(st);
		});
		executorChain.addExecutor((st, chain) -> {
			st.append("test");
			chain.execute(st);
		});
		
		executorChain.execute(stringBuilder);
		
		assertEquals("This is test", stringBuilder.toString());
	}
	
	@Test
	public void testFifo() throws ExecutorException {
		Integer integer = 0;
		ExecutorChain<Integer> executorChain = new ExecutorChain<>();
		
		executorChain.addExecutor((i, chain) -> {
			i *= 2;
			System.out.println(i);
			chain.execute(i);
		});
		executorChain.addExecutor((i, chain) -> {
			i++;
			System.out.println(i);
			chain.execute(i);
		});
		executorChain.addExecutor((i, chain) -> {
			i *= 2;
			System.out.println(i);
			chain.execute(i);
		});
		executorChain.addExecutor((i, chain) -> {
			i++;
			System.out.println(i);
			chain.execute(i);
		});
		executorChain.addExecutor((i, chain) -> {
			i *= 2;
			System.out.println(i);
			assertEquals(6, i);
			chain.execute(i);
		});
		
		executorChain.execute(integer);
	}
}

package com.garganttua.executor.chain;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class TestGGExecutorChain {

	@Test
	public void testSimpleAdder() throws GGExecutorException {
		int integer = 0;
		GGExecutorChain<Integer> executorChain = new GGExecutorChain<>();
		
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
	public void testStringConcatenation() throws GGExecutorException {
		StringBuilder stringBuilder = new StringBuilder();
		GGExecutorChain<StringBuilder> executorChain = new GGExecutorChain<>();
		
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
	public void testFifo() throws GGExecutorException {
		Integer integer = 0;
		GGExecutorChain<Integer> executorChain = new GGExecutorChain<>();
		
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

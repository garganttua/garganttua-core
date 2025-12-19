package com.garganttua.core.expression.context;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.garganttua.core.expression.IExpressionNode;
import com.garganttua.core.reflection.ObjectAddress;
import com.garganttua.core.supply.ISupplier;
import static com.garganttua.core.supply.dsl.NullSupplierBuilder.*;

public class ExpressionNodeFactoryTest {

    static class TestService {
        
        static public String string(String string) {
            return string;
        }
        static public String greet(String name) {
            return "Hello, " + name;
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testExpressionNodeFactoryCreation() throws Exception {

        ExpressionNodeFactory<String, ISupplier<String>> leafFactory = new ExpressionNodeFactory<String, ISupplier<String>>(
                of(TestService.class).build(),
                (Class<ISupplier<String>>) (Class<?>) ISupplier.class,
                TestService.class.getMethod("string", String.class),
                new ObjectAddress("string"),
                List.of(false),
                Optional.of("string"),
                Optional.of("String converter"));

        ExpressionNodeFactory<String, ISupplier<String>> nodefactory = new ExpressionNodeFactory<String, ISupplier<String>>(
                of(TestService.class).build(),
                (Class<ISupplier<String>>) (Class<?>) ISupplier.class,
                TestService.class.getMethod("greet", String.class),
                new ObjectAddress("greet"),
                List.of(false),
                Optional.of("greet"),
                Optional.of("Greeting function"));

        Optional<IExpressionNode<String,ISupplier<String>>> leaf = leafFactory.supply(new ExpressionNodeContext(List.of("greet")));

        Optional<IExpressionNode<String,ISupplier<String>>> expression = nodefactory.supply(new ExpressionNodeContext(List.of(leaf.get())));

        assertEquals("Hello, greet", expression.get().evaluate().supply().get());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testExpressionNodeCanHandleEitherOtherExpressionNodeAndObject() throws Exception {

        ExpressionNodeFactory<String, ISupplier<String>> nodefactory = new ExpressionNodeFactory<String, ISupplier<String>>(
                of(TestService.class).build(),
                (Class<ISupplier<String>>) (Class<?>) ISupplier.class,
                TestService.class.getMethod("greet", String.class),
                new ObjectAddress("greet"),
                List.of(false),
                Optional.of("greet"),
                Optional.of("Greeting function"));

        Optional<IExpressionNode<String,ISupplier<String>>> expression = nodefactory.supply(new ExpressionNodeContext(List.of("greet")));

        assertEquals("Hello, greet", expression.get().evaluate().supply().get());
    }
}

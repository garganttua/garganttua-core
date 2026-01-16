package com.garganttua.core.reflection.binders;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.reflection.binders.dsl.AbstractMethodBinderBuilder;
import com.garganttua.core.supply.dsl.FixedSupplierBuilder;
import com.garganttua.core.supply.dsl.ISupplierBuilder;
import com.garganttua.core.supply.dsl.NullSupplierBuilder;

public class MethodBinderTest {


    static class MethodObject {
        public MethodObject(){

        }
        String echo(String message) {
            return message;
        }

        static String staticEcho(String message){
            return message;
        }
    }

    class ConcreteMethodBinderBuilder
            extends AbstractMethodBinderBuilder<String, ConcreteMethodBinderBuilder, Object, IMethodBinder<String>> {

        protected ConcreteMethodBinderBuilder(Object up, ISupplierBuilder<?, ?> supplier) throws DslException {
            super(up, supplier);
        }

        @Override
        protected void doAutoDetection() throws DslException {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException("Unimplemented method 'doAutoDetection'");
        }

    }

    @Test
    public void testEchoMethod() {

        ConcreteMethodBinderBuilder b = new ConcreteMethodBinderBuilder(new Object(), FixedSupplierBuilder.of(new MethodObject()))
                .method("echo", String.class, String.class).withParam("Hello");
        IMethodBinder<String> mb = b.build();

        assertEquals("Hello", mb.supply().get());

    }

    @Test
    public void testStaticEchoMethod() {

        ConcreteMethodBinderBuilder b = new ConcreteMethodBinderBuilder(new Object(), new NullSupplierBuilder<>(MethodObject.class))
                .method("staticEcho", String.class, String.class).withParam("Hello");
        IMethodBinder<String> mb = b.build();

        assertEquals("Hello", mb.supply().get());

    }

}

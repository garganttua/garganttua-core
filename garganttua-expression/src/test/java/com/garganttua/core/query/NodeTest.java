package com.garganttua.core.query;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.garganttua.core.reflection.ObjectAddress;
import com.garganttua.core.reflection.binders.ContextualMethodBinder;
import com.garganttua.core.supply.FixedSupplier;
import com.garganttua.core.supply.ISupplier;
import com.garganttua.core.supply.Supplier;

public class NodeTest {

        class StringConcatenator {
                String concatenate(String string, String string2) {
                        return string + "" + string2;
                }
        }

        @SuppressWarnings("unchecked")
        @Test
        public void testSimpleConcatenationExpression() throws Exception {

                ExpressionNode<String> node1 = new ExpressionNode<String>("", params -> {
                        ISupplier<String> supplier = new FixedSupplier<String>("Hello world from node 1");
                        return supplier;
                }, String.class);

                ExpressionNode<String> node2 = new ExpressionNode<String>("", params -> {
                        ISupplier<String> supplier = (ISupplier<String>) params[0];
                        String t = supplier.supply().get() + " node 2";
                        return new FixedSupplier<String>(t);
                }, List.of(node1), String.class);

                ExpressionNode<String> node3 = new ExpressionNode<String>("", params -> {
                        ISupplier<String> supplier = (ISupplier<String>) params[0];
                        String t = supplier.supply().get() + " node 3";
                        return new FixedSupplier<String>(t);
                }, List.of(node2), String.class);

                assertEquals("Hello world from node 1 node 2 node 3", node3.evaluate().supply().get());
                assertEquals("Hello world from node 1 node 2 node 3", node3.supply().get().supply().get());

                Expression<String> exp = new Expression<>(node3);

                assertEquals("Hello world from node 1 node 2 node 3", exp.evaluate().supply().get());
                assertEquals("Hello world from node 1 node 2 node 3", exp.supply().get().supply().get());
        }

        @Test
        public void testContextualEvaluationWithinNonContextualExpressionNode() throws Exception {

                final ExpressionContext context = new ExpressionContext();

                ExpressionNode<String> node1 = new ExpressionNode<String>("", params -> {

                        ContextualMethodBinder<String, ExpressionContext> mb = new ContextualMethodBinder<>(
                                        new FixedSupplier<>(
                                                        new StringConcatenator()),
                                        new ObjectAddress("concatenate"),
                                        List.of(
                                                        new FixedSupplier<String>("Hello from node 1"),
                                                        new FixedSupplier<String>("")),
                                        String.class);

                        return mb;
                }, String.class);

                ExpressionNode<String> node2 = new ExpressionNode<String>("", params -> {
                        ContextualMethodBinder<String, ExpressionContext> mb = new ContextualMethodBinder<>(
                                        new FixedSupplier<>(
                                                        new StringConcatenator()),
                                        new ObjectAddress("concatenate"),
                                        List.of(
                                                        params[0],
                                                        new FixedSupplier<String>(" node 2")),
                                        String.class);

                        return mb;
                }, List.of(node1), String.class);

                ExpressionNode<String> node3 = new ExpressionNode<String>("", params -> {
                        ContextualMethodBinder<String, ExpressionContext> mb = new ContextualMethodBinder<>(
                                        new FixedSupplier<>(
                                                        new StringConcatenator()),
                                        new ObjectAddress("concatenate"),
                                        List.of(
                                                        params[0],
                                                        new FixedSupplier<String>(" node 3")),
                                        String.class);

                        return mb;
                }, List.of(node2), String.class);

                Expression<String> exp = new Expression<>(node3);
                assertDoesNotThrow(exp::evaluate);
                assertEquals("Hello from node 1 node 2 node 3",
                                Supplier.contextualSupply(exp.evaluate(), new ExpressionContext()));

        }

        /*
         * @Test
         * public void testContextualEvaluationWithinContextualExpressionNode() throws
         * Exception {
         * 
         * IExpressionNode<String> node1 = new ContextualExpressionNode<String>("", (c,
         * params) -> {
         * ContextualMethodBinder<String, ExpressionContext> mb = new
         * ContextualMethodBinder<>(
         * new FixedSupplier<>(
         * new StringConcatenator()),
         * new ObjectAddress("concatenate"),
         * List.of(
         * new FixedSupplier<String>("Hello from node 1"),
         * new FixedSupplier<String>("")),
         * String.class,
         * ExpressionContext.class);
         * 
         * return mb.execute(c);
         * }, String.class);
         * 
         * IExpressionNode<String> node2 = new ContextualExpressionNode<String>("", (c,
         * params) -> {
         * ContextualMethodBinder<String, ExpressionContext> mb = new
         * ContextualMethodBinder<>(
         * new FixedSupplier<>(
         * new StringConcatenator()),
         * new ObjectAddress("concatenate"),
         * List.of(
         * params[0],
         * new FixedSupplier<String>(" node 2")),
         * String.class,
         * ExpressionContext.class);
         * 
         * return mb.execute(c);
         * }, List.of(node1), String.class);
         * 
         * IExpressionNode<String> node3 = new ContextualExpressionNode<String>("", (c,
         * params) -> {
         * ContextualMethodBinder<String, ExpressionContext> mb = new
         * ContextualMethodBinder<>(
         * new FixedSupplier<>(
         * new StringConcatenator()),
         * new ObjectAddress("concatenate"),
         * List.of(
         * params[0],
         * new FixedSupplier<String>(" node 3")),
         * String.class,
         * ExpressionContext.class);
         * 
         * return mb.execute(c);
         * }, List.of(node2), String.class);
         * 
         * IExpression<String> exp = new Expression<>(node3);
         * 
         * assertEquals("Hello from node 1 node 2 node 3", exp.evaluate().get());
         * }
         */

        /*
         * @Test
         * public void testMixedEvaluation() throws Exception {
         * 
         * IExpressionNode<String, ISupplier<String>> node1 = new
         * ContextualExpressionNode<>("", (c, params) -> {
         * ContextualMethodBinder<String, ExpressionContext> mb = new
         * ContextualMethodBinder<>(
         * new FixedSupplier<>(
         * new StringConcatenator()),
         * new ObjectAddress("concatenate"),
         * List.of(
         * new FixedSupplier<String>("Hello from node 1"),
         * new FixedSupplier<String>("")),
         * String.class,
         * ExpressionContext.class);
         * 
         * return Optional.of(mb);
         * }, String.class);
         * 
         * IExpressionNode<String, ISupplier<String>> node2 = new
         * ExpressionNode<>("", (params) -> {
         * MethodBinder<String> mb = new MethodBinder<>(
         * new FixedSupplier<>(
         * new StringConcatenator()),
         * new ObjectAddress("concatenate"),
         * List.of(
         * params[0],
         * new FixedSupplier<String>(" node 2")),
         * String.class);
         * 
         * return Optional.of(mb);
         * }, List.of(node1), String.class);
         * 
         * IExpressionNode<String, ISupplier<String>> node3 = new
         * ContextualExpressionNode<>("", (c, params) -> {
         * ContextualMethodBinder<String, ExpressionContext> mb = new
         * ContextualMethodBinder<>(
         * new FixedSupplier<>(
         * new StringConcatenator()),
         * new ObjectAddress("concatenate"),
         * List.of(
         * params[0],
         * new FixedSupplier<String>(" node 3")),
         * String.class,
         * ExpressionContext.class);
         * 
         * return Optional.of(mb);
         * }, List.of(node2), String.class);
         * 
         * IExpression<String> exp = new Expression<>(node3);
         * 
         * assertEquals("Hello from node 1 node 2 node 3", exp.evaluate().get());
         * }
         * 
         * @Test
         * public void testReturningDirectlyMethodBinders() throws Exception {
         * 
         * IExpressionNode<String> node1 = new ContextualExpressionNode<String>("", (c,
         * params) -> {
         * ContextualMethodBinder<String, ExpressionContext> mb = new
         * ContextualMethodBinder<>(
         * new FixedSupplier<>(
         * new StringConcatenator()),
         * new ObjectAddress("concatenate"),
         * List.of(
         * new FixedSupplier<String>("Hello from node 1"),
         * new FixedSupplier<String>("")),
         * String.class,
         * ExpressionContext.class);
         * 
         * return mb;
         * }, String.class);
         * 
         * IExpressionNode<String> node2 = new ExpressionNode<String>("", (params) -> {
         * MethodBinder<String> mb = new MethodBinder<>(
         * new FixedSupplier<>(
         * new StringConcatenator()),
         * new ObjectAddress("concatenate"),
         * List.of(
         * params[0],
         * new FixedSupplier<String>(" node 2")),
         * String.class);
         * 
         * return mb.execute();
         * }, List.of(node1), String.class);
         * 
         * IExpressionNode<String> node3 = new ContextualExpressionNode<String>("", (c,
         * params) -> {
         * ContextualMethodBinder<String, ExpressionContext> mb = new
         * ContextualMethodBinder<>(
         * new FixedSupplier<>(
         * new StringConcatenator()),
         * new ObjectAddress("concatenate"),
         * List.of(
         * params[0],
         * new FixedSupplier<String>(" node 3")),
         * String.class,
         * ExpressionContext.class);
         * 
         * return mb.execute(c);
         * }, List.of(node2), String.class);
         * 
         * IExpression<String> exp = new Expression<>(node3);
         * 
         * assertEquals("Hello from node 1 node 2 node 3", exp.evaluate().get());
         * }
         */

}

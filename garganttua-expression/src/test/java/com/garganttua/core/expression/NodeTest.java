package com.garganttua.core.expression;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.garganttua.core.expression.context.ExpressionContext;
import com.garganttua.core.expression.context.IExpressionContext;
import com.garganttua.core.reflection.ObjectAddress;
import com.garganttua.core.reflection.binders.ContextualMethodBinder;
import com.garganttua.core.reflection.binders.MethodBinder;
import com.garganttua.core.supply.FixedSupplier;
import com.garganttua.core.supply.IContextualSupplier;
import com.garganttua.core.supply.ISupplier;
import com.garganttua.core.supply.Supplier;
import com.garganttua.core.supply.SupplyException;

public class NodeTest {

        static class StringConcatenator {
                String concatenate(String string, String string2) {
                        return string + "" + string2;
                }
        }

        @SuppressWarnings("unchecked")
        @Test
        public void testSimpleConcatenationExpression() throws Exception {

                ExpressionLeaf<String> leaf = new ExpressionLeaf<>("", params -> {
                        return new FixedSupplier<String>((String)params[0]);
                }, String.class, "Hello world from");

                ExpressionNode<String> node1 = new ExpressionNode<String>("", params -> {
                        ISupplier<String> supplier = (ISupplier<String>) params[0];
                        String t = supplier.supply().get() + " node 1";
                        return new FixedSupplier<String>(t);
                },String.class, List.of(leaf));

                ExpressionNode<String> node2 = new ExpressionNode<String>("", params -> {
                        ISupplier<String> supplier = (ISupplier<String>) params[0];
                        String t = supplier.supply().get() + " node 2";
                        return new FixedSupplier<String>(t);
                }, String.class, List.of(node1));

                ExpressionNode<String> node3 = new ExpressionNode<String>("", params -> {
                        ISupplier<String> supplier = (ISupplier<String>) params[0];
                        String t = supplier.supply().get() + " node 3";
                        return new FixedSupplier<String>(t);
                }, String.class, List.of(node2));

                assertEquals("Hello world from node 1 node 2 node 3", node3.evaluate().supply().get());
                assertEquals("Hello world from node 1 node 2 node 3", node3.supply().get().supply().get());

                Expression<String> exp = new Expression<>(node3);

                assertEquals("Hello world from node 1 node 2 node 3", exp.evaluate().supply().get());
                assertEquals("Hello world from node 1 node 2 node 3", exp.supply().get().supply().get());
        }

        @Test
        public void testContextualEvaluationWithinNonContextualExpressionNode() throws Exception {

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
                }, String.class, List.of(node1));

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
                }, String.class, List.of(node2));

                Expression<String> exp = new Expression<>(node3);
                assertDoesNotThrow(exp::evaluate);
                assertEquals("Hello from node 1 node 2 node 3",
                                Supplier.contextualRecursiveSupply(exp.evaluate(), new ExpressionContext(Set.of())));

        }

        @Test
        public void testContextualEvaluationWithinContextualExpressionNode() throws Exception {

                IExpressionNode<String, ? extends ISupplier<String>> node1 = new ContextualExpressionNode<String>("",
                                (c,
                                                params) -> {
                                        ContextualMethodBinder<String, IExpressionContext> mb = new ContextualMethodBinder<>(
                                                        new FixedSupplier<>(
                                                                        new StringConcatenator()),
                                                        new ObjectAddress("concatenate"),
                                                        List.of(
                                                                        new FixedSupplier<String>("Hello from node 1"),
                                                                        new FixedSupplier<String>("")),
                                                        String.class);

                                        return mb;
                                }, String.class);

                IExpressionNode<String, ? extends ISupplier<String>> node2 = new ContextualExpressionNode<String>("",
                                (c,
                                                params) -> {
                                        ContextualMethodBinder<String, IExpressionContext> mb = new ContextualMethodBinder<>(
                                                        new FixedSupplier<>(
                                                                        new StringConcatenator()),
                                                        new ObjectAddress("concatenate"),
                                                        List.of(
                                                                        params[0],
                                                                        new FixedSupplier<String>(" node 2")),
                                                        String.class);

                                        return mb;
                                }, String.class, List.of(node1));

                IExpressionNode<String, ? extends ISupplier<String>> node3 = new ContextualExpressionNode<String>("",
                                (c,
                                                params) -> {
                                        ContextualMethodBinder<String, IExpressionContext> mb = new ContextualMethodBinder<>(
                                                        new FixedSupplier<>(
                                                                        new StringConcatenator()),
                                                        new ObjectAddress("concatenate"),
                                                        List.of(
                                                                        params[0],
                                                                        new FixedSupplier<String>(" node 3")),
                                                        String.class);

                                        return mb;
                                }, String.class, List.of(node2));

                Expression<String> exp = new Expression<>(node3);

                assertEquals("Hello from node 1 node 2 node 3",
                                Supplier.contextualRecursiveSupply(exp, new ExpressionContext(Set.of())));
        }

        @Test
        public void testMixedEvaluation() throws Exception {

                IExpressionNode<String, ? extends ISupplier<String>> node1 = new ContextualExpressionNode<>("",
                                (c, params) -> {
                                        ContextualMethodBinder<String, IExpressionContext> mb = new ContextualMethodBinder<>(
                                                        new FixedSupplier<>(
                                                                        new StringConcatenator()),
                                                        new ObjectAddress("concatenate"),
                                                        List.of(
                                                                        new FixedSupplier<String>("Hello from node 1"),
                                                                        new FixedSupplier<String>("")),
                                                        String.class);

                                        return mb;
                                }, String.class);

                IExpressionNode<String, ? extends ISupplier<String>> node2 = new ExpressionNode<>("", (params) -> {
                        MethodBinder<String> mb = new MethodBinder<>(
                                        new FixedSupplier<>(
                                                        new StringConcatenator()),
                                        new ObjectAddress("concatenate"),
                                        List.of(
                                                        params[0],
                                                        new FixedSupplier<String>(" node 2")),
                                        String.class);

                        return mb;
                }, String.class, List.of(node1));

                IExpressionNode<String, ? extends ISupplier<String>> node3 = new ContextualExpressionNode<>("",
                                (c, params) -> {
                                        ContextualMethodBinder<String, IExpressionContext> mb = new ContextualMethodBinder<>(
                                                        new FixedSupplier<>(
                                                                        new StringConcatenator()),
                                                        new ObjectAddress("concatenate"),
                                                        List.of(
                                                                        params[0],
                                                                        new FixedSupplier<String>(" node 3")),
                                                        String.class);

                                        return mb;
                                }, String.class, List.of(node2));

                Expression<String> exp = new Expression<>(node3);

                assertEquals("Hello from node 1 node 2 node 3",
                                Supplier.contextualRecursiveSupply(exp, new ExpressionContext(Set.of())));
        }

        @Test
        public void testContextualSupplierForMethodParameter() throws Exception {

                IExpressionNode<String, ? extends ISupplier<String>> node1 = new ContextualExpressionNode<>("",
                                (c, params) -> {
                                        ContextualMethodBinder<String, IExpressionContext> mb = new ContextualMethodBinder<>(
                                                        new FixedSupplier<>(
                                                                        new StringConcatenator()),
                                                        new ObjectAddress("concatenate"),
                                                        List.of(
                                                                        new FixedSupplier<String>("Hello from node 1"),
                                                                        new FixedSupplier<String>("")),
                                                        String.class);

                                        return mb;
                                }, String.class);

                IExpressionNode<String, ? extends ISupplier<String>> node2 = new ExpressionNode<>("", (params) -> {
                        ContextualMethodBinder<String, IExpressionContext> mb = new ContextualMethodBinder<>(
                                        new FixedSupplier<>(
                                                        new StringConcatenator()),
                                        new ObjectAddress("concatenate"),
                                        List.of(
                                                        params[0],
                                                        new IContextualSupplier<String, String>() {

                                                                @Override
                                                                public Type getSuppliedType() {
                                                                        return String.class;
                                                                }

                                                                @Override
                                                                public Class<String> getOwnerContextType() {
                                                                        return String.class;
                                                                }

                                                                @Override
                                                                public Optional<String> supply(String ownerContext,
                                                                                Object... otherContexts)
                                                                                throws SupplyException {
                                                                        return Optional.of(ownerContext);
                                                                }
                                                        }),
                                        String.class);

                        return mb;
                }, String.class, List.of(node1));

                Expression<String> exp = new Expression<>(node2);

                assertEquals("Hello from node 1 node 2",
                                Supplier.contextualRecursiveSupply(exp, new ExpressionContext(Set.of()), " node 2"));
        }

        @Test
        @DisplayName("This test checks that an exception is thrown when leaf node is not contextual but parent nodes are. In this case, the contexts are not propagated to the parent nodes. ")
        public void testContextualSupplierForMethodOwner_error() throws Exception {

                IExpressionNode<String, ? extends ISupplier<String>> node1 = new ContextualExpressionNode<>("",
                                (c, params) -> {
                                        ContextualMethodBinder<String, IExpressionContext> mb = new ContextualMethodBinder<>(
                                                        new IContextualSupplier<StringConcatenator, StringConcatenator>() {

                                                                @Override
                                                                public Type getSuppliedType() {
                                                                        return StringConcatenator.class;
                                                                }

                                                                @Override
                                                                public Class<StringConcatenator> getOwnerContextType() {
                                                                        return StringConcatenator.class;
                                                                }

                                                                @Override
                                                                public Optional<StringConcatenator> supply(
                                                                                StringConcatenator ownerContext,
                                                                                Object... otherContexts)
                                                                                throws SupplyException {
                                                                        return Optional.of(ownerContext);
                                                                }
                                                        },
                                                        new ObjectAddress("concatenate"),
                                                        List.of(
                                                                        new FixedSupplier<String>("Hello from node 1"),
                                                                        new FixedSupplier<String>("")),
                                                        String.class);

                                        return mb;
                                }, String.class);

                IExpressionNode<String, ? extends ISupplier<String>> node2 = new ExpressionNode<>("", (params) -> {
                        MethodBinder<String> mb = new MethodBinder<>(
                                        new FixedSupplier<>(
                                                        new StringConcatenator()),
                                        new ObjectAddress("concatenate"),
                                        List.of(
                                                        params[0],
                                                        new FixedSupplier<String>(" node 2")),
                                        String.class);

                        return mb;
                }, String.class, List.of(node1));

                Expression<String> exp = new Expression<>(node2);

                SupplyException exception = assertThrows(SupplyException.class,
                                () -> Supplier.contextualRecursiveSupply(exp, new ExpressionContext(Set.of()),
                                                new StringConcatenator()));

                assertEquals("Error on parameter 0", exception.getMessage());
        }

        @Test
        public void testContextualSupplierForMethodOwner_success() throws Exception {

                IExpressionNode<String, ? extends ISupplier<String>> node1 = new ContextualExpressionNode<>("",
                                (c, params) -> {
                                        ContextualMethodBinder<String, IExpressionContext> mb = new ContextualMethodBinder<>(
                                                        new IContextualSupplier<StringConcatenator, StringConcatenator>() {

                                                                @Override
                                                                public Type getSuppliedType() {
                                                                        return StringConcatenator.class;
                                                                }

                                                                @Override
                                                                public Class<StringConcatenator> getOwnerContextType() {
                                                                        return StringConcatenator.class;
                                                                }

                                                                @Override
                                                                public Optional<StringConcatenator> supply(
                                                                                StringConcatenator ownerContext,
                                                                                Object... otherContexts)
                                                                                throws SupplyException {
                                                                        return Optional.of(ownerContext);
                                                                }
                                                        },
                                                        new ObjectAddress("concatenate"),
                                                        List.of(
                                                                        new FixedSupplier<String>("Hello from node 1"),
                                                                        new FixedSupplier<String>("")),
                                                        String.class);

                                        return mb;
                                }, String.class);

                IExpressionNode<String, ? extends ISupplier<String>> node2 = new ExpressionNode<>("", (params) -> {
                        ContextualMethodBinder<String, IExpressionContext> mb = new ContextualMethodBinder<>(
                                        new FixedSupplier<>(
                                                        new StringConcatenator()),
                                        new ObjectAddress("concatenate"),
                                        List.of(
                                                        params[0],
                                                        new FixedSupplier<String>(" node 2")),
                                        String.class);

                        return mb;
                }, String.class, List.of(node1));

                Expression<String> exp = new Expression<>(node2);

                assertEquals("Hello from node 1 node 2",
                                Supplier.contextualRecursiveSupply(exp, new ExpressionContext(Set.of()), new StringConcatenator()));
        }

}

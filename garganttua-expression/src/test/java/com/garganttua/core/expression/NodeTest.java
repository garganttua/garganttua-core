package com.garganttua.core.expression;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.expression.context.ExpressionContext;
import com.garganttua.core.expression.context.IExpressionContext;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IMethodReturn;
import com.garganttua.core.reflection.IReflection;
import com.garganttua.core.reflection.ReflectionException;
import com.garganttua.core.reflection.binders.ContextualMethodBinder;
import com.garganttua.core.reflection.binders.MethodBinder;
import com.garganttua.core.reflection.dsl.ReflectionBuilder;
import com.garganttua.core.reflection.methods.MethodResolver;
import com.garganttua.core.reflection.methods.ResolvedMethod;
import com.garganttua.core.reflection.runtime.RuntimeReflectionProvider;
import com.garganttua.core.supply.FixedSupplier;
import com.garganttua.core.supply.IContextualSupplier;
import com.garganttua.core.supply.ISupplier;
import com.garganttua.core.supply.Supplier;
import com.garganttua.core.supply.SupplyException;

public class NodeTest {

        private static RuntimeReflectionProvider reflectionProvider;

        @BeforeAll
        static void setUpReflection() throws DslException {
                reflectionProvider = new RuntimeReflectionProvider();
                IReflection reflection = ReflectionBuilder.builder()
                                .withProvider(reflectionProvider)
                                .build();
                IClass.setReflection(reflection);
        }

        @AfterAll
        static void tearDownReflection() {
                IClass.setReflection(null);
        }

        static class StringConcatenator {
                String concatenate(String string, String string2) {
                        return string + "" + string2;
                }
        }

        /**
         * Resolves the "concatenate" method on StringConcatenator.
         */
        private static ResolvedMethod resolveConcatenateMethod() {
                try {
                        return MethodResolver.methodByName(
                                        IClass.getClass(StringConcatenator.class),
                                        reflectionProvider,
                                        "concatenate",
                                        IClass.getClass(String.class),
                                        IClass.getClass(String.class), IClass.getClass(String.class));
                } catch (ReflectionException e) {
                        throw new RuntimeException("Failed to resolve concatenate method", e);
                }
        }

        /**
         * Wrapper that extracts the value from IMethodReturn for non-contextual suppliers.
         */
        private static class MethodReturnUnwrappingSupplier<T> implements ISupplier<T> {
                private final ISupplier<IMethodReturn<T>> delegate;
                private final IClass<T> returnType;

                MethodReturnUnwrappingSupplier(ISupplier<IMethodReturn<T>> delegate, IClass<T> returnType) {
                        this.delegate = delegate;
                        this.returnType = returnType;
                }

                @Override
                public Optional<T> supply() throws SupplyException {
                        return delegate.supply()
                                        .flatMap(methodReturn -> methodReturn.firstOptional());
                }

                @Override
                public Type getSuppliedType() {
                        return returnType.getType();
                }

                @Override
                public IClass<T> getSuppliedClass() {
                        return returnType;
                }
        }

        /**
         * Wrapper that extracts the value from IMethodReturn for contextual suppliers.
         */
        private static class MethodReturnUnwrappingContextualSupplier<T, C> implements IContextualSupplier<T, C> {
                private final IContextualSupplier<IMethodReturn<T>, C> delegate;
                private final IClass<T> returnType;

                MethodReturnUnwrappingContextualSupplier(IContextualSupplier<IMethodReturn<T>, C> delegate, IClass<T> returnType) {
                        this.delegate = delegate;
                        this.returnType = returnType;
                }

                @Override
                public Optional<T> supply(C context, Object... otherContexts) throws SupplyException {
                        return delegate.supply(context, otherContexts)
                                        .flatMap(methodReturn -> methodReturn.firstOptional());
                }

                @Override
                public IClass<C> getOwnerContextType() {
                        return delegate.getOwnerContextType();
                }

                @Override
                public Type getSuppliedType() {
                        return returnType.getType();
                }

                @Override
                public IClass<T> getSuppliedClass() {
                        return returnType;
                }
        }

        @SuppressWarnings("unchecked")
        @Test
        public void testSimpleConcatenationExpression() throws Exception {

                ExpressionNode<String> leaf = new ExpressionNode<>("", params -> {
                        return new FixedSupplier<String>((String) params[0], IClass.getClass(String.class));
                }, IClass.getClass(String.class), List.of("Hello world from"));

                ExpressionNode<String> node1 = new ExpressionNode<String>("", params -> {
                        ISupplier<String> supplier = (ISupplier<String>) params[0];
                        String t = supplier.supply().get() + " node 1";
                        return new FixedSupplier<String>(t, IClass.getClass(String.class));
                }, IClass.getClass(String.class), List.of(leaf));

                ExpressionNode<String> node2 = new ExpressionNode<String>("", params -> {
                        ISupplier<String> supplier = (ISupplier<String>) params[0];
                        String t = supplier.supply().get() + " node 2";
                        return new FixedSupplier<String>(t, IClass.getClass(String.class));
                }, IClass.getClass(String.class), List.of(node1));

                ExpressionNode<String> node3 = new ExpressionNode<String>("", params -> {
                        ISupplier<String> supplier = (ISupplier<String>) params[0];
                        String t = supplier.supply().get() + " node 3";
                        return new FixedSupplier<String>(t, IClass.getClass(String.class));
                }, IClass.getClass(String.class), List.of(node2));

                assertEquals("Hello world from node 1 node 2 node 3", node3.evaluate().supply().get());
                assertEquals("Hello world from node 1 node 2 node 3", node3.supply().get().supply().get());

                Expression<String> exp = new Expression<>(node3);

                assertEquals("Hello world from node 1 node 2 node 3", exp.evaluate().supply().get());
                assertEquals("Hello world from node 1 node 2 node 3", exp.build().supply().get());
        }

        @SuppressWarnings("unchecked")
        @Test
        public void testContextualEvaluationWithinNonContextualExpressionNode() throws Exception {

                ResolvedMethod concatenateMethod = resolveConcatenateMethod();

                ExpressionNode<String> node1 = new ExpressionNode<String>("", params -> {

                        ContextualMethodBinder<String, ExpressionContext> mb = new ContextualMethodBinder<>(
                                        new FixedSupplier<>(
                                                        new StringConcatenator(), IClass.getClass(StringConcatenator.class)),
                                        concatenateMethod,
                                        List.of(
                                                        new FixedSupplier<String>("Hello from node 1", IClass.getClass(String.class)),
                                                        new FixedSupplier<String>("", IClass.getClass(String.class))));

                        return new MethodReturnUnwrappingContextualSupplier<>(mb, IClass.getClass(String.class));
                }, IClass.getClass(String.class));

                ExpressionNode<String> node2 = new ExpressionNode<String>("", params -> {
                        ContextualMethodBinder<String, ExpressionContext> mb = new ContextualMethodBinder<>(
                                        new FixedSupplier<>(
                                                        new StringConcatenator(), IClass.getClass(StringConcatenator.class)),
                                        concatenateMethod,
                                        List.of(
                                                        (ISupplier<String>) params[0],
                                                        new FixedSupplier<String>(" node 2", IClass.getClass(String.class))));

                        return new MethodReturnUnwrappingContextualSupplier<>(mb, IClass.getClass(String.class));
                }, IClass.getClass(String.class), List.of(node1));

                ExpressionNode<String> node3 = new ExpressionNode<String>("", params -> {
                        ContextualMethodBinder<String, ExpressionContext> mb = new ContextualMethodBinder<>(
                                        new FixedSupplier<>(
                                                        new StringConcatenator(), IClass.getClass(StringConcatenator.class)),
                                        concatenateMethod,
                                        List.of(
                                                        (ISupplier<String>) params[0],
                                                        new FixedSupplier<String>(" node 3", IClass.getClass(String.class))));

                        return new MethodReturnUnwrappingContextualSupplier<>(mb, IClass.getClass(String.class));
                }, IClass.getClass(String.class), List.of(node2));

                Expression<String> exp = new Expression<>(node3);
                assertDoesNotThrow(exp::evaluate);
                assertEquals("Hello from node 1 node 2 node 3",
                                Supplier.contextualRecursiveSupply(exp.evaluate(), new ExpressionContext(Set.of())));

        }

        @SuppressWarnings("unchecked")
        @Test
        public void testContextualEvaluationWithinContextualExpressionNode() throws Exception {

                ResolvedMethod concatenateMethod = resolveConcatenateMethod();

                IExpressionNode<String, ? extends ISupplier<String>> node1 = new ContextualExpressionNode<String>("",
                                (c,
                                                params) -> {
                                        ContextualMethodBinder<String, IExpressionContext> mb = new ContextualMethodBinder<>(
                                                        new FixedSupplier<>(
                                                                        new StringConcatenator(), IClass.getClass(StringConcatenator.class)),
                                                        concatenateMethod,
                                                        List.of(
                                                                        new FixedSupplier<String>("Hello from node 1", IClass.getClass(String.class)),
                                                                        new FixedSupplier<String>("", IClass.getClass(String.class))));

                                        return new MethodReturnUnwrappingContextualSupplier<>(mb, IClass.getClass(String.class));
                                }, IClass.getClass(String.class));

                IExpressionNode<String, ? extends ISupplier<String>> node2 = new ContextualExpressionNode<String>("",
                                (c,
                                                params) -> {
                                        ContextualMethodBinder<String, IExpressionContext> mb = new ContextualMethodBinder<>(
                                                        new FixedSupplier<>(
                                                                        new StringConcatenator(), IClass.getClass(StringConcatenator.class)),
                                                        concatenateMethod,
                                                        List.of(
                                                                        (ISupplier<String>) params[0],
                                                                        new FixedSupplier<String>(" node 2", IClass.getClass(String.class))));

                                        return new MethodReturnUnwrappingContextualSupplier<>(mb, IClass.getClass(String.class));
                                }, IClass.getClass(String.class), List.of(node1));

                IExpressionNode<String, ? extends ISupplier<String>> node3 = new ContextualExpressionNode<String>("",
                                (c,
                                                params) -> {
                                        ContextualMethodBinder<String, IExpressionContext> mb = new ContextualMethodBinder<>(
                                                        new FixedSupplier<>(
                                                                        new StringConcatenator(), IClass.getClass(StringConcatenator.class)),
                                                        concatenateMethod,
                                                        List.of(
                                                                        (ISupplier<String>) params[0],
                                                                        new FixedSupplier<String>(" node 3", IClass.getClass(String.class))));

                                        return new MethodReturnUnwrappingContextualSupplier<>(mb, IClass.getClass(String.class));
                                }, IClass.getClass(String.class), List.of(node2));

                Expression<String> exp = new Expression<>(node3);

                assertEquals("Hello from node 1 node 2 node 3",
                                Supplier.contextualRecursiveSupply(exp.build(), new ExpressionContext(Set.of())));
        }

        @SuppressWarnings("unchecked")
        @Test
        public void testMixedEvaluation() throws Exception {

                ResolvedMethod concatenateMethod = resolveConcatenateMethod();

                IExpressionNode<String, ? extends ISupplier<String>> node1 = new ContextualExpressionNode<>("",
                                (c, params) -> {
                                        ContextualMethodBinder<String, IExpressionContext> mb = new ContextualMethodBinder<>(
                                                        new FixedSupplier<>(
                                                                        new StringConcatenator(), IClass.getClass(StringConcatenator.class)),
                                                        concatenateMethod,
                                                        List.of(
                                                                        new FixedSupplier<String>("Hello from node 1", IClass.getClass(String.class)),
                                                                        new FixedSupplier<String>("", IClass.getClass(String.class))));

                                        return new MethodReturnUnwrappingContextualSupplier<>(mb, IClass.getClass(String.class));
                                }, IClass.getClass(String.class));

                IExpressionNode<String, ? extends ISupplier<String>> node2 = new ExpressionNode<>("", (params) -> {
                        MethodBinder<String> mb = new MethodBinder<>(
                                        new FixedSupplier<>(
                                                        new StringConcatenator(), IClass.getClass(StringConcatenator.class)),
                                        concatenateMethod,
                                        List.of(
                                                        (ISupplier<String>) params[0],
                                                        new FixedSupplier<String>(" node 2", IClass.getClass(String.class))));

                        return new MethodReturnUnwrappingSupplier<>(mb, IClass.getClass(String.class));
                }, IClass.getClass(String.class), List.of(node1));

                IExpressionNode<String, ? extends ISupplier<String>> node3 = new ContextualExpressionNode<>("",
                                (c, params) -> {
                                        ContextualMethodBinder<String, IExpressionContext> mb = new ContextualMethodBinder<>(
                                                        new FixedSupplier<>(
                                                                        new StringConcatenator(), IClass.getClass(StringConcatenator.class)),
                                                        concatenateMethod,
                                                        List.of(
                                                                        (ISupplier<String>) params[0],
                                                                        new FixedSupplier<String>(" node 3", IClass.getClass(String.class))));

                                        return new MethodReturnUnwrappingContextualSupplier<>(mb, IClass.getClass(String.class));
                                }, IClass.getClass(String.class), List.of(node2));

                Expression<String> exp = new Expression<>(node3);

                assertEquals("Hello from node 1 node 2 node 3",
                                Supplier.contextualRecursiveSupply(exp.build(), new ExpressionContext(Set.of())));
        }

        @SuppressWarnings("unchecked")
        @Test
        public void testContextualSupplierForMethodParameter() throws Exception {

                ResolvedMethod concatenateMethod = resolveConcatenateMethod();

                IExpressionNode<String, ? extends ISupplier<String>> node1 = new ContextualExpressionNode<>("",
                                (c, params) -> {
                                        ContextualMethodBinder<String, IExpressionContext> mb = new ContextualMethodBinder<>(
                                                        new FixedSupplier<>(
                                                                        new StringConcatenator(), IClass.getClass(StringConcatenator.class)),
                                                        concatenateMethod,
                                                        List.of(
                                                                        new FixedSupplier<String>("Hello from node 1", IClass.getClass(String.class)),
                                                                        new FixedSupplier<String>("", IClass.getClass(String.class))));

                                        return new MethodReturnUnwrappingContextualSupplier<>(mb, IClass.getClass(String.class));
                                }, IClass.getClass(String.class));

                IExpressionNode<String, ? extends ISupplier<String>> node2 = new ExpressionNode<>("", (params) -> {
                        ContextualMethodBinder<String, IExpressionContext> mb = new ContextualMethodBinder<>(
                                        new FixedSupplier<>(
                                                        new StringConcatenator(), IClass.getClass(StringConcatenator.class)),
                                        concatenateMethod,
                                        List.of(
                                                        (ISupplier<String>) params[0],
                                                        new IContextualSupplier<String, String>() {

                                                                @Override
                                                                public Type getSuppliedType() {
                                                                        return String.class;
                                                                }

                                                                @Override
                                                                public IClass<String> getOwnerContextType() {
                                                                        return IClass.getClass(String.class);
                                                                }

                                                                @Override
                                                                public IClass<String> getSuppliedClass() {
                                                                        return IClass.getClass(String.class);
                                                                }

                                                                @Override
                                                                public Optional<String> supply(String ownerContext,
                                                                                Object... otherContexts)
                                                                                throws SupplyException {
                                                                        return Optional.of(ownerContext);
                                                                }
                                                        }));

                        return new MethodReturnUnwrappingContextualSupplier<>(mb, IClass.getClass(String.class));
                }, IClass.getClass(String.class), List.of(node1));

                Expression<String> exp = new Expression<>(node2);

                assertEquals("Hello from node 1 node 2",
                                Supplier.contextualRecursiveSupply(exp.build(), new ExpressionContext(Set.of()),
                                                " node 2"));
        }

        @SuppressWarnings("unchecked")
        @Test
        @DisplayName("This test checks that an exception is thrown when leaf node is not contextual but parent nodes are. In this case, the contexts are not propagated to the parent nodes. ")
        public void testContextualSupplierForMethodOwner_error() throws Exception {

                ResolvedMethod concatenateMethod = resolveConcatenateMethod();

                IExpressionNode<String, ? extends ISupplier<String>> node1 = new ContextualExpressionNode<>("",
                                (c, params) -> {
                                        ContextualMethodBinder<String, IExpressionContext> mb = new ContextualMethodBinder<>(
                                                        new IContextualSupplier<StringConcatenator, StringConcatenator>() {

                                                                @Override
                                                                public Type getSuppliedType() {
                                                                        return StringConcatenator.class;
                                                                }

                                                                @Override
                                                                public IClass<StringConcatenator> getOwnerContextType() {
                                                                        return IClass.getClass(StringConcatenator.class);
                                                                }

                                                                @Override
                                                                public IClass<StringConcatenator> getSuppliedClass() {
                                                                        return IClass.getClass(StringConcatenator.class);
                                                                }

                                                                @Override
                                                                public Optional<StringConcatenator> supply(
                                                                                StringConcatenator ownerContext,
                                                                                Object... otherContexts)
                                                                                throws SupplyException {
                                                                        return Optional.of(ownerContext);
                                                                }
                                                        },
                                                        concatenateMethod,
                                                        List.of(
                                                                        new FixedSupplier<String>("Hello from node 1", IClass.getClass(String.class)),
                                                                        new FixedSupplier<String>("", IClass.getClass(String.class))));

                                        return new MethodReturnUnwrappingContextualSupplier<>(mb, IClass.getClass(String.class));
                                }, IClass.getClass(String.class));

                IExpressionNode<String, ? extends ISupplier<String>> node2 = new ExpressionNode<>("", (params) -> {
                        MethodBinder<String> mb = new MethodBinder<>(
                                        new FixedSupplier<>(
                                                        new StringConcatenator(), IClass.getClass(StringConcatenator.class)),
                                        concatenateMethod,
                                        List.of(
                                                        (ISupplier<String>) params[0],
                                                        new FixedSupplier<String>(" node 2", IClass.getClass(String.class))));

                        return new MethodReturnUnwrappingSupplier<>(mb, IClass.getClass(String.class));
                }, IClass.getClass(String.class), List.of(node1));

                Expression<String> exp = new Expression<>(node2);

                SupplyException exception = assertThrows(SupplyException.class,
                                () -> Supplier.contextualRecursiveSupply(exp.build(), new ExpressionContext(Set.of()),
                                                new StringConcatenator()));

                assertEquals("Error on parameter 0", exception.getMessage());
        }

        @SuppressWarnings("unchecked")
        @Test
        public void testContextualSupplierForMethodOwner_success() throws Exception {

                ResolvedMethod concatenateMethod = resolveConcatenateMethod();

                IExpressionNode<String, ? extends ISupplier<String>> node1 = new ContextualExpressionNode<>("",
                                (c, params) -> {
                                        ContextualMethodBinder<String, IExpressionContext> mb = new ContextualMethodBinder<>(
                                                        new IContextualSupplier<StringConcatenator, StringConcatenator>() {

                                                                @Override
                                                                public Type getSuppliedType() {
                                                                        return StringConcatenator.class;
                                                                }

                                                                @Override
                                                                public IClass<StringConcatenator> getOwnerContextType() {
                                                                        return IClass.getClass(StringConcatenator.class);
                                                                }

                                                                @Override
                                                                public IClass<StringConcatenator> getSuppliedClass() {
                                                                        return IClass.getClass(StringConcatenator.class);
                                                                }

                                                                @Override
                                                                public Optional<StringConcatenator> supply(
                                                                                StringConcatenator ownerContext,
                                                                                Object... otherContexts)
                                                                                throws SupplyException {
                                                                        return Optional.of(ownerContext);
                                                                }
                                                        },
                                                        concatenateMethod,
                                                        List.of(
                                                                        new FixedSupplier<String>("Hello from node 1", IClass.getClass(String.class)),
                                                                        new FixedSupplier<String>("", IClass.getClass(String.class))));

                                        return new MethodReturnUnwrappingContextualSupplier<>(mb, IClass.getClass(String.class));
                                }, IClass.getClass(String.class));

                IExpressionNode<String, ? extends ISupplier<String>> node2 = new ExpressionNode<>("", (params) -> {
                        ContextualMethodBinder<String, IExpressionContext> mb = new ContextualMethodBinder<>(
                                        new FixedSupplier<>(
                                                        new StringConcatenator(), IClass.getClass(StringConcatenator.class)),
                                        concatenateMethod,
                                        List.of(
                                                        (ISupplier<String>) params[0],
                                                        new FixedSupplier<String>(" node 2", IClass.getClass(String.class))));

                        return new MethodReturnUnwrappingContextualSupplier<>(mb, IClass.getClass(String.class));
                }, IClass.getClass(String.class), List.of(node1));

                Expression<String> exp = new Expression<>(node2);

                assertEquals("Hello from node 1 node 2",
                                Supplier.contextualRecursiveSupply(exp.build(), new ExpressionContext(Set.of()),
                                                new StringConcatenator()));
        }

}

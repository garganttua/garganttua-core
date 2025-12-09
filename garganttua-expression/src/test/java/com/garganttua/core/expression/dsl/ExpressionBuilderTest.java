package com.garganttua.core.expression.dsl;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.garganttua.core.dsl.DslException;

/**
 * Test class for ExpressionContextBuilder.
 *
 * @since 2.0.0-ALPHA01
 */
class ExpressionContextBuilderTest {

    /**
     * Helper class with static methods for testing.
     */
    public static class TestExpressions {

        public static String getString() {
            return "test string";
        }

        public static Integer getInteger() {
            return 42;
        }

        public static Double getDouble() {
            return 3.14;
        }

        public static Boolean getBoolean() {
            return true;
        }

        // Non-static method - should fail
        public String getNonStaticString() {
            return "non-static";
        }
    }

    @Test
    void testCreateExpressionContextBuilder() {
        // Test that we can create an ExpressionContextBuilder
        assertDoesNotThrow(() -> {
            ExpressionContextBuilder builder = ExpressionContextBuilder.create();
            assertNotNull(builder);
        });
    }

    @Test
    void testWithPackage() {
        // Test adding a single package
        ExpressionContextBuilder builder = ExpressionContextBuilder.create();

        assertDoesNotThrow(() -> {
            builder.withPackage("com.example.test");
        });

        String[] packages = builder.getPackages();
        assertEquals(1, packages.length);
        assertEquals("com.example.test", packages[0]);
    }

    @Test
    void testWithPackages() {
        // Test adding multiple packages
        ExpressionContextBuilder builder = ExpressionContextBuilder.create();

        String[] packagesToAdd = {
            "com.example.test1",
            "com.example.test2",
            "com.example.test3"
        };

        assertDoesNotThrow(() -> {
            builder.withPackages(packagesToAdd);
        });

        String[] packages = builder.getPackages();
        assertEquals(3, packages.length);
        assertEquals("com.example.test1", packages[0]);
        assertEquals("com.example.test2", packages[1]);
        assertEquals("com.example.test3", packages[2]);
    }

    @Test
    void testWithExpressionCreatesMethodBinderBuilder() {
        // Test that withExpression returns a method binder builder
        ExpressionContextBuilder builder = ExpressionContextBuilder.create();

        assertDoesNotThrow(() -> {
            IExpressionMethodBinderBuilder<String> methodBuilder =
                builder.withExpression(TestExpressions.class, String.class);
            assertNotNull(methodBuilder);
        });
    }

    @Test
    void testWithExpressionStaticMethod() throws DslException {
        // Test binding a static method
        ExpressionContextBuilder builder = ExpressionContextBuilder.create();

        assertDoesNotThrow(() -> {
            IExpressionMethodBinderBuilder<String> methodBuilder =
                builder.withExpression(TestExpressions.class, String.class)
                    .method("getString");
            assertNotNull(methodBuilder);
        });
    }

    @Test
    void testWithExpressionNonStaticMethodFails() throws DslException {
        // Test that binding a non-static method throws an exception
        ExpressionContextBuilder builder = ExpressionContextBuilder.create();

        assertThrows(DslException.class, () -> {
            builder.withExpression(TestExpressions.class, String.class)
                .method("getNonStaticString");
        });
    }

    @Test
    void testWithExpressionDifferentTypes() throws DslException {
        // Test binding methods with different return types
        ExpressionContextBuilder builder = ExpressionContextBuilder.create();

        // String
        assertDoesNotThrow(() -> {
            IExpressionMethodBinderBuilder<String> stringBuilder =
                builder.withExpression(TestExpressions.class, String.class)
                    .method("getString");
            assertNotNull(stringBuilder);
        });

        // Integer
        assertDoesNotThrow(() -> {
            IExpressionMethodBinderBuilder<Integer> intBuilder =
                builder.withExpression(TestExpressions.class, Integer.class)
                    .method("getInteger");
            assertNotNull(intBuilder);
        });

        // Double
        assertDoesNotThrow(() -> {
            IExpressionMethodBinderBuilder<Double> doubleBuilder =
                builder.withExpression(TestExpressions.class, Double.class)
                    .method("getDouble");
            assertNotNull(doubleBuilder);
        });

        // Boolean
        assertDoesNotThrow(() -> {
            IExpressionMethodBinderBuilder<Boolean> booleanBuilder =
                builder.withExpression(TestExpressions.class, Boolean.class)
                    .method("getBoolean");
            assertNotNull(booleanBuilder);
        });
    }

    @Test
    void testWithParamIsInoperative() throws DslException {
        // Test that withParam doesn't fail but is inoperative
        ExpressionContextBuilder builder = ExpressionContextBuilder.create();

        assertDoesNotThrow(() -> {
            IExpressionMethodBinderBuilder<String> methodBuilder =
                builder.withExpression(TestExpressions.class, String.class)
                    .method("getString")
                    .withParam("test")  // Should be ignored
                    .withParam(0, "test")  // Should be ignored
                    .withParam("paramName", "test");  // Should be ignored
            assertNotNull(methodBuilder);
        });
    }

    @Test
    void testWithReturnIsInoperative() throws DslException {
        // Test that withReturn doesn't fail but is inoperative
        ExpressionContextBuilder builder = ExpressionContextBuilder.create();

        assertDoesNotThrow(() -> {
            IExpressionMethodBinderBuilder<String> methodBuilder =
                builder.withExpression(TestExpressions.class, String.class)
                    .method("getString")
                    .withReturn(null);  // Should be ignored
            assertNotNull(methodBuilder);
        });
    }

    @Test
    void testAutoDetect() {
        // Test auto-detect functionality
        ExpressionContextBuilder builder = ExpressionContextBuilder.create();

        assertDoesNotThrow(() -> {
            builder.autoDetect(true);
        });

        assertDoesNotThrow(() -> {
            builder.autoDetect(false);
        });
    }

    @Test
    void testChainedCalls() {
        // Test method chaining
        ExpressionContextBuilder builder = ExpressionContextBuilder.create();

        assertDoesNotThrow(() -> {
            builder
                .withPackage("com.example.test1")
                .withPackage("com.example.test2")
                .autoDetect(true)
                .withExpression(TestExpressions.class, String.class)
                    .method("getString");
        });

        String[] packages = builder.getPackages();
        assertEquals(2, packages.length);
    }

    @Test
    void testNullPackageNameThrowsException() {
        // Test that null package name throws exception
        ExpressionContextBuilder builder = ExpressionContextBuilder.create();

        assertThrows(NullPointerException.class, () -> {
            builder.withPackage(null);
        });
    }

    @Test
    void testNullPackageArrayThrowsException() {
        // Test that null package array throws exception
        ExpressionContextBuilder builder = ExpressionContextBuilder.create();

        assertThrows(NullPointerException.class, () -> {
            builder.withPackages(null);
        });
    }

    @Test
    void testBuildThrowsUnsupportedOperationException() {
        // Test that build() throws UnsupportedOperationException
        ExpressionContextBuilder builder = ExpressionContextBuilder.create();

        assertThrows(UnsupportedOperationException.class, () -> {
            builder.build();
        });
    }

    @Test
    void testMultipleExpressionsOnSameBuilder() throws DslException {
        // Test adding multiple expressions to the same builder
        ExpressionContextBuilder builder = ExpressionContextBuilder.create();

        assertDoesNotThrow(() -> {
            // First expression
            builder.withExpression(TestExpressions.class, String.class)
                .method("getString");

            // Second expression
            builder.withExpression(TestExpressions.class, Integer.class)
                .method("getInteger");

            // Third expression
            builder.withExpression(TestExpressions.class, Boolean.class)
                .method("getBoolean");
        });
    }

    /**
     * Helper class with methods that have parameters - should fail since
     * expressions don't support parameters.
     */
    public static class TestExpressionsWithParams {

        public static String getStringWithParam(String param) {
            return param;
        }

        public static Integer addIntegers(Integer a, Integer b) {
            return a + b;
        }
    }

    @Test
    void testMethodResolution() throws DslException {
        // Test that we can resolve different methods by name
        ExpressionContextBuilder builder = ExpressionContextBuilder.create();

        // Should work - correct method name
        assertDoesNotThrow(() -> {
            builder.withExpression(TestExpressions.class, String.class)
                .method("getString");
        });

        // Should fail - method doesn't exist
        assertThrows(DslException.class, () -> {
            builder.withExpression(TestExpressions.class, String.class)
                .method("nonExistentMethod");
        });
    }

    @Test
    void testWithExpressionNullMethodOwnerThrowsException() {
        // Test that null method owner throws exception
        ExpressionContextBuilder builder = ExpressionContextBuilder.create();

        assertThrows(NullPointerException.class, () -> {
            builder.withExpression(null, String.class);
        });
    }

    @Test
    void testWithExpressionNullSuppliedTypeThrowsException() {
        // Test that null supplied type throws exception
        ExpressionContextBuilder builder = ExpressionContextBuilder.create();

        assertThrows(NullPointerException.class, () -> {
            builder.withExpression(TestExpressions.class, null);
        });
    }
}

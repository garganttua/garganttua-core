package com.garganttua.core.query.dsl;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.reflections.util.QueryBuilder;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.supply.ISupplier;
import com.garganttua.core.supply.dsl.FixedSupplierBuilder;

public class QueryBuilderTest {

    public static final ISupplier<String> fixed(String value) {
        return new FixedSupplierBuilder<String>(value).build();
    }

    public final ISupplier<String> nonStatic(String value) {
        return new FixedSupplierBuilder<String>(value).build();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void dummyTest() {

       /*  QueryBuilder qb = new QueryBuilder();

        qb.withPackage("com.garganttua.core.query.functions");

        Class<?>[] params = { String.class };
        IQuery query = qb.withQuery(QueryBuilderTest.class, String.class)
                .method("fixed", (Class<ISupplier<String>>) (Class<?>) ISupplier.class, params).up()
                .build(); */

/*         query.query("double[12,12,12]");
        query.query("42");
        query.query("\"hello\"");
        query.query("'c'");
        query.query("true");
        query.query("false");
        query.query("null");

        query.query("int[1,2,3]");
        query.query("double[3.14,2.71]");
        query.query("char['a','b']");
        query.query("boolean[true,false,true]");
        query.query("string[\"a\",\"b\",\"c\"]");
        query.query("Class<Integer>");

        query.query("{\"name\":\"Alice\"}");
        query.query("{\"age\":int[30]}");
        query.query("{\"coords\":float[1.0,2.0]}");
        query.query("{\"flags\":boolean[true,false]}");
        query.query("{\"nested\":{\"x\":int[1],\"y\":int[2]}}");
        query.query("{\"matrix\":int[ int[1,2], int[3,4] ]}"); // attention, si tu veux que les arrays bruts soient interdits,
                                                         // tu peux remplacer par int[...]

        query.query("sum(1,2,3)");
        query.query("concat(\"a\",\"b\")");
        query.query("max(int[1,2],int[3,4])");
        query.query("min(double[3.1,4.2],double[5.3,6.4])");
        query.query("process(int[1],float[2.0])");
        query.query("wrap(Class<String>)");
        query.query("identity(int[42])");
        query.query("filter(int[1,2,3],boolean[true,false,true])");
        query.query("compute(Class<?> ,int[10])");
        query.query("merge({\"a\":int[1]}, {\"b\":int[2]})");
        query.query("format(\"%s\", \"hello\")");
        query.query("outer(inner(int[42]))"); */
        /*
         * query.query("\"hello\"");
         * query.query("true");
         * query.query("false");
         * query.query("null");
         * query.query("[1,2,3]");
         * query.query("[\"a\",\"b\",3.14,false]");
         * query.query("{\"name\":\"Bob\"}");
         * query.query("{\"x\":1,\"y\":[2,3]}");
         * query.query("sum(1,2,3)");
         * query.query("concat(\"a\",\"b\",\"c\")");
         * query.query("int");
         * query.query("long[]");
         * query.query("float[][]");
         * query.query("char");
         * query.query("Class<String>");
         * query.query("Class<?>");
         * query.query("java.util.List<int>");
         * query.query("java.util.Map<String,int>");
         * query.query("max(10,20)");
         * query.query("echo(\"test\")");
         * query.query("myFunction([1,2],{\"a\":10})");
         * query.query("process(int[], 42)");
         * query.query("compute(Class<Long>, 123)");
         * query.query("add(sum(1,2), sum(3,4))");
         * query.query("identifier");
         * query.query("myVar");
         * query.query("deepCall(sum(1, process(2)), Class<?> )");
         * query.query("Class<java.util.List>");
         * query.query("long[][][]");
         * query.query("[{\"a\":1},{\"b\":2}]");
         * query.query("{\"nested\":{\"deep\":[1,2]}}");
         * query.query("map(\"key\", [1,2,3])");
         * query.query("Class<java.lang.String[]>");
         * query.query("combine(\"x\", int, Class<Float>)");
         * query.query("0");
         * query.query("-12");
         * query.query("3.1415");
         * query.query("\"text\"");
         * query.query("\"line\\nbreak\"");
         * query.query("[ ]");
         * query.query("[null]");
         * query.query("[true,false,true]");
         * query.query("[1,[2,[3]]]");
         * query.query("[{\"k\":1}, {\"k\":2}]");
         * query.query("{\"a\":true}");
         * query.query("{\"a\":1,\"b\":2.5}");
         * query.query("{\"arr\":[1,2,3]}");
         * query.query("{\"obj\":{\"x\":10}}");
         * query.query("boolean");
         * query.query("double");
         * query.query("short[]");
         * query.query("byte[][]");
         * query.query("char[][][]");
         * query.query("Class<int>");
         * query.query("Class<double>");
         * query.query("Class<long[]>");
         * query.query("Class<?>");
         * query.query("java.util.Set<String>");
         * query.query("java.util.Map<String,boolean>");
         * query.query("java.lang.String");
         * query.query("Long");
         * query.query("Integer[]");
         * query.query("Float[][]");
         * query.query("process()");
         * query.query("process(1)");
         * query.query("process(1,2)");
         * query.query("process(\"a\",true)");
         * query.query("mix(1,[2,3],{\"x\":1})");
         * query.query("wrap(int)");
         * query.query("wrap(long[])");
         * query.query("wrap(Class<String>)");
         * query.query("parse(\"x\", int, 42)");
         * query.query("f( g(1) )");
         * query.query("f( g( h(2) ) )");
         * query.query("combine( add(1,2), multiply(3,4) )");
         * query.query("sum([1,2,3])");
         * query.query("sum({\"a\":1,\"b\":2})");
         * query.query("resolve(Class<Integer>)");
         * query.query("resolve(Class<?> )");
         * query.query("boolOp(true,false)");
         * query.query("negate(false)");
         * query.query("cast(10,int)");
         * query.query("cast(3.14,double)");
         * query.query("convert(\"123\", int)");
         * query.query("java.util.List<int[]>");
         * query.query("java.util.List<Class<String>>");
         * query.query("Class<java.util.List>");
         * query.query("Class<java.util.Map>");
         * query.query("long");
         * query.query("float");
         * query.query("boolean[][]");
         * query.query("Class<char>");
         * query.query("Class<char[]>");
         * query.query("Class<float[][]>");
         * query.query("myIdentifier");
         * query.query("_hiddenVar");
         * query.query("__x123");
         * query.query("YELLING");
         * query.query("CamelCase");
         * query.query("call( value )");
         * query.query("call( arr[], obj{} )");
         * query.query("outer(inner(42))");
         * query.query("outer(inner(inner(true)))");
         * query.query("format(\"%s\", \"hello\")");
         * query.query("merge([1,2],[3,4])");
         * query.query("merge({\"a\":1},{\"b\":2})");
         * query.query("replace(\"abc\",\"b\",\"x\")");
         * query.query("rand()");
         * query.query("rand(int)");
         * query.query("min(1,2,3,4)");
         * query.query("max(1,2,3,4)");
         * query.query("filter([1,2,3], Class<Integer>)");
         * query.query("filter([], Class<?> )");
         * query.query("identity(x)");
         * query.query("identity(int)");
         * query.query("identity(Class<String>)");
         * query.query("wrapObject({\"k\":\"v\"})");
         * query.query("wrapArray([1, true, \"x\"])");
         * query.query("callNested({\"a\":[1,{\"b\":2}]} )");
         * query.query("math.add(10,20)");
         * query.query("math.pow(2,8)");
         * query.query("java.lang.Integer");
         * query.query("java.lang.String[]");
         * query.query("java.lang.Double[][]");
         * query.query("Class<java.lang.String>");
         * query.query("Class<java.lang.String[]>");
         * query.query("Class<java.lang.Long>");
         * query.query("newType(int)");
         * query.query("newType(java.util.List<double>)");
         * query.query("superNested([[1,2],[3,[4,5]]] )");
         */

    }

    /* @SuppressWarnings("unchecked")
    @Test
    public void testSimpleQuery() {

        QueryBuilder qb = new QueryBuilder();

        qb.withPackage("com.garganttua.core.query.functions");

        Class<?>[] paramsFixed = { String.class };
        Class<?>[] paramsString = { String.class };
        IQuery query = qb.withQuery(QueryBuilderTest.class, String.class)
                .method("fixed", (Class<ISupplier<String>>) (Class<?>) ISupplier.class, paramsFixed).up()
                .withQuery(com.garganttua.core.query.functions.TypeConverters.class, String.class)
                .method("string", (Class<ISupplier<String>>) (Class<?>) ISupplier.class, paramsString).up()
                .build();

        Optional<String> supplied = (Optional<String>) query.query("fixed(string(\"Hello world\"))").get().supply();

        assertNotNull(supplied);
        assertTrue(supplied.isPresent());
        assertEquals("Hello world", supplied.get());

    }

    @SuppressWarnings("unchecked")
    @Test
    public void testNonStaticQueryShouldThrowException() {

        QueryBuilder qb = new QueryBuilder();

        qb.withPackage("com.garganttua.core.query.functions");

        Class<?>[] params = { String.class };
        DslException exception = assertThrows(DslException.class, () -> {
            qb.withQuery(QueryBuilderTest.class, String.class)
                    .method("nonStatic", (Class<ISupplier<String>>) (Class<?>) ISupplier.class, params).up()
                    .build();
        });

        assertEquals("Method nonStatic is not static in class com.garganttua.core.query.dsl.QueryBuilderTest",
                exception.getMessage());
    } */

}

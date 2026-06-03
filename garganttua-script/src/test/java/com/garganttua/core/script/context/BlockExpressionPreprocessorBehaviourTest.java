package com.garganttua.core.script.context;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * Behaviour tests for the package-private {@link BlockExpressionPreprocessor}: the
 * pure string-rewriting logic that lifts {@code (\n ... )} function-argument blocks
 * out of the source and replaces them with {@code @__blkN} references, while leaving
 * statement groups, strings and comments untouched.
 */
class BlockExpressionPreprocessorBehaviourTest {

    private static String pp(String source) {
        return new BlockExpressionPreprocessor().preprocess(source);
    }

    @Test
    void plainSourceWithoutBlocksIsUnchanged() {
        String src = "a <- \"hello\"\n\"done\" -> 0\n";
        BlockExpressionPreprocessor p = new BlockExpressionPreprocessor();
        assertEquals(src, p.preprocess(src));
        assertTrue(p.getBlockSources().isEmpty(), "no block should be extracted");
    }

    @Test
    void ifBlockArgumentIsExtractedToBlkReference() {
        // "(" preceded by "," and followed by newline => function-argument block.
        String src = "if(true, (\n    x <- 1\n))\n";
        BlockExpressionPreprocessor p = new BlockExpressionPreprocessor();
        String out = p.preprocess(src);
        // The block content is replaced by a single @__blkN reference.
        assertTrue(out.contains("@__blk"), "expected a block reference, got: " + out);
        assertFalse(out.contains("x <- 1"), "block body must be lifted out of the inline source");
        assertEquals(1, p.getBlockSources().size());
        String body = p.getBlockSources().values().iterator().next();
        // Leading newline + trailing whitespace are stripped; inner indentation is kept.
        assertEquals("    x <- 1", body, "extracted body keeps inner indentation");
    }

    @Test
    void statementGroupAfterNewlineIsNotTreatedAsBlock() {
        // "(" is at the start of a line (preceded by newline, not by , ( or >)
        // => it is a statement group, NOT a function-argument block, so left intact.
        String src = "(\n    x <- 1\n)\n";
        BlockExpressionPreprocessor p = new BlockExpressionPreprocessor();
        String out = p.preprocess(src);
        assertEquals(src, out);
        assertTrue(p.getBlockSources().isEmpty());
    }

    @Test
    void parenFollowedBySameLineContentIsNotABlock() {
        // "(" not followed by newline => ordinary call, not a block.
        String src = "if(true, (x))\n";
        BlockExpressionPreprocessor p = new BlockExpressionPreprocessor();
        assertEquals(src, p.preprocess(src));
        assertTrue(p.getBlockSources().isEmpty());
    }

    @Test
    void parenAfterArrowOperatorIsABlock() {
        // ">" (as in "=>") preceding the "(" also marks a block context.
        String src = "f = (a) =>(\n    a\n)\n";
        BlockExpressionPreprocessor p = new BlockExpressionPreprocessor();
        String out = p.preprocess(src);
        assertTrue(out.contains("@__blk"), out);
        assertEquals(1, p.getBlockSources().size());
        assertEquals("    a", p.getBlockSources().values().iterator().next());
    }

    @Test
    void parensInsideStringLiteralAreIgnored() {
        String src = "x <- \"value (\nnot a block)\"\n";
        BlockExpressionPreprocessor p = new BlockExpressionPreprocessor();
        // The "(" lives inside a string literal so it must be skipped entirely.
        assertEquals(src, p.preprocess(src));
        assertTrue(p.getBlockSources().isEmpty());
    }

    @Test
    void parenInsideLineCommentIsIgnored() {
        String src = "// here is a (\nfake block\nx <- 1\n";
        BlockExpressionPreprocessor p = new BlockExpressionPreprocessor();
        assertEquals(src, p.preprocess(src));
        assertTrue(p.getBlockSources().isEmpty());
    }

    @Test
    void parenInsideBlockCommentIsIgnored() {
        String src = "/* a (\n still comment */\nx <- 1\n";
        BlockExpressionPreprocessor p = new BlockExpressionPreprocessor();
        assertEquals(src, p.preprocess(src));
        assertTrue(p.getBlockSources().isEmpty());
    }

    @Test
    void nestedBlocksAreFlattenedIntoSeparateEntries() {
        // Outer if-block contains an inner if-block; both must be extracted.
        String src = "if(true, (\n    if(false, (\n        y <- 2\n    ))\n))\n";
        BlockExpressionPreprocessor p = new BlockExpressionPreprocessor();
        String out = p.preprocess(src);
        assertTrue(out.contains("@__blk"), out);
        // Two blocks total: inner + outer.
        assertEquals(2, p.getBlockSources().size(), p.getBlockSources().toString());
        // One of the extracted bodies is the innermost statement (indentation kept).
        assertTrue(p.getBlockSources().values().stream().anyMatch(b -> b.trim().equals("y <- 2")),
                "inner block body must be present: " + p.getBlockSources());
        // The outer body references the inner block (the inner body was lifted out).
        assertTrue(p.getBlockSources().values().stream().anyMatch(b -> b.contains("@__blk")),
                "outer block must reference the inner block: " + p.getBlockSources());
    }

    @Test
    void unbalancedParenLeavesSourceUntouched() {
        // No matching ")" found => findMatchingParen returns -1 => the "(" is emitted verbatim.
        String src = "if(true, (\n    x <- 1\n";
        BlockExpressionPreprocessor p = new BlockExpressionPreprocessor();
        String out = p.preprocess(src);
        assertEquals(src, out);
        assertTrue(p.getBlockSources().isEmpty());
    }

    @Test
    void blockCounterMakesNamesUniqueAcrossInstances() {
        // Two separate preprocessing passes must not collide on block names.
        BlockExpressionPreprocessor p1 = new BlockExpressionPreprocessor();
        p1.preprocess("if(true, (\n    a\n))\n");
        BlockExpressionPreprocessor p2 = new BlockExpressionPreprocessor();
        p2.preprocess("if(true, (\n    b\n))\n");
        Map<String, String> m1 = p1.getBlockSources();
        Map<String, String> m2 = p2.getBlockSources();
        assertEquals(1, m1.size());
        assertEquals(1, m2.size());
        String n1 = m1.keySet().iterator().next();
        String n2 = m2.keySet().iterator().next();
        assertFalse(n1.equals(n2), "block names from distinct runs must be distinct: " + n1 + " vs " + n2);
    }

    @Test
    void escapedQuoteInsideStringDoesNotEndItEarly() {
        // The escaped \" must not terminate the string scan; the "(" stays inside the literal.
        String src = "x <- \"a\\\" (\n still string\"\n";
        BlockExpressionPreprocessor p = new BlockExpressionPreprocessor();
        assertEquals(src, p.preprocess(src));
        assertTrue(p.getBlockSources().isEmpty());
    }
}

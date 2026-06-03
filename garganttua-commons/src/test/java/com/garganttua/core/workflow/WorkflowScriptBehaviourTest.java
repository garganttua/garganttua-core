package com.garganttua.core.workflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.garganttua.core.workflow.WorkflowScript.ScriptSource;
import com.garganttua.core.workflow.WorkflowScript.ScriptSourceType;
import com.garganttua.core.workflow.chaining.CodeAction;

class WorkflowScriptBehaviourTest {

    @Test
    void buildRequiresSource() {
        assertThrows(NullPointerException.class, () -> WorkflowScript.builder().name("n").build());
    }

    @Test
    void emptyMapsWhenUnset() {
        WorkflowScript s = WorkflowScript.builder().source(ScriptSource.of("x")).build();
        assertTrue(s.getInputs().isEmpty());
        assertTrue(s.getOutputs().isEmpty());
        assertTrue(s.getCodeActions().isEmpty());
        assertNull(s.getDescription());
        assertFalse(s.isInline());
    }

    @Test
    void mapsAreDefensivelyCopiedAndUnmodifiable() {
        java.util.Map<String, String> ins = new java.util.HashMap<>();
        ins.put("a", "@0");
        WorkflowScript s = WorkflowScript.builder()
                .source(ScriptSource.of("x"))
                .inputs(ins)
                .build();
        ins.put("b", "@1"); // mutate after build
        assertEquals(1, s.getInputs().size());
        assertThrows(UnsupportedOperationException.class, () -> s.getInputs().put("c", "y"));
    }

    @Test
    void scriptSourceOfStringVsClasspath() {
        assertEquals(ScriptSourceType.STRING, ScriptSource.of("plain").type());
        ScriptSource cp = ScriptSource.of("classpath:foo.gs");
        assertEquals(ScriptSourceType.CLASSPATH, cp.type());
        assertEquals("classpath:foo.gs", cp.value());
    }

    @Test
    void isFileTrueForFilePathClasspath() {
        assertTrue(WorkflowScript.builder().source(ScriptSource.of(new File("a.gs"))).build().isFile());
        assertTrue(WorkflowScript.builder().source(ScriptSource.of(Path.of("a.gs"))).build().isFile());
        assertTrue(WorkflowScript.builder().source(ScriptSource.of("classpath:a.gs")).build().isFile());
        assertFalse(WorkflowScript.builder().source(ScriptSource.of("inline")).build().isFile());
    }

    @Test
    void getPathResolvesAbsoluteForFileAndPathButNullForString() {
        File f = new File("rel.gs");
        WorkflowScript fileScript = WorkflowScript.builder().source(ScriptSource.of(f)).build();
        assertEquals(f.getAbsolutePath(), fileScript.getPath());

        WorkflowScript classpathScript = WorkflowScript.builder().source(ScriptSource.of("classpath:x.gs")).build();
        assertEquals("classpath:x.gs", classpathScript.getPath());

        WorkflowScript inline = WorkflowScript.builder().source(ScriptSource.of("inline")).build();
        assertNull(inline.getPath());
    }

    @Test
    void loadContentStringReturnsLiteral() throws Exception {
        WorkflowScript s = WorkflowScript.builder().source(ScriptSource.of("hello world")).build();
        assertEquals("hello world", s.loadContent());
    }

    @Test
    void loadContentReader() throws Exception {
        WorkflowScript s = WorkflowScript.builder().source(ScriptSource.of(new StringReader("from reader"))).build();
        assertEquals("from reader", s.loadContent());
    }

    @Test
    void loadContentInputStream() throws Exception {
        WorkflowScript s = WorkflowScript.builder()
                .source(ScriptSource.of(new ByteArrayInputStream("from stream".getBytes(StandardCharsets.UTF_8))))
                .build();
        assertEquals("from stream", s.loadContent());
    }

    @Test
    void loadContentFileReadsBytes() throws Exception {
        Path tmp = Files.createTempFile("wfscript", ".gs");
        Files.writeString(tmp, "file body");
        try {
            WorkflowScript s = WorkflowScript.builder().source(ScriptSource.of(tmp.toFile())).build();
            assertEquals("file body", s.loadContent());
            WorkflowScript ps = WorkflowScript.builder().source(ScriptSource.of(tmp)).build();
            assertEquals("file body", ps.loadContent());
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    @Test
    void loadContentMissingClasspathThrowsWorkflowException() {
        WorkflowScript s = WorkflowScript.builder()
                .source(ScriptSource.of("classpath:does/not/exist.gs"))
                .build();
        assertThrows(WorkflowException.class, s::loadContent);
    }

    @Test
    void codeActionsAndExpressionsArePreserved() {
        WorkflowScript s = WorkflowScript.builder()
                .source(ScriptSource.of("x"))
                .condition("when()")
                .catchExpression("handle()")
                .catchDownstreamExpression("down()")
                .codeActions(Map.of(1, CodeAction.ABORT))
                .build();
        assertEquals("when()", s.getCondition());
        assertEquals("handle()", s.getCatchExpression());
        assertEquals("down()", s.getCatchDownstreamExpression());
        assertEquals(CodeAction.ABORT, s.getCodeActions().get(1));
    }
}

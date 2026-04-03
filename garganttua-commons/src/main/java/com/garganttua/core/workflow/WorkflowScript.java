package com.garganttua.core.workflow;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;

import com.garganttua.core.workflow.chaining.CodeAction;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class WorkflowScript {

    private final String name;
    private final String description;
    private final ScriptSource source;
    private final boolean inline;
    private final String condition;
    private final String catchExpression;
    private final String catchDownstreamExpression;

    @Builder.Default
    private final Map<String, String> inputs = Collections.emptyMap();

    @Builder.Default
    private final Map<String, String> outputs = Collections.emptyMap();

    @Builder.Default
    private final Map<Integer, CodeAction> codeActions = Collections.emptyMap();

    public boolean isFile() {
        return source.type() == ScriptSourceType.FILE
                || source.type() == ScriptSourceType.PATH
                || source.type() == ScriptSourceType.CLASSPATH;
    }

    public String getPath() {
        return switch (source.type()) {
            case FILE -> ((File) source.value()).getAbsolutePath();
            case PATH -> ((Path) source.value()).toAbsolutePath().toString();
            case CLASSPATH -> (String) source.value();
            default -> null;
        };
    }

    public String loadContent() throws WorkflowException {
        try {
            return switch (source.type()) {
                case STRING -> (String) source.value();
                case FILE -> Files.readString(((File) source.value()).toPath(), StandardCharsets.UTF_8);
                case PATH -> Files.readString((Path) source.value(), StandardCharsets.UTF_8);
                case CLASSPATH -> {
                    String path = (String) source.value();
                    String resource = path.startsWith("classpath:") ? path.substring("classpath:".length()) : path;
                    InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(resource);
                    if (is == null) {
                        throw new IOException("Classpath resource not found: " + path);
                    }
                    try (is) {
                        yield new String(is.readAllBytes(), StandardCharsets.UTF_8);
                    }
                }
                case INPUT_STREAM -> new String(((InputStream) source.value()).readAllBytes(), StandardCharsets.UTF_8);
                case READER -> {
                    Reader reader = (Reader) source.value();
                    StringBuilder sb = new StringBuilder();
                    char[] buffer = new char[8192];
                    int read;
                    while ((read = reader.read(buffer)) != -1) {
                        sb.append(buffer, 0, read);
                    }
                    yield sb.toString();
                }
            };
        } catch (IOException e) {
            throw new WorkflowException("Failed to load script content", e);
        }
    }

    public enum ScriptSourceType {
        STRING,
        FILE,
        PATH,
        CLASSPATH,
        INPUT_STREAM,
        READER
    }

    public record ScriptSource(ScriptSourceType type, Object value) {
        public static ScriptSource of(String content) {
            if (content != null && content.startsWith("classpath:")) {
                return new ScriptSource(ScriptSourceType.CLASSPATH, content);
            }
            return new ScriptSource(ScriptSourceType.STRING, content);
        }

        public static ScriptSource of(File file) {
            return new ScriptSource(ScriptSourceType.FILE, file);
        }

        public static ScriptSource of(Path path) {
            return new ScriptSource(ScriptSourceType.PATH, path);
        }

        public static ScriptSource of(InputStream inputStream) {
            return new ScriptSource(ScriptSourceType.INPUT_STREAM, inputStream);
        }

        public static ScriptSource of(Reader reader) {
            return new ScriptSource(ScriptSourceType.READER, reader);
        }
    }
}

package com.garganttua.core.workflow;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import com.garganttua.core.workflow.chaining.CodeAction;

public class WorkflowScript {

    private final String name;
    private final String description;
    private final ScriptSource source;
    private final boolean inline;
    private final String condition;
    private final String catchExpression;
    private final String catchDownstreamExpression;
    private final Map<String, String> inputs;
    private final Map<String, String> outputs;
    private final Map<Integer, CodeAction> codeActions;

    private WorkflowScript(Builder b) {
        this.name = b.name;
        this.description = b.description;
        this.source = b.source;
        this.inline = b.inline;
        this.condition = b.condition;
        this.catchExpression = b.catchExpression;
        this.catchDownstreamExpression = b.catchDownstreamExpression;
        this.inputs = b.inputs == null
                ? Collections.emptyMap()
                : Collections.unmodifiableMap(new LinkedHashMap<>(b.inputs));
        this.outputs = b.outputs == null
                ? Collections.emptyMap()
                : Collections.unmodifiableMap(new LinkedHashMap<>(b.outputs));
        this.codeActions = b.codeActions == null
                ? Collections.emptyMap()
                : Collections.unmodifiableMap(new LinkedHashMap<>(b.codeActions));
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getName() {
        return this.name;
    }

    public String getDescription() {
        return this.description;
    }

    public ScriptSource getSource() {
        return this.source;
    }

    public boolean isInline() {
        return this.inline;
    }

    public String getCondition() {
        return this.condition;
    }

    public String getCatchExpression() {
        return this.catchExpression;
    }

    public String getCatchDownstreamExpression() {
        return this.catchDownstreamExpression;
    }

    public Map<String, String> getInputs() {
        return this.inputs;
    }

    public Map<String, String> getOutputs() {
        return this.outputs;
    }

    public Map<Integer, CodeAction> getCodeActions() {
        return this.codeActions;
    }

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

    /**
     * Fluent builder for {@link WorkflowScript}. Hand-written replacement for
     * the former Lombok-generated {@code @Builder} — same public surface so
     * existing call sites compile unchanged.
     */
    public static final class Builder {
        private String name;
        private String description;
        private ScriptSource source;
        private boolean inline;
        private String condition;
        private String catchExpression;
        private String catchDownstreamExpression;
        private Map<String, String> inputs;
        private Map<String, String> outputs;
        private Map<Integer, CodeAction> codeActions;

        private Builder() {
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder source(ScriptSource source) {
            this.source = source;
            return this;
        }

        public Builder inline(boolean inline) {
            this.inline = inline;
            return this;
        }

        public Builder condition(String condition) {
            this.condition = condition;
            return this;
        }

        public Builder catchExpression(String catchExpression) {
            this.catchExpression = catchExpression;
            return this;
        }

        public Builder catchDownstreamExpression(String catchDownstreamExpression) {
            this.catchDownstreamExpression = catchDownstreamExpression;
            return this;
        }

        public Builder inputs(Map<String, String> inputs) {
            this.inputs = inputs == null ? null : new LinkedHashMap<>(inputs);
            return this;
        }

        public Builder outputs(Map<String, String> outputs) {
            this.outputs = outputs == null ? null : new LinkedHashMap<>(outputs);
            return this;
        }

        public Builder codeActions(Map<Integer, CodeAction> codeActions) {
            this.codeActions = codeActions == null ? null : new LinkedHashMap<>(codeActions);
            return this;
        }

        public WorkflowScript build() {
            Objects.requireNonNull(this.source, "source must be set before build()");
            return new WorkflowScript(this);
        }
    }
}

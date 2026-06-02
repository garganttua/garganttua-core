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

/**
 * Immutable definition of a single script within a workflow stage.
 *
 * <p>Holds the script {@linkplain ScriptSource source} (inline string, file,
 * classpath resource, stream or reader), its input/output variable mappings,
 * conditional and catch expressions, and the {@link CodeAction} routing per
 * exit code. Instances are created through the fluent {@link Builder}.
 */
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

    /**
     * @return a new fluent {@link Builder}
     */
    public static Builder builder() {
        return new Builder();
    }

    /** @return the script's logical name */
    public String getName() {
        return this.name;
    }

    /** @return the human-readable description, may be {@code null} */
    public String getDescription() {
        return this.description;
    }

    /** @return the script source descriptor */
    public ScriptSource getSource() {
        return this.source;
    }

    /** @return {@code true} if the script is inlined into the generated workflow script */
    public boolean isInline() {
        return this.inline;
    }

    /** @return the {@code when} condition expression, or {@code null} if unconditional */
    public String getCondition() {
        return this.condition;
    }

    /** @return the immediate catch ({@code !}) handler expression, or {@code null} */
    public String getCatchExpression() {
        return this.catchExpression;
    }

    /** @return the downstream catch ({@code *}) handler expression, or {@code null} */
    public String getCatchDownstreamExpression() {
        return this.catchDownstreamExpression;
    }

    /** @return unmodifiable map of input mappings ({@code scriptVar -> expression}) */
    public Map<String, String> getInputs() {
        return this.inputs;
    }

    /** @return unmodifiable map of output mappings ({@code workflowVar -> scriptVar}) */
    public Map<String, String> getOutputs() {
        return this.outputs;
    }

    /** @return unmodifiable map of exit-code routing actions ({@code code -> action}) */
    public Map<Integer, CodeAction> getCodeActions() {
        return this.codeActions;
    }

    /**
     * @return {@code true} if the source is file-based (file, path or classpath resource)
     */
    public boolean isFile() {
        return source.type() == ScriptSourceType.FILE
                || source.type() == ScriptSourceType.PATH
                || source.type() == ScriptSourceType.CLASSPATH;
    }

    /**
     * @return the absolute path / classpath location of a file-based source,
     *         or {@code null} for non-file sources
     */
    public String getPath() {
        return switch (source.type()) {
            case FILE -> ((File) source.value()).getAbsolutePath();
            case PATH -> ((Path) source.value()).toAbsolutePath().toString();
            case CLASSPATH -> (String) source.value();
            default -> null;
        };
    }

    /**
     * Reads and returns the script's textual content, resolving the source
     * (file, classpath resource, stream or reader) as needed.
     *
     * @return the script source text
     * @throws WorkflowException if the underlying source cannot be read
     */
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

    /**
     * Kind of backing store a {@link ScriptSource} wraps.
     */
    public enum ScriptSourceType {
        STRING,
        FILE,
        PATH,
        CLASSPATH,
        INPUT_STREAM,
        READER
    }

    /**
     * Tagged union describing where a script's content comes from.
     *
     * @param type  the source kind
     * @param value the backing value (a {@code String}, {@code File}, {@code Path},
     *              {@code InputStream} or {@code Reader} depending on {@code type})
     */
    public record ScriptSource(ScriptSourceType type, Object value) {
        /**
         * @param content inline script text, or a {@code classpath:} reference
         * @return a {@code CLASSPATH} source if {@code content} starts with
         *         {@code classpath:}, otherwise a {@code STRING} source
         */
        public static ScriptSource of(String content) {
            if (content != null && content.startsWith("classpath:")) {
                return new ScriptSource(ScriptSourceType.CLASSPATH, content);
            }
            return new ScriptSource(ScriptSourceType.STRING, content);
        }

        /** @return a {@code FILE} source backed by {@code file} */
        public static ScriptSource of(File file) {
            return new ScriptSource(ScriptSourceType.FILE, file);
        }

        /** @return a {@code PATH} source backed by {@code path} */
        public static ScriptSource of(Path path) {
            return new ScriptSource(ScriptSourceType.PATH, path);
        }

        /** @return an {@code INPUT_STREAM} source backed by {@code inputStream} */
        public static ScriptSource of(InputStream inputStream) {
            return new ScriptSource(ScriptSourceType.INPUT_STREAM, inputStream);
        }

        /** @return a {@code READER} source backed by {@code reader} */
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

        /** @return this builder, with the script name set */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /** @return this builder, with the description set */
        public Builder description(String description) {
            this.description = description;
            return this;
        }

        /** @return this builder, with the {@link ScriptSource} set (required) */
        public Builder source(ScriptSource source) {
            this.source = source;
            return this;
        }

        /** @return this builder, with the inline flag set */
        public Builder inline(boolean inline) {
            this.inline = inline;
            return this;
        }

        /** @return this builder, with the {@code when} condition expression set */
        public Builder condition(String condition) {
            this.condition = condition;
            return this;
        }

        /** @return this builder, with the immediate catch ({@code !}) expression set */
        public Builder catchExpression(String catchExpression) {
            this.catchExpression = catchExpression;
            return this;
        }

        /** @return this builder, with the downstream catch ({@code *}) expression set */
        public Builder catchDownstreamExpression(String catchDownstreamExpression) {
            this.catchDownstreamExpression = catchDownstreamExpression;
            return this;
        }

        /**
         * Sets the input mappings; the map is defensively copied.
         *
         * @return this builder
         */
        public Builder inputs(Map<String, String> inputs) {
            this.inputs = inputs == null ? null : new LinkedHashMap<>(inputs);
            return this;
        }

        /**
         * Sets the output mappings; the map is defensively copied.
         *
         * @return this builder
         */
        public Builder outputs(Map<String, String> outputs) {
            this.outputs = outputs == null ? null : new LinkedHashMap<>(outputs);
            return this;
        }

        /**
         * Sets the exit-code routing actions; the map is defensively copied.
         *
         * @return this builder
         */
        public Builder codeActions(Map<Integer, CodeAction> codeActions) {
            this.codeActions = codeActions == null ? null : new LinkedHashMap<>(codeActions);
            return this;
        }

        /**
         * @return the built {@link WorkflowScript}
         * @throws NullPointerException if no {@link ScriptSource} was set
         */
        public WorkflowScript build() {
            Objects.requireNonNull(this.source, "source must be set before build()");
            return new WorkflowScript(this);
        }
    }
}

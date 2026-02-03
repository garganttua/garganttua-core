package com.garganttua.core.bootstrap.banner;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

/**
 * A banner loaded from a file or classpath resource.
 *
 * <p>
 * This banner implementation loads its content from an external file or
 * classpath resource, allowing users to customize the startup banner.
 * The banner file can contain placeholders that are replaced at runtime:
 * </p>
 * <ul>
 *   <li>{@code ${version}} - replaced with the application version</li>
 *   <li>{@code ${name}} - replaced with the application name</li>
 * </ul>
 *
 * <h2>Banner File Example</h2>
 * <pre>
 *   __  __         _
 *  |  \/  |_   _  / \   _ __  _ __
 *  | |\/| | | | |/ _ \ | '_ \| '_ \
 *  | |  | | |_| / ___ \| |_) | |_) |
 *  |_|  |_|\__, /_/   \_\ .__/| .__/
 *          |___/        |_|   |_|
 *
 *  :: My Application :: (${version})
 * </pre>
 *
 * @since 2.0.0-ALPHA01
 */
@Slf4j
public class FileBanner implements IBanner {

    /**
     * Default banner file name to look for in classpath.
     */
    public static final String DEFAULT_BANNER_LOCATION = "banner.txt";

    private final List<String> lines;
    private final String version;
    private final String name;

    /**
     * Creates a FileBanner from the default classpath location (banner.txt).
     *
     * @return a new FileBanner, or null if the default banner is not found
     */
    public static FileBanner fromClasspath() {
        return fromClasspath(DEFAULT_BANNER_LOCATION, null, null);
    }

    /**
     * Creates a FileBanner from the specified classpath resource.
     *
     * @param resource the classpath resource path
     * @param version the version to use for placeholder replacement
     * @param name the application name to use for placeholder replacement
     * @return a new FileBanner, or null if the resource is not found
     */
    public static FileBanner fromClasspath(String resource, String version, String name) {
        try (InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(resource)) {
            if (is == null) {
                log.atDebug().log("Banner resource not found: {}", resource);
                return null;
            }
            List<String> lines = readLines(is);
            return new FileBanner(lines, version, name);
        } catch (IOException e) {
            log.atWarn().log("Failed to read banner from classpath: {}", resource, e);
            return null;
        }
    }

    /**
     * Creates a FileBanner from the specified file.
     *
     * @param file the banner file
     * @param version the version to use for placeholder replacement
     * @param name the application name to use for placeholder replacement
     * @return a new FileBanner
     * @throws IOException if the file cannot be read
     */
    public static FileBanner fromFile(File file, String version, String name) throws IOException {
        if (file == null || !file.exists()) {
            throw new IOException("Banner file does not exist: " + file);
        }
        List<String> lines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
        return new FileBanner(lines, version, name);
    }

    /**
     * Creates a FileBanner from the specified path.
     *
     * @param path the banner file path
     * @param version the version to use for placeholder replacement
     * @param name the application name to use for placeholder replacement
     * @return a new FileBanner
     * @throws IOException if the file cannot be read
     */
    public static FileBanner fromPath(Path path, String version, String name) throws IOException {
        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        return new FileBanner(lines, version, name);
    }

    /**
     * Creates a FileBanner from the specified string content.
     *
     * @param content the banner content
     * @param version the version to use for placeholder replacement
     * @param name the application name to use for placeholder replacement
     * @return a new FileBanner
     */
    public static FileBanner fromString(String content, String version, String name) {
        List<String> lines = List.of(content.split("\n"));
        return new FileBanner(lines, version, name);
    }

    private FileBanner(List<String> lines, String version, String name) {
        this.lines = new ArrayList<>(lines);
        this.version = version != null ? version : "UNKNOWN";
        this.name = name != null ? name : "Garganttua";
    }

    @Override
    public void print(PrintStream out) {
        for (String line : lines) {
            String processed = replacePlaceholders(line);
            out.println(processed);
        }
        out.println();
    }

    /**
     * Replaces placeholders in the line with actual values.
     */
    private String replacePlaceholders(String line) {
        return line
                .replace("${version}", version)
                .replace("${name}", name)
                .replace("${application.version}", version)
                .replace("${application.name}", name);
    }

    /**
     * Reads lines from an input stream.
     */
    private static List<String> readLines(InputStream is) throws IOException {
        List<String> lines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
        }
        return lines;
    }

    /**
     * Returns the banner content as a string.
     *
     * @return the banner text with placeholders replaced
     */
    public String getBannerText() {
        StringBuilder sb = new StringBuilder();
        for (String line : lines) {
            sb.append(replacePlaceholders(line)).append("\n");
        }
        return sb.toString();
    }
}

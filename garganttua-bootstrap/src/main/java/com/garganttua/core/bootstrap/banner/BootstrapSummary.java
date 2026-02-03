package com.garganttua.core.bootstrap.banner;

import java.io.PrintStream;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Displays a summary of the bootstrap process with colored output.
 *
 * <p>
 * This class provides a formatted summary of the bootstrap results including:
 * </p>
 * <ul>
 *   <li>Startup time</li>
 *   <li>Number of builders processed</li>
 *   <li>Number of objects built</li>
 *   <li>Custom information from individual builders</li>
 * </ul>
 *
 * @since 2.0.0-ALPHA01
 */
public class BootstrapSummary {

    // ANSI color codes
    private static final String RESET = "\u001B[0m";
    private static final String BOLD = "\u001B[1m";
    private static final String GREEN = "\u001B[32m";
    private static final String CYAN = "\u001B[36m";
    private static final String YELLOW = "\u001B[33m";
    private static final String WHITE = "\u001B[37m";
    private static final String BLUE = "\u001B[34m";
    private static final String MAGENTA = "\u001B[35m";
    private static final String DIM = "\u001B[2m";

    private final boolean useColors;
    private final List<SummaryItem> items = new ArrayList<>();
    private Duration startupTime;
    private int buildersCount;
    private int builtObjectsCount;
    private String applicationName;
    private String applicationVersion;

    /**
     * Creates a new BootstrapSummary.
     *
     * @param useColors whether to use ANSI colors in output
     */
    public BootstrapSummary(boolean useColors) {
        this.useColors = useColors;
    }

    /**
     * Sets the startup time.
     */
    public BootstrapSummary startupTime(Duration duration) {
        this.startupTime = duration;
        return this;
    }

    /**
     * Sets the number of builders processed.
     */
    public BootstrapSummary buildersCount(int count) {
        this.buildersCount = count;
        return this;
    }

    /**
     * Sets the number of built objects.
     */
    public BootstrapSummary builtObjectsCount(int count) {
        this.builtObjectsCount = count;
        return this;
    }

    /**
     * Sets the application name.
     */
    public BootstrapSummary applicationName(String name) {
        this.applicationName = name;
        return this;
    }

    /**
     * Sets the application version.
     */
    public BootstrapSummary applicationVersion(String version) {
        this.applicationVersion = version;
        return this;
    }

    /**
     * Adds a summary item.
     */
    public BootstrapSummary addItem(String category, String name, String value) {
        items.add(new SummaryItem(category, name, value));
        return this;
    }

    /**
     * Adds a summary item with an icon.
     */
    public BootstrapSummary addItem(String category, String name, String value, String icon) {
        items.add(new SummaryItem(category, name, value, icon));
        return this;
    }

    /**
     * Prints the summary to the specified output stream.
     */
    public void print(PrintStream out) {
        String line = "─".repeat(70);

        // Header
        out.println();
        out.println(color(GREEN, BOLD) + "  ✓ " + color(WHITE, BOLD) +
                (applicationName != null ? applicationName : "Garganttua") +
                " started successfully!" + reset());
        out.println(color(DIM) + "  " + line + reset());

        // Startup metrics
        out.println();
        out.println(color(CYAN, BOLD) + "  ⚡ Startup Metrics" + reset());
        out.println();

        if (startupTime != null) {
            String timeStr = formatDuration(startupTime);
            out.println(formatLine("Startup time", timeStr, "⏱"));
        }

        out.println(formatLine("Builders processed", String.valueOf(buildersCount), "🔧"));
        out.println(formatLine("Components built", String.valueOf(builtObjectsCount), "📦"));

        // Group items by category
        Map<String, List<SummaryItem>> groupedItems = new java.util.LinkedHashMap<>();
        for (SummaryItem item : items) {
            groupedItems.computeIfAbsent(item.category, k -> new ArrayList<>()).add(item);
        }

        // Print each category
        for (Map.Entry<String, List<SummaryItem>> entry : groupedItems.entrySet()) {
            out.println();
            out.println(color(CYAN, BOLD) + "  📋 " + entry.getKey() + reset());
            out.println();
            for (SummaryItem item : entry.getValue()) {
                out.println(formatLine(item.name, item.value, item.icon));
            }
        }

        // Footer
        out.println();
        out.println(color(DIM) + "  " + line + reset());
        if (applicationVersion != null) {
            out.println(color(DIM) + "  " + applicationName + " v" + applicationVersion + reset());
        }
        out.println();
    }

    /**
     * Formats a line with label and value.
     */
    private String formatLine(String label, String value, String icon) {
        String iconStr = icon != null ? icon + " " : "  ";
        return color(DIM) + "     " + iconStr + reset() +
               color(WHITE) + label + reset() +
               color(DIM) + ": " + reset() +
               color(YELLOW, BOLD) + value + reset();
    }

    /**
     * Formats a duration to a human-readable string.
     */
    private String formatDuration(Duration duration) {
        long millis = duration.toMillis();
        if (millis < 1000) {
            return millis + " ms";
        } else if (millis < 60000) {
            double seconds = millis / 1000.0;
            return String.format("%.2f s", seconds);
        } else {
            long minutes = duration.toMinutes();
            long remainingSeconds = duration.minusMinutes(minutes).getSeconds();
            return String.format("%d min %d s", minutes, remainingSeconds);
        }
    }

    /**
     * Wraps text with ANSI color codes if colors are enabled.
     */
    private String color(String... codes) {
        if (!useColors) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (String code : codes) {
            sb.append(code);
        }
        return sb.toString();
    }

    /**
     * Returns the ANSI reset code if colors are enabled.
     */
    private String reset() {
        return useColors ? RESET : "";
    }

    /**
     * A single summary item.
     */
    private static class SummaryItem {
        final String category;
        final String name;
        final String value;
        final String icon;

        SummaryItem(String category, String name, String value) {
            this(category, name, value, null);
        }

        SummaryItem(String category, String name, String value, String icon) {
            this.category = category;
            this.name = name;
            this.value = value;
            this.icon = icon;
        }
    }
}

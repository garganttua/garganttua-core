package com.garganttua.core.bootstrap.dsl;

import java.io.ByteArrayOutputStream;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

import com.garganttua.core.bootstrap.GarganttuaModules;
import com.garganttua.core.bootstrap.banner.BannerMode;
import com.garganttua.core.bootstrap.banner.BootstrapSummary;
import com.garganttua.core.bootstrap.banner.IBootstrapSummaryContributor;
import com.garganttua.core.bootstrap.banner.StageTimings;
import com.garganttua.core.observability.Logger;

/**
 * Console rendering for {@link Bootstrap}: phase headers, per-builder progress,
 * lifecycle actions and the post-build summary. Extracted to keep {@code Bootstrap}
 * focused on orchestration. All methods log at debug (and skip console output)
 * when the banner mode is {@code OFF}.
 */
final class BootstrapConsoleReporter {

    private static final Logger log = Logger.getLogger(BootstrapConsoleReporter.class);

    /** ANSI escape (0x1B) as a String — built from the char code to avoid source-encoding pitfalls. */
    private static final String ESC = String.valueOf((char) 27);
    private static final String RESET = ESC + "[0m";
    private static final String BOLD = ESC + "[1m";
    private static final String DIM = ESC + "[2m";
    private static final String CYAN = ESC + "[36m";
    private static final String YELLOW = ESC + "[33m";
    private static final String GREEN = ESC + "[32m";
    private static final String BLUE = ESC + "[34m";

    /**
     * UTF-8 console stream for printing banner art and progress glyphs (▶, ○, ✓, →).
     * On JVMs where the default {@code System.out} charset is not UTF-8 (e.g.
     * ANSI_X3.4-1968 on locale-less Linux containers), Unicode glyphs are replaced
     * by '?' when written through {@code System.out}. This stream writes UTF-8 bytes
     * directly to {@link FileDescriptor#out}, bypassing the default encoding.
     */
    static final PrintStream CONSOLE_OUT = createUtf8ConsoleStream();

    private BootstrapConsoleReporter() {
    }

    private static PrintStream createUtf8ConsoleStream() {
        try {
            if (StandardCharsets.UTF_8.equals(System.out.charset())) {
                return System.out;
            }
        } catch (Throwable ignored) {
            // PrintStream.charset() is Java 18+; pre-21 fall through to wrap.
        }
        try {
            return new PrintStream(new FileOutputStream(FileDescriptor.out), true, StandardCharsets.UTF_8);
        } catch (Throwable e) {
            return System.out;
        }
    }

    static void printPhase(BannerMode bannerMode, int phaseNumber, String phaseName, String details) {
        if (bannerMode == BannerMode.OFF) {
            log.debug("Phase {}: {} ({})", phaseNumber, phaseName, details);
            return;
        }
        CONSOLE_OUT.println();
        CONSOLE_OUT.println(CYAN + BOLD + "  ▶ Phase " + phaseNumber + ": " + phaseName + RESET
                + DIM + " (" + details + ")" + RESET);
    }

    static void printBuilderStart(BannerMode bannerMode, String builderName) {
        if (bannerMode == BannerMode.OFF) {
            log.debug("Building: {}", builderName);
            return;
        }
        CONSOLE_OUT.println(DIM + "     ○ " + RESET + YELLOW + builderName + RESET + DIM + " ..." + RESET);
    }

    static void printBuilderComplete(BannerMode bannerMode, String builderName) {
        if (bannerMode == BannerMode.OFF) {
            log.debug("Built: {}", builderName);
            return;
        }
        CONSOLE_OUT.print(ESC + "[1A"); // Move up one line
        CONSOLE_OUT.print(ESC + "[2K"); // Clear line
        CONSOLE_OUT.println(GREEN + "     ✓ " + RESET + builderName + DIM + " ready" + RESET);
    }

    static void printLifecycleAction(BannerMode bannerMode, String action, String componentName) {
        if (bannerMode == BannerMode.OFF) {
            log.debug("{}: {}", action, componentName);
            return;
        }
        CONSOLE_OUT.println(BLUE + "     → " + RESET + action + " " + DIM + componentName + RESET);
    }

    @SuppressWarnings("java:S107")
    static void printSummary(BannerMode bannerMode, String applicationName, String applicationVersion,
            int buildersCount, List<Object> builtObjects, Duration startupTime, StageTimings lastBuildTimings) {
        BootstrapSummary summary = new BootstrapSummary(bannerMode != BannerMode.OFF)
                .applicationName(applicationName)
                .applicationVersion(applicationVersion)
                .startupTime(startupTime)
                .buildersCount(buildersCount)
                .builtObjectsCount(builtObjects.size());

        // Inventory of garganttua-* modules detected on the classpath (coordinates
        // shipped in each JAR's MANIFEST.MF by the parent POM's maven-jar-plugin).
        for (GarganttuaModules.ModuleInfo m : GarganttuaModules.discover()) {
            summary.addItem("Modules", m.artifactId(), m.version(), "📦");
        }

        // Collect summary contributions from built objects
        for (Object built : builtObjects) {
            if (built instanceof IBootstrapSummaryContributor contributor) {
                String category = contributor.getSummaryCategory();
                contributor.getSummaryItems().forEach((name, value) -> summary.addItem(category, name, value));
            }
        }

        // Per-stage timing breakdown: phase totals (no ':') + per-builder (slowest first).
        if (lastBuildTimings != null) {
            var snapshot = lastBuildTimings.snapshot();
            snapshot.entrySet().stream()
                    .filter(e -> !e.getKey().contains(":"))
                    .forEach(e -> summary.addItem("Stage timings", e.getKey(),
                            StageTimings.format(e.getValue())));
            snapshot.entrySet().stream()
                    .filter(e -> e.getKey().contains(":"))
                    .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                    .forEach(e -> summary.addItem("Per-builder timings",
                            e.getKey(), StageTimings.format(e.getValue())));
        }

        if (bannerMode == BannerMode.CONSOLE) {
            summary.print(CONSOLE_OUT);
        } else if (bannerMode == BannerMode.LOG) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            summary.print(new PrintStream(baos, true, StandardCharsets.UTF_8));
            String summaryText = baos.toString(StandardCharsets.UTF_8);
            for (String line : summaryText.split("\n")) {
                if (!line.isBlank()) {
                    log.info(line);
                }
            }
        }
    }
}

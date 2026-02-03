package com.garganttua.core.bootstrap.banner;

import java.io.PrintStream;

import com.garganttua.core.bootstrap.GarganttuaVersion;

/**
 * Default Garganttua banner displaying ASCII art logo.
 *
 * <p>
 * This banner displays the Garganttua logo in ASCII art format along with
 * version information and a tagline.
 * </p>
 *
 * @since 2.0.0-ALPHA01
 */
public class GarganttuaBanner implements IBanner {

    /**
     * ANSI color codes for terminal output.
     */
    private static final String ANSI_RESET = "\u001B[0m";
    private static final String ANSI_CYAN = "\u001B[36m";
    private static final String ANSI_YELLOW = "\u001B[33m";
    private static final String ANSI_GREEN = "\u001B[32m";
    private static final String ANSI_BLUE = "\u001B[34m";
    private static final String ANSI_MAGENTA = "\u001B[35m";
    private static final String ANSI_BOLD = "\u001B[1m";

    private static final String[] BANNER_LINES = {
        "",
        "   ██████╗  █████╗ ██████╗  ██████╗  █████╗ ███╗   ██╗████████╗████████╗██╗   ██╗ █████╗ ",
        "  ██╔════╝ ██╔══██╗██╔══██╗██╔════╝ ██╔══██╗████╗  ██║╚══██╔══╝╚══██╔══╝██║   ██║██╔══██╗",
        "  ██║  ███╗███████║██████╔╝██║  ███╗███████║██╔██╗ ██║   ██║      ██║   ██║   ██║███████║",
        "  ██║   ██║██╔══██║██╔══██╗██║   ██║██╔══██║██║╚██╗██║   ██║      ██║   ██║   ██║██╔══██║",
        "  ╚██████╔╝██║  ██║██║  ██║╚██████╔╝██║  ██║██║ ╚████║   ██║      ██║   ╚██████╔╝██║  ██║",
        "   ╚═════╝ ╚═╝  ╚═╝╚═╝  ╚═╝ ╚═════╝ ╚═╝  ╚═╝╚═╝  ╚═══╝   ╚═╝      ╚═╝    ╚═════╝ ╚═╝  ╚═╝",
        ""
    };

    private static final String TAGLINE = "  :: Garganttua Core ::";

    private final String version;
    private final boolean useColors;

    /**
     * Creates a new GarganttuaBanner with the version from configuration.
     */
    public GarganttuaBanner() {
        this(GarganttuaVersion.getVersion(), true);
    }

    /**
     * Creates a new GarganttuaBanner with the specified version.
     *
     * @param version the version string to display
     */
    public GarganttuaBanner(String version) {
        this(version, true);
    }

    /**
     * Creates a new GarganttuaBanner with the specified version and color setting.
     *
     * @param version the version string to display
     * @param useColors whether to use ANSI colors in the output
     */
    public GarganttuaBanner(String version, boolean useColors) {
        this.version = version != null ? version : "UNKNOWN";
        this.useColors = useColors;
    }

    @Override
    public void print(PrintStream out) {
        // Print ASCII art logo
        for (String line : BANNER_LINES) {
            if (useColors) {
                out.println(ANSI_CYAN + ANSI_BOLD + line + ANSI_RESET);
            } else {
                out.println(line);
            }
        }

        // Print tagline and version
        String versionPadding = createPadding(TAGLINE, version);
        if (useColors) {
            out.println(ANSI_GREEN + TAGLINE + versionPadding + ANSI_YELLOW + "(" + version + ")" + ANSI_RESET);
        } else {
            out.println(TAGLINE + versionPadding + "(" + version + ")");
        }

        out.println();
    }

    /**
     * Creates padding to align the version with the banner width.
     */
    private String createPadding(String tagline, String version) {
        int bannerWidth = BANNER_LINES[1].length();
        int contentWidth = tagline.length() + version.length() + 2; // +2 for parentheses
        int padding = bannerWidth - contentWidth;
        if (padding < 1) {
            padding = 1;
        }
        return " ".repeat(padding);
    }

    /**
     * Returns the banner as a string without colors.
     *
     * @return the banner text
     */
    public String getBannerText() {
        StringBuilder sb = new StringBuilder();
        for (String line : BANNER_LINES) {
            sb.append(line).append("\n");
        }
        sb.append(TAGLINE).append(" (").append(version).append(")\n");
        return sb.toString();
    }
}

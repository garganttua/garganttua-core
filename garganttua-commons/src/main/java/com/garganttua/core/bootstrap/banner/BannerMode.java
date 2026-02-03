package com.garganttua.core.bootstrap.banner;

/**
 * Enumeration of possible banner display modes.
 *
 * @since 2.0.0-ALPHA01
 */
public enum BannerMode {

    /**
     * Disable banner printing.
     */
    OFF,

    /**
     * Print the banner to System.out (console).
     */
    CONSOLE,

    /**
     * Log the banner using the logging framework.
     */
    LOG

}

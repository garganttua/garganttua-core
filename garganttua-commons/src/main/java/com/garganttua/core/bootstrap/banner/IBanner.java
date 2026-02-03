package com.garganttua.core.bootstrap.banner;

import java.io.PrintStream;

/**
 * Interface for application banners displayed at startup.
 *
 * <p>
 * A banner is typically an ASCII art representation of the application name
 * or logo, displayed when the application starts. This interface allows for
 * custom banner implementations.
 * </p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * IBanner banner = new GarganttuaBanner();
 * banner.print(System.out);
 * }</pre>
 *
 * @since 2.0.0-ALPHA01
 */
@FunctionalInterface
public interface IBanner {

    /**
     * Prints the banner to the specified output stream.
     *
     * @param out the output stream to print the banner to
     */
    void print(PrintStream out);

    /**
     * A banner that prints nothing.
     */
    IBanner OFF = out -> {};

}

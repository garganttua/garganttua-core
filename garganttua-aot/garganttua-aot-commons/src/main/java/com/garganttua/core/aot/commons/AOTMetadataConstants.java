package com.garganttua.core.aot.commons;

/**
 * Constants for AOT metadata resource locations.
 *
 * <p>These paths are used by the annotation processor to write generated
 * metadata, and by the runtime to discover it on the classpath.</p>
 */
public final class AOTMetadataConstants {

    private AOTMetadataConstants() {
    }

    /** Directory for annotation index files (one file per indexed annotation). */
    public static final String INDEX_DIR = "META-INF/garganttua/index/";

    /** Directory for AOT class descriptor listings. */
    public static final String AOT_CLASSES_DIR = "META-INF/garganttua/aot/classes/";

    /** Directory for AOT binder listings. */
    public static final String AOT_BINDERS_DIR = "META-INF/garganttua/aot/binders/";

    /** Prefix for class entries in index files. */
    public static final String CLASS_ENTRY_PREFIX = "C:";

    /** Prefix for method entries in index files. */
    public static final String METHOD_ENTRY_PREFIX = "M:";

}

package org.vcell.libvcell;

import java.io.File;

/**
 * Points VCell at the BioNetGen libvcell bundles. VCell runs {@code perl <vcell.installDir>/bionetgen/BNG2.pl} to
 * expand a rule-based application's reaction rules into a reaction network when it generates the math; the Python
 * package ships BioNetGen in {@code libvcell/vcell_install/bionetgen/} and names that directory in the
 * {@value #INSTALL_DIR_ENV} environment variable (each native call runs in its own isolate, so the setting is made
 * at the start of every call rather than once).
 */
public final class BioNetGenSupport {

    /** The environment variable naming a directory that holds {@code bionetgen/BNG2.pl}. */
    public static final String INSTALL_DIR_ENV = "LIBVCELL_VCELL_INSTALL_DIR";
    private static final String INSTALL_DIR_PROPERTY = "vcell.installDir";

    private BioNetGenSupport() {
    }

    /** Set {@code vcell.installDir} from {@value #INSTALL_DIR_ENV}, unless it is already set or names no BioNetGen. */
    public static void configure() {
        if (System.getProperty(INSTALL_DIR_PROPERTY) != null) {
            return;
        }
        String dir = System.getenv(INSTALL_DIR_ENV);
        if (dir != null && !dir.isEmpty() && new File(new File(dir, "bionetgen"), "BNG2.pl").isFile()) {
            System.setProperty(INSTALL_DIR_PROPERTY, dir);
        }
    }

    /** The innermost message of a failure (e.g. "failed to find installed perl ..."), for reporting why. */
    static String rootMessage(Throwable t) {
        Throwable root = t;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        String message = root.getMessage();
        return (message == null || message.isBlank()) ? root.getClass().getSimpleName() : message.strip();
    }
}

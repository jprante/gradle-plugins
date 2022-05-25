package org.xbib.gradle.plugin.shadow.zip;

/**
 * The different modes {@link ZipOutputStream} can operate in.
 *
 * @see ZipOutputStream#setUseZip64
 */
public enum Zip64Mode {
    /**
     * Use Zip64 extensions for all entries, even if it is clear it is
     * not required.
     */
    Always,
    /**
     * Don't use Zip64 extensions for any entries.
     *
     * <p>This will cause a {@link Zip64RequiredException} to be
     * thrown if {@link ZipOutputStream} detects it needs Zip64
     * support.</p>
     */
    Never,
    /**
     * Use Zip64 extensions for all entries where they are required,
     * don't use them for entries that clearly don't require them.
     */
    AsNeeded
}

package org.xbib.gradle.plugin.shadow.zip;

/**
 * Various constants used throughout the package.
 */
final class ZipConstants {

    private ZipConstants() { }

    /** Masks last eight bits */
    static final int BYTE_MASK = 0xFF;

    /** length of a ZipShort in bytes */
    static final int SHORT = 2;

    /** length of a ZipLong in bytes */
    static final int WORD = 4;

    /** length of a ZipEightByteInteger in bytes */
    static final int DWORD = 8;

    /** Initial ZIP specification version */
    static final int INITIAL_VERSION = 10;

    /** ZIP specification version that introduced data descriptor method */
    static final int DATA_DESCRIPTOR_MIN_VERSION = 20;

    /** ZIP specification version that introduced ZIP64 */
    static final int ZIP64_MIN_VERSION = 45;

    /**
     * Value stored in two-byte size and similar fields if ZIP64
     * extensions are used.
     */
    static final int ZIP64_MAGIC_SHORT = 0xFFFF;

    /**
     * Value stored in four-byte size and similar fields if ZIP64
     * extensions are used.
     */
    static final long ZIP64_MAGIC = 0xFFFFFFFFL;

}

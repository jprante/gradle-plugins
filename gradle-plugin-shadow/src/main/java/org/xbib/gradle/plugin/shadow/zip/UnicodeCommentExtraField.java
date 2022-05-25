package org.xbib.gradle.plugin.shadow.zip;

/**
 * Info-ZIP Unicode Comment Extra Field (0x6375):
 *
 * <p>Stores the UTF-8 version of the file comment as stored in the
 * central directory header.</p>
 *
 * <p>See <a href="http://www.pkware.com/documents/casestudies/APPNOTE.TXT">PKWARE's
 * APPNOTE.TXT, section 4.6.8</a>.</p>
 *
 */
public class UnicodeCommentExtraField extends AbstractUnicodeExtraField {

    public static final ZipShort UCOM_ID = new ZipShort(0x6375);

    public UnicodeCommentExtraField() {
    }

    /**
     * Assemble as unicode comment extension from the name given as
     * text as well as the encoded bytes actually written to the archive.
     *
     * @param text The file name
     * @param bytes the bytes actually written to the archive
     * @param off The offset of the encoded comment in <code>bytes</code>.
     * @param len The length of the encoded comment or comment in
     * <code>bytes</code>.
     */
    public UnicodeCommentExtraField(final String text, final byte[] bytes, final int off,
                                    final int len) {
        super(text, bytes, off, len);
    }

    /**
     * Assemble as unicode comment extension from the comment given as
     * text as well as the bytes actually written to the archive.
     *
     * @param comment The file comment
     * @param bytes the bytes actually written to the archive
     */
    public UnicodeCommentExtraField(final String comment, final byte[] bytes) {
        super(comment, bytes);
    }

    /** {@inheritDoc} */
    public ZipShort getHeaderId() {
        return UCOM_ID;
    }

}
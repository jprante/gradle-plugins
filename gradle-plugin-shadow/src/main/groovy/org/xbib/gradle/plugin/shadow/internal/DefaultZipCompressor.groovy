package org.xbib.gradle.plugin.shadow.internal

import org.gradle.api.UncheckedIOException
import org.xbib.gradle.plugin.shadow.zip.Zip64Mode
import org.xbib.gradle.plugin.shadow.zip.ZipOutputStream

class DefaultZipCompressor implements ZipCompressor {

    private final int entryCompressionMethod
    private final Zip64Mode zip64Mode

     DefaultZipCompressor(boolean allowZip64Mode, int entryCompressionMethod) {
        this.entryCompressionMethod = entryCompressionMethod
        zip64Mode = allowZip64Mode ? Zip64Mode.AsNeeded : Zip64Mode.Never
    }

    @Override
    ZipOutputStream createArchiveOutputStream(File destination) {
        try {
            ZipOutputStream zipOutputStream = new ZipOutputStream(destination)
            zipOutputStream.setUseZip64(zip64Mode)
            zipOutputStream.setMethod(entryCompressionMethod)
            return zipOutputStream
        } catch (Exception e) {
            throw new UncheckedIOException("unable to create ZIP output stream for file " + destination, e)
        }
    }
}

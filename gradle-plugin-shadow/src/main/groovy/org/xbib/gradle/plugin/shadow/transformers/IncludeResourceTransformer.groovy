package org.xbib.gradle.plugin.shadow.transformers

import org.gradle.api.file.FileTreeElement
import org.xbib.gradle.plugin.shadow.internal.Utils
import org.xbib.gradle.plugin.shadow.zip.ZipEntry
import org.xbib.gradle.plugin.shadow.zip.ZipOutputStream

/**
 * A resource processor that allows the addition of an arbitrary file content into the shaded JAR.
 * Modified from org.apache.maven.plugins.shade.resource.IncludeResourceTransformer
 */
class IncludeResourceTransformer implements Transformer {

    File file

    String resource

    @Override
    boolean canTransformResource(FileTreeElement element) {
        return false
    }

    @Override
    void transform(TransformerContext context) {
        // no op
    }

    @Override
    boolean hasTransformedResource() {
        return file != null ? file.exists() : false
    }

    @Override
    void modifyOutputStream(ZipOutputStream outputStream, boolean preserveFileTimestamps) {
        ZipEntry entry = new ZipEntry(resource)
        entry.time = TransformerContext.getEntryTimestamp(preserveFileTimestamps, entry.time)
        outputStream.putNextEntry(entry)
        InputStream is = new FileInputStream(file)
        Utils.copyLarge(is, outputStream)
        is.close()
    }
}

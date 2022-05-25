package org.xbib.gradle.plugin.shadow.transformers

import org.gradle.api.file.FileTreeElement
import org.xbib.gradle.plugin.shadow.internal.Utils
import org.xbib.gradle.plugin.shadow.zip.ZipEntry
import org.xbib.gradle.plugin.shadow.zip.ZipOutputStream

/**
 * A resource processor that appends content for a resource, separated by a newline.
 * Modified from org.apache.maven.plugins.shade.resource.AppendingTransformer.
 */
class AppendingTransformer implements Transformer {
    String resource

    ByteArrayOutputStream data = new ByteArrayOutputStream()

    @Override
    boolean canTransformResource(FileTreeElement element) {
        def path = element.relativePath.pathString
        resource != null && resource.equalsIgnoreCase(path)
    }

    @Override
    void transform(TransformerContext context) {
        Utils.copyLarge(context.inputStream, data)
        data.write('\n'.bytes)
        context.inputStream.close()
    }

    @Override
    boolean hasTransformedResource() {
        return data.size() > 0
    }

    @Override
    void modifyOutputStream(ZipOutputStream os, boolean preserveFileTimestamps) {
        ZipEntry entry = new ZipEntry(resource)
        entry.time = TransformerContext.getEntryTimestamp(preserveFileTimestamps, entry.time)
        os.putNextEntry(entry)
        Utils.copyLarge(new ByteArrayInputStream(data.toByteArray()), os)
        data.reset()
    }
}

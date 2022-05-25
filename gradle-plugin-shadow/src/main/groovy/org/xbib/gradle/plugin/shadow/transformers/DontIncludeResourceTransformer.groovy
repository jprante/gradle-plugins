package org.xbib.gradle.plugin.shadow.transformers

import org.gradle.api.file.FileTreeElement
import org.xbib.gradle.plugin.shadow.zip.ZipOutputStream

/**
 * A resource processor that prevents the inclusion of an arbitrary resource into the shaded JAR.
 * Modified from org.apache.maven.plugins.shade.resource.DontIncludeResourceTransformer
 */
class DontIncludeResourceTransformer implements Transformer {
    String resource

    @Override
    boolean canTransformResource(FileTreeElement element) {
        def path = element.relativePath.pathString
        if (path.endsWith(resource)) {
            return true
        }

        return false
    }

    @Override
    void transform(TransformerContext context) {
        // no op
    }

    @Override
    boolean hasTransformedResource() {
        return false
    }

    @Override
    void modifyOutputStream(ZipOutputStream os, boolean preserveFileTimestamps) {
        // no op
    }
}

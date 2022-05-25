package org.xbib.gradle.plugin.shadow.transformers

import org.gradle.api.file.FileTreeElement
import org.xbib.gradle.plugin.shadow.zip.ZipOutputStream

/**
 * Modified from org.apache.maven.plugins.shade.resource.ResourceTransformer.
 */
interface Transformer {

    boolean canTransformResource(FileTreeElement element)

    void transform(TransformerContext context)

    boolean hasTransformedResource()

    void modifyOutputStream(ZipOutputStream jos, boolean preserveFileTimestamps)
}

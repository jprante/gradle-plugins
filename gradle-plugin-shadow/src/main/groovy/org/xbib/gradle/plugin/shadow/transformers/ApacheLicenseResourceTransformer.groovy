package org.xbib.gradle.plugin.shadow.transformers

import org.gradle.api.file.FileTreeElement
import org.xbib.gradle.plugin.shadow.zip.ZipOutputStream

/**
 * Prevents duplicate copies of the license
 * Modified from org.apache.maven.plugins.shade.resouce.ApacheLicenseResourceTransformer.
 */
class ApacheLicenseResourceTransformer implements Transformer {

    private static final String LICENSE_PATH = "META-INF/LICENSE"

    private static final String LICENSE_TXT_PATH = "META-INF/LICENSE.txt"

    @Override
    boolean canTransformResource(FileTreeElement element) {
        def path = element.relativePath.pathString
        return LICENSE_PATH.equalsIgnoreCase(path) ||
                LICENSE_TXT_PATH.regionMatches(true, 0, path, 0, LICENSE_TXT_PATH.length())
    }

    @Override
    void transform(TransformerContext context) {
    }

    @Override
    boolean hasTransformedResource() {
        return false
    }

    @Override
    void modifyOutputStream(ZipOutputStream jos, boolean preserveFileTimestamps) {
    }
}

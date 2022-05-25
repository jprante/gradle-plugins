package org.xbib.gradle.plugin.shadow.transformers

import org.gradle.api.file.FileTreeElement
import org.xbib.gradle.plugin.shadow.zip.ZipEntry
import org.xbib.gradle.plugin.shadow.zip.ZipOutputStream

import java.util.jar.*
import java.util.jar.Attributes.Name

/**
 * A resource processor that allows the arbitrary addition of attributes to
 * the first MANIFEST.MF that is found in the set of JARs being processed, or
 * to a newly created manifest for the shaded JAR.
 * Modified from org.apache.maven.plugins.shade.resource.ManifestResourceTransformer
 */
class ManifestResourceTransformer implements Transformer {

    private String mainClass

    private Map<String, Attributes> manifestEntries

    private boolean manifestDiscovered

    private Manifest manifest

    @Override
    boolean canTransformResource(FileTreeElement element) {
        def path = element.relativePath.pathString
        if (JarFile.MANIFEST_NAME.equalsIgnoreCase(path)) {
            return true
        }
        return false
    }

    @Override
    void transform(TransformerContext context) {
        if (!manifestDiscovered) {
            manifest = new Manifest(context.inputStream)
            manifestDiscovered = true
            if (context.inputStream) {
                context.inputStream.close()
            }
        }
    }

    @Override
    boolean hasTransformedResource() {
        true
    }

    @Override
    void modifyOutputStream(ZipOutputStream os, boolean preserveFileTimestamps) {
        if (manifest == null) {
            manifest = new Manifest()
        }
        Attributes attributes = manifest.getMainAttributes()
        if (mainClass != null) {
            attributes.put(Name.MAIN_CLASS, mainClass)
        }
        if (manifestEntries != null) {
            for (Map.Entry<String, Attributes> entry : manifestEntries.entrySet()) {
                attributes.put(new Name(entry.getKey()), entry.getValue())
            }
        }
        ZipEntry entry = new ZipEntry(JarFile.MANIFEST_NAME)
        entry.time = TransformerContext.getEntryTimestamp(preserveFileTimestamps, entry.time)
        os.putNextEntry(entry)
        manifest.write(os)
    }

    ManifestResourceTransformer attributes(Map<String, ?> attributes) {
        if (manifestEntries == null) {
            manifestEntries = [:]
        }
        manifestEntries.putAll(attributes)
        this
    }
}

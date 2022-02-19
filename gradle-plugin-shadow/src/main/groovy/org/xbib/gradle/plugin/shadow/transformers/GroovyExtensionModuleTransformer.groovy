package org.xbib.gradle.plugin.shadow.transformers

import org.gradle.api.file.FileTreeElement
import org.xbib.gradle.plugin.shadow.internal.Utils
import org.xbib.gradle.plugin.shadow.zip.ZipEntry
import org.xbib.gradle.plugin.shadow.zip.ZipOutputStream

/**
 * Modified from eu.appsatori.gradle.fatjar.tasks.PrepareFiles.groovy
 * Resource transformer that merges Groovy extension module descriptor files into a single file. If there are several
 * META-INF/services/org.codehaus.groovy.runtime.ExtensionModule resources spread across many JARs the individual
 * entries will all be merged into a single META-INF/services/org.codehaus.groovy.runtime.ExtensionModule resource
 * packaged into the resultant JAR produced by the shadowing process.
 */
class GroovyExtensionModuleTransformer implements Transformer {

    private static final GROOVY_EXTENSION_MODULE_DESCRIPTOR_PATH =
            "META-INF/services/org.codehaus.groovy.runtime.ExtensionModule"

    private static final MODULE_NAME_KEY = 'moduleName'
    private static final MODULE_VERSION_KEY = 'moduleVersion'
    private static final EXTENSION_CLASSES_KEY = 'extensionClasses'
    private static final STATIC_EXTENSION_CLASSES_KEY = 'staticExtensionClasses'

    private static final MERGED_MODULE_NAME = 'MergedByShadowJar'
    private static final MERGED_MODULE_VERSION = '1.0.0'

    private final Properties module = new Properties()

    @Override
    boolean canTransformResource(FileTreeElement element) {
        return element.relativePath.pathString == GROOVY_EXTENSION_MODULE_DESCRIPTOR_PATH
    }

    @Override
    void transform(TransformerContext context) {
        def props = new Properties()
        props.load(context.inputStream)
        props.each { String key, String value ->
            switch (key) {
                case MODULE_NAME_KEY:
                    handle(key, value) {
                        module.setProperty(key, MERGED_MODULE_NAME)
                    }
                    break
                case MODULE_VERSION_KEY:
                    handle(key, value) {
                        module.setProperty(key, MERGED_MODULE_VERSION)
                    }
                    break
                case [EXTENSION_CLASSES_KEY, STATIC_EXTENSION_CLASSES_KEY]:
                    handle(key, value) { String existingValue ->
                        def newValue = "${existingValue},${value}"
                        module.setProperty(key, newValue)
                    }
                    break
            }
        }
    }

    @Override
    boolean hasTransformedResource() {
        return module.size() > 0
    }

    @Override
    void modifyOutputStream(ZipOutputStream os, boolean preserveFileTimestamps) {
        ZipEntry entry = new ZipEntry(GROOVY_EXTENSION_MODULE_DESCRIPTOR_PATH)
        entry.time = TransformerContext.getEntryTimestamp(preserveFileTimestamps, entry.time)
        os.putNextEntry(entry)
        Utils.copyLarge(toInputStream(module), os)
        os.closeEntry()
    }

    private static InputStream toInputStream(Properties props) {
        def baos = new ByteArrayOutputStream()
        props.store(baos, null)
        return new ByteArrayInputStream(baos.toByteArray())
    }

    private handle(String key, String value, Closure mergeValue) {
        def existingValue = module.getProperty(key)
        if (existingValue) {
            mergeValue(existingValue)
        } else {
            module.setProperty(key, value)
        }
    }
}

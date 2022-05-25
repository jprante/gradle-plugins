package org.xbib.gradle.plugin.shadow.transformers

import org.xbib.gradle.plugin.shadow.internal.ServiceStream
import org.xbib.gradle.plugin.shadow.internal.Utils
import org.xbib.gradle.plugin.shadow.relocation.RelocateClassContext
import org.gradle.api.file.FileTreeElement
import org.gradle.api.specs.Spec
import org.gradle.api.tasks.util.PatternFilterable
import org.gradle.api.tasks.util.PatternSet
import org.xbib.gradle.plugin.shadow.zip.ZipEntry
import org.xbib.gradle.plugin.shadow.zip.ZipOutputStream

/**
 * Modified from org.apache.maven.plugins.shade.resource.ServiceResourceTransformer.java
 * Resources transformer that appends entries in META-INF/services resources into
 * a single resource. For example, if there are several META-INF/services/org.apache.maven.project.ProjectBuilder
 * resources spread across many JARs the individual entries will all be concatenated into a single
 * META-INF/services/org.apache.maven.project.ProjectBuilder resource packaged into the resultant JAR produced
 * by the shading process.
 */
class ServiceFileTransformer implements Transformer, PatternFilterable {

    private static final String SERVICES_PATTERN = "META-INF/services/**"

    private static final String GROOVY_EXTENSION_MODULE_DESCRIPTOR_PATTERN =
            "META-INF/services/org.codehaus.groovy.runtime.ExtensionModule"

    Map<String, ServiceStream> serviceEntries = [:].withDefault { new ServiceStream() }

    private final PatternSet patternSet =
            new PatternSet().include(SERVICES_PATTERN).exclude(GROOVY_EXTENSION_MODULE_DESCRIPTOR_PATTERN)

    void setPath(String path) {
        patternSet.setIncludes(["${path}/**"])
    }

    @Override
    boolean canTransformResource(FileTreeElement element) {
        return patternSet.asSpec.isSatisfiedBy(element)
    }

    @Override
    void transform(TransformerContext context) {
        def lines = context.inputStream.readLines()
        def targetPath = context.path
        context.relocators.each {rel ->
            if(rel.canRelocateClass(RelocateClassContext.builder().className(new File(targetPath).name).stats(context.stats).build())) {
                targetPath = rel.relocateClass(RelocateClassContext.builder().className(targetPath).stats(context.stats).build())
            }
            lines.eachWithIndex { String line, int i ->
                def lineContext = RelocateClassContext.builder().className(line).stats(context.stats).build()
                if(rel.canRelocateClass(lineContext)) {
                    lines[i] = rel.relocateClass(lineContext)
                }
            }
        }
        lines.each {line -> serviceEntries[targetPath].append(new ByteArrayInputStream(line.getBytes()))}
    }

    @Override
    boolean hasTransformedResource() {
        return serviceEntries.size() > 0
    }

    @Override
    void modifyOutputStream(ZipOutputStream os, boolean preserveFileTimestamps) {
        serviceEntries.each { String path, ServiceStream stream ->
            ZipEntry entry = new ZipEntry(path)
            entry.time = TransformerContext.getEntryTimestamp(preserveFileTimestamps, entry.time)
            os.putNextEntry(entry)
            Utils.copyLarge(stream.toInputStream(), os)
            os.closeEntry()
        }
    }

    @Override
    ServiceFileTransformer include(String... includes) {
        patternSet.include(includes)
        this
    }

    @Override
    ServiceFileTransformer include(Iterable<String> includes) {
        patternSet.include(includes)
        this
    }

    @Override
    ServiceFileTransformer include(Spec<FileTreeElement> includeSpec) {
        patternSet.include(includeSpec)
        this
    }

    @Override
    ServiceFileTransformer include(Closure includeSpec) {
        patternSet.include(includeSpec)
        this
    }

    @Override
    ServiceFileTransformer exclude(String... excludes) {
        patternSet.exclude(excludes)
        this
    }

    @Override
    ServiceFileTransformer exclude(Iterable<String> excludes) {
        patternSet.exclude(excludes)
        this
    }

    @Override
    ServiceFileTransformer exclude(Spec<FileTreeElement> excludeSpec) {
        patternSet.exclude(excludeSpec)
        this
    }

    @Override
    ServiceFileTransformer exclude(Closure excludeSpec) {
        patternSet.exclude(excludeSpec)
        this
    }

    @Override
    Set<String> getIncludes() {
        patternSet.includes
    }

    @Override
    ServiceFileTransformer setIncludes(Iterable<String> includes) {
        patternSet.includes = includes
        this
    }

    @Override
    Set<String> getExcludes() {
        patternSet.excludes
    }

    @Override
    ServiceFileTransformer setExcludes(Iterable<String> excludes) {
        patternSet.excludes = excludes
        this
    }
}

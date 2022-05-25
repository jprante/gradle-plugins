package org.xbib.gradle.plugin.shadow.tasks

import org.gradle.api.Action
import org.gradle.api.internal.file.FileResolver
import org.gradle.api.java.archives.Attributes
import org.gradle.api.java.archives.Manifest
import org.gradle.api.java.archives.ManifestException
import org.gradle.api.java.archives.ManifestMergeSpec
import org.gradle.api.java.archives.internal.DefaultManifest
import org.gradle.api.java.archives.internal.DefaultManifestMergeSpec
import org.gradle.util.ConfigureUtil

class DefaultInheritManifest implements InheritManifest {

    private List<DefaultManifestMergeSpec> inheritMergeSpecs = []

    private final FileResolver fileResolver

    private final Manifest internalManifest

    DefaultInheritManifest(FileResolver fileResolver) {
        this.internalManifest = new DefaultManifest(fileResolver)
        this.fileResolver = fileResolver
    }

    InheritManifest inheritFrom(Object... inheritPaths) {
        inheritFrom(inheritPaths, null)
        this
    }

    InheritManifest inheritFrom(Object inheritPaths, Closure closure) {
        DefaultManifestMergeSpec mergeSpec = new DefaultManifestMergeSpec()
        mergeSpec.from(inheritPaths)
        inheritMergeSpecs.add(mergeSpec)
        ConfigureUtil.configure(closure, mergeSpec)
        this
    }

    @Override
    Attributes getAttributes() {
        internalManifest.getAttributes()
    }

    @Override
    Map<String, Attributes> getSections() {
        internalManifest.getSections()
    }

    @Override
    Manifest attributes(Map<String, ?> map) throws ManifestException {
        internalManifest.attributes(map)
        this
    }

    @Override
    Manifest attributes(Map<String, ?> map, String s) throws ManifestException {
        internalManifest.attributes(map, s)
        this
    }

    @Override
    DefaultManifest getEffectiveManifest() {
        DefaultManifest base = new DefaultManifest(fileResolver)
        inheritMergeSpecs.each {
            base = it.merge(base, fileResolver)
        }
        base.from internalManifest
        base.getEffectiveManifest()
    }

    Manifest writeTo(Writer writer) {
        this.getEffectiveManifest().writeTo((Object) writer)
        this
    }

    @Override
    Manifest writeTo(Object o) {
        this.getEffectiveManifest().writeTo(o)
        this
    }

    @Override
    Manifest from(Object... objects) {
        internalManifest.from(objects)
        this
    }

    @Override
    Manifest from(Object o, Closure<?> closure) {
        internalManifest.from(o, closure)
        this
    }

    @Override
    Manifest from(Object o, Action<ManifestMergeSpec> action) {
        internalManifest.from(o, action)
        this
    }
}

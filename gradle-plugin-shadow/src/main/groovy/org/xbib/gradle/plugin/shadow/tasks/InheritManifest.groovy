package org.xbib.gradle.plugin.shadow.tasks

import org.gradle.api.java.archives.Manifest

interface InheritManifest extends Manifest {

    InheritManifest inheritFrom(Object... inheritPaths)

    InheritManifest inheritFrom(Object inheritPaths, Closure closure)
}

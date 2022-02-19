package org.xbib.gradle.plugin.shadow.internal

import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.SourceSet

class UnusedTracker {

    private final FileCollection toMinimize

    private final List<ClazzpathUnit> projectUnits

    private final Clazzpath clazzPath

    private UnusedTracker(List<File> classDirs, FileCollection toMinimize) {
        this.toMinimize = toMinimize
        this.clazzPath = new Clazzpath()
        this.projectUnits = classDirs.collect { clazzPath.addClazzpathUnit(it) }
    }

    Set<String> findUnused() {
        Set<Clazz> unused = clazzPath.clazzes
        for (cpu in projectUnits) {
            unused.removeAll(cpu.clazzes)
            unused.removeAll(cpu.transitiveDependencies)
        }
        unused.collect { it.name }.toSet()
    }

    void addDependency(File jarOrDir) {
        if (toMinimize.contains(jarOrDir)) {
            clazzPath.addClazzpathUnit(jarOrDir)
        }
    }

    static UnusedTracker forProject(Project project, FileCollection toMinimize) {
        final List<File> classDirs = new ArrayList<>()
        for (SourceSet sourceSet in project.sourceSets) {
            Iterable<File> classesDirs = sourceSet.output.classesDirs
            classDirs.addAll(classesDirs.findAll { it.isDirectory() })
        }
        return new UnusedTracker(classDirs, toMinimize)
    }
}

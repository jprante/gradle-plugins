package org.xbib.gradle.plugin.shadow.internal

import org.objectweb.asm.ClassReader

import java.util.jar.JarEntry
import java.util.jar.JarInputStream

class DependencyUtils {

    static Collection<String> getDependenciesOfJar(InputStream is ) throws IOException {
        Set<String> dependencies = []
        JarInputStream inputStream = new JarInputStream(is)
        inputStream.withCloseable {
            while (true) {
                JarEntry entry = inputStream.getNextJarEntry()
                if (entry == null) {
                    break
                }
                if (entry.isDirectory()) {
                    inputStream.readAllBytes()
                    continue
                }
                if (entry.getName().endsWith('.class')) {
                    DependenciesClassRemapper dependenciesClassAdapter = new DependenciesClassRemapper()
                    new ClassReader(inputStream.readAllBytes()).accept(dependenciesClassAdapter, 0)
                    dependencies.addAll(dependenciesClassAdapter.getDependencies())
                } else {
                    inputStream.readAllBytes()
                }
            }
        }
        dependencies
    }

    static Collection<String> getDependenciesOfClass(InputStream is) throws IOException {
        final DependenciesClassRemapper dependenciesClassAdapter = new DependenciesClassRemapper()
        new ClassReader(is.readAllBytes()).accept(dependenciesClassAdapter, ClassReader.EXPAND_FRAMES)
        dependenciesClassAdapter.getDependencies()
    }

    static Collection<String> getDependenciesOfClass(Class<?> clazz) throws IOException {
        getDependenciesOfClass(clazz.getResourceAsStream('/' + clazz.getName().replace('.', '/') + '.class'))
    }
}

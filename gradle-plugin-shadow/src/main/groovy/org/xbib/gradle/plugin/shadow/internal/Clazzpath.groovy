package org.xbib.gradle.plugin.shadow.internal

import org.objectweb.asm.ClassReader

import java.util.jar.JarEntry
import java.util.jar.JarInputStream
import java.nio.file.Files
import java.nio.file.Path
import java.util.stream.Stream
import java.util.stream.StreamSupport

class Clazzpath {

    private final Set<ClazzpathUnit> units

    private final Map<String, Clazz> missing

    private final Map<String, Clazz> clazzes

    Clazzpath() {
        units = new HashSet<ClazzpathUnit>()
        missing = new HashMap<String, Clazz>()
        clazzes = new HashMap<String, Clazz>()
    }

    boolean removeClazzpathUnit(ClazzpathUnit unit) {
        Set<Clazz> unitClazzes = unit.getClazzes()
        for (Clazz clazz : unitClazzes) {
            clazz.removeClazzpathUnit(unit)
            if (clazz.getClazzpathUnits().size() == 0) {
                clazzes.remove(clazz.toString())
            }
        }
        units.remove(unit)
    }

    ClazzpathUnit addClazzpathUnit(File file) throws IOException {
        addClazzpathUnit(file.toPath())
    }

    ClazzpathUnit addClazzpathUnit(File file, String s) throws IOException {
        addClazzpathUnit(file.toPath(), s)
    }

    ClazzpathUnit addClazzpathUnit(Path path) throws IOException {
        addClazzpathUnit(path, path.toString())
    }

    ClazzpathUnit addClazzpathUnit(Path path1, String s) throws IOException {
        Path path = path1.toAbsolutePath()
        if (Files.isRegularFile(path)) {
            return addClazzpathUnit(Files.newInputStream(path), s)
        } else if (Files.isDirectory(path)) {
            String prefix = Utils.separatorsToUnix(Utils.normalize(path.toString() + File.separatorChar))
            List<Resource> list = []
            path.traverse { p ->
                if (Files.isRegularFile(p) && isValidResourceName(p.getFileName().toString())) {
                    list << new Resource(p.toString().substring(prefix.length())) {
                        @Override
                        InputStream getInputStream() throws IOException {
                            Files.newInputStream(p)
                        }
                    }
                }
            }
            return addClazzpathUnit(list, s, true)
        }
        throw new IllegalArgumentException("neither file nor directory")
    }

    ClazzpathUnit addClazzpathUnit(InputStream inputStream, String s) throws IOException {
        final JarInputStream jarInputStream = new JarInputStream(inputStream)
        try {
            Stream stream = toEntryStream(jarInputStream).map { e -> e.getName() }
                    .filter { name -> isValidResourceName(name) }
                    .map { name -> new Resource(name) {
                               @Override
                               InputStream getInputStream() throws IOException {
                                   jarInputStream
                               }
                           }
                    }
            addClazzpathUnit(stream.&iterator, s, false)
        } finally {
            jarInputStream.close()
        }
    }

    ClazzpathUnit addClazzpathUnit(Iterable<Resource> resources, String s, boolean shouldCloseResourceStream)
            throws IOException {
        Map<String, Clazz> unitClazzes = new HashMap<String, Clazz>()
        Map<String, Clazz> unitDependencies = new HashMap<String, Clazz>()
        ClazzpathUnit unit = new ClazzpathUnit(s, unitClazzes, unitDependencies)
        for (Resource resource : resources) {
            String clazzName = resource.name
            Clazz clazz = getClazz(clazzName)
            if (clazz == null) {
                clazz = missing.get(clazzName)
                if (clazz != null) {
                    // already marked missing
                    clazz = missing.remove(clazzName)
                } else {
                    clazz = new Clazz(clazzName)
                }
            }
            clazz.addClazzpathUnit(unit)
            clazzes.put(clazzName, clazz)
            unitClazzes.put(clazzName, clazz)
            DependenciesClassRemapper dependenciesClassAdapter = new DependenciesClassRemapper()
            InputStream inputStream = resource.getInputStream()
            try {
                new ClassReader(inputStream.readAllBytes()).accept(dependenciesClassAdapter, ClassReader.EXPAND_FRAMES | ClassReader.SKIP_DEBUG)
            } finally {
                if (shouldCloseResourceStream) {
                    inputStream.close()
                }
            }
            Set<String> depNames = dependenciesClassAdapter.getDependencies()
            for (String depName : depNames) {
                Clazz dep = getClazz(depName)
                if (dep == null) {
                    // there is no such clazz yet
                    dep = missing.get(depName)
                }
                if (dep == null) {
                    // it is also not recorded to be missing
                    dep = new Clazz(depName)
                    dep.addClazzpathUnit(unit)
                    missing.put(depName, dep)
                }
                if (dep != clazz) {
                    unitDependencies.put(depName, dep)
                    clazz.addDependency(dep)
                }
            }
        }
        units.add(unit)
        unit
    }

    Set<Clazz> getClazzes() {
        new HashSet<Clazz>(clazzes.values())
    }

    Set<Clazz> getClashedClazzes() {
        Set<Clazz> all = new HashSet<Clazz>()
        for (Clazz clazz : clazzes.values()) {
            if (clazz.getClazzpathUnits().size() > 1) {
                all.add(clazz)
            }
        }
        all
    }

    Set<Clazz> getMissingClazzes() {
        new HashSet<Clazz>(missing.values())
    }

    Clazz getClazz(String clazzName) {
        (Clazz) clazzes.get(clazzName)
    }

    ClazzpathUnit[] getUnits() {
        units.toArray(new ClazzpathUnit[units.size()])
    }

    private Stream<JarEntry> toEntryStream(JarInputStream jarInputStream) {
        StreamSupport.stream(Spliterators.spliteratorUnknownSize(new JarEntryIterator(jarInputStream),
                Spliterator.IMMUTABLE), false)
    }

    private class JarEntryIterator implements Iterator<JarEntry> {

        JarInputStream jarInputStream
        JarEntry entry

        JarEntryIterator(JarInputStream jarInputStream) {
            this.jarInputStream = jarInputStream
            this.entry = null
        }

        @Override
        boolean hasNext() {
            try {
                if (entry == null) {
                    entry = jarInputStream.getNextJarEntry()
                }
                return entry != null
            } catch (IOException e) {
                throw new RuntimeException(e)
            }
        }

        @Override
        JarEntry next() {
            try {
                JarEntry result = entry != null ? entry : jarInputStream.getNextJarEntry()
                entry = null
                return result
            } catch (IOException e) {
                throw new RuntimeException(e)
            }
        }
    }

    private static abstract class Resource {

        private static int ext = '.class'.length()

        String name

        Resource(String name) {
            // foo/bar/Foo.class -> // foo.bar.Foo
            this.name = name.substring(0, name.length() - ext).replace('/', '.')
        }

        abstract InputStream getInputStream() throws IOException

        @Override
        String toString() {
            name
        }
    }

    private static boolean isValidResourceName(String name) {
        (name != null) && name.endsWith('.class') && !name.contains('-')
    }
}

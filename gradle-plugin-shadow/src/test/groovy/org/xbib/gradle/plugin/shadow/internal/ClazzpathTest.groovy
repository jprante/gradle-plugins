package org.xbib.gradle.plugin.shadow.internal

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertNotNull
import static org.junit.Assert.assertNull
import static org.junit.Assert.assertTrue

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters

@RunWith(Parameterized.class)
class ClazzpathTest {

    private final AddClazzpathUnit addClazzpathUnit

    private static abstract class AddClazzpathUnit {

        abstract ClazzpathUnit to(Clazzpath clazzpath, String filename, String id) throws IOException

        ClazzpathUnit to(Clazzpath clazzpath, String filename) throws IOException {
            to(clazzpath, filename, filename)
        }
    }

    /**
     * Parameters for test
     *
     * 1. AddClazzpathUnit for classpath-based jars
     * 2. AddClazzpathUnit for filesystem-based jars
     * 3. AddClazzpathUnit for filesystem-based directories
     */
    @Parameters(name = "{index}: {1}")
    static Collection<Object[]> data() {
        def obj1 = [
            new AddClazzpathUnit() {
                @Override
                ClazzpathUnit to(Clazzpath toClazzpath, String filename, String id) throws IOException {
                    InputStream resourceAsStream = getClass()
                        .getClassLoader()
                        .getResourceAsStream(filename + ".jar")
                    assertNotNull(resourceAsStream)
                    toClazzpath.addClazzpathUnit(resourceAsStream, id)
                }
            }, "classpath"] as Object[]
        def obj2 = [
            new AddClazzpathUnit() {
                @Override
                ClazzpathUnit to(Clazzpath toClazzpath, String filename, String id) throws IOException {
                    File file = new File(new File("src/test/resources/" + filename + ".jar").getAbsolutePath())
                    assertTrue(file.exists())
                    assertTrue(file.isFile())
                    toClazzpath.addClazzpathUnit(file, id)
                }
            }, "file-jar" ] as Object[]
        def obj3 = [
            new AddClazzpathUnit() {
                @Override
                ClazzpathUnit to(Clazzpath toClazzpath, String filename, String id) throws IOException {
                    File file = new File(new File("src/test/resources/" + filename).getAbsolutePath())
                    assertTrue(file.exists())
                    assertTrue(file.isDirectory())
                    toClazzpath.addClazzpathUnit(file, id)
                }
            }, "file-directory" ] as Object[]
        def obj4 = [
            new AddClazzpathUnit() {
                @Override
                ClazzpathUnit to(Clazzpath toClazzpath, String filename, String id) throws IOException {
                    Path path = Paths.get("src/test/resources/" + filename + ".jar")
                    assertTrue(Files.exists(path))
                    assertTrue(Files.isRegularFile(path))
                    toClazzpath.addClazzpathUnit(path, id)
                }
            }, "path-jar"] as Object[]
        def obj5 = [
            new AddClazzpathUnit() {
                @Override
                ClazzpathUnit to(Clazzpath toClazzpath, String filename, String id) throws IOException {
                    Path path = Paths.get("src/test/resources/" + filename)
                    assertTrue(Files.exists(path))
                    assertTrue(Files.isDirectory(path))
                    toClazzpath.addClazzpathUnit(path, id)
                }
            }, "path-directory"] as Object[]
        [obj1, obj2, obj3, obj4, obj5]
    }

    ClazzpathTest(AddClazzpathUnit addClazzpathUnit, String kind) {
        super()
        this.addClazzpathUnit = addClazzpathUnit
    }

    @Test
    void testShouldAddClasses() throws IOException {
        Clazzpath cp = new Clazzpath()
        addClazzpathUnit.to(cp, "jar1")
        addClazzpathUnit.to(cp, "jar2")
        ClazzpathUnit[] units = cp.getUnits()
        assertEquals(2, units.length)
        assertEquals(129, cp.getClazzes().size())
    }

    @Test
    void testShouldRemoveClasspathUnit() throws IOException {
        Clazzpath cp = new Clazzpath()
        ClazzpathUnit unit1 = addClazzpathUnit.to(cp, "jar1")
        assertEquals(59, cp.getClazzes().size())
        ClazzpathUnit unit2 = addClazzpathUnit.to(cp, "jar2")
        assertEquals(129, cp.getClazzes().size())
        cp.removeClazzpathUnit(unit1)
        assertEquals(70, cp.getClazzes().size())
        cp.removeClazzpathUnit(unit2)
        assertEquals(0, cp.getClazzes().size())
    }

    @Test
    void testShouldRevealMissingClasses() throws IOException {
        Clazzpath cp = new Clazzpath()
        addClazzpathUnit.to(cp, "jar1-missing")
        Set<Clazz> missing = cp.getMissingClazzes()
        Set<String> actual = new HashSet<String>()
        for (Clazz clazz : missing) {
            String name = clazz.getName()
            // ignore the rt
            if (!name.startsWith("java")) {
                actual.add(name)
            }
        }
        Set<String> expected = new HashSet<String>(Arrays.asList(
            "org.apache.commons.io.output.ProxyOutputStream",
            "org.apache.commons.io.input.ProxyInputStream"
            ))
        assertEquals(expected, actual)
    }

    @Test
    void testShouldShowClasspathUnitsResponsibleForClash() throws IOException {
        Clazzpath cp = new Clazzpath()
        ClazzpathUnit a = addClazzpathUnit.to(cp, "jar1")
        ClazzpathUnit b = addClazzpathUnit.to(cp, "jar1", "foo")
        Set<Clazz> clashed = cp.getClashedClazzes()
        Set<Clazz> all = cp.getClazzes()
        assertEquals(all, clashed)
        for (Clazz clazz : clashed) {
            assertTrue(clazz.getClazzpathUnits().contains(a))
            assertTrue(clazz.getClazzpathUnits().contains(b))
        }
    }

    @Test
    void testShouldFindUnusedClasses() throws IOException {
        Clazzpath cp = new Clazzpath()
        ClazzpathUnit artifact = addClazzpathUnit.to(cp, "jar3using1")
        addClazzpathUnit.to(cp, "jar1")
        Set<Clazz> removed = cp.getClazzes()
        removed.removeAll(artifact.getClazzes())
        removed.removeAll(artifact.getTransitiveDependencies())
        assertEquals("" + removed, 56, removed.size())
        Set<Clazz> kept = cp.getClazzes()
        kept.removeAll(removed)
        assertEquals("" + kept, 4, kept.size())
    }

    @Test
    void testWithModuleInfo() throws Exception {
        Clazzpath cp = new Clazzpath()
        ClazzpathUnit artifact = addClazzpathUnit.to(cp, "asm-6.0_BETA")
        assertNull(artifact.getClazz("module-info"))
    }
}

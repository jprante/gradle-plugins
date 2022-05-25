package org.xbib.gradle.plugin.shadow.relocation

import org.junit.BeforeClass
import org.junit.Test
import org.xbib.gradle.plugin.shadow.ShadowStats

import static org.junit.Assert.assertEquals

/**
 * Test for {@link SimpleRelocator}.
 * Modified from org.apache.maven.plugins.shade.relocation.SimpleRelocatorTest
 */
class SimpleRelocatorTest {

    static ShadowStats stats

    @BeforeClass
    static void setUp() {
      stats = new ShadowStats()
    }

    @Test
    void testCanRelocatePath() {
        SimpleRelocator relocator

        relocator = new SimpleRelocator("org.foo", null, null, null)
        assertEquals(true, relocator.canRelocatePath(pathContext("org/foo/Class")))
        assertEquals(true, relocator.canRelocatePath(pathContext("org/foo/Class.class")))
        assertEquals(true, relocator.canRelocatePath(pathContext("org/foo/bar/Class")))
        assertEquals(true, relocator.canRelocatePath(pathContext("org/foo/bar/Class.class")))
        assertEquals(false, relocator.canRelocatePath(pathContext("com/foo/bar/Class")))
        assertEquals(false, relocator.canRelocatePath(pathContext("com/foo/bar/Class.class")))
        assertEquals(false, relocator.canRelocatePath(pathContext("org/Foo/Class")))
        assertEquals(false, relocator.canRelocatePath(pathContext("org/Foo/Class.class")))

        relocator = new SimpleRelocator("org.foo", null, null, Arrays.asList(
                [ "org.foo.Excluded", "org.foo.public.*", "org.foo.Public*Stuff" ] as String[]))
        assertEquals(true, relocator.canRelocatePath(pathContext("org/foo/Class")))
        assertEquals(true, relocator.canRelocatePath(pathContext("org/foo/Class.class")))
        assertEquals(true, relocator.canRelocatePath(pathContext("org/foo/excluded")))
        assertEquals(false, relocator.canRelocatePath(pathContext("org/foo/Excluded")))
        assertEquals(false, relocator.canRelocatePath(pathContext("org/foo/Excluded.class")))
        assertEquals(false, relocator.canRelocatePath(pathContext("org/foo/public")))
        assertEquals(false, relocator.canRelocatePath(pathContext("org/foo/public/Class")))
        assertEquals(false, relocator.canRelocatePath(pathContext("org/foo/public/Class.class")))
        assertEquals(true, relocator.canRelocatePath(pathContext("org/foo/publicRELOC/Class")))
        assertEquals(true, relocator.canRelocatePath(pathContext("org/foo/PrivateStuff")))
        assertEquals(true, relocator.canRelocatePath(pathContext("org/foo/PrivateStuff.class")))
        assertEquals(false, relocator.canRelocatePath(pathContext("org/foo/PublicStuff")))
        assertEquals(false, relocator.canRelocatePath(pathContext("org/foo/PublicStuff.class")))
        assertEquals(false, relocator.canRelocatePath(pathContext("org/foo/PublicUtilStuff")))
        assertEquals(false, relocator.canRelocatePath(pathContext("org/foo/PublicUtilStuff.class")))
    }

    @Test
    void testCanRelocateClass() {
        SimpleRelocator relocator

        relocator = new SimpleRelocator("org.foo", null, null, null)
        assertEquals(true, relocator.canRelocateClass(classContext("org.foo.Class")))
        assertEquals(true, relocator.canRelocateClass(classContext("org.foo.bar.Class")))
        assertEquals(false, relocator.canRelocateClass(classContext("com.foo.bar.Class")))
        assertEquals(false, relocator.canRelocateClass(classContext("org.Foo.Class")))

        relocator = new SimpleRelocator("org.foo", null, null, Arrays.asList(
                [ "org.foo.Excluded", "org.foo.public.*", "org.foo.Public*Stuff" ] as String[]))
        assertEquals(true, relocator.canRelocateClass(classContext("org.foo.Class")))
        assertEquals(true, relocator.canRelocateClass(classContext("org.foo.excluded")))
        assertEquals(false, relocator.canRelocateClass(classContext("org.foo.Excluded")))
        assertEquals(false, relocator.canRelocateClass(classContext("org.foo.public")))
        assertEquals(false, relocator.canRelocateClass(classContext("org.foo.public.Class")))
        assertEquals(true, relocator.canRelocateClass(classContext("org.foo.publicRELOC.Class")))
        assertEquals(true, relocator.canRelocateClass(classContext("org.foo.PrivateStuff")))
        assertEquals(false, relocator.canRelocateClass(classContext("org.foo.PublicStuff")))
        assertEquals(false, relocator.canRelocateClass(classContext("org.foo.PublicUtilStuff")))
    }

    @Test
    void testCanRelocateRawString() {
        SimpleRelocator relocator

        relocator = new SimpleRelocator("org/foo", null, null, null, true)
        assertEquals(true, relocator.canRelocatePath(pathContext("(I)org/foo/bar/Class")))

        relocator = new SimpleRelocator("^META-INF/org.foo.xml\$", null, null, null, true)
        assertEquals(true, relocator.canRelocatePath(pathContext("META-INF/org.foo.xml")))
    }

    //MSHADE-119, make sure that the easy part of this works.
    @Test
    void testCanRelocateAbsClassPath() {
        SimpleRelocator relocator = new SimpleRelocator("org.apache.velocity", "org.apache.momentum", null, null)
        assertEquals("/org/apache/momentum/mass.properties", relocator.relocatePath(pathContext("/org/apache/velocity/mass.properties")))

    }

    @Test
    void testRelocatePath() {
        SimpleRelocator relocator

        relocator = new SimpleRelocator("org.foo", null, null, null)
        assertEquals("hidden/org/foo/bar/Class.class", relocator.relocatePath(pathContext("org/foo/bar/Class.class")))

        relocator = new SimpleRelocator("org.foo", "private.stuff", null, null)
        assertEquals("private/stuff/bar/Class.class", relocator.relocatePath(pathContext("org/foo/bar/Class.class")))
    }

    @Test
    void testRelocateClass() {
        SimpleRelocator relocator

        relocator = new SimpleRelocator("org.foo", null, null, null)
        assertEquals("hidden.org.foo.bar.Class", relocator.relocateClass(classContext("org.foo.bar.Class")))

        relocator = new SimpleRelocator("org.foo", "private.stuff", null, null)
        assertEquals("private.stuff.bar.Class", relocator.relocateClass(classContext("org.foo.bar.Class")))
    }

    @Test
    void testRelocateRawString() {
        SimpleRelocator relocator

        relocator = new SimpleRelocator("Lorg/foo", "Lhidden/org/foo", null, null, true)
        assertEquals("(I)Lhidden/org/foo/bar/Class", relocator.relocatePath(pathContext("(I)Lorg/foo/bar/Class")))

        relocator = new SimpleRelocator("^META-INF/org.foo.xml\$", "META-INF/hidden.org.foo.xml", null, null, true)
        assertEquals("META-INF/hidden.org.foo.xml", relocator.relocatePath(pathContext("META-INF/org.foo.xml")))
    }
    
    protected RelocatePathContext pathContext(String path) {
        return RelocatePathContext.builder().path(path).stats(stats).build()
    }

    protected RelocateClassContext classContext(String className) {
        return RelocateClassContext.builder().className(className).stats(stats).build()
    }
}

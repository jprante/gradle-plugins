package org.xbib.gradle.plugin.shadow.relocation

import org.junit.Test

import static org.junit.Assert.fail

/**
 * Modified from org.apache.maven.plugins.shade.relocation.SimpleRelocatorParameterTest
 */
class SimpleRelocatorParameterTest {

    @Test
    void testThatNullPatternInConstructorShouldNotThrowNullPointerException() {
        constructThenFailOnNullPointerException(null, "")
    }

    @Test
    void testThatNullShadedPatternInConstructorShouldNotThrowNullPointerException() {
        constructThenFailOnNullPointerException("", null)
    }

    private static void constructThenFailOnNullPointerException(String pattern, String shadedPattern) {
        try {
            new SimpleRelocator(pattern, shadedPattern, Collections.<String> emptyList(), Collections.<String> emptyList())
        }
        catch (NullPointerException e) {
            fail("Constructor should not throw null pointer exceptions")
        }
    }
}

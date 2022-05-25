package org.xbib.gradle.plugin.shadow.internal

import java.util.stream.Collectors

import static org.junit.Assert.assertEquals

import org.junit.Test

import static org.xbib.gradle.plugin.shadow.internal.DependencyUtils.getDependenciesOfClass

class DependencyUtilsTest {

    @Test
    void testShouldFindDependenciesOfClassObject() throws Exception {
        Collection<String> dependencies = getDependenciesOfClass(Object).sort()
        Collection<String> expectedDependencies = Arrays.asList(
                "java.lang.Class",
                "java.lang.CloneNotSupportedException",
                "java.lang.Deprecated",
                "java.lang.IllegalArgumentException",
                "java.lang.Integer",
                "java.lang.InterruptedException",
                "java.lang.Object",
                "java.lang.String",
                "java.lang.StringBuilder",
                "java.lang.Throwable")
                .sort()
        // we do not care about jdk.internal classes, they vary between JDK versions and even JVMs
        assertEquals(expectedDependencies, dependencies.stream()
                .filter(d -> !d.startsWith("jdk.internal")).collect(Collectors.toList()) )
    }
}

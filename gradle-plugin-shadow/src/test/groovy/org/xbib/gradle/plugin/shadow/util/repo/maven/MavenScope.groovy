package org.xbib.gradle.plugin.shadow.util.repo.maven

class MavenScope {
    Map<String, MavenDependency> dependencies = [:]

    void assertDependsOn(String[] expected) {
        assert dependencies.size() == expected.length
        expected.each {
            String key = substringBefore(it, "@")
            def dependency = expectDependency(key)

            String type = null
            if (it != key) {
                type = StringUtils.substringAfter(it, "@")
            }
            assert dependency.hasType(type)
        }
    }

    MavenDependency expectDependency(String key) {
        final dependency = dependencies[key]
        if (dependency == null) {
            throw new AssertionError("Could not find expected dependency $key. Actual: ${dependencies.values()}")
        }
        return dependency
    }

    static String substringBeforeLast(final String str, final String separator) {
        if (isEmpty(str) || isEmpty(separator)) {
            return str
        }
        final int pos = str.lastIndexOf(separator);
        if (pos == INDEX_NOT_FOUND) {
            return str
        }
        return str.substring(0, pos)
    }

    static boolean isEmpty(final CharSequence cs) {
        return cs == null || cs.length() == 0
    }

    static final int INDEX_NOT_FOUND = -1
}

package org.xbib.gradle.plugin.asciidoctor

interface AsciidoctorProxy {

    String convertFile(File filename, Map<String, Object> options)

    void requireLibrary(String... requiredLibraries)
}

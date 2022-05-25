package org.xbib.gradle.plugin.asciidoctor

class AsciidoctorProxyImpl implements AsciidoctorProxy {

    def delegate

    @Override
    String convertFile(File filename, Map<String, Object> options) {
        delegate.convertFile(filename, options)
    }

    @Override
    void requireLibrary(String... requiredLibraries) {
        delegate.requireLibrary(requiredLibraries)
    }
}

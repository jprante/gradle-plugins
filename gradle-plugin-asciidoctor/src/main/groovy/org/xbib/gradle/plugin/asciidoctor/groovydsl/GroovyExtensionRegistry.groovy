package org.xbib.gradle.plugin.asciidoctor.groovydsl

import org.asciidoctor.Asciidoctor
import org.asciidoctor.jruby.extension.spi.ExtensionRegistry

/**
 * The service implementation for org.asciidoctor.extension.spi.ExtensionRegistry.
 * It simply delegates the register() call to {@link AsciidoctorExtensions} 
 * that owns all configured extensions and registers it on the Asciidoctor instance.
 */
class GroovyExtensionRegistry implements ExtensionRegistry {

    @Override
    void register(Asciidoctor asciidoctor) {
        AsciidoctorExtensions.registerTo(asciidoctor)
    }
}

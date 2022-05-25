package org.xbib.gradle.plugin.asciidoctor.groovydsl.extensions

import org.asciidoctor.ast.StructuralNode
import org.asciidoctor.extension.BlockProcessor
import org.asciidoctor.extension.Reader
import org.xbib.gradle.plugin.asciidoctor.groovydsl.AsciidoctorExtensionHandler

class DelegatingBlockProcessor extends BlockProcessor {

    private final Closure cl

    DelegatingBlockProcessor(Map<String, Object> attributes, @DelegatesTo(BlockProcessor) Closure cl) {
        super(attributes[AsciidoctorExtensionHandler.OPTION_NAME] as String, attributes)
        this.cl = cl
        cl.delegate = this
    }

    @Override
    Object process(StructuralNode parent, Reader reader, Map<String, Object> attributes) {
        cl.call(parent, reader, attributes)
    }
}
